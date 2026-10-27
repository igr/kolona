package dev.oblac.kolona.game

import kotlin.math.*
import kotlin.random.Random

/** One game's city: the walkable street network and everything drawn on top of it. */
class CityMap(val city: City, val scale: Float, val network: StreetNetwork, val stops: List<Square>, val buildings: List<Building>,
              val landmarks: List<Landmark>, val trees: List<Tree>, val labels: List<MapLabel>, val heroStart: Vec2, val props: List<Prop>) {
    val width: Float get() = network.width
    val height: Float get() = network.height
    val route: MarchRoute get() = network.route
    fun blocked(p: Vec2, padding: Float = 12f): Boolean = network.blocked(p, padding)
    fun nearestWalkable(point: Vec2): Vec2 = network.nearestWalkable(point)
    fun path(from: Vec2, requested: Vec2): MutableList<Vec2> = network.path(from, requested)
}

fun mapScale(seed: Int, maxMapScale: Float): Float = 1f + Random(seed xor 0x5ca1e).nextFloat() * (max(1f, maxMapScale) - 1f)

private const val ROUTE_WIDTH = 120f
private const val FILLER_WIDTH = 90f
private const val EDGE = 60f // how far past the map edge a street that leaves the map ends
private val SIMILAR = cos(25f * PI.toFloat() / 180f)
private val SPOKE_GAP = cos(20f * PI.toFloat() / 180f)
private const val LANDMARK_GAP = 20f
private const val LANDMARK_REACH = 340f // the farthest a landmark's center may be from its route point
private val LANDMARK_SIZES = listOf(1f, .9f, .8f, .7f, .6f, .5f, .4f, .3f, .2f)
private const val PROP_GAP = 11f // from the street edge to a prop's front
private const val PROP_REACH = 220f // the farthest a prop may be from its anchor
private const val PROP_APART = 300f // the closest two props of one kind may be
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
    val named = template.streets.map { street ->
        require(street.points.size >= 2) { "$city: street ${street.name} needs at least 2 points, but has ${street.points.size}" }
        val points = street.points.map { (u, v) -> at(u, v) }.toMutableList()
        val last = points.lastIndex
        fun edge(from: Vec2, toward: Vec2) =
            requireNotNull(clip(from, toward - from, width, height)) { "$city: street ${street.name} never enters the map" }.last()
        // An end outside 0..1 runs the street off the map.
        if (street.points.first().outside()) points[0] = edge(points[1], points[0])
        if (street.points.last().outside()) points[last] = edge(points[last - 1], points[last])
        Street(street.name, random.wobble(points, street.wobble), street.width)
    }
    val guides = (listOf(route) + named.map { it.line }).flatMap { it.guides() }
        .filter { it.position.x in 0f..width && it.position.y in 0f..height }
    val fillers = when (template.style) {
        StreetStyle.Grid -> random.grid(stops.first().center, stops.last().center, width, height, guides)
        StreetStyle.Radial -> random.radial(template.stops.zip(stops), width, height, guides)
    }

    var network = StreetNetwork(width, height, route, ROUTE_WIDTH, named + fillers, stops)
    val start = route.samples.first().position
    val kept = fillers.filter { filler ->
        filler.line.samples.any { it.position.x in 0f..width && it.position.y in 0f..height && network.connected(start, it.position) }
    }
    if (kept.size < fillers.size) network = StreetNetwork(width, height, route, ROUTE_WIDTH, named + kept, stops)

    val squareNames = template.stops.zip(stops) { stop, square ->
        // The game HUD covers the top of the view, so a name near the top edge goes below its square,
        // moved sideways toward the middle of the map to keep clear of a route that leaves downward.
        val above = square.center.y - square.radius - 22f
        val side = if (square.center.x < width / 2) 1f else -1f
        val position = if (above >= 120f) Vec2(square.center.x, above)
            else Vec2(square.center.x + side * square.radius, square.center.y + square.radius + 22f)
        MapLabel(stop.name, position, 0f).inside(width, height)
    }

    // Landmarks take no random numbers, so the streets and the other buildings stay where they were.
    val stopDistances = stops.map { stop -> route.samples.minBy { it.position.distanceTo(stop.center) }.distance }
    val lots = mutableListOf<Lot>()
    for (spec in template.landmarks) {
        require(spec.stop in template.stops.indices) {
            "$city: landmark ${spec.name} is at stop ${spec.stop}, but the stops are 0..${template.stops.lastIndex}"
        }
        val along = stopDistances[spec.stop] + spec.along
        val anchor = route.at(along)
        val normal = route.at(along, 1f) - anchor
        // Named streets crowd the stops on a small map, so a landmark with no room shrinks a step at a time.
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
                        lots.none { it.overlaps(lot.left, lot.top, lot.right, lot.bottom) } && lot.landmark.probes().all { network.clearance(it) <= -8f }
                }
        }.firstOrNull()
        lots += requireNotNull(lot) {
            "$city: landmark ${spec.name} (${spec.width.toInt()} × ${spec.height.toInt()}) has no room in a block within ${LANDMARK_REACH.toInt()} units " +
                "of its place, even at ${(LANDMARK_SIZES.last() * 100).toInt()}% of its size"
        }
    }
    fun clearOfLandmarks(left: Float, top: Float, right: Float, bottom: Float) = lots.none { it.overlaps(left, top, right, bottom) }

    val buildings = mutableListOf<Building>()
    for (row in 0 until ceil(height / 64f).toInt()) for (col in 0 until ceil(width / 72f).toInt()) {
        val w = 48f + random.nextFloat() * 38f
        val h = 42f + random.nextFloat() * 32f
        val x = (col + .5f) * 72f + random.nextFloat() * 12f - 6f - w / 2
        val y = (row + .5f) * 64f + random.nextFloat() * 12f - 6f - h / 2
        val b = Building(Vec2(x, y), w, h, random.nextInt(5))
        if (b.probes().all { network.clearance(it) <= -8f } && buildings.none { it.overlaps(b) } && clearOfLandmarks(x, y, x + w, y + h)) buildings += b
    }

    fun onStreet(p: Vec2, except: Street? = null) = route.distanceFrom(p) < ROUTE_WIDTH / 2 ||
        network.streets.any { it !== except && it.line.distanceFrom(p) < it.width / 2 }
    fun tree(p: Vec2) = Tree(p, 11f + random.nextFloat() * 9f)
    val trees = when (template.style) {
        StreetStyle.Radial -> network.streets.filter { it.name?.startsWith("Bd") == true }.flatMap { street ->
            buildList {
                var d = 23f
                while (d < street.line.length) {
                    for (side in listOf(-1f, 1f)) {
                        val p = street.line.at(d, side * (street.width / 2 - 14f))
                        val onMap = p.x in 0f..width && p.y in 0f..height
                        if (onMap && !onStreet(p, street) && stops.none { it.center.distanceTo(p) < it.radius }) add(tree(p))
                    }
                    d += 46f
                }
            }
        }
        StreetStyle.Grid -> buildList {
            stops.filter { it.kind == SquareKind.Roundabout }.forEach { square ->
                val rim = square.radius - 14f
                val count = (2 * PI.toFloat() * rim / 46f).toInt()
                repeat(count) {
                    val angle = it * 2 * PI.toFloat() / count
                    val p = square.center + Vec2(cos(angle), sin(angle)) * rim
                    if (!onStreet(p)) add(tree(p))
                }
            }
            var attempts = 0
            while (size < 60 && attempts++ < 2000) {
                val p = Vec2(random.nextFloat() * width, random.nextFloat() * height)
                val t = tree(p)
                if (network.clearance(p) <= -30f && buildings.none { it.touches(t) } &&
                    clearOfLandmarks(p.x - t.radius, p.y - t.radius, p.x + t.radius, p.y + t.radius)) add(t)
            }
        }
    }.take(220)

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
        template.streets.zip(named) { spec, street ->
            // Clear of the squares too, so a street that starts at a stop is not named under the crowd.
            val clear = street.line.samples.filter { s ->
                s.position.x in 0f..width && s.position.y in 0f..height && stops.none { it.center.distanceTo(s.position) < it.radius + 40f }
            }
            require(clear.isNotEmpty()) { "$city: street ${spec.name} has no part on the map clear of the squares for its label" }
            add(label(spec.name, street.line, clear.first().distance + (clear.last().distance - clear.first().distance) * .35f))
        }
        lots.forEach { add(MapLabel(it.landmark.name, it.label, 0f)) }
    }.map { it.inside(width, height) }

    val heroStart = network.nearestWalkable(route.at(heroDistance))
    // Props come last and take their own random numbers, so everything above stays where it was.
    val props = props(template.props, Random(seed * 31 + 7), network, lots, buildings, trees, labels)
    return CityMap(city, scale, network, stops, buildings, lots.map { it.landmark }, trees, labels, heroStart, props)
}

/** About one prop per 140 units of route, each kind once before any repeats, as near as it fits to a landmark, a square, or the route. */
private fun props(kinds: List<PropKind>, random: Random, network: StreetNetwork, lots: List<Lot>, buildings: List<Building>,
                  trees: List<Tree>, labels: List<MapLabel>): List<Prop> {
    val route = network.route
    val squares = network.squares
    val obstacles = buildings.map { Box.of(it.position.x - 2f, it.position.y - 2f, it.position.x + it.width + 2f, it.position.y + it.height + 14f) } +
        lots.map { Box.of(it.left, it.top, it.right, it.bottom) } +
        trees.map { Box.of(it.position.x - it.radius - 2f, it.position.y - it.radius - 2f, it.position.x + it.radius + 7f, it.position.y + it.radius + 11f) } +
        labels.map { Box(it.position, Vec2(-sin(it.angle), cos(it.angle)), nameReach(it.text) + 4f, 10f) } +
        listOf(route.samples.first(), route.samples.last()).map { Box.of(it.position.x - 34f, it.position.y - 34f, it.position.x + 34f, it.position.y + 34f) }
    val props = mutableListOf<Prop>()
    val taken = mutableListOf<Box>()
    fun place(kind: PropKind, center: Vec2, facing: Vec2, around: List<Box>): Boolean {
        val shape = kind.shape(center, facing)
        val probes = shape.flatMap { it.probes }
        if (probes.any { it.x !in 4f..network.width - 4f || it.y !in 4f..network.height - 4f }) return false
        val ground = if (kind == PropKind.Pigeons) probes.all { network.clearance(it) >= 2f }
            else probes.all { network.clearance(it) <= -9f } && network.clearance(center + facing * (kind.depth / 2)) >= -24f
        if (!ground || props.any { it.kind == kind && it.position.distanceTo(center) < PROP_APART }) return false
        if (shape.any { box -> around.any { box.overlaps(it) } || taken.any { box.overlaps(it, 6f) } }) return false
        props += Prop(kind, center, atan2(facing.y, facing.x))
        taken += shape
        return true
    }
    fun near(anchor: Vec2) = obstacles.filter { it.center.distanceTo(anchor) < PROP_REACH + 150f }

    val edges = (listOf(route to network.routeWidth / 2) + network.streets.map { it.line to it.width / 2 }).flatMap { (line, half) ->
        (0 until (line.length / 12f).toInt()).flatMap { i ->
            val base = line.at(6f + i * 12f)
            listOf(-1f, 1f).map { side -> Triple(base, (line.at(6f + i * 12f, side) - base).normalized(), half) }
        }
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
    private val radius = sqrt(halfWidth * halfWidth + halfDepth * halfDepth)
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

private fun Pair<Float, Float>.outside() = first !in 0f..1f || second !in 0f..1f

private class Guide(val position: Vec2, val direction: Vec2)
private fun MarchRoute.guides() = samples.zipWithNext { a, b -> Guide(a.position, (b.position - a.position).normalized()) }

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

/** The part of the line through [origin] along [direction] inside the map grown by [EDGE]. */
private fun clip(origin: Vec2, direction: Vec2, width: Float, height: Float): List<Vec2>? {
    val d = direction.normalized()
    var enter = -Float.MAX_VALUE
    var leave = Float.MAX_VALUE
    for ((o, step, size) in listOf(Triple(origin.x, d.x, width), Triple(origin.y, d.y, height))) {
        if (abs(step) < 1e-6f) {
            if (o < -EDGE || o > size + EDGE) return null
            continue
        }
        val a = (-EDGE - o) / step
        val b = (size + EDGE - o) / step
        enter = max(enter, min(a, b))
        leave = min(leave, max(a, b))
    }
    return if (leave - enter > 1f) listOf(origin + d * enter, origin + d * leave) else null
}

// Samples within 120 units of the line that run within 25° of it. A filler near any of them would double that street.
private fun List<Guide>.doubled(origin: Vec2, direction: Vec2, along: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE): Int {
    val normal = Vec2(-direction.y, direction.x)
    return count {
        val offset = it.position - origin
        (offset dot direction) in along && abs(offset dot normal) < 120f && abs(it.direction dot direction) > SIMILAR
    }
}

/** Two families of streets, along the first-to-last stop axis and across it, 230..300 units apart edge to edge. */
private fun Random.grid(from: Vec2, to: Vec2, width: Float, height: Float, guides: List<Guide>): List<Street> {
    val axis = (to - from).normalized()
    val corners = listOf(Vec2(0f, 0f), Vec2(width, 0f), Vec2(0f, height), Vec2(width, height))
    return listOf(axis, Vec2(-axis.y, axis.x)).flatMap { direction ->
        val across = Vec2(-direction.y, direction.x)
        val last = corners.maxOf { it dot across }
        var offset = corners.minOf { it dot across } + nextFloat() * (FILLER_WIDTH + 300f)
        buildList {
            while (offset < last) {
                val origin = across * offset
                val span = clip(origin, direction, width, height)
                if (span != null && guides.doubled(origin, direction) == 0) add(Street(null, wobble(span, 0f..25f), FILLER_WIDTH))
                offset += FILLER_WIDTH + 230f + nextFloat() * 70f
            }
        }
    }
}

/** Spokes from every star square, then two long diagonals. */
private fun Random.radial(stops: List<Pair<Stop, Square>>, width: Float, height: Float, guides: List<Guide>): List<Street> = buildList {
    for ((stop, square) in stops) {
        if (stop.spokes == 0) continue
        val taken = guides.map { it.position - square.center }.filter { it.length() in square.radius..square.radius + 60f }.map { it.normalized() }
        val start = nextFloat() * 2 * PI.toFloat()
        repeat(stop.spokes) {
            val angle = start + it * 2 * PI.toFloat() / stop.spokes
            val direction = Vec2(cos(angle), sin(angle))
            if (taken.any { t -> (t dot direction) > SPOKE_GAP }) return@repeat
            val span = requireNotNull(clip(square.center, direction, width, height)) { "Square ${stop.name}: spoke ${it + 1} of ${stop.spokes} never enters the map" }
            val length = min(700f, span.last().distanceTo(square.center))
            // Streets that leave the square at a different angle can still bend toward the spoke farther out.
            if (guides.doubled(square.center, direction, square.radius + 60f..length) > 0) return@repeat
            add(Street(null, wobble(listOf(square.center, square.center + direction * length), 0f..25f), FILLER_WIDTH))
        }
    }
    var avoid = guides
    repeat(2) {
        // Of 20 random candidates, the one that doubles the fewest guide samples; Paris boulevards are diagonal too.
        val (origin, direction) = List(20) {
            val angle = (25f + nextFloat() * 40f) * PI.toFloat() / 180f * (if (nextBoolean()) 1f else -1f)
            Vec2(width * (.25f + nextFloat() * .5f), height * (.25f + nextFloat() * .5f)) to Vec2(cos(angle), sin(angle))
        }.minBy { (origin, direction) -> avoid.doubled(origin, direction) }
        val span = requireNotNull(clip(origin, direction, width, height)) { "Diagonal ${it + 1} of 2 through $origin never enters the map" }
        val diagonal = Street(null, wobble(span, 0f..25f), FILLER_WIDTH)
        add(diagonal)
        avoid = avoid + diagonal.line.guides()
    }
}

private fun label(text: String, line: MarchRoute, distance: Float): MapLabel {
    val tangent = line.at(distance + 2f) - line.at(distance - 2f)
    var angle = atan2(tangent.y, tangent.x)
    if (angle > PI / 2) angle -= PI.toFloat() else if (angle < -PI / 2) angle += PI.toFloat()
    return MapLabel(text, line.at(distance), angle)
}

private fun Building.probes() =
    listOf(0f, .5f, 1f).flatMap { fx -> listOf(0f, .5f, 1f).map { fy -> position + Vec2(width * fx, height * fy) } }
// At most 20 units apart, so no street fits between them.
private fun Landmark.probes(): List<Vec2> {
    val columns = 2 * ceil(width / 40f).toInt()
    val rows = 2 * ceil(height / 40f).toInt()
    return (0..columns).flatMap { i -> (0..rows).map { j -> position + Vec2(width * i / columns, height * j / rows) } }
}
private fun Building.overlaps(other: Building) =
    position.x < other.position.x + other.width && other.position.x < position.x + width &&
        position.y < other.position.y + other.height && other.position.y < position.y + height
private fun Building.touches(tree: Tree): Boolean {
    val dx = tree.position.x - tree.position.x.coerceIn(position.x, position.x + width)
    val dy = tree.position.y - tree.position.y.coerceIn(position.y, position.y + height)
    return dx * dx + dy * dy < tree.radius * tree.radius
}
