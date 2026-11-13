package dev.oblac.kolona.ui

import dev.oblac.gamekit.Audio
import dev.oblac.gamekit.MemoryStore
import dev.oblac.gamekit.Sound
import dev.oblac.gamekit.wav
import dev.oblac.kolona.game.CommandResult
import dev.oblac.kolona.game.Cue
import dev.oblac.kolona.game.GameConfig
import dev.oblac.kolona.game.Vec2
import kotlin.test.*

/** The sounds of the original web game: heard only while the sound is on. gamekit tests the tones. */
class SoundTest {
    /** Tells which cue each sound is by its WAV bytes, and keeps the cues it hears. */
    private class Ears : Audio {
        private val cues = Cue.entries.flatMap { cue -> cue.variants().map { wav(it).toList() to cue } }.toMap()
        val heard = mutableListOf<Cue>()
        override fun sound(wav: ByteArray): Sound {
            val cue = cues.getValue(wav.toList())
            return Sound { heard += cue }
        }
    }

    @Test fun theSoundIsOffUntilThePlayerTurnsItOnAndTheSwitchIsSaved() {
        val ears = Ears()
        val store = MemoryStore()
        val session = GameSession(store, audio = ears)
        assertFalse(session.sound)
        session.switchSound(true)
        assertTrue(session.sound)
        assertEquals("true", store.string("sound"))
        assertEquals(listOf(Cue.SoundOn), ears.heard)
        assertTrue(GameSession(store).sound)
        session.switchSound(false)
        assertEquals("false", store.string("sound"))
        assertEquals(listOf(Cue.SoundOn), ears.heard)
    }

    @Test fun aGameIsHeardOnlyWhileTheSoundIsOn() {
        for (on in listOf(true, false)) {
            val ears = Ears()
            val session = GameSession(MemoryStore().apply { put("sound", on.toString()) }, GameConfig(spawnEnemies = false), ears)
            session.start()
            assertEquals(CommandResult.Bomb, session.engine.throwBomb(session.engine.hero.position + Vec2(0f, -150f)))
            session.tick(1f / 60)
            assertEquals(if (on) listOf(Cue.Start, Cue.Bomb) else emptyList(), ears.heard, "Sound on: $on")
            assertTrue(session.engine.cues.isEmpty(), "The session keeps no cue it has heard")
        }
    }
}
