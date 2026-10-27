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

/** The postcard is the city's food: its colour fills the middle of the cream card. */
class SouvenirRenderingTest {
    @Test fun eachCityItemFillsTheMiddleOfItsCard() {
        for (city in City.entries) {
            val pixels = ImageBitmap(W, H).also {
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(W.toFloat(), H.toFloat())) { souvenir(city) }
            }.toPixelMap()
            val middle = (-5..5).flatMap { dx -> (-5..5).map { dy -> pixels[W / 2 + dx, H / 2 + dy] } }
            assertTrue(middle.count { close(it, city.souvenirColor) } * 2 >= middle.size, "$city: the middle is ${pixels[W / 2, H / 2]}")
            City.entries.filter { it != city }.forEach { other ->
                assertTrue(middle.none { close(it, other.souvenirColor) }, "$city: the middle has the colour of $other")
            }
            assertTrue(close(pixels[12, H / 2], postcard), "$city: the card's left edge is ${pixels[12, H / 2]}")
        }
    }

    private fun close(a: Color, b: Color) = abs(a.red - b.red) < .03f && abs(a.green - b.green) < .03f && abs(a.blue - b.blue) < .03f

    private companion object { const val W = 240; const val H = 160 }
}
