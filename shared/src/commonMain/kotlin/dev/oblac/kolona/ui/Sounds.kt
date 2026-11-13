package dev.oblac.kolona.ui

import dev.oblac.gamekit.Audio
import dev.oblac.gamekit.Sound
import dev.oblac.gamekit.Tone
import dev.oblac.gamekit.Wave
import dev.oblac.gamekit.wav
import dev.oblac.kolona.game.Cue

/** The tones of each cue, with the pitch, length, volume, and wave of the original web game. Firing has five pitches from 120 to 170 Hz, and each shot plays one of them. */
internal fun Cue.variants(): List<List<Tone>> = when (this) {
    Cue.Start -> one(Tone(370f, .1f, .08f), Tone(520f, .15f, .07f, at = .1f))
    Cue.SoundOn -> one(Tone(440f, .1f, .04f, Wave.Sine))
    Cue.Move -> one(Tone(680f, .06f, .025f, Wave.Sine))
    Cue.Fire -> List(5) { listOf(Tone(120f + it * 12.5f, .045f, .02f)) }
    Cue.Bomb -> one(Tone(450f, .16f, .05f, Wave.Sine))
    Cue.Blast -> one(Tone(55f, .35f, .12f, Wave.Sawtooth))
    Cue.Beaten -> one(Tone(100f, .12f, .04f, Wave.Sawtooth))
    Cue.Wrecked -> one(Tone(65f, .12f, .04f, Wave.Sawtooth))
    Cue.Hit -> one(Tone(80f, .08f, .025f))
    Cue.Phase -> one(Tone(160f, .4f, .07f, Wave.Sawtooth))
    Cue.Speech -> one(Tone(500f, .2f, .04f, Wave.Sawtooth))
    Cue.Won -> one(Tone(730f, .5f, .07f, Wave.Sawtooth))
    Cue.Lost -> one(Tone(130f, .5f, .07f, Wave.Sawtooth))
}

private fun one(vararg tones: Tone) = listOf(tones.toList())

/** The sounds of every cue, ready to play on the platform's [audio]. */
internal class Sounds(audio: Audio) {
    private val sounds = Cue.entries.associateWith { cue -> cue.variants().map { audio.sound(wav(it)) } }
    fun play(cue: Cue) = sounds.getValue(cue).random().play()
}

/** No sound at all, for the tests. */
internal object Silence : Audio {
    override fun sound(wav: ByteArray) = Sound {}
}
