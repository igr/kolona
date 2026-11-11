package dev.oblac.kolona.game

import kotlin.math.*
import kotlin.random.Random

/** One game's city: the street the march takes, the ground everyone else walks on, and everything drawn on top. */
class CityMap(val city: City, val scale: Float, val street: Street, val ground: Ground, val stops: List<Square>, val buildings: List<Building>,
              val landmarks: List<Landmark>, val trees: List<Tree>, val labels: List<MapLabel>, val heroStart: Vec2, val props: List<Prop>,
              val supporters: List<Supporters> = listOf()) {
    val width: Float get() = ground.width
    val height: Float get() = ground.height
    val route: MarchRoute get() = street.route
    fun blocked(p: Vec2, padding: Float = 12f): Boolean = ground.blocked(p, padding)
    fun nearestWalkable(point: Vec2): Vec2 = ground.nearestWalkable(point)
    fun path(from: Vec2, requested: Vec2): MutableList<Vec2> = ground.path(from, requested)
}

fun mapScale(seed: Int, maxMapScale: Float): Float = 1f + Random(seed xor 0x5ca1e).nextFloat() * (max(1f, maxMapScale) - 1f)

private const val ROUTE_WIDTH = 120f
// One building to a lot, at least BUILDING_GAP from the next, so people (about 25 units wide) walk between any two.
private const val LOT_WIDTH = 130f
private const val LOT_HEIGHT = 124f
private const val BUILDING_GAP = 48f
private const val EMPTY_LOTS = .2f
private const val TREES = 40 // on a 1x map; a larger map gets more for its area
private const val LANDMARK_GAP = 20f
private const val LANDMARK_REACH = 340f // the farthest a landmark's center may be from its route point
private val LANDMARK_SIZES = listOf(1f, .9f, .8f, .7f, .6f, .5f, .4f, .3f, .2f)
private const val PROP_GAP = 11f // from the street edge to a prop's front
private const val PROP_REACH = 220f // the farthest a prop may be from its anchor
private const val PROP_APART = 300f // the closest two props of one kind may be
private const val SUPPORTERS_EVERY = 70f // about one row of supporters for this much route
private const val SUPPORTER_GAP = 5f // from the street edge to the front of a row
// 520 covers almost all of LANDMARK_REACH from any start; a thin far arc is never tried.
private val NEAREST_FIRST = (-52..52).flatMap { i -> (-52..52).map { j -> Vec2(i * 10f, j * 10f) } }
    .filter { it.length() <= 520f }.sortedBy { it.length() }

fun generateMap(city: City, seed: Int, scale: Float): CityMap = generateMap(city, city.template, seed, scale)

internal fun generateMap(city: City, template: CityTemplate, seed: Int, scale: Float): CityMap {
    require(template.stops.size >= 2) { "$city: the number of stops (${template.stops.size}) must be at least 2" }
    require(template.legs.size == template.stops.size - 1) {
        "$city: the number of legs (${template.legs.size}) must be one less than the number of stops (${template.stops.size})"
    }
    val random = Random(seed)
    val width = 1400f * scale
    val height = 1120f * scale
    fun at(u: Float, v: Float) = Vec2(110f + u * (width - 220f), 110f + v * (height - 220f))

    val stops = template.stops.map { Square(it.name, at(it.u, it.v), it.radius, it.kind) }
    val route = MarchRoute(random.wobble(buildList {
        stops.forEachIndexed { i, stop ->
            add(stop.center)
            template.legs.getOrNull(i)?.via?.forEach { (u, v) -> add(at(u, v)) }
        }
    }, 25f..55f))
    val street = Street(width, height, route, ROUTE_WIDTH, stops)

    val squareNames = template.stops.zip(stops) { stop, square ->
        // The game HUD covers the top of the view, so a name near the top edge goes below its square,
        // moved sideways toward the middle of the map to keep clear of a route that leaves downward.
        val above = square.center.y - square.radius - 22f
        val side = if (square.center.x < width / 2) 1f else -1f
        val position = if (above >= 120f) Vec2(square.center.x, above)
            else Vec2(square.center.x + side * square.radius, square.center.y + square.radius + 22f)
        MapLabel(stop.name, position, 0f).inside(width, height)
    }

    // Landmarks take no random numbers, so the street and the buildings stay where they were.
    val stopDistances = stops.map { stop -> route.samples.minBy { it.position.distanceTo(stop.center) }.distance }
    val lots = mutableListOf<Lot>()
    for (spec in template.landmarks) {
        require(spec.stop in template.stops.indices) {
            "$city: landmark ${spec.name} is at stop ${spec.stop}, but the stops are 0..${template.stops.lastIndex}"
        }
        val along = stopDistances[spec.stop] + spec.along
        val anchor = route.at(along)
        val normal = route.at(along, 1f) - anchor
        // Squares and other landmarks crowd a stop on a small map, so a landmark with no room shrinks a step at a time.
        val lot = LANDMARK_SIZES.asSequence().mapNotNull { size ->
            val w = spec.width * size
            val h = spec.height * size
            val from = route.at(along, spec.side * (ROUTE_WIDTH / 2 + LANDMARK_GAP + (abs(normal.x) * w + abs(normal.y) * h) / 2))
            NEAREST_FIRST.asSequence().map { from + it }
                .filter {
                    it.distanceTo(anchor) <= LANDMARK_REACH &&
                        it.x - w / 2 >= 10f && it.y - h / 2 >= 10f && it.x + w / 2 <= width - 10f && it.y + h / 2 <= height - 10f
                }
                .map { Lot(Landmark(spec.name, spec.kind, it - Vec2(w / 2, h / 2), w, h), height) }
                .firstOrNull { lot ->
                    squareNames.none { lot.overlaps(it.left, it.position.y - 9f, it.right, it.position.y + 9f) } &&
                        lots.none { it.overlaps(lot.left, lot.top, lot.right, lot.bottom) } && lot.landmark.probes().all { street.clearance(it) <= -8f }
                }
        }.firstOrNull()
        lots += requireNotNull(lot) {
            "$city: landmark ${spec.name} (${spec.width.toInt()} × ${spec.height.toInt()}) has no room off the street within ${LANDMARK_REACH.toInt()} units " +
                "of its place, even at ${(LANDMARK_SIZES.last() * 100).toInt()}% of its size"
        }
    }
    fun clearOfLandmarks(left: Float, top: Float, right: Float, bottom: Float) = lots.none { it.overlaps(left, top, right, bottom) }

    // Some lots stay empty, so the ground opens up here and there.
    val buildings = mutableListOf<Building>()
    for (row in 0 until ceil(height / LOT_HEIGHT).toInt()) for (col in 0 until ceil(width / LOT_WIDTH).toInt()) {
        val w = 52f + random.nextFloat() * 30f
        val h = 40f + random.nextFloat() * 24f
        val x = col * LOT_WIDTH + BUILDING_GAP / 2 + random.nextFloat() * (LOT_WIDTH - BUILDING_GAP - w)
        val y = row * LOT_HEIGHT + BUILDING_GAP / 2 + random.nextFloat() * (LOT_HEIGHT - BUILDING_GAP - h - Building.FRONT)
        val b = Building(Vec2(x, y), w, h, random.nextInt(5))
        if (random.nextFloat() < EMPTY_LOTS) continue
        if (b.probes().all { street.clearance(it) <= -8f } &&
            clearOfLandmarks(x - BUILDING_GAP, y - BUILDING_GAP, x + w + BUILDING_GAP, y + h + Building.FRONT + BUILDING_GAP)) buildings += b
    }
    val ground = Ground(width, height, buildings, lots.map { it.landmark }, route.samples.first().position)

    fun tree(p: Vec2) = Tree(p, 11f + random.nextFloat() * 9f)
    val trees = buildList {
        stops.filter { it.kind == SquareKind.Roundabout }.forEach { square ->
            val rim = square.radius - 14f
            val count = (2 * PI.toFloat() * rim / 46f).toInt()
            repeat(count) {
                val angle = it * 2 * PI.toFloat() / count
                val p = square.center + Vec2(cos(angle), sin(angle)) * rim
                if (route.distanceFrom(p) >= ROUTE_WIDTH / 2) add(tree(p))
            }
        }
        val goal = size + (TREES * scale * scale).roundToInt()
        var attempts = 0
        while (size < goal && attempts++ < goal * 50) {
            val p = Vec2(random.nextFloat() * width, random.nextFloat() * height)
            val t = tree(p)
            if (street.clearance(p) <= -30f && buildings.none { it.touches(t) } &&
                clearOfLandmarks(p.x - t.radius, p.y - t.radius, p.x + t.radius, p.y + t.radius)) add(t)
        }
    }

    val heroDistance = min(360f, route.length * .25f)
    val labels = buildList {
        addAll(squareNames)
        template.legs.forEachIndexed { i, leg ->
            if (leg.names.isEmpty()) return@forEachIndexed
            // Evenly spaced, but never under the hero or the waiting crowd at the start.
            val first = max(stopDistances[i] + (stopDistances[i + 1] - stopDistances[i]) / (leg.names.size + 1), heroDistance + 110f)
            val step = (stopDistances[i + 1] - first) / leg.names.size
            require(step > 0f) {
                "$city: leg ${leg.names.joinToString(" / ")} ends at ${stopDistances[i + 1].toInt()} on the route, " +
                    "before its labels can start clear of the hero at ${first.toInt()}"
            }
            leg.names.forEachIndexed { k, name -> add(label(name, route, first + step * k)) }
        }
        lots.forEach { add(MapLabel(it.landmark.name, it.label, 0f)) }
    }.map { it.inside(width, height) }

    val heroStart = ground.nearestWalkable(route.at(heroDistance))
    // Props and then supporters come last, each with their own random numbers, so everything above stays where it was.
    val obstacles = obstacles(route, lots, buildings, trees, labels)
    val props = props(template.props, Random(seed * 31 + 7), street, width, height, lots, obstacles)
    val supporters = supporters(Random(seed * 37 + 11), street, width, height, obstacles, props)
    return CityMap(city, scale, street, ground, stops, buildings, lots.map { it.landmark }, trees, labels, heroStart, props, supporters)
}

/** What props and supporters keep clear of: the buildings with their fronts, the landmarks with their names, the trees, the labels, and the A and B markers. */
private fun obstacles(route: MarchRoute, lots: List<Lot>, buildings: List<Building>, trees: List<Tree>, labels: List<MapLabel>) =
    buildings.map { Box.of(it.position.x - 2f, it.position.y - 2f, it.position.x + it.width + 2f, it.position.y + it.height + Building.FRONT + 2f) } +
        lots.map { Box.of(it.left, it.top, it.right, it.bottom) } +
        trees.map { Box.of(it.position.x - it.radius - 2f, it.position.y - it.radius - 2f, it.position.x + it.radius + 7f, it.position.y + it.radius + 11f) } +
        labels.map { Box(it.position, Vec2(-sin(it.angle), cos(it.angle)), nameReach(it.text) + 4f, 10f) } +
        listOf(route.samples.first(), route.samples.last()).map { Box.of(it.position.x - 34f, it.position.y - 34f, it.position.x + 34f, it.position.y + 34f) }

/**
 * A row of 2 to 5 supporters about every SUPPORTERS_EVERY units of route, at the edge of the street on one side or the other.
 * A row that does not fit there tries the other side, then a little along the street, then with fewer people.
 */
private fun supporters(random: Random, street: Street, width: Float, height: Float, obstacles: List<Box>, props: List<Prop>): List<Supporters> {
    val route = street.route
    val taken = props.flatMap { it.kind.shape(it.position, Vec2(cos(it.angle), sin(it.angle))) }.toMutableList()
    val rows = mutableListOf<Supporters>()
    var around = listOf<Box>()
    fun place(along: Float, side: Float, count: Int, banner: Boolean): Boolean {
        val base = route.at(along)
        val out = (route.at(along, side) - base).normalized()
        // A row faces the street. A banner row faces the screen, so the banner reads the same whichever way the street runs.
        val facing = if (banner) Vec2(0f, 1f) else out * -1f
        val across = Vec2(-facing.y, facing.x)
        val half = count * Supporters.SPACING / 2
        // Out from the middle of the street until the whole row stands off it: in a square, that is farther.
        for (step in 0..14) {
            val center = base + out * (street.routeWidth / 2 + SUPPORTER_GAP + Supporters.DEPTH / 2 + step * 6f)
            val shape = listOf(0f, Supporters.HEIGHT / 2, Supporters.HEIGHT).map { Box(center - Vec2(0f, it), facing, half, Supporters.DEPTH / 2) }
            // Five points across the row, not three: a curving street edge can reach in between.
            val feet = (0..4).flatMap { i -> (0..2).map { j -> center + facing * (Supporters.DEPTH * (j / 2f - .5f)) + across * (half * (i / 2f - 1f)) } }
            if (feet.any { street.clearance(it) > -SUPPORTER_GAP }) continue
            // Where a curve or a square's rim keeps one end of the row on the street, the first spot off it can be too far back.
            if (feet.maxOf { street.clearance(it) } < -18f) return false
            if (shape.flatMap { it.probes }.any { it.x !in 4f..width - 4f || it.y !in 4f..height - 4f }) return false
            if (shape.any { box -> around.any { box.overlaps(it) } || taken.any { box.overlaps(it, 4f) } }) return false
            rows += Supporters(center, atan2(facing.y, facing.x), count, banner)
            taken += shape
            return true
        }
        return false
    }
    var along = 40f + random.nextFloat() * SUPPORTERS_EVERY
    while (along < route.length - 40f) {
        val count = 2 + random.nextInt(4)
        val banner = count >= 3 && random.nextFloat() < .45f
        val side = if (random.nextBoolean()) 1f else -1f
        val here = route.at(along)
        around = obstacles.filter { it.center.distanceTo(here) < 200f + it.radius }
        (count downTo 2).any { n ->
            listOf(0f, 18f, -18f, 36f, -36f).any { shift ->
                place(along + shift, side, n, banner && n >= 3) || place(along + shift, -side, n, banner && n >= 3)
            }
        }
        along += SUPPORTERS_EVERY * (.6f + random.nextFloat() * .8f)
    }
    return rows
}

/** About one prop per 140 units of route, each kind once before any repeats, as near as it fits to a landmark, a square, or the route. */
private fun props(kinds: List<PropKind>, random: Random, street: Street, width: Float, height: Float, lots: List<Lot>, obstacles: List<Box>): List<Prop> {
    val route = street.route
    val squares = street.squares
    val props = mutableListOf<Prop>()
    val taken = mutableListOf<Box>()
    fun place(kind: PropKind, center: Vec2, facing: Vec2, around: List<Box>): Boolean {
        val shape = kind.shape(center, facing)
        val probes = shape.flatMap { it.probes }
        if (probes.any { it.x !in 4f..width - 4f || it.y !in 4f..height - 4f }) return false
        val ground = if (kind == PropKind.Pigeons) probes.all { street.clearance(it) >= 2f }
            else probes.all { street.clearance(it) <= -9f } && street.clearance(center + facing * (kind.depth / 2)) >= -24f
        if (!ground || props.any { it.kind == kind && it.position.distanceTo(center) < PROP_APART }) return false
        if (shape.any { box -> around.any { box.overlaps(it) } || taken.any { box.overlaps(it, 6f) } }) return false
        props += Prop(kind, center, atan2(facing.y, facing.x))
        taken += shape
        return true
    }
    // A prop stands at most about PROP_REACH + 150 from its anchor, so a big landmark can reach it from farther away.
    fun near(anchor: Vec2) = obstacles.filter { it.center.distanceTo(anchor) < PROP_REACH + 150f + it.radius }

    val edges = (0 until (route.length / 12f).toInt()).flatMap { i ->
        val base = route.at(6f + i * 12f)
        listOf(-1f, 1f).map { side -> Triple(base, (route.at(6f + i * 12f, side) - base).normalized(), street.routeWidth / 2) }
    } + squares.flatMap { square ->
        val count = (2 * PI.toFloat() * square.radius / 12f).toInt()
        (0 until count).map { i -> Vec2(cos(i * 2 * PI.toFloat() / count), sin(i * 2 * PI.toFloat() / count)).let { Triple(square.center + it * square.radius, it, 0f) } }
    }
    val shops = kinds.filter { it != PropKind.Pigeons }
    val flocks = if (PropKind.Pigeons in kinds) squares.drop(1) else listOf()
    val count = max(0, (route.length / 140f).roundToInt() - flocks.size)
    val anchors = lots.map { it.landmark.position + Vec2(it.landmark.width / 2, it.landmark.height / 2) } + squares.map { it.center }
    // A later round fills in where an earlier one found no room.
    val spread = max(1, count - anchors.size)
    val along = (0 until 3).flatMap { round -> List(spread) { route.at((it + (round + .3f + random.nextFloat() * .4f) / 3f) * route.length / spread) } }
    val queue = ArrayDeque<PropKind>()
    if (shops.isNotEmpty()) for (anchor in anchors + along) {
        if (props.size >= count) break
        if (queue.isEmpty()) queue += shops.shuffled(random)
        val around = near(anchor)
        val spots = edges.filter { it.first.distanceTo(anchor) < PROP_REACH }.map { it to it.first.distanceTo(anchor) + random.nextFloat() * 60f }
            .sortedBy { it.second }.map { it.first }
        val kind = queue.firstOrNull { kind ->
            spots.any { (base, out, half) -> place(kind, base + out * (half + PROP_GAP + kind.depth / 2), out * -1f, around) }
        }
        if (kind != null) queue.remove(kind)
    }
    // Pigeons sit beside the march's path, so it comes close enough to scare them.
    for (square in flocks) {
        val facing = Vec2(1f, 0f)
        val spots = listOf(.5f, .62f, .74f).flatMap { f -> (0 until 36).map { square.center + Vec2(cos(it * PI.toFloat() / 18), sin(it * PI.toFloat() / 18)) * (square.radius * f) } }
            .filter { route.distanceFrom(it) in 40f..62f }.shuffled(random)
        val around = near(square.center)
        spots.firstOrNull { place(PropKind.Pigeons, it, facing, around) }
    }
    return props
}

private fun PropKind.shape(center: Vec2, facing: Vec2) =
    (if (height > 0f) listOf(0f, height / 2, height) else listOf(0f)).map { Box(center - Vec2(0f, it), facing, width / 2, depth / 2) }

/** A rectangle around [center], [halfWidth] across [facing] and [halfDepth] along it. */
private class Box(val center: Vec2, val facing: Vec2, val halfWidth: Float, val halfDepth: Float) {
    private val side = Vec2(-facing.y, facing.x)
    val radius = sqrt(halfWidth * halfWidth + halfDepth * halfDepth)
    // The corners, the middles of the edges, and the center: at most 29 units apart on any prop, so no street fits between them.
    val probes = listOf(-1f, 0f, 1f).flatMap { i -> listOf(-1f, 0f, 1f).map { j -> center + side * (i * halfWidth) + facing * (j * halfDepth) } }
    private fun reach(axis: Vec2) = abs(side dot axis) * halfWidth + abs(facing dot axis) * halfDepth
    // Two boxes are apart if one of their four edge directions separates them by at least [gap].
    fun overlaps(other: Box, gap: Float = 0f) = center.distanceTo(other.center) < radius + other.radius + gap &&
        listOf(facing, side, other.facing, other.side).all { abs((other.center - center) dot it) < reach(it) + other.reach(it) + gap }
    companion object {
        fun of(left: Float, top: Float, right: Float, bottom: Float) =
            Box(Vec2((left + right) / 2, (top + bottom) / 2), Vec2(0f, 1f), (right - left) / 2, (bottom - top) / 2)
    }
}

/** A landmark with its name below it (above it at the bottom of the map) and a strip of ground around both. */
private class Lot(val landmark: Landmark, mapHeight: Float) {
    val label = landmark.position.let { p ->
        val below = p.y + landmark.height + 13f
        Vec2(p.x + landmark.width / 2, if (below <= mapHeight - 40f) below else p.y - 13f)
    }
    private val reach = max(landmark.width / 2, nameReach(landmark.name))
    val left = label.x - reach - 8f
    val right = label.x + reach + 8f
    val top = min(landmark.position.y, label.y - 10f) - 8f
    val bottom = max(landmark.position.y + landmark.height, label.y + 10f) + 8f
    fun overlaps(left: Float, top: Float, right: Float, bottom: Float) =
        left < this.right && this.left < right && top < this.bottom && this.top < bottom
}

private fun MapLabel.inside(width: Float, height: Float) =
    copy(position = Vec2(position.x.coerceIn(40f, width - 40f), position.y.coerceIn(40f, height - 40f)))
// Half the width of a name in capitals at size 10, at about 6.6 units a letter.
private fun nameReach(text: String) = text.length * 3.3f + 4f
private val MapLabel.left get() = position.x - nameReach(text)
private val MapLabel.right get() = position.x + nameReach(text)



private infix fun Vec2.dot(other: Vec2) = x * other.x + y * other.y

/** Splits every segment into two or more pieces of at most 250 units and shifts the new points sideways by ±[amount]. */
private fun Random.wobble(points: List<Vec2>, amount: ClosedFloatingPointRange<Float>): List<Vec2> = buildList {
    points.zipWithNext { a, b ->
        add(a)
        val pieces = max(2, ceil(a.distanceTo(b) / 250f).toInt())
        val side = (b - a).normalized().let { Vec2(-it.y, it.x) }
        for (i in 1 until pieces) {
            val shift = amount.start + nextFloat() * (amount.endInclusive - amount.start)
            add(a + (b - a) * (i.toFloat() / pieces) + side * (if (nextBoolean()) shift else -shift))
        }
    }
    add(points.last())
}

private fun label(text: String, line: MarchRoute, distance: Float): MapLabel {
    val tangent = line.at(distance + 2f) - line.at(distance - 2f)
    var angle = atan2(tangent.y, tangent.x)
    if (angle > PI / 2) angle -= PI.toFloat() else if (angle < -PI / 2) angle += PI.toFloat()
    return MapLabel(text, line.at(distance), angle)
}

private fun Building.probes() =
    listOf(0f, .5f, 1f).flatMap { fx -> listOf(0f, .5f, 1f).map { fy -> position + Vec2(width * fx, (height + Building.FRONT) * fy) } }
// At most 20 units apart, so no street fits between them.
private fun Landmark.probes(): List<Vec2> {
    val columns = 2 * ceil(width / 40f).toInt()
    val rows = 2 * ceil(height / 40f).toInt()
    return (0..columns).flatMap { i -> (0..rows).map { j -> position + Vec2(width * i / columns, height * j / rows) } }
}
private fun Building.touches(tree: Tree): Boolean {
    val dx = tree.position.x - tree.position.x.coerceIn(position.x, position.x + width)
    val dy = tree.position.y - tree.position.y.coerceIn(position.y, position.y + height + Building.FRONT)
    return dx * dx + dy * dy < tree.radius * tree.radius
}
