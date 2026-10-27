package dev.oblac.kolona.game

import kotlin.math.abs
import kotlin.test.*

class GameEngineTest {
    private fun game() = GameEngine(seed = 7, config = GameConfig(spawnEnemies = false))
    private fun advance(game: GameEngine, seconds: Float) { repeat((seconds * 60).toInt()) { game.advance(1f / 60) } }
    private fun street(game: GameEngine, dx: Float, dy: Float) = game.map.nearestWalkable(game.hero.position + Vec2(dx, dy))
    // Outside the fire range, so only a bomb can stop enemies there.
    private fun farStreet(game: GameEngine) = street(game, 430f, -190f).also {
        assertTrue(it.distanceTo(game.hero.position) >= 300f, "$it is too close to the hero")
    }

    @Test fun startsWith72MarchersAndDeployedBarricade() {
        val game = game()
        assertEquals(72, game.survivors)
        assertFalse(game.hero.moving)
        assertEquals(1f, game.hero.barricade)
    }

    @Test fun pauseFreezesMovementCrowdAndCooldowns() {
        val game = game()
        game.command(street(game, 230f, -90f))
        advance(game, .25f)
        game.pause()
        val hero = game.hero.position
        val crowd = game.people.map { it.position }
        val elapsed = game.elapsed
        advance(game, 2f)
        assertEquals(hero, game.hero.position)
        assertEquals(crowd, game.people.map { it.position })
        assertEquals(elapsed, game.elapsed)
        assertEquals(CommandResult.Ignored, game.command(street(game, 280f, -140f)))
        game.resume()
        advance(game, .25f)
        assertTrue(game.elapsed > elapsed)
        assertNotEquals(hero, game.hero.position)
    }

    @Test fun movementDisablesFireAndStoppingDeploysBarricade() {
        val game = game()
        val target = Enemy(1, EnemyKind.Thug, game.hero.position + Vec2(40f, -80f)).apply { warning = 0f }
        game.enemies += target
        assertEquals(CommandResult.Move, game.command(street(game, 340f, -100f)))
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

    @Test fun bombsRequireFourEnemiesAndEnforceCooldownWithoutMovingHero() {
        val game = game()
        val p = farStreet(game)
        repeat(4) { game.enemies += Enemy(it, EnemyKind.Thug, p + Vec2(it * 12f, 0f)).apply { warning = 0f } }
        val original = game.hero.position
        assertEquals(CommandResult.Bomb, game.command(p))
        assertEquals(5f, game.bombCooldown)
        assertEquals(CommandResult.CoolingDown, game.command(p))
        assertEquals(1, game.bombs.size)
        assertEquals(original, game.hero.position)
        advance(game, .8f)
        assertEquals(4, game.defended)
        assertEquals(72, game.survivors)
        advance(game, 4.3f)
        assertEquals(0f, game.bombCooldown)
    }

    @Test fun threeEnemiesAreAMoveCommandAndWarningEnemiesDoNotQualify() {
        val game = game()
        val p = farStreet(game)
        repeat(3) { game.enemies += Enemy(it, EnemyKind.Thug, p).apply { warning = 0f } }
        game.enemies += Enemy(4, EnemyKind.Thug, p)
        assertEquals(CommandResult.Move, game.command(p))
        assertTrue(game.bombs.isEmpty())
        assertEquals(0f, game.bombCooldown)
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

    @Test fun enemyPushedIntoABlockWalksBackToTheStreet() {
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
        assertEquals(CommandResult.Ignored, game.command(street(game, 230f, -90f)))
    }

    @Test fun fixedTimestepMatchesAcrossDisplayRefreshRates() {
        val a = game()
        val b = game()
        val target = street(a, 280f, -90f)
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
}
