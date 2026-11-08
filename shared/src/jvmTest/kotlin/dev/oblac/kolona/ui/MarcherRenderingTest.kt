package dev.oblac.kolona.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.oblac.kolona.game.Vec2
import kotlin.test.Test
import kotlin.test.assertFalse

/** A stopped march still looks alive: every marcher sways and bobs, out of step with the others. */
class MarcherRenderingTest {
    @Test fun stoppedMarchersKeepMoving() {
        for (id in 0 until 12) assertFalse(render(id, 0f).contentEquals(render(id, .4f)), "marcher $id stands frozen while the march is stopped")
    }

    @Test fun marchersDoNotMoveInStep() {
        val time = 1.3f
        assertFalse((1 until 12).all { marcherWiggle(time, it) == marcherWiggle(time, 0) }, "every marcher sways the same way")
    }

    private fun render(id: Int, time: Float) = ImageBitmap(60, 90).also {
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(60f, 90f)) {
            person(Vec2(30f, 60f), Palette.Mint, id, step = 0f, sign = id == 0, wiggle = marcherWiggle(time, id))
        }
    }.toPixelMap().buffer
}
