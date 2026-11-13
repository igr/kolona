package dev.oblac.kolona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.oblac.gamekit.MemoryStore
import dev.oblac.kolona.game.GameConfig
import dev.oblac.kolona.i18n.Language
import dev.oblac.kolona.i18n.Strings
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The bomb button under the map: while the bomb reloads, a gold bar fills its dark track. */
@OptIn(ExperimentalComposeUiApi::class)
class BombButtonTest {
    private fun render(reload: Float, language: Language = Language.English): PixelMap {
        val scene = ImageComposeScene(600, 240, Density(2f)) {
            Box(Modifier.background(Palette.Ink).padding(10.dp)) { BombButton(reload, Strings(language)) }
        }
        scene.render(0L)
        return scene.render(100_000_000L).toComposeImageBitmap().toPixelMap().also { scene.close() }
    }

    private fun close(a: Color, b: Color) = abs(a.red - b.red) < .02f && abs(a.green - b.green) < .02f && abs(a.blue - b.blue) < .02f

    /** The gold share of the bar, on the row with the most pixels in the bar's two colours. */
    private fun filled(pixels: PixelMap): Float {
        val (gold, track) = (0 until pixels.height).map { y ->
            (0 until pixels.width).count { close(pixels[it, y], Palette.Gold) } to (0 until pixels.width).count { close(pixels[it, y], Palette.Deep) }
        }.maxBy { it.first + it.second }
        assertTrue(gold + track > 40, "No bar: $gold gold and $track track pixels on its longest row")
        return gold.toFloat() / (gold + track)
    }

    @Test fun theBarFillsUpAsTheBombReloads() {
        for (reload in listOf(0f, .25f, .5f, .75f)) assertEquals(reload, filled(render(reload)), .06f, "Reload $reload")
    }

    /** The size of the button, in dp. */
    private fun size(reload: Float, language: Language): IntSize {
        var size = IntSize.Zero
        val scene = ImageComposeScene(600, 240, Density(2f)) { BombButton(reload, Strings(language), Modifier.onSizeChanged { size = it }) }
        scene.render(0L)
        scene.close()
        return IntSize(size.width / 2, size.height / 2)
    }

    // The bomb button floats over the map: the map reaches the bottom of the game screen, with no stripe under it.
    @Test fun theMapReachesTheBottomOfTheGameScreen() {
        val session = GameSession(MemoryStore(), GameConfig(spawnEnemies = false)).apply { start() }
        val scene = ImageComposeScene(402 * 2, 874 * 2, Density(2f)) { KolonaApp(session) }
        repeat(4) { scene.render(it * 16_000_000L) }
        val pixels = scene.render(64_000_000L).toComposeImageBitmap().toPixelMap()
        scene.close()
        val bottom = (pixels.height - 40 until pixels.height).flatMap { y -> (0 until pixels.width).map { x -> pixels[x, y] } }
        val dark = bottom.count { close(it, Palette.Ink) }
        assertTrue(dark < bottom.size / 5, "$dark of the ${bottom.size} pixels in the lowest 20 dp are the dark background")
    }

    // At 2 px per dp: the button with its 12 dp margin fills the lower-left corner, and a marker is a circle of 12 dp.
    @Test fun anEdgeMarkerStaysClearOfTheBombButton() {
        val view = Size(800f, 1500f)
        val corner = Size(170f, 180f)
        val lowerLeft = markerSpot(Offset(-300f, 1900f), view, corner, 2f)
        assertEquals(40f, lowerLeft.x)
        assertTrue(lowerLeft.y + 24f <= view.height - corner.height + 24f, "The marker at $lowerLeft covers the bomb button")
        assertEquals(Offset(600f, 1428f), markerSpot(Offset(600f, 1900f), view, corner, 2f))
        assertEquals(Offset(40f, 500f), markerSpot(Offset(-300f, 500f), view, corner, 2f))
    }

    // The weapon's name sits under its icon, so the button stays narrow in every language. It keeps its size while the bomb reloads, so the map above never moves.
    @Test fun theButtonIsNarrowAndKeepsItsSize() {
        for (language in Language.entries) {
            val ready = size(1f, language)
            assertTrue(ready.width <= 80, "$language: the button is ${ready.width} dp wide")
            assertEquals(ready, size(.5f, language), "$language: the button changes its size while the bomb reloads")
        }
    }
}
