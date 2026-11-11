package dev.oblac.kolona.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.oblac.kolona.game.Fallen
import dev.oblac.kolona.game.GameConfig
import dev.oblac.kolona.game.GameEngine
import dev.oblac.kolona.game.Marcher
import dev.oblac.kolona.game.Supporters
import dev.oblac.kolona.game.Vec2
import dev.oblac.kolona.i18n.slogans
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A walking protester steps, sways, and bobs, out of step with the others; a stopped protest stands still. Protesters shout slogans. A beaten one falls down. */
class MarcherRenderingTest {
    @Test fun aWalkingProtesterStepsAndSways() {
        for (id in 0 until 12) assertFalse(render(id, 0f, stopped = false).contentEquals(render(id, .4f, stopped = false)), "protester $id stands frozen while the protest walks")
    }

    // While the protest stops for a speech, nobody walks in place: the legs, the body, and the sign hold still.
    @Test fun aStoppedProtestStandsStill() {
        for (id in 0 until 12) assertTrue(render(id, 0f, stopped = true).contentEquals(render(id, .4f, stopped = true)), "protester $id moves while the protest is stopped")
    }

    @Test fun marchersDoNotMoveInStep() {
        val time = 1.3f
        assertFalse((1 until 12).all { marcherWiggle(time, it) == marcherWiggle(time, 0) }, "every marcher sways the same way")
    }

    @Test fun aFallenMarcherLiesDown() {
        val (standingWidth, standingHeight) = extent(Fallen(Vec2(60f, 60f), 1, null))
        assertTrue(standingHeight > standingWidth, "a marcher hit this instant still stands")
        val (lyingWidth, lyingHeight) = extent(Fallen(Vec2(60f, 60f), 1, null).apply { age = .6f })
        assertTrue(lyingWidth > lyingHeight, "a fallen marcher is $lyingWidth wide and $lyingHeight tall, so it does not lie down")
    }

    // A new shout starts every SHOUT_EVERY seconds, so half a minute goes through every slogan, never the same one twice in a row.
    @Test fun theProtestShoutsChangingSlogans() {
        val shouted = (0 until (30 / SHOUT_EVERY).toInt()).map { shoutSlogan(it) }
        assertEquals(slogans.toSet(), shouted.toSet())
        assertTrue(shouted.zipWithNext().none { (a, b) -> a == b }, "$shouted")
    }

    @Test fun shoutsHaveOneToThreeExclamationMarks() {
        assertEquals(setOf(1, 2, 3), (0 until 30).map { shoutMarks(it) }.toSet())
    }

    // Two shouts overlap for a moment, so they come from opposite halves of the protest. A sign holder has both hands full.
    @Test fun shoutsComeFromDifferentProtesters() {
        val people = GameEngine(1, GameConfig(spawnEnemies = false)).people
        val speakers = (0 until 20).map { shoutSpeaker(people, it) }
        assertTrue(speakers.toSet().size >= 15, "$speakers")
        assertTrue(speakers.all { it >= 0 && !people[it].holdsSign }, "$speakers")
        assertTrue(speakers.zipWithNext().all { (a, b) -> (a < people.size / 2) != (b < people.size / 2) }, "$speakers")
    }

    @Test fun onlyAProtesterStillMarchingShouts() {
        val people = GameEngine(1, GameConfig(spawnEnemies = false)).people
        people.forEachIndexed { i, p -> if (i % 3 == 0) p.health = 0 else if (i % 3 == 1) p.arrived = true }
        for (slot in 0 until 100) {
            val speaker = shoutSpeaker(people, slot)
            assertTrue(speaker >= 0 && people[speaker].alive && !people[speaker].arrived, "slot $slot: $speaker")
        }
        people.forEach { it.health = 0 }
        assertEquals(-1, shoutSpeaker(people, 7))
    }

    // The bubble floats over the speaker's head, never on it, and fades out before the shout ends.
    @Test fun aShoutFloatsAboveTheSpeakerThenFades() {
        val measurer = TextMeasurer(createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr)
        fun bubble(age: Float) = ImageBitmap(200, 100).also {
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(200f, 100f)) {
                shout(measurer, "FREEDOM!!", Offset(100f, 80f), age, 3f)
            }
        }.toPixelMap()
        val loud = bubble(.6f)
        val drawn = (0 until 200).flatMap { x -> (0 until 100).filter { y -> loud[x, y].alpha > .5f }.map { y -> x to y } }
        assertTrue(drawn.isNotEmpty() && drawn.all { it.second <= 80 }, "the bubble covers the speaker's head")
        assertTrue(drawn.maxOf { it.first } - drawn.minOf { it.first } >= 40, "the bubble is too narrow for the text")
        val fading = bubble(SHOUT_SECONDS - .1f)
        assertTrue((0 until 200).all { x -> (0 until 100).all { y -> fading[x, y].alpha < .5f } }, "the shout does not fade out")
    }

    // Supporters at the side of the street hop and wave, each in their own rhythm.
    @Test fun supportersCheer() {
        val group = Supporters(Vec2(60f, 70f), (PI / 2).toFloat(), 4, banner = false)
        assertFalse(cheer(group, 0f).buffer.contentEquals(cheer(group, .3f).buffer), "the supporters stand frozen")
    }

    // When the protest comes by, they cheer harder. A protest far away changes nothing.
    @Test fun supportersCheerHarderAsTheProtestPasses() {
        val group = Supporters(Vec2(60f, 70f), (PI / 2).toFloat(), 4, banner = false)
        val alone = cheer(group, .3f).buffer
        assertTrue(alone.contentEquals(cheer(group, .3f, listOf(Marcher(0, 0, 0f, Vec2(60f, 900f)))).buffer), "a protest far away changed the supporters")
        assertFalse(alone.contentEquals(cheer(group, .3f, listOf(Marcher(0, 0, 0f, Vec2(60f, 130f)))).buffer), "the protest passed and nobody cheered harder")
    }

    // The two at the ends hold a banner over the heads, across most of the row.
    @Test fun aBannerGroupHoldsUpABanner() {
        val group = Supporters(Vec2(60f, 70f), (PI / 2).toFloat(), 4, banner = true)
        val with = cheer(group, .3f)
        val without = cheer(group.copy(banner = false), .3f)
        val columns = (0 until 120).count { x -> (32 until 48).any { y -> with[x, y] != without[x, y] } }
        assertTrue(columns >= group.width * .7f, "the banner spans $columns of ${group.width} columns over the heads")
    }

    // People at the tables move a little, each in their own rhythm, and raise an arm when the protest comes by.
    @Test fun peopleAtTheTablesMove() {
        fun sit(time: Float, cheering: Boolean = false) = ImageBitmap(40, 40).also {
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(40f, 40f)) { sitter(Offset(20f, 30f), Palette.Mint, Palette.Cream, time, 0f, cheering) }
        }.toPixelMap().buffer
        assertFalse(sit(0f).contentEquals(sit(.5f)), "a seated person sits frozen")
        assertFalse(sit(.5f).contentEquals(sit(.5f, cheering = true)), "a seated person does not cheer the protest")
    }

    private fun cheer(group: Supporters, time: Float, people: List<Marcher> = listOf()) = ImageBitmap(120, 100).also {
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(120f, 100f)) { supporters(group, time, people) }
    }.toPixelMap()

    /** The width and height of everything a fallen body draws. */
    private fun extent(body: Fallen): Pair<Int, Int> {
        val image = ImageBitmap(120, 120).also {
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(120f, 120f)) { fallen(body) }
        }.toPixelMap()
        val drawn = (0 until 120).flatMap { x -> (0 until 120).filter { y -> image[x, y].alpha > .5f }.map { y -> x to y } }
        return (drawn.maxOf { it.first } - drawn.minOf { it.first }) to (drawn.maxOf { it.second } - drawn.minOf { it.second })
    }

    private fun render(id: Int, time: Float, stopped: Boolean) = ImageBitmap(60, 90).also {
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(60f, 90f)) {
            marcher(Marcher(id, 0, 0f, Vec2(30f, 60f)), time, stopped)
        }
    }.toPixelMap().buffer
}
