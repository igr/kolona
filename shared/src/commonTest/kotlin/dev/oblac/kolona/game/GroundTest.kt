package dev.oblac.kolona.game

import kotlin.math.ceil
import kotlin.math.max
import kotlin.test.*

class GroundTest {
    private val start = Vec2(200f, 560f)
    private fun ground(vararg buildings: Building, landmarks: List<Landmark> = listOf()) = Ground(1400f, 1120f, buildings.toList(), landmarks, start)
    // 80 units thick, from y 360 down to 772 with its front wall.
    private val wall = Building(Vec2(660f, 360f), 80f, 400f, 0)

    private fun assertWalkable(ground: Ground, from: Vec2, path: List<Vec2>) {
        (listOf(from) + path).zipWithNext().forEach { (a, b) ->
            val steps = max(1, ceil(a.distanceTo(b) / 3f).toInt())
            for (i in 0..steps) assertFalse(ground.blocked(a + (b - a) * (i.toFloat() / steps), 10f), "Collision on $a -> $b")
        }
    }
    private fun length(from: Vec2, path: List<Vec2>) = (listOf(from) + path).zipWithNext { a, b -> a.distanceTo(b) }.sum()

    @Test fun clearanceIsTheDistanceToTheNearestBuildingLandmarkOrEdge() {
        val ground = ground(Building(Vec2(600f, 500f), 100f, 80f, 0), landmarks = listOf(Landmark("L", LandmarkKind.Museum, Vec2(1000f, 200f), 120f, 80f)))
        assertEquals(20f, ground.clearance(Vec2(580f, 540f)), 4f)
        assertTrue(ground.clearance(Vec2(650f, 540f)) < 0f)
        // The front wall shows 12 units below the roof, so it stands in the way too.
        assertEquals(8f, ground.clearance(Vec2(650f, 600f)), 4f)
        assertTrue(ground.clearance(Vec2(1060f, 240f)) < 0f)
        assertEquals(8f, ground.clearance(Vec2(8f, 300f)), 4f)
        assertTrue(ground.clearance(Vec2(300f, 300f)) >= 25f)
        assertTrue(ground.blocked(Vec2(650f, 540f)))
        assertFalse(ground.blocked(Vec2(300f, 300f)))
    }

    // The line passes about 33 units below the wall's corner.
    @Test fun peopleCrossOpenGroundInAStraightLine() {
        assertEquals(listOf(Vec2(1100f, 1050f)), ground(wall).path(start, Vec2(1100f, 1050f)))
    }

    @Test fun pathsGoAroundBuildingsTheShortWay() {
        val ground = ground(wall)
        val from = Vec2(400f, 560f)
        val path = ground.path(from, Vec2(1000f, 560f))
        assertEquals(Vec2(1000f, 560f), path.last())
        assertWalkable(ground, from, path)
        // Around the top corners, 13 units clear of them, is about 760.
        assertTrue(length(from, path) <= 830f, "The way around is ${length(from, path)} long: $path")
    }

    @Test fun peopleWalkThroughA48UnitGapBetweenBuildings() {
        val ground = ground(Building(Vec2(660f, 100f), 80f, 412f, 0), Building(Vec2(660f, 572f), 80f, 500f, 0))
        val from = Vec2(400f, 548f)
        val path = ground.path(from, Vec2(1000f, 548f))
        assertWalkable(ground, from, path)
        assertTrue(length(from, path) <= 620f, "The way is ${length(from, path)} long: $path")
    }

    @Test fun aTapInsideABuildingEndsBesideIt() {
        val ground = ground(wall)
        val end = ground.path(start, Vec2(700f, 560f)).last()
        assertFalse(ground.blocked(end))
        assertTrue(end.x in 600f..800f, "The hero stops at $end")
    }

    @Test fun aYardWalledInByBuildingsIsNotWalkable() {
        val ground = ground(
            Building(Vec2(500f, 300f), 400f, 28f, 0), Building(Vec2(500f, 600f), 400f, 28f, 0),
            Building(Vec2(500f, 300f), 40f, 328f, 0), Building(Vec2(860f, 300f), 40f, 328f, 0),
        )
        val yard = Vec2(700f, 470f)
        assertFalse(ground.blocked(yard))
        assertFalse(ground.connected(start, yard))
        assertEquals(ground.walkableCells, ground.reachableCells(start))
        val end = ground.path(start, yard).last()
        assertFalse(ground.blocked(end))
        assertFalse(end.x in 540f..860f && end.y in 340f..600f, "The path ends in the yard at $end")
    }

    @Test fun repeatedSearchesReturnTheSameAsFreshOnes() {
        val ground = ground(wall)
        val west = Vec2(400f, 560f)
        val east = Vec2(1000f, 600f)
        val first = ground.path(west, east)
        val second = ground.path(east, Vec2(420f, 500f))
        val third = ground.path(west, east)
        assertTrue(first.size >= 2 && second.size >= 2, "Expected real searches, not straight lines: $first, $second")
        assertEquals(first, third)
        assertEquals(ground(wall).path(west, east), first)
        assertEquals(ground(wall).path(east, Vec2(420f, 500f)), second)
    }

    @Test fun aStartInsideABuildingFailsFast() {
        assertFailsWith<IllegalArgumentException> { ground(Building(Vec2(150f, 500f), 100f, 100f, 0)) }
    }

    @Test fun nobodyWalksOffTheMap() {
        val ground = ground()
        assertTrue(ground.blocked(Vec2(5f, 560f)))
        assertTrue(ground.nearestWalkable(Vec2(1399f, 560f)).x <= 1400f - 13f)
        assertTrue(ground.nearestWalkable(Vec2(700f, 1119f)).y <= 1120f - 13f)
        assertFalse(ground.blocked(ground.path(start, Vec2(1400f, 1120f)).last()))
    }
}
