package dev.oblac.kolona.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.oblac.kolona.game.City
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** Stripe order and direction are what make a tricolour recognizable. */
class FlagRenderingTest {
    private val red = Color(0xFFC6363C)
    private val blue = Color(0xFF0C4076)
    private val frenchBlue = Color(0xFF0055A4)
    private val frenchRed = Color(0xFFEF4135)
    private val italianGreen = Color(0xFF009246)
    private val italianRed = Color(0xFFCE2B37)

    @Test fun serbiaIsRedBlueWhiteFromTheTop() {
        val pixels = render(City.Belgrade)
        assertColor(red, pixels[45, 10], "top")
        assertColor(blue, pixels[45, 30], "middle")
        assertColor(Color.White, pixels[45, 50], "bottom")
    }

    @Test fun franceIsBlueWhiteRedFromTheLeft() {
        val pixels = render(City.Paris)
        assertColor(frenchBlue, pixels[15, 30], "left")
        assertColor(Color.White, pixels[45, 30], "middle")
        assertColor(frenchRed, pixels[75, 30], "right")
    }

    @Test fun italyIsGreenWhiteRedFromTheLeft() {
        val pixels = render(City.Milan)
        assertColor(italianGreen, pixels[15, 30], "left")
        assertColor(Color.White, pixels[45, 30], "middle")
        assertColor(italianRed, pixels[75, 30], "right")
    }

    private fun render(city: City) = ImageBitmap(90, 60).also {
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(90f, 60f)) { flag(city) }
    }.toPixelMap()

    private fun assertColor(expected: Color, actual: Color, where: String) = assertTrue(
        abs(expected.red - actual.red) < .02f && abs(expected.green - actual.green) < .02f && abs(expected.blue - actual.blue) < .02f,
        "$where stripe: expected $expected, got $actual")
}
