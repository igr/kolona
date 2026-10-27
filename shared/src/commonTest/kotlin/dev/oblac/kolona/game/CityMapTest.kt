package dev.oblac.kolona.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.test.*
import kotlin.time.TimeSource

private val scales = listOf(1f, 1.37f, 2f)
private val seeds = 1..3

private class Generated(val seed: Int, val map: CityMap) {
    override fun toString() = "${map.city} ${map.scale}x seed $seed"
}

// Generated once per test run; every test checks the same maps.
private val generated: List<Generated> by lazy {
    City.entries.flatMap { city -> scales.flatMap { scale -> seeds.map { Generated(it, generateMap(city, it, scale)) } } }
}

class CityMapTest {
    private fun forEachMap(check: (String, CityMap) -> Unit) = generated.forEach { check(it.toString(), it.map) }
    private fun map(city: City, scale: Float, seed: Int) =
        generated.first { it.map.city == city && it.map.scale == scale && it.seed == seed }.map
    private fun corners(map: CityMap) = listOf(Vec2(0f, 0f), Vec2(map.width, 0f), Vec2(0f, map.height), Vec2(map.width, map.height))
    private fun probes(position: Vec2, width: Float, height: Float) =
        listOf(0f, .5f, 1f).flatMap { fx -> listOf(0f, .5f, 1f).map { fy -> position + Vec2(width * fx, height * fy) } }
    private fun probes(b: Building) = probes(b.position, b.width, b.height)
    private fun overlaps(l: Landmark, position: Vec2, width: Float, height: Float) =
        l.position.x < position.x + width && position.x < l.position.x + l.width &&
            l.position.y < position.y + height && position.y < l.position.y + l.height
    private fun inside(p: Vec2, position: Vec2, width: Float, height: Float) =
        p.x > position.x && p.x < position.x + width && p.y > position.y && p.y < position.y + height
    private fun inside(p: Vec2, b: Building) = inside(p, b.position, b.width, b.height)
    // A 5 × 5 grid over the turned footprint, also raised by half and all of the prop's height (people and columns stand up from it).
    private fun footprint(prop: Prop): List<Vec2> {
        val facing = Vec2(cos(prop.angle), sin(prop.angle))
        val side = Vec2(-facing.y, facing.x)
        val levels = if (prop.kind.height > 0f) listOf(0f, prop.kind.height / 2, prop.kind.height) else listOf(0f)
        return levels.flatMap { up ->
            (0..4).flatMap { i ->
                (0..4).map { j -> prop.position + side * (prop.kind.width * (i / 4f - .5f)) + facing * (prop.kind.depth * (j / 4f - .5f)) - Vec2(0f, up) }
            }
        }
    }
    private fun covers(prop: Prop, p: Vec2): Boolean {
        val facing = Vec2(cos(prop.angle), sin(prop.angle))
        val side = Vec2(-facing.y, facing.x)
        return listOf(0f, prop.kind.height / 2, prop.kind.height).any { up ->
            val d = p + Vec2(0f, up) - prop.position
            abs(d.x * side.x + d.y * side.y) < prop.kind.width / 2 && abs(d.x * facing.x + d.y * facing.y) < prop.kind.depth / 2
        }
    }

    @Test fun cityCodesRoundTripAndUnknownCodesGiveBelgrade() {
        City.entries.forEach { assertEquals(it, City.fromCode(it.code)) }
        assertEquals(City.Belgrade, City.fromCode(null))
        assertEquals(City.Belgrade, City.fromCode("atlantis"))
    }

    @Test fun routeVisitsTheTemplateStopsInOrder() = forEachMap { name, map ->
        val samples = map.route.samples
        assertEquals(map.city.template.stops.map { it.name }, map.stops.map { it.name }, name)
        assertTrue(samples.first().position.distanceTo(map.stops.first().center) <= 1f, name)
        assertTrue(samples.last().position.distanceTo(map.stops.last().center) <= 1f, name)
        map.stops.drop(1).dropLast(1).forEach {
            assertTrue(map.route.distanceFrom(it.center) <= map.network.routeWidth / 2, "$name: ${it.name}")
        }
        val distances = map.stops.map { stop -> samples.minBy { it.position.distanceTo(stop.center) }.distance }
        assertTrue(distances.zipWithNext().all { (a, b) -> b > a }, "$name: $distances")
    }

    @Test fun routeHasOrderedDistancesAndCorrectEndpoints() = forEachMap { name, map ->
        assertTrue(map.route.samples.zipWithNext().all { (a, b) -> b.distance > a.distance }, name)
        assertEquals(map.route.samples.first().position, map.route.at(-500f), name)
        assertEquals(map.route.samples.last().position, map.route.at(map.route.length + 500), name)
    }

    @Test fun routeLengthIsComparableAcrossCities() {
        for (scale in scales) for (seed in seeds) {
            val lengths = City.entries.map { map(it, scale, seed).route.length }
            println("route length ${scale}x seed $seed: ${City.entries.zip(lengths)}")
            lengths.forEach { assertTrue(it / scale in 1400f..2000f, "${scale}x seed $seed: $lengths") }
            assertTrue((lengths.max() - lengths.min()) / lengths.max() <= .15f, "${scale}x seed $seed: $lengths")
        }
    }

    @Test fun noBuildingTouchesTheWalkableNetwork() = forEachMap { name, map ->
        assertTrue(map.buildings.size >= 5, name)
        map.buildings.forEach { b -> probes(b).forEach { assertTrue(map.network.clearance(it) <= -6f, "$name: $b at $it") } }
    }

    @Test fun everyTemplateNameIsLabelledOnTheMap() = forEachMap { name, map ->
        val template = map.city.template
        val texts = map.labels.map { it.text }
        (template.stops.map { it.name } + template.legs.flatMap { it.names } + template.streets.map { it.name })
            .forEach { assertTrue(it in texts, "$name: $it") }
        map.labels.forEach {
            assertTrue(it.position.x in 40f..map.width - 40f && it.position.y in 40f..map.height - 40f, "$name: ${it.text} at ${it.position}")
        }
    }

    @Test fun everyLandmarkIsPlacedInABlockNearItsPlace() = forEachMap { name, map ->
        val specs = map.city.template.landmarks
        assertEquals(specs.size, map.landmarks.size, name)
        val stops = map.stops.map { stop -> map.route.samples.minBy { it.position.distanceTo(stop.center) }.distance }
        val texts = map.labels.map { it.text }
        specs.zip(map.landmarks) { spec, landmark ->
            val where = "$name: ${spec.name} at ${landmark.position}"
            assertEquals(spec.name, landmark.name, name)
            assertEquals(spec.kind, landmark.kind, where)
            // On a crowded map a landmark may shrink, but it keeps its shape.
            assertTrue(landmark.width in spec.width * .2f..spec.width, "$where: ${landmark.width} wide")
            assertEquals(landmark.width / spec.width, landmark.height / spec.height, .001f, where)
            probes(landmark.position, landmark.width, landmark.height).forEach {
                assertTrue(map.network.clearance(it) <= -6f, "$where: $it is not in a block")
            }
            val center = landmark.position + Vec2(landmark.width / 2, landmark.height / 2)
            val anchor = map.route.at(stops[spec.stop] + spec.along)
            assertTrue(center.distanceTo(anchor) <= 350f, "$where is ${center.distanceTo(anchor)} from its anchor $anchor")
            assertTrue(landmark.position.x >= 0f && landmark.position.y >= 0f &&
                landmark.position.x + landmark.width <= map.width && landmark.position.y + landmark.height <= map.height, "$where is off the map")
            assertTrue(spec.name in texts, "$where has no label")
            map.landmarks.filter { it !== landmark }.forEach {
                assertFalse(overlaps(landmark, it.position, it.width, it.height), "$where overlaps ${it.name}")
            }
            map.buildings.forEach { assertFalse(overlaps(landmark, it.position, it.width, it.height), "$where overlaps $it") }
        }
    }

    // A street that starts at a stop is named outside its square, not under the waiting crowd.
    @Test fun streetNamesStayClearOfTheSquares() = forEachMap { name, map ->
        map.city.template.streets.forEach { street ->
            val label = map.labels.first { it.text == street.name }
            val stop = map.stops.minBy { it.center.distanceTo(label.position) - it.radius }
            assertTrue(label.position.distanceTo(stop.center) >= stop.radius + 40f, "$name: ${street.name} at ${label.position} is in ${stop.name}")
        }
    }

    @Test fun everyDrawnStreetIsReachable() = forEachMap { name, map ->
        map.network.streets.forEach { street ->
            assertTrue(street.line.samples.any { !map.network.blocked(it.position, 17f) && map.network.connected(map.heroStart, it.position) },
                "$name: ${street.name ?: "filler"} from ${street.line.samples.first().position}")
        }
    }

    // Pruning hides pockets; a generator that makes many of them would leave streets nobody can reach.
    @Test fun prunedCellsAreFew() {
        var worst = 0f
        forEachMap { name, map ->
            val cell = StreetNetwork.CELL
            var open = 0
            for (row in 0 until ceil(map.height / cell).toInt()) for (col in 0 until ceil(map.width / cell).toInt()) {
                val center = Vec2((col + .5f) * cell, (row + .5f) * cell)
                if (center.x <= map.width && center.y <= map.height && map.network.clearance(center) >= 17f) open++
            }
            val pruned = (open - map.network.walkableCells) / open.toFloat()
            worst = max(worst, pruned)
            assertTrue(pruned <= .05f, "$name: $open open cells, ${map.network.walkableCells} walkable")
        }
        println("worst pruned cells: ${worst * 100}%")
    }

    @Test fun tapsInsideBuildingsResolveToWalkableDestinations() = forEachMap { name, map ->
        map.buildings.take(5).forEach { b ->
            val path = map.path(map.heroStart, b.position + Vec2(b.width / 2, b.height / 2))
            assertTrue(path.isNotEmpty(), "$name: $b")
            assertFalse(map.blocked(path.last()), "$name: $b")
        }
    }

    @Test fun heroAndMarcherStartsAreWalkable() = forEachMap { name, map ->
        assertFalse(map.blocked(map.heroStart), name)
        for (row in 0..15) for (offset in listOf(-26f, -13f, 0f, 13f, 26f))
            assertFalse(map.blocked(map.route.at(280f - row * 15f, offset), 6f), "$name: row $row offset $offset")
    }

    @Test fun navigationNeverCutsBuildingCorners() = forEachMap { name, map ->
        (corners(map) + Vec2(map.width / 2, map.height / 2) + map.route.samples.last().position).forEach { target ->
            val path = map.path(map.heroStart, target)
            assertTrue(path.isNotEmpty(), "$name: $target")
            (listOf(map.heroStart) + path).zipWithNext().forEach { (a, b) ->
                val steps = max(1, ceil(a.distanceTo(b) / 3f).toInt())
                for (i in 0..steps) {
                    val p = a + (b - a) * (i.toFloat() / steps)
                    assertFalse(map.blocked(p, 10f), "$name: collision on $a -> $b")
                    assertTrue(map.buildings.none { inside(p, it) }, "$name: $p is inside a building")
                }
            }
        }
    }

    @Test fun tapsAtMapCornersResolveToStreets() = forEachMap { name, map ->
        corners(map).forEach { corner ->
            val path = map.path(map.heroStart, corner)
            assertTrue(path.isNotEmpty(), "$name: $corner")
            assertFalse(map.blocked(path.last()), "$name: $corner")
        }
    }

    // Props are decoration: in blocks, clear of everything else, and placed last with their own random numbers.
    @Test fun propsDecorateEveryCityWithoutTouchingTheGame() = generated.forEach { g ->
        val name = g.toString()
        val map = g.map
        val kinds = map.city.template.props
        assertTrue(map.props.isNotEmpty(), name)
        assertTrue(map.props.all { it.kind in kinds }, name)
        if (map.scale == 2f) kinds.forEach { kind -> assertTrue(map.props.any { it.kind == kind }, "$name: no $kind") }
        map.props.forEach { prop ->
            val where = "$name: ${prop.kind} at ${prop.position}"
            val samples = footprint(prop)
            samples.forEach { assertTrue(it.x in 0f..map.width && it.y in 0f..map.height, "$where: $it is off the map") }
            if (prop.kind != PropKind.Pigeons) samples.forEach { assertTrue(map.network.clearance(it) <= -2f, "$where: $it is not in a block") }
            map.buildings.filter { it.position.distanceTo(prop.position) < 200f }.forEach { b ->
                assertFalse(samples.any { inside(it, b) } || probes(b).any { covers(prop, it) }, "$where overlaps $b")
            }
            map.landmarks.forEach { l ->
                assertFalse(samples.any { inside(it, l.position, l.width, l.height) } || probes(l.position, l.width, l.height).any { covers(prop, it) },
                    "$where overlaps ${l.name}")
            }
            map.props.filter { it !== prop }.forEach { other ->
                assertFalse(samples.any { covers(other, it) }, "$where overlaps ${other.kind} at ${other.position}")
            }
        }
        assertEquals(map.props, generateMap(map.city, g.seed, map.scale).props, name)
        val bare = generateMap(map.city, map.city.template.copy(props = listOf()), g.seed, map.scale)
        assertTrue(bare.props.isEmpty(), name)
        assertEquals(bare.route.samples, map.route.samples, name)
        assertEquals(bare.network.streets.map { it.line.samples }, map.network.streets.map { it.line.samples }, name)
        assertEquals(bare.network.walkableCells, map.network.walkableCells, name)
        assertEquals(bare.buildings, map.buildings, name)
        assertEquals(bare.landmarks, map.landmarks, name)
        assertEquals(bare.trees, map.trees, name)
        assertEquals(bare.labels, map.labels, name)
        assertEquals(bare.heroStart, map.heroStart, name)
    }

    @Test fun sameSeedSameMapDifferentSeedDifferentMap() {
        val a = generateMap(City.Paris, 5, 1.5f)
        val b = generateMap(City.Paris, 5, 1.5f)
        assertEquals(a.route.samples, b.route.samples)
        assertEquals(a.buildings, b.buildings)
        assertTrue(a.landmarks.isNotEmpty())
        assertEquals(a.landmarks, b.landmarks)
        assertNotEquals(a.route.samples, generateMap(City.Paris, 6, 1.5f).route.samples)
    }

    // The shipped templates cannot fail like this; a bad new template must fail with a clear message.
    @Test fun streetThatNeverEntersTheMapFailsWithItsName() {
        val belgrade = City.Belgrade.template
        val template = belgrade.copy(streets = belgrade.streets + NamedStreet("Nowhere", listOf(.2f to -.6f, .8f to -.6f), 95f))
        val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
        assertContains(error.message.orEmpty(), "Nowhere")
    }

    // At 1x, every part of this street on the map is within 40 units of Trg Republike.
    @Test fun streetWithNoRoomForItsLabelFailsWithItsName() {
        val belgrade = City.Belgrade.template
        val template = belgrade.copy(streets = belgrade.streets + NamedStreet("Stub", listOf(.96f to .08f, 1.05f to .08f), 95f))
        val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
        assertContains(error.message.orEmpty(), "Stub")
    }

    @Test fun landmarkTooBigForItsBlocksShrinks() {
        val belgrade = City.Belgrade.template
        val template = belgrade.copy(landmarks = belgrade.landmarks + LandmarkSpec("Big", LandmarkKind.Museum, 1, 0f, 1, 400f, 300f))
        val big = generateMap(City.Belgrade, template, 1, 1f).landmarks.last()
        assertEquals("Big", big.name)
        assertTrue(big.width < 400f && big.height < 300f, "$big")
    }

    // Even at its smallest size, this is taller than the map.
    @Test fun landmarkWithNoRoomFailsWithItsName() {
        val belgrade = City.Belgrade.template
        val template = belgrade.copy(landmarks = belgrade.landmarks + LandmarkSpec("Giant", LandmarkKind.Museum, 1, 0f, 1, 4000f, 4000f))
        val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
        assertContains(error.message.orEmpty(), "Belgrade")
        assertContains(error.message.orEmpty(), "Giant")
    }

    @Test fun landmarkAtAStopThatDoesNotExistFailsClearly() {
        val belgrade = City.Belgrade.template
        for (stop in listOf(3, -1)) {
            val template = belgrade.copy(landmarks = belgrade.landmarks + LandmarkSpec("Lost", LandmarkKind.Museum, stop, 0f, 1, 50f, 50f))
            val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
            assertContains(error.message.orEmpty(), "Belgrade")
            assertContains(error.message.orEmpty(), "Lost")
            assertContains(error.message.orEmpty(), "stop $stop")
        }
    }

    // With Terazije next to Slavija, the first leg ends before its label can clear the hero.
    @Test fun tooShortFirstLegFailsClearly() {
        val belgrade = City.Belgrade.template
        val template = belgrade.copy(
            stops = listOf(belgrade.stops[0], belgrade.stops[1].copy(u = .1f, v = .85f), belgrade.stops[2]),
            legs = listOf(belgrade.legs[0].copy(via = listOf()), belgrade.legs[1]),
        )
        val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
        assertContains(error.message.orEmpty(), "Belgrade")
        assertContains(error.message.orEmpty(), "Kralja Milana")
    }

    // From this far off the map, the map fills less than 45°, so at least one of Nation's 8 spokes misses it.
    @Test fun spokeThatNeverEntersTheMapFailsWithTheSquareName() {
        val paris = City.Paris.template
        val template = paris.copy(stops = listOf(paris.stops[0], paris.stops[1], paris.stops[2].copy(u = 3f, v = 3f)))
        val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Paris, template, 1, 1f) }
        assertContains(error.message.orEmpty(), "Nation")
    }

    @Test fun legCountThatDoesNotMatchTheStopsFailsClearly() {
        val belgrade = City.Belgrade.template
        for (legs in listOf(belgrade.legs + Leg(listOf("Extra"), listOf()), belgrade.legs.take(1))) {
            val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, belgrade.copy(legs = legs), 1, 1f) }
            assertContains(error.message.orEmpty(), "Belgrade")
            assertContains(error.message.orEmpty(), "legs (${legs.size})")
            assertContains(error.message.orEmpty(), "stops (3)")
        }
    }

    // A leg may be an unnamed hop between two stops.
    @Test fun legWithoutNamesGetsNoLabels() {
        val paris = City.Paris.template
        val template = paris.copy(legs = listOf(paris.legs[0].copy(names = listOf()), paris.legs[1]))
        val texts = generateMap(City.Paris, template, 1, 1f).labels.map { it.text }
        paris.legs[0].names.forEach { assertFalse(it in texts, "$it is still labelled") }
        paris.legs[1].names.forEach { assertTrue(it in texts, "$it is missing") }
    }

    @Test fun streetNeedsAtLeastTwoPoints() {
        val belgrade = City.Belgrade.template
        for (points in listOf(listOf(1.05f to .5f), listOf<Pair<Float, Float>>())) {
            val template = belgrade.copy(streets = belgrade.streets + NamedStreet("Dot", points, 95f))
            val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
            assertContains(error.message.orEmpty(), "Dot")
        }
    }

    @Test fun templateNeedsAtLeastTwoStops() {
        val belgrade = City.Belgrade.template
        val template = belgrade.copy(stops = belgrade.stops.take(1), legs = listOf())
        val error = assertFailsWith<IllegalArgumentException> { generateMap(City.Belgrade, template, 1, 1f) }
        assertContains(error.message.orEmpty(), "Belgrade")
        assertContains(error.message.orEmpty(), "stops (1)")
    }

    @Test fun mapScaleStaysWithinConfig() {
        val values = (0..99).map { mapScale(it, 2f) }
        assertTrue(values.all { it in 1f..2f }, "$values")
        assertTrue(values.distinct().size >= 20, "$values")
        for (seed in 0..99) {
            assertEquals(1f, mapScale(seed, 1f))
            assertEquals(1f, mapScale(seed, .5f))
        }
    }

    @Test fun generationIsFastEnough() {
        City.entries.forEach { city ->
            val times = (1..3).map { seed ->
                val start = TimeSource.Monotonic.markNow()
                generateMap(city, seed, 2f)
                start.elapsedNow().inWholeMilliseconds
            }.sorted()
            println("$city 2x generation: median ${times[1]} ms (target 200 ms), runs $times")
            assertTrue(times[1] < 600, "$city: $times")
        }
    }
}
