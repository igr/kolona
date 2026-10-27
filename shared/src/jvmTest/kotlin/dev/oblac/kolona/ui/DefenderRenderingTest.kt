package dev.oblac.kolona.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** Render actual pixels so a canvas-center pivot regression cannot pass a geometry-only test. */
class DefenderRenderingTest {
    private val gunColor = Color(0xFFAAC1C1)

    @Test
    fun gunStaysAttachedAtEveryAimAngleAndCharacterPosition() {
        for (position in listOf(Offset(96f, 140f), Offset(350f, 500f))) {
            for (degrees in 0 until 360 step 45) {
                val image = render(512, 768) {
                    defender(position, barricade = 0f, angle = degrees * PI.toFloat() / 180f)
                }
                val gun = pixelsMatching(image, gunColor)
                assertTrue(gun.isNotEmpty(), "Gun disappeared at $degrees degrees, hero=$position")
                assertTrue(gun.all { (it - position).getDistance() < 36f },
                    "Gun detached from hero at $degrees degrees, hero=$position")
            }
        }
    }

    @Test
    fun cameraTranslationAndZoomKeepGunAndHighlightOnHero() {
        val worldHero = Offset(420f, 550f)
        val screenHero = Offset(180f, 225f)
        val zoom = 1.5f
        val image = render(480, 640) {
            withTransform({
                scale(zoom, zoom, Offset.Zero)
                translate(-300f, -400f)
            }) {
                defender(worldHero, barricade = 0f, angle = PI.toFloat() / 2f)
            }
        }
        val gun = pixelsMatching(image, gunColor)
        assertTrue(gun.isNotEmpty(), "Gun should remain visible after camera transforms")
        assertTrue(gun.all { (it - screenHero).getDistance() < 36f * zoom })

        val ring = pixelsMatching(image, Palette.Gold)
        assertTrue(ring.isNotEmpty(), "Selection ring should surround the hero")
        assertTrue(ring.all { (it - screenHero).getDistance() in 23f * zoom..27f * zoom })
        assertTrue(ring.any { it.x < screenHero.x - 20f * zoom })
        assertTrue(ring.any { it.x > screenHero.x + 20f * zoom })
    }

    private fun render(width: Int, height: Int, block: DrawScope.() -> Unit): ImageBitmap {
        val bitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap),
            Size(width.toFloat(), height.toFloat()), block)
        return bitmap
    }

    private fun pixelsMatching(image: ImageBitmap, color: Color): List<Offset> {
        val pixels = image.toPixelMap()
        return buildList {
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val pixel = pixels[x, y]
                if (pixel.alpha > .5f && abs(pixel.red - color.red) < .015f &&
                    abs(pixel.green - color.green) < .015f && abs(pixel.blue - color.blue) < .015f) {
                    add(Offset(x + .5f, y + .5f))
                }
            }
        }
    }
}
