package dev.oblac.kolona.game

import kotlin.math.abs
import kotlin.test.*

class GameEngineTest {
    private fun game() = GameEngine(seed = 7, config = GameConfig(spawnEnemies = false))
    private fun advance(game: GameEngine, seconds: Float) { repeat((seconds * 60).toInt()) { game.advance(1f / 60) } }
    private fun spot(game: GameEngine, dx: Float, dy: Float) = game.map.nearestWalkable(game.hero.position + Vec2(dx, dy))
    // Outside the fire range, so only a bomb can stop enemies there.
    private fun farSpot(game: GameEngine) = spot(game, 430f, -190f).also {
        assertTrue(it.distanceTo(game.hero.position) >= 300f, "$it is too close to the hero")
    }

    // Well off the street and its squares, and clear of every building (with its front wall) and landmark.
    private fun openGround(map: CityMap, near: Vec2): Vec2 =
        (-10..10).flatMap { i -> (-10..10).map { j -> near + Vec2(i * 20f, j * 20f) } }
            .filter { p ->
                map.route.distanceFrom(p) > 120f && map.stops.none { it.center.distanceTo(p) < it.radius + 40f } &&
                    map.buildings.none { b -> p.x > b.position.x - 25f && p.x < b.position.x + b.width + 25f && p.y > b.position.y - 25f && p.y < b.position.y + b.height + Building.FRONT + 25f } &&
                    map.landmarks.none { l -> p.x > l.position.x - 25f && p.x < l.position.x + l.width + 25f && p.y > l.position.y - 25f && p.y < l.position.y + l.height + 25f }
            }
            .minByOrNull { it.distanceTo(near) } ?: fail("No open ground within 200 units of $near")

    @Test fun heroWalksAcrossOpenGroundToTheVerySpot() {
        val game = game()
        val spot = openGround(game.map, game.hero.position)
        assertEquals(CommandResult.Move, game.command(spot))
        advance(game, 4f)
        assertTrue(game.hero.position.distanceTo(spot) < 1f, "The hero stopped at ${game.hero.position}, not at $spot")
    }

    // The hero stands 30 units below a building, and an attacker has squeezed in between.
    @Test fun theBarricadeNeverShovesAnAttackerIntoABuilding() {
        val building = Building(Vec2(660f, 590f), 80f, 68f, 0)
        val route = MarchRoute(listOf(Vec2(100f, 900f), Vec2(1300f, 900f)))
        val stops = listOf(Square("A", route.at(0f), 90f, SquareKind.Open), Square("B", route.at(route.length), 90f, SquareKind.Open))
        val map = CityMap(City.Belgrade, 1f, Street(1400f, 1120f, route, 120f, stops), Ground(1400f, 1120f, listOf(building), listOf(), route.at(0f)),
            stops, listOf(building), listOf(), listOf(), listOf(), Vec2(700f, 700f), listOf())
        val game = GameEngine(7, GameConfig(spawnEnemies = false), map)
        val enemy = Enemy(1, EnemyKind.Thug, Vec2(700f, 685f)).apply { warning = 0f }
        game.enemies += enemy
        advance(game, .1f)
        assertFalse(game.map.blocked(enemy.position, 0f), "The barricade shoved the attacker into the building, to ${enemy.position}")
    }

    @Test fun startsWith72MarchersAndDeployedBarricade() {
        val game = game()
        assertEquals(72, game.survivors)
        assertFalse(game.hero.moving)
        assertEquals(1f, game.hero.barricade)
    }

    @Test fun pauseFreezesMovementCrowdAndCooldowns() {
        val game = game()
        game.command(spot(game, 230f, -90f))
        advance(game, .25f)
        game.pause()
        val hero = game.hero.position
        val crowd = game.people.map { it.position }
        val elapsed = game.elapsed
        advance(game, 2f)
        assertEquals(hero, game.hero.position)
        assertEquals(crowd, game.people.map { it.position })
        assertEquals(elapsed, game.elapsed)
        assertEquals(CommandResult.Ignored, game.command(spot(game, 280f, -140f)))
        game.resume()
        advance(game, .25f)
        assertTrue(game.elapsed > elapsed)
        assertNotEquals(hero, game.hero.position)
    }

    @Test fun movementDisablesFireAndStoppingDeploysBarricade() {
        val game = game()
        val target = Enemy(1, EnemyKind.Thug, game.hero.position + Vec2(40f, -80f)).apply { warning = 0f }
        game.enemies += target
        assertEquals(CommandResult.Move, game.command(spot(game, 340f, -100f)))
        advance(game, .2f)
        assertTrue(game.hero.moving)
        assertEquals(0, game.shots.size)
        advance(game, 4f)
        assertFalse(game.hero.moving)
        assertEquals(1f, game.hero.barricade)
    }

    @Test fun stationaryDefenderAutomaticallyStopsAThreat() {
        val game = game()
        game.enemies += Enemy(1, EnemyKind.Thug, game.hero.position + Vec2(90f, 0f)).apply { warning = 0f }
        advance(game, 2f)
        assertEquals(1, game.defended)
        assertTrue(game.enemies.isEmpty())
    }

    @Test fun aBombGoesWhereItIsThrownAndRechargesWithoutMovingTheHero() {
        val game = game()
        val p = farSpot(game)
        repeat(4) { game.enemies += Enemy(it, EnemyKind.Thug, p + Vec2(it * 12f, 0f)).apply { warning = 0f } }
        val original = game.hero.position
        assertEquals(CommandResult.Bomb, game.throwBomb(p))
        assertEquals(p, game.bombs.single().target)
        assertEquals(5f, game.bombCooldown)
        assertEquals(CommandResult.CoolingDown, game.throwBomb(p))
        assertEquals(1, game.bombs.size)
        assertEquals(original, game.hero.position)
        advance(game, .8f)
        assertEquals(4, game.defended)
        assertEquals(72, game.survivors)
        advance(game, 4.3f)
        assertEquals(0f, game.bombCooldown)
    }

    // Two attackers on one spot look like one, so four would pass for three and draw a bomb.
    @Test fun attackersNeverStandOnTopOfEachOther() {
        val game = GameEngine(3)
        repeat(90 * 60) {
            game.advance(1f / 60)
            game.enemies.forEachIndexed { i, a ->
                game.enemies.drop(i + 1).forEach { b ->
                    assertTrue(a.position.distanceTo(b.position) >= 2f, "Attackers ${a.id} and ${b.id} stand on one spot at ${a.position} after ${game.elapsed} s")
                }
            }
        }
    }

    // A swipe aims the bomb, so a bomb needs no group, and a tap on a group only moves the hero.
    @Test fun aBombNeedsNoGroupAndATapNeverThrowsOne() {
        val game = game()
        val p = farSpot(game)
        repeat(4) { game.enemies += Enemy(it, EnemyKind.Thug, p + Vec2(it * 12f, 0f)).apply { warning = 0f } }
        assertEquals(CommandResult.Move, game.command(p))
        assertTrue(game.bombs.isEmpty())
        assertEquals(0f, game.bombCooldown)
        assertEquals(CommandResult.Bomb, game.throwBomb(game.hero.position + Vec2(0f, -150f)))
        assertEquals(CommandResult.Ignored, game.throwBomb(Vec2(Float.NaN, 0f)))
    }

    @Test fun speechStopsMarchThenMarchResumes() {
        val game = GameEngine(1, GameConfig(speechesAt = listOf(1f), speechSeconds = 2f, spawnEnemies = false))
        advance(game, 1.1f)
        assertTrue(game.speechRemaining > 0f)
        val positions = game.people.map { it.position }
        advance(game, 1f)
        assertEquals(positions, game.people.map { it.position })
        advance(game, 1.5f)
        assertEquals(0f, game.speechRemaining)
        assertNotEquals(positions, game.people.map { it.position })
    }

    // The first wave, at 3 seconds, is thugs. While the protest stops for a speech, the same wave brings twice as many.
    @Test fun thugsComeInTwiceTheNumberWhileTheProtestStops() {
        fun firstWave(config: GameConfig): List<Enemy> {
            val game = GameEngine(1, config)
            while (game.enemies.isEmpty()) advance(game, 1f / 60)
            return game.enemies
        }
        val walking = firstWave(GameConfig(speechesAt = listOf()))
        val stopped = firstWave(GameConfig(speechesAt = listOf(0f), speechSeconds = 20f))
        assertTrue((walking + stopped).all { it.kind == EnemyKind.Thug })
        assertEquals(2 * walking.size, stopped.size)
    }

    @Test fun completesWholeRouteWithTwoSpeechesAndAllMarchers() {
        val game = game()
        advance(game, 181f)
        assertEquals(GameStatus.Won, game.status)
        assertEquals(72, game.survivors)
        assertTrue(game.people.all { it.arrived })
        assertEquals(1f, game.progress)
        assertTrue(game.elapsed in 179f..181f)
    }

    @Test fun noEnemyRunWinsInEveryCityAndScale() {
        for (city in City.entries) for (scale in listOf(1f, 2f)) {
            val game = GameEngine(7, GameConfig(spawnEnemies = false), generateMap(city, 7, scale))
            advance(game, 181f)
            assertEquals(GameStatus.Won, game.status, "$city ${scale}x")
            assertEquals(72, game.survivors, "$city ${scale}x")
            assertTrue(game.people.all { it.arrived }, "$city ${scale}x")
        }
    }

    @Test fun anEnemyInsideABuildingWalksOut() {
        val game = game()
        val start = game.map.buildings.map { it.position + Vec2(it.width / 2, it.height / 2) }
            .first { it.distanceTo(game.hero.position) > 400f }
        assertTrue(game.map.blocked(start, 0f))
        val enemy = Enemy(1, EnemyKind.Thug, start).apply { warning = 0f }
        game.enemies += enemy
        advance(game, 6f)
        assertFalse(game.map.blocked(enemy.position, 0f), "Stuck at ${enemy.position}")
    }

    @Test fun losingThresholdIsBelow36AndFinishedGameCannotAdvance() {
        val game = game()
        game.people.take(36).forEach { it.health = 0 }
        game.advance(.02f)
        assertEquals(GameStatus.Playing, game.status)
        game.people[36].health = 0
        game.advance(.02f)
        assertEquals(GameStatus.Lost, game.status)
        val elapsed = game.elapsed
        advance(game, 1f)
        assertEquals(elapsed, game.elapsed)
        assertEquals(CommandResult.Ignored, game.command(spot(game, 230f, -90f)))
    }

    @Test fun fixedTimestepMatchesAcrossDisplayRefreshRates() {
        val a = game()
        val b = game()
        val target = spot(a, 280f, -90f)
        a.command(target)
        b.command(target)
        repeat(600) { a.advance(1f / 60) }
        repeat(1200) { b.advance(1f / 120) }
        assertEquals(a.elapsed, b.elapsed)
        assertEquals(a.hero.position, b.hero.position)
        assertEquals(a.people.map { it.position }, b.people.map { it.position })
    }

    @Test fun stallsAndInvalidTimeCannotFastForwardOrCorruptSimulation() {
        val game = game()
        game.advance(Float.NaN)
        game.advance(Float.POSITIVE_INFINITY)
        game.advance(-1f)
        assertEquals(0f, game.elapsed)
        game.advance(60f)
        assertTrue(game.elapsed <= .101f)
        assertEquals(CommandResult.Ignored, game.command(Vec2(Float.NaN, 0f)))
    }

    // With maxMapScale 1 there is no size range to interpolate over. A NaN interval would stop the waves after the first.
    @Test fun wavesKeepComingWhenMapsNeverGrow() {
        val game = GameEngine(7, GameConfig(maxMapScale = 1f))
        fun spawned() = game.enemies.size + game.defended
        advance(game, 4f)
        val first = spawned()
        assertTrue(first > 0, "The first wave comes at 3 s")
        advance(game, 16f)
        assertTrue(spawned() > first, "Only the first wave came: $first enemies in 20 s")
    }

    @Test fun seededWavesAreRepeatableAndEscalate() {
        val a = GameEngine(42)
        val b = GameEngine(42)
        advance(a, 8f)
        advance(b, 8f)
        assertTrue(a.enemies.isNotEmpty())
        assertEquals(a.enemies.map { it.position }, b.enemies.map { it.position })
        assertEquals(a.survivors, b.survivors)
    }

    // A beaten person falls and lies on the map for a moment; only then does the body go.
    @Test fun aBeatenEnemyFallsWhereItStoodThenLeaves() {
        val game = game()
        val spot = spot(game, 120f, 0f)
        game.enemies += Enemy(900, EnemyKind.Police, spot).apply { health = 0 }
        advance(game, 1f / 60)
        assertTrue(game.enemies.isEmpty())
        assertEquals(1, game.defended)
        val body = game.fallen.single()
        assertEquals(EnemyKind.Police, body.kind)
        assertEquals(spot, body.position)
        advance(game, GameEngine.FALL_SECONDS + .1f)
        assertTrue(game.fallen.isEmpty())
    }

    @Test fun aKilledMarcherFallsOnce() {
        val game = game()
        game.people[5].health = 0
        advance(game, GameEngine.FALL_SECONDS / 2)
        val body = game.fallen.single()
        assertNull(body.kind)
        assertEquals(5, body.id)
        advance(game, GameEngine.FALL_SECONDS / 2 + .1f)
        assertTrue(game.fallen.isEmpty())
    }

    // As in the original: a bomb blast shakes the screen hardest, and the shake dies down within a second.
    @Test fun aBombBlastShakesTheScreenThenItSettles() {
        val game = game()
        val p = farSpot(game)
        repeat(4) { game.enemies += Enemy(it, EnemyKind.Thug, p + Vec2(it * 12f, 0f)).apply { warning = 0f } }
        game.throwBomb(p)
        while (game.bombs.isNotEmpty()) game.advance(1f / 60)
        assertEquals(9f, game.shake)
        advance(game, 1f)
        assertTrue(game.shake < .1f, "The screen still shakes by ${game.shake}")
    }

    // The last row starts beyond the hero's fire range, so nothing stops the thug.
    @Test fun aHitOnTheProtestShakesTheScreenALittle() {
        val game = game()
        val marcher = game.people.last()
        game.enemies += Enemy(902, EnemyKind.Thug, marcher.position + Vec2(5f, 0f)).apply { warning = 0f }
        assertEquals(0f, game.shake)
        game.advance(1f / 60)
        assertTrue(marcher.health < 3, "The thug did not hit")
        assertEquals(3f, game.shake)
    }

    @Test fun aWreckedCarShakesTheScreen() {
        val game = game()
        game.enemies += Enemy(903, EnemyKind.Car, spot(game, 120f, 0f)).apply { health = 0 }
        game.advance(1f / 60)
        assertEquals(5f, game.shake)
    }

    @Test fun aBeatenCarLeavesNoBody() {
        val game = game()
        game.enemies += Enemy(901, EnemyKind.Car, spot(game, 120f, 0f)).apply { health = 0 }
        advance(game, 1f / 60)
        assertTrue(game.enemies.isEmpty())
        assertTrue(game.fallen.isEmpty())
    }
}
