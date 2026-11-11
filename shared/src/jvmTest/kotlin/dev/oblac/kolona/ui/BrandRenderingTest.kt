package dev.oblac.kolona.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** The brand's two lines are pinned to the icon's height, so a larger system text size must not change them. */
class BrandRenderingTest {
    @Test fun brandKeepsItsSizeWithLargerSystemText() {
        for (compact in listOf(false, true)) {
            assertTrue(render(compact, 1f).contentEquals(render(compact, 1.35f)),
                "compact = $compact: the brand changes with the system text size")
        }
    }

    // The brand in the header draws the raised fist of the app icon, so the two never drift apart.
    @Test fun brandDrawsTheFistOfTheAppIcon() {
        val icon = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "androidApp/src/main/res/drawable/ic_kolona.xml") }.first { it.exists() }.readText()
        assertTrue("android:fillColor=\"#F4CF71\" android:pathData=\"${Fist.PATH}\"" in icon, "ic_kolona.xml has another fist")
    }

    private fun render(compact: Boolean, fontScale: Float): IntArray {
        val scene = ImageComposeScene(600, 140, Density(2f, fontScale)) {
            KolonaTheme { Box(Modifier.padding(10.dp)) { Brand(compact) } }
        }
        var time = 0L
        repeat(4) { time += 100_000_000L; scene.render(time) }
        return scene.render(time).toComposeImageBitmap().toPixelMap().buffer.also { scene.close() }
    }
}
