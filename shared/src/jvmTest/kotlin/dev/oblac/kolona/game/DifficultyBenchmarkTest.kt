package dev.oblac.kolona.game

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

const val BASELINE_MEAN_SURVIVORS = 59.6f
const val BASELINE_WIN_RATE = 1f
val BENCHMARK_SEEDS = 1..10
// The baseline was measured on BENCHMARK_SEEDS. A 10-seed mean has an SE of about 3 survivors,
// which is too close to the ±7 bands, so the band test uses 40 seeds.
val BAND_SEEDS = 1..40

private const val MAX_STEPS = 200 * 60
private const val STEPS_PER_DECISION = 30
private const val STANDOFF = 90f
// 120 made the bot win every seed with about 69 survivors, which cannot tell difficulties apart.
private const val REPOSITION_DISTANCE = 350f

/** Bombs any group of four, otherwise holds a post between the crowd and the nearest threat. */
internal fun playScripted(game: GameEngine): Int {
    var step = 0
    while (game.status == GameStatus.Playing && step < MAX_STEPS) {
        if (step % STEPS_PER_DECISION == 0) decide(game)
        game.advance(1f / 60)
        step++
    }
    return game.survivors
}

internal fun survivorsBySeed(seeds: IntRange, game: (Int) -> GameEngine): List<Int> =
    seeds.map { playScripted(game(it)) }

private fun decide(game: GameEngine) {
    val active = game.enemies.filter { it.health > 0 && it.warning <= 0f }
    val bombTarget = active.firstOrNull { enemy -> active.count { it.position.distanceTo(enemy.position) <= GameEngine.BOMB_RADIUS } >= 4 }
    if (game.bombCooldown <= 0f && bombTarget != null) {
        game.command(bombTarget.position)
        return
    }
    val crowd = game.crowdCenter
    val nearest = active.minByOrNull { it.position.distanceTo(crowd) }
    val target = nearest?.let { crowd + (it.position - crowd).normalized() * STANDOFF } ?: crowd
    if (!game.hero.moving && game.hero.position.distanceTo(target) > REPOSITION_DISTANCE) game.command(target)
}

class DifficultyBenchmarkTest {
    @Test fun difficultyMatchesTodayInEveryCityAndScale() {
        val config = GameConfig()
        // playScripted stops at MAX_STEPS. A longer game would be scored in the middle of the march.
        assertTrue(config.marchSeconds + config.speechSeconds * config.speechesAt.size < MAX_STEPS / 60f, "The game must end before the bot's cap")
        val band = .12f * BASELINE_MEAN_SURVIVORS
        val scales = listOf(1f, config.maxMapScale)
        println("baseline: mean=$BASELINE_MEAN_SURVIVORS winRate=$BASELINE_WIN_RATE band=±$band")
        val failures = mutableListOf<String>()
        for (scale in scales) {
            val means = City.entries.map { city ->
                val survivors = survivorsBySeed(BAND_SEEDS) { GameEngine(it, config, generateMap(city, it, scale)) }
                val mean = survivors.average().toFloat()
                val winRate = survivors.count { it >= GameEngine.MIN_SURVIVORS } / survivors.size.toFloat()
                println("$city ${scale}x: mean=$mean winRate=$winRate survivors=$survivors")
                if (abs(mean - BASELINE_MEAN_SURVIVORS) > band) failures += "$city ${scale}x: mean $mean is outside $BASELINE_MEAN_SURVIVORS ± $band"
                mean
            }
            val gap = means.max() - means.min()
            println("${scale}x: city gap=$gap")
            if (gap > band) failures += "${scale}x: the cities differ by $gap, more than $band"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
