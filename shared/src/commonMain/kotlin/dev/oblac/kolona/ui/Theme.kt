package dev.oblac.kolona.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import dev.oblac.kolona.resources.Res
import dev.oblac.kolona.resources.barlow_condensed_bold
import dev.oblac.kolona.resources.dm_sans

object Palette {
    val Ink = Color(0xFF0D1D27)
    val Deep = Color(0xFF09151D)
    val Panel = Color(0xFF152C36)
    val Line = Color(0xFF304A53)
    val Gold = Color(0xFFF4CF71)
    val Cream = Color(0xFFF0EFE2)
    val Mint = Color(0xFFA8D8BD)
    val Muted = Color(0xFF91A8AD)
    val Faint = Color(0xFF5A747D)
    val Red = Color(0xFFE18A78)
    val Ground = Color(0xFF23414A)
}

val LocalDisplayFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

@Composable
fun KolonaTheme(content: @Composable () -> Unit) {
    val body = FontFamily(Font(Res.font.dm_sans))
    val display = FontFamily(Font(Res.font.barlow_condensed_bold, FontWeight.Bold))
    CompositionLocalProvider(LocalDisplayFont provides display) {
        MaterialTheme(
            colors = darkColors(
                primary = Palette.Gold, secondary = Palette.Mint,
                background = Palette.Ink, surface = Palette.Panel,
                onPrimary = Palette.Ink, onSurface = Palette.Cream,
                onBackground = Palette.Cream, error = Palette.Red,
            ),
            typography = Typography(
                defaultFontFamily = body,
                body1 = TextStyle(fontFamily = body, fontSize = 14.sp, lineHeight = 21.sp),
                body2 = TextStyle(fontFamily = body, fontSize = 12.sp, lineHeight = 18.sp),
                button = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp),
                caption = TextStyle(fontFamily = body, fontSize = 10.sp, letterSpacing = 1.sp),
            ), content = content,
        )
    }
}
