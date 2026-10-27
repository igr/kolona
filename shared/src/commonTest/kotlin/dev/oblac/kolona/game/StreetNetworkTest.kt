package dev.oblac.kolona.game

import kotlin.math.ceil
import kotlin.math.max
import kotlin.test.*

class StreetNetworkTest {
    private fun build(routeWidth: Float = 120f) = StreetNetwork(
        1400f, 1120f, MarchRoute(listOf(Vec2(100f, 560f), Vec2(1300f, 560f))), routeWidth,
        listOf(
            Street("V", listOf(Vec2(700f, 100f), Vec2(700f, 1020f)), 100f),
            Street("Island", listOf(Vec2(150f, 150f), Vec2(450f, 150f)), 90f),
        ),
        listOf(Square("S", Vec2(1300f, 560f), 100f, SquareKind.Open)),
    )
    private val network = build()
    private val start = Vec2(200f, 560f)

    @Test fun clearanceIsPositiveOnStreetsAndNegativeInBlocks() {
        assertTrue(network.clearance(Vec2(400f, 560f)) in 57f..63f)
        assertTrue(network.clearance(Vec2(700f, 300f)) in 47f..53f)
        assertTrue(network.clearance(Vec2(400f, 800f)) < 0f)
        assertTrue(network.blocked(Vec2(400f, 800f)))
        assertFalse(network.blocked(Vec2(400f, 560f)))
    }

    @Test fun pathsStayOnStreets() {
        val path = network.path(start, Vec2(700f, 1000f))
        assertTrue(path.isNotEmpty())
        val points = listOf(start) + path
        points.zipWithNext().forEach { (a, b) ->
            val steps = max(1, ceil(a.distanceTo(b) / 3f).toInt())
            for (i in 0..steps) assertFalse(network.blocked(a + (b - a) * (i.toFloat() / steps), 10f), "Collision on $a -> $b")
        }
        val length = points.zipWithNext { a, b -> a.distanceTo(b) }.sum()
        assertTrue(length <= (500f + 440f) * 1.1f, "Path is $length long")
    }

    @Test fun tapInsideABlockResolvesToTheNearestStreet() {
        val end = network.path(start, Vec2(420f, 760f)).last()
        assertFalse(network.blocked(end))
        assertTrue(end.y in 500f..620f)
    }

    @Test fun disconnectedStreetIsPrunedFromNavigation() {
        assertFalse(network.connected(start, Vec2(300f, 150f)))
        assertEquals(network.walkableCells, network.reachableCells(start))
        val end = network.path(start, Vec2(300f, 150f)).last()
        assertFalse(network.blocked(end))
        assertFalse(end.x in 100f..500f && end.y in 100f..200f, "Path ends on the island at $end")
    }

    // Both trips turn at the crossing, so the second search runs over cells the first one used.
    @Test fun repeatedSearchesReturnTheSameAsFreshOnes() {
        val corner = Vec2(700f, 1000f)
        val top = Vec2(700f, 150f)
        val square = Vec2(1300f, 650f)
        val first = network.path(start, corner)
        val second = network.path(top, square)
        val third = network.path(start, corner)
        assertTrue(first.size > 2 && second.size > 2, "Expected real searches, not direct segments: $first, $second")
        assertEquals(first, third)
        assertEquals(build().path(start, corner), first)
        assertEquals(build().path(top, square), second)
    }

    // A 10-unit route has a clearance of at most 5, below the 17 a walkable cell needs.
    @Test fun unwalkableRouteStartFailsFast() {
        assertFailsWith<IllegalArgumentException> { build(routeWidth = 10f) }
    }

    @Test fun cellsPastTheMapEdgeAreNotWalkable() {
        // 1410 and 1130 are not multiples of 35, so the last column and row have their centers past the edge.
        val edge = StreetNetwork(1410f, 1130f, MarchRoute(listOf(Vec2(100f, 560f), Vec2(1500f, 560f))), 120f,
            listOf(Street("Down", listOf(Vec2(700f, 560f), Vec2(700f, 1200f)), 100f)), listOf())
        assertTrue(edge.nearestWalkable(Vec2(1409f, 560f)).x <= 1410f)
        assertTrue(edge.nearestWalkable(Vec2(700f, 1129f)).y <= 1130f)
    }
}
