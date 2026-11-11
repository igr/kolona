package dev.oblac.kolona.ui

import dev.oblac.kolona.game.CommandResult
import dev.oblac.kolona.game.Enemy
import dev.oblac.kolona.game.EnemyKind
import dev.oblac.kolona.game.GameConfig
import dev.oblac.kolona.game.GameEngine
import dev.oblac.kolona.game.Vec2
import kotlin.test.*

/** A swipe from the defender aims a bomb at the finger, and letting go throws it there. */
class BombSwipeTest {
    private val game = GameEngine(1, GameConfig(spawnEnemies = false))
    private val hero = game.hero.position
    private val swipe = BombSwipe()
    private fun group(at: Vec2) = repeat(4) { game.enemies += Enemy(it, EnemyKind.Thug, at + Vec2(it * 12f, 0f)).apply { warning = 0f } }

    @Test fun aSwipeFromTheDefenderThrowsABombWhereItEnds() {
        group(hero + Vec2(150f, -60f))
        swipe.start(game, hero + Vec2(12f, -20f))
        swipe.move(hero + Vec2(150f, -60f))
        assertEquals(hero + Vec2(150f, -60f), swipe.aim)
        assertEquals(CommandResult.Bomb, swipe.end(game))
        assertEquals(hero + Vec2(150f, -60f), game.bombs.single().target)
        assertNull(swipe.aim)
    }

    @Test fun aSwipeThatStartsAwayFromTheDefenderThrowsNothing() {
        swipe.start(game, hero + Vec2(160f, 0f))
        swipe.move(hero + Vec2(300f, 0f))
        assertNull(swipe.aim)
        assertEquals(CommandResult.Ignored, swipe.end(game))
        assertTrue(game.bombs.isEmpty())
    }

    // A slip of the finger on the defender is not a throw.
    @Test fun aShortSwipeThrowsNothing() {
        swipe.start(game, hero)
        swipe.move(hero + Vec2(25f, 0f))
        assertEquals(CommandResult.Ignored, swipe.end(game))
        assertTrue(game.bombs.isEmpty())
    }

    @Test fun aSwipeWhileTheBombRechargesThrowsNothing() {
        group(hero + Vec2(0f, 200f))
        repeat(2) {
            swipe.start(game, hero)
            swipe.move(hero + Vec2(0f, 200f))
            swipe.end(game)
        }
        assertEquals(1, game.bombs.size)
        assertTrue(game.bombCooldown > 0f)
    }

    @Test fun aCancelledSwipeThrowsNothing() {
        swipe.start(game, hero)
        swipe.move(hero + Vec2(0f, 200f))
        swipe.cancel()
        assertNull(swipe.aim)
        assertEquals(CommandResult.Ignored, swipe.end(game))
        assertTrue(game.bombs.isEmpty())
    }
}
