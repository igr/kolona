package dev.oblac.kolona.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import dev.oblac.kolona.data.MemoryPreferenceStore
import dev.oblac.kolona.game.City
import dev.oblac.kolona.game.GameConfig
import dev.oblac.kolona.game.GameStatus
import dev.oblac.kolona.i18n.Language
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Every game starts with the city dialog; taps go to the real screens. */
@OptIn(ExperimentalComposeUiApi::class)
class CityFlowTest {
    private var time = 0L

    private fun ImageComposeScene.frame(): Image { repeat(4) { time += 100_000_000L; render(time) }; return render(time) }

    private fun ImageComposeScene.tap(x: Float, y: Float) {
        sendPointerEvent(PointerEventType.Press, Offset(x, y), type = PointerType.Touch)
        sendPointerEvent(PointerEventType.Release, Offset(x, y), type = PointerType.Touch)
    }

    /** The rows of the gold primary button on the vertical centre line, below the header. */
    private fun primaryButtonRows(image: Image): List<Int> {
        val bitmap = Bitmap.makeFromImage(image)
        val gold = Palette.Gold.toArgb()
        val rows = (300 until image.height).filter { y ->
            val c = bitmap.getColor(image.width / 2, y)
            listOf(16, 8, 0).all { abs(((c shr it) and 0xFF) - ((gold shr it) and 0xFF)) < 6 }
        }
        return assertNotNull(rows.takeIf { it.isNotEmpty() }, "No primary button")
    }

    private fun primaryButtonY(image: Image) = primaryButtonRows(image).let { (it.first() + it.last()) / 2f }

    @Test fun enterTheCityAsksForTheCityThenStartsTheGame() {
        for (width in listOf(430, 360)) {
            val session = GameSession(MemoryPreferenceStore())
            val scene = ImageComposeScene(width * 2, 870 * 2, Density(2f)) { KolonaApp(session) }
            scene.tap(width.toFloat(), primaryButtonY(scene.frame()))
            val dialog = scene.frame()
            assertEquals(Screen.Welcome, session.screen, "Enter the city must open the city dialog")
            scene.tap(width.toFloat(), primaryButtonY(dialog))
            scene.frame()
            assertEquals(Screen.Game, session.screen)
            scene.close()
        }
    }

    // Every city card, the language box, and the button fit a 360 × 700 dp phone in every language.
    // The button is 55 dp tall; a dialog that has to scroll cuts it at its lower edge. Only the dialog's button starts the game.
    @Test fun cityDialogFitsASmallPhoneWithoutScrolling() {
        for (language in Language.entries) {
            val session = GameSession(MemoryPreferenceStore()).apply { select(language) }
            val scene = ImageComposeScene(360 * 2, 700 * 2, Density(2f)) { KolonaApp(session) }
            scene.tap(360f, primaryButtonY(scene.frame()))
            val rows = primaryButtonRows(scene.frame())
            assertTrue(rows.last() - rows.first() + 1 >= 108, "$language: the dialog's button shows only rows ${rows.first()}..${rows.last()}")
            scene.tap(360f, (rows.first() + rows.last()) / 2f)
            scene.frame()
            assertEquals(Screen.Game, session.screen, "$language: the button in the city dialog must start the game")
            scene.close()
        }
    }

    // The souvenir is a reward: a lost game shows none, and a won one shows it in the dialog,
    // which still fits a 360 × 700 dp phone with both buttons whole.
    @Test fun onlyAWinShowsTheCitySouvenir() {
        for (city in City.entries) for ((won, width, height) in listOf(Triple(true, 430, 870), Triple(true, 360, 870), Triple(true, 360, 700), Triple(false, 430, 870))) {
            val session = GameSession(MemoryPreferenceStore(), GameConfig(spawnEnemies = false))
            session.selectCity(city)
            session.start()
            if (!won) session.engine.people.take(37).forEach { it.health = 0 }
            repeat(3000) { if (session.engine.status == GameStatus.Playing) session.tick(.1f) }
            assertEquals(if (won) GameStatus.Won else GameStatus.Lost, session.engine.status)
            val scene = ImageComposeScene(width * 2, height * 2, Density(2f)) { KolonaApp(session) }
            val pixels = scene.frame().toComposeImageBitmap().toPixelMap()
            scene.close()
            val hits = (0 until pixels.width).sumOf { x -> (0 until pixels.height).count { y -> close(pixels[x, y], city.souvenirColor) } }
            if (won) assertTrue(hits >= 200, "$city at $width × $height dp: the win dialog shows $hits souvenir pixels")
            else assertEquals(0, hits, "$city: a lost game shows the souvenir")
            // Each button is 55 dp tall; a dialog that has to scroll cuts the lower one.
            if (won) for ((name, color) in listOf("primary" to Palette.Gold, "secondary" to Palette.Panel)) {
                val band = band(pixels, color)
                assertTrue(band >= 100, "$city at $width × $height dp: the $name button shows only $band px")
            }
        }
    }

    private fun close(a: Color, b: Color) = abs(a.red - b.red) < .02f && abs(a.green - b.green) < .02f && abs(a.blue - b.blue) < .02f

    /** The longest run of rows in [color] down the column at 65% of the width, clear of the button labels and arrows. */
    private fun band(pixels: PixelMap, color: Color): Int {
        val x = (pixels.width * .65f).toInt()
        var best = 0
        var run = 0
        for (y in 0 until pixels.height) {
            run = if (close(pixels[x, y], color)) run + 1 else 0
            best = maxOf(best, run)
        }
        return best
    }

    @Test fun defendTheMarchOnTheMenuAsksForTheCityFirst() {
        val session = GameSession(MemoryPreferenceStore()).apply { screen = Screen.Menu }
        val scene = ImageComposeScene(720, 1740, Density(2f)) { KolonaApp(session) }
        scene.tap(360f, primaryButtonY(scene.frame()))
        scene.frame()
        assertEquals(Screen.Menu, session.screen, "Defend the march must open the city dialog")
        session.selectCity(City.Paris)
        scene.tap(360f, primaryButtonY(scene.frame()))
        scene.frame()
        assertEquals(Screen.Game, session.screen)
        assertEquals(City.Paris, session.engine.map.city)
        scene.close()
    }
}
