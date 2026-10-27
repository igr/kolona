package dev.oblac.kolona.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.oblac.kolona.i18n.Language
import dev.oblac.kolona.i18n.Strings
import kotlin.test.Test
import kotlin.test.assertTrue

/** The brand's two lines are pinned to the icon's height, so a larger system text size must not change them. */
class BrandRenderingTest {
    @Test fun brandKeepsItsSizeWithLargerSystemText() {
        for (language in Language.entries) for (compact in listOf(false, true)) {
            assertTrue(render(language, compact, 1f).contentEquals(render(language, compact, 1.35f)),
                "$language, compact = $compact: the brand changes with the system text size")
        }
    }

    private fun render(language: Language, compact: Boolean, fontScale: Float): IntArray {
        val scene = ImageComposeScene(600, 140, Density(2f, fontScale)) {
            KolonaTheme { Box(Modifier.padding(10.dp)) { Brand(Strings(language), compact) } }
        }
        var time = 0L
        repeat(4) { time += 100_000_000L; scene.render(time) }
        return scene.render(time).toComposeImageBitmap().toPixelMap().buffer.also { scene.close() }
    }
}
