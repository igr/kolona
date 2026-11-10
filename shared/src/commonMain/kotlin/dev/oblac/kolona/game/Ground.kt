package dev.oblac.kolona.game

import kotlin.math.*

enum class SquareKind { Roundabout, Round, Open }
data class Square(val name: String?, val center: Vec2, val radius: Float, val kind: SquareKind)
data class MapLabel(val text: String, val position: Vec2, val angle: Float) // radians, already in -PI/2..PI/2

/** The map's one street: the march's route, widened into a square at each stop. Only the march keeps to it. */
class Street(width: Float, height: Float, val route: MarchRoute, val routeWidth: Float, val squares: List<Square>) {
    private val columns = ceil(width / RASTER).toInt()
    private val rows = ceil(height / RASTER).toInt()
    private val raster = FloatArray(columns * rows) { -30f }

    init {
        route.samples.zipWithNext { a, b -> stamp(a.position, b.position, routeWidth / 2) }
        squares.forEach { stamp(it.center, it.center, it.radius) }
    }

    /** Distance to the street's edge: > 0 on the street, < 0 off it, clamped at -30. */
    fun clearance(p: Vec2): Float =
        raster[(p.y / RASTER).toInt().coerceIn(0, rows - 1) * columns + (p.x / RASTER).toInt().coerceIn(0, columns - 1)]

    // Raise every raster cell near segment ab to its clearance in the corridor; farther cells stay at -30.
    private fun stamp(a: Vec2, b: Vec2, halfWidth: Float) {
        val reach = halfWidth + 30f
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        for (y in max(0, ((min(a.y, b.y) - reach) / RASTER).toInt())..min(rows - 1, ((max(a.y, b.y) + reach) / RASTER).toInt()))
            for (x in max(0, ((min(a.x, b.x) - reach) / RASTER).toInt())..min(columns - 1, ((max(a.x, b.x) + reach) / RASTER).toInt())) {
                val px = (x + .5f) * RASTER - a.x
                val py = (y + .5f) * RASTER - a.y
                val t = if (lengthSquared > 0f) ((px * dx + py * dy) / lengthSquared).coerceIn(0f, 1f) else 0f
                val ex = px - dx * t
                val ey = py - dy * t
                val value = halfWidth - sqrt(ex * ex + ey * ey)
                val i = y * columns + x
                if (value > raster[i]) raster[i] = value
            }
    }

    private companion object { const val RASTER = 5f }
}

/** People walk anywhere on the map, on the street or off it, but never through a building or a landmark, or off the map. */
class Ground(val width: Float, val height: Float, buildings: List<Building>, landmarks: List<Landmark>, start: Vec2) {
    private val columns = ceil(width / CELL).toInt()
    private val rows = ceil(height / CELL).toInt()
    // The raster covers the grid exactly, so every cell center is also a raster cell center.
    private val rasterColumns = columns * (CELL / RASTER).toInt()
    private val rasterRows = rows * (CELL / RASTER).toInt()
    private val raster = FloatArray(rasterColumns * rasterRows) {
        val x = (it % rasterColumns + .5f) * RASTER
        val y = (it / rasterColumns + .5f) * RASTER
        minOf(REACH, x, y, width - x, height - y)
    }

    init {
        buildings.forEach { stamp(it.position.x, it.position.y, it.position.x + it.width, it.position.y + it.height + Building.FRONT) }
        landmarks.forEach { stamp(it.position.x, it.position.y, it.position.x + it.width, it.position.y + it.height) }
    }

    /** Distance to the nearest building, landmark, or map edge: < 0 inside a building, at most 30. */
    fun clearance(p: Vec2): Float = raster[(p.y / RASTER).toInt().coerceIn(0, rasterRows - 1) * rasterColumns +
        (p.x / RASTER).toInt().coerceIn(0, rasterColumns - 1)]
    fun blocked(p: Vec2, padding: Float = 12f): Boolean = clearance(p) < padding

    private val walkable = BooleanArray(columns * rows) { clearance(center(it)) >= 14f }

    // Geometry never changes. Cache legal edges instead of testing clearance in every enemy path search.
    private val neighbors = Array(walkable.size) { current ->
        buildList {
            if (walkable[current]) for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val x = current % columns + dx
                val y = current / columns + dy
                if (x !in 0 until columns || y !in 0 until rows) continue
                val next = y * columns + x
                if (!walkable[next]) continue
                if (dx != 0 && dy != 0 && (!walkable[current + dx] || !walkable[current + dy * columns])) continue
                if (segmentClear(center(current), center(next))) add(next)
            }
        }.toIntArray()
    }

    // A yard walled in by buildings would trap anyone sent there, so only ground the route's start reaches is walkable.
    init {
        require(walkable[cellOf(start)]) { "The route starts at $start, which is not walkable, so nothing on the map is reachable" }
        val reached = flood(cellOf(start))
        for (i in walkable.indices) walkable[i] = walkable[i] && reached[i]
    }
    private val walkableIndices = walkable.indices.filter { walkable[it] }.toIntArray()
    val walkableCells: Int = walkableIndices.size

    fun reachableCells(from: Vec2): Int = flood(nearestCell(from)).count { it }

    // Pruning leaves a single component, so any two walkable cells are connected.
    fun connected(a: Vec2, b: Vec2): Boolean = walkable[cellOf(a)] && walkable[cellOf(b)]

    fun nearestWalkable(point: Vec2): Vec2 = center(nearestCell(point))

    // Enemies search often, so the search state is allocated once and reset per search. The simulation is single-threaded.
    private val previous = IntArray(walkable.size)
    private val cost = FloatArray(walkable.size)
    private val closed = BooleanArray(walkable.size)
    private val open = CellHeap()

    /** A straight line when nothing stands in the way; otherwise A* over the cells, straightened. */
    fun path(from: Vec2, requested: Vec2): MutableList<Vec2> {
        val to = Vec2(requested.x.coerceIn(18f, width - 18f), requested.y.coerceIn(18f, height - 18f))
        if (segmentClear(from, to)) return mutableListOf(to)
        val start = nearestCell(from)
        val end = nearestCell(to)
        previous.fill(-1)
        cost.fill(Float.POSITIVE_INFINITY)
        closed.fill(false)
        open.clear()
        fun heuristic(cell: Int): Float {
            val dx = abs(cell % columns - end % columns)
            val dy = abs(cell / columns - end / columns)
            return max(dx, dy) + (sqrt(2f) - 1f) * min(dx, dy)
        }
        cost[start] = 0f
        open.push(start, heuristic(start))
        while (open.isNotEmpty()) {
            val current = open.pop()
            if (closed[current]) continue
            if (current == end) {
                val cells = mutableListOf<Vec2>()
                var cursor = end
                while (cursor != start) {
                    cells += center(cursor)
                    cursor = previous[cursor]
                }
                cells += center(start)
                cells.reverse()
                if (segmentClear(cells.last(), to)) cells += to
                return straighten(from, cells)
            }
            closed[current] = true
            for (next in neighbors[current]) {
                if (closed[next]) continue
                val dx = next % columns - current % columns
                val dy = next / columns - current / columns
                val candidate = cost[current] + if (dx != 0 && dy != 0) sqrt(2f) else 1f
                if (candidate < cost[next]) {
                    cost[next] = candidate
                    previous[next] = current
                    open.push(next, candidate + heuristic(next))
                }
            }
        }
        return mutableListOf() // An unreachable destination must never teleport the hero.
    }

    // Skips every corner the walker can cut in a clear straight line, so nobody zig-zags along the grid.
    // From inside a building no line is clear, so the walker first steps out to the start cell.
    private fun straighten(from: Vec2, points: List<Vec2>): MutableList<Vec2> {
        val result = mutableListOf<Vec2>()
        var anchor = from
        var i = 0
        while (i < points.size) {
            var j = i
            while (j + 1 < points.size && segmentClear(anchor, points[j + 1])) j++
            result += points[j]
            anchor = points[j]
            i = j + 1
        }
        return result
    }

    // Lower every raster cell near the box to its distance from the box: negative inside, at most REACH.
    private fun stamp(left: Float, top: Float, right: Float, bottom: Float) {
        for (y in max(0, ((top - REACH) / RASTER).toInt())..min(rasterRows - 1, ((bottom + REACH) / RASTER).toInt()))
            for (x in max(0, ((left - REACH) / RASTER).toInt())..min(rasterColumns - 1, ((right + REACH) / RASTER).toInt())) {
                val px = (x + .5f) * RASTER
                val py = (y + .5f) * RASTER
                val dx = max(left - px, px - right)
                val dy = max(top - py, py - bottom)
                val distance = if (dx > 0f || dy > 0f) sqrt(max(dx, 0f).pow(2) + max(dy, 0f).pow(2)) else max(dx, dy)
                val i = y * rasterColumns + x
                if (distance < raster[i]) raster[i] = distance
            }
    }

    private fun center(cell: Int) = Vec2((cell % columns + .5f) * CELL, (cell / columns + .5f) * CELL)
    private fun cellOf(p: Vec2) = (p.y / CELL).toInt().coerceIn(0, rows - 1) * columns + (p.x / CELL).toInt().coerceIn(0, columns - 1)
    private fun nearestCell(p: Vec2): Int {
        val index = cellOf(p)
        if (walkable[index]) return index
        return walkableIndices.minBy { center(it).distanceTo(p) }
    }
    private fun segmentClear(a: Vec2, b: Vec2): Boolean {
        val steps = max(1, ceil(a.distanceTo(b) / 4f).toInt())
        return (0..steps).all { !blocked(a + (b - a) * (it.toFloat() / steps), 13f) }
    }
    private fun flood(start: Int): BooleanArray {
        val seen = BooleanArray(walkable.size)
        val stack = IntArray(walkable.size)
        var top = 0
        seen[start] = true
        stack[top++] = start
        while (top > 0) for (next in neighbors[stack[--top]]) if (!seen[next]) {
            seen[next] = true
            stack[top++] = next
        }
        return seen
    }

    companion object {
        const val CELL = 20f
        const val RASTER = 5f
        private const val REACH = 30f
    }
}

/** Binary min-heap of grid cells keyed by f. A cell may be queued more than once; A* skips the closed copies. */
private class CellHeap {
    private var cells = IntArray(64)
    private var keys = FloatArray(64)
    private var size = 0

    fun isNotEmpty() = size > 0
    fun clear() { size = 0 }

    fun push(cell: Int, key: Float) {
        if (size == cells.size) {
            cells = cells.copyOf(size * 2)
            keys = keys.copyOf(size * 2)
        }
        var i = size++
        while (i > 0 && keys[(i - 1) / 2] > key) {
            cells[i] = cells[(i - 1) / 2]
            keys[i] = keys[(i - 1) / 2]
            i = (i - 1) / 2
        }
        cells[i] = cell
        keys[i] = key
    }

    fun pop(): Int {
        val top = cells[0]
        val cell = cells[--size]
        val key = keys[size]
        var i = 0
        while (true) {
            var child = 2 * i + 1
            if (child >= size) break
            if (child + 1 < size && keys[child + 1] < keys[child]) child++
            if (keys[child] >= key) break
            cells[i] = cells[child]
            keys[i] = keys[child]
            i = child
        }
        cells[i] = cell
        keys[i] = key
        return top
    }
}
