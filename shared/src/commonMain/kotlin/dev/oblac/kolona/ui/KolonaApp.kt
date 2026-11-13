package dev.oblac.kolona.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.oblac.kolona.game.City
import dev.oblac.kolona.game.template
import dev.oblac.kolona.i18n.*
import kotlin.math.roundToInt

@Composable
fun KolonaApp(session: GameSession) {
    KolonaTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Deep).safeDrawingPadding(), contentAlignment = Alignment.Center) {
            // Also preserve a portrait playfield on tablets, desktop, and windowed Android.
            val portraitWidth = minOf(maxWidth, maxHeight * .62f, 520.dp)
            Box(Modifier.width(portraitWidth).fillMaxHeight().background(Palette.Ink)) {
                val strings = remember(session.language) { Strings(session.language) }
                when (session.screen) {
                    Screen.Welcome -> WelcomeScreen(session, strings)
                    Screen.Menu -> MenuScreen(session, strings)
                    Screen.Guide -> GuideScreen(strings) { session.screen = Screen.Menu }
                    Screen.Game -> GameScreen(session, strings)
                }
            }
        }
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = Palette.Mint, modifier: Modifier = Modifier) {
    Text(text, modifier, color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
}

@Composable
internal fun Display(text: String, size: Int = 46, color: Color = Palette.Cream, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(text, modifier, color, fontFamily = LocalDisplayFont.current, fontWeight = FontWeight.Bold,
        fontSize = size.sp, lineHeight = (size * .98f).sp, textAlign = align)
}

internal enum class Symbol { Arrow, Back, Shield, Guide, Globe, Pin, Pause, Check, Close, Bomb, Mail, Speaker, Muted }

@Composable
internal fun SymbolIcon(symbol: Symbol, modifier: Modifier = Modifier.size(22.dp), color: Color = Palette.Gold) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        withTransform({ scale(s, s, Offset.Zero) }) {
            fun line(a: Offset, b: Offset) = drawLine(color, a, b, 1.7f, StrokeCap.Round)
            when (symbol) {
                Symbol.Arrow -> { line(Offset(4f, 12f), Offset(20f, 12f)); line(Offset(14f, 6f), Offset(20f, 12f)); line(Offset(20f, 12f), Offset(14f, 18f)) }
                Symbol.Back -> { line(Offset(20f, 12f), Offset(4f, 12f)); line(Offset(10f, 6f), Offset(4f, 12f)); line(Offset(4f, 12f), Offset(10f, 18f)) }
                Symbol.Shield -> {
                    val path = Path().apply { moveTo(12f, 2f); lineTo(21f, 6f); lineTo(20f, 14f); quadraticTo(18f, 20f, 12f, 23f); quadraticTo(6f, 20f, 4f, 14f); lineTo(3f, 6f); close() }
                    drawPath(path, color, style = Stroke(1.5f)); line(Offset(8f, 12f), Offset(11f, 15f)); line(Offset(11f, 15f), Offset(16f, 9f))
                }
                Symbol.Guide -> { drawCircle(color, 9f, Offset(12f, 12f), style = Stroke(1.5f)); line(Offset(12f, 11f), Offset(12f, 17f)); drawCircle(color, 1.1f, Offset(12f, 7f)) }
                Symbol.Globe -> {
                    drawCircle(color, 9f, Offset(12f, 12f), style = Stroke(1.5f))
                    drawOval(color, Offset(8f, 3f), androidx.compose.ui.geometry.Size(8f, 18f), style = Stroke(1.3f))
                    line(Offset(3f, 12f), Offset(21f, 12f)); line(Offset(5f, 7f), Offset(19f, 7f)); line(Offset(5f, 17f), Offset(19f, 17f))
                }
                Symbol.Pin -> {
                    val path = Path().apply {
                        moveTo(12f, 22f); cubicTo(8f, 17.5f, 5f, 13.5f, 5f, 9.5f)
                        arcTo(androidx.compose.ui.geometry.Rect(5f, 2.5f, 19f, 16.5f), 180f, 180f, false)
                        cubicTo(19f, 13.5f, 16f, 17.5f, 12f, 22f); close()
                    }
                    drawPath(path, color, style = Stroke(1.5f)); drawCircle(color, 2.5f, Offset(12f, 9.5f), style = Stroke(1.5f))
                }
                Symbol.Pause -> { round(6f, 4f, 4f, 16f, color, 1f); round(14f, 4f, 4f, 16f, color, 1f) }
                Symbol.Check -> { line(Offset(5f, 12f), Offset(10f, 17f)); line(Offset(10f, 17f), Offset(20f, 6f)) }
                Symbol.Close -> { line(Offset(6f, 6f), Offset(18f, 18f)); line(Offset(18f, 6f), Offset(6f, 18f)) }
                Symbol.Bomb -> { drawCircle(color, 7f, Offset(10f, 15f), style = Stroke(1.8f)); line(Offset(15f, 9f), Offset(18f, 6f)); line(Offset(18f, 6f), Offset(21f, 7f)); line(Offset(18f, 2f), Offset(18f, 3f)); line(Offset(22f, 3f), Offset(21f, 4f)) }
                Symbol.Mail -> { drawRoundRect(color, Offset(3f, 5.5f), Size(18f, 13f), CornerRadius(2f), style = Stroke(1.5f)); line(Offset(4.5f, 7.5f), Offset(12f, 13f)); line(Offset(12f, 13f), Offset(19.5f, 7.5f)) }
                Symbol.Speaker, Symbol.Muted -> {
                    val body = Path().apply { moveTo(3.5f, 9.5f); lineTo(7f, 9.5f); lineTo(11.5f, 5.5f); lineTo(11.5f, 18.5f); lineTo(7f, 14.5f); lineTo(3.5f, 14.5f); close() }
                    drawPath(body, color, style = Stroke(1.5f))
                    if (symbol == Symbol.Speaker) {
                        drawArc(color, -50f, 100f, false, Offset(8f, 8f), Size(8f, 8f), style = Stroke(1.5f, cap = StrokeCap.Round))
                        drawArc(color, -50f, 100f, false, Offset(4.5f, 4.5f), Size(15f, 15f), style = Stroke(1.5f, cap = StrokeCap.Round))
                    } else { line(Offset(15.5f, 9.5f), Offset(20.5f, 14.5f)); line(Offset(20.5f, 9.5f), Offset(15.5f, 14.5f)) }
                }
            }
        }
    }
}

/** The country's tricolour: Serbia's civil flag runs top to bottom, France's and Italy's left to right. */
internal fun DrawScope.flag(city: City, area: Size = size) {
    val (stripes, vertical) = when (city) {
        City.Belgrade -> listOf(Color(0xFFC6363C), Color(0xFF0C4076), Color.White) to false
        City.Paris -> listOf(Color(0xFF0055A4), Color.White, Color(0xFFEF4135)) to true
        City.Milan -> listOf(Color(0xFF009246), Color.White, Color(0xFFCE2B37)) to true
    }
    stripes.forEachIndexed { i, color ->
        if (vertical) drawRect(color, Offset(area.width * i / 3, 0f), Size(area.width / 3, area.height))
        else drawRect(color, Offset(0f, area.height * i / 3), Size(area.width, area.height / 3))
    }
}

// The raised fist of the app icon (androidApp's ic_kolona.xml), in its 108-unit square.
internal object Fist {
    const val PATH = "M41.23,19.48A4.48,4.48 0 0,1 47.07,17L50.3,18.31A4.48,4.48 0 0,1 52.78,24.14L47.07,38.28A1.2,1.2 0 0,1 45.51,38.94L36.18,35.17A1.2,1.2 0 0,1 35.52,33.61ZM55.56,22.58A4.48,4.48 0 0,1 61.4,20.1L65.09,21.6A4.48,4.48 0 0,1 67.57,27.43L60.92,43.88A1.2,1.2 0 0,1 59.37,44.54L49.57,40.58A1.2,1.2 0 0,1 48.91,39.03ZM68.86,29.57A4.48,4.48 0 0,1 74.69,27.09L77.47,28.21A4.48,4.48 0 0,1 79.94,34.05L72.11,53.45A3.49,3.49 0 0,1 67.56,55.37L62.95,53.51A3.49,3.49 0 0,1 61.02,48.97ZM80.3,38.49A4.48,4.48 0 0,1 86.14,36.01L87.52,36.57A4.48,4.48 0 0,1 90,42.41L83.65,58.11A3.49,3.49 0 0,1 79.11,60.04L75.88,58.73A3.49,3.49 0 0,1 73.95,54.19ZM30.15,33.6L54.65,44.78A5.48,5.48 0 0,1 57.68,51.92L56.56,54.69A5.48,5.48 0 0,1 49.42,57.72L42.03,54.73C48.13,58.27 51.8,65.13 51.41,71.42C53.65,66.95 56.35,60.52 58.41,55.44A1.49,1.49 0 0,1 60.35,54.61L78.83,62.08A2.99,2.99 0 0,1 80.48,65.97L64.53,89.21C63.29,91.02 62.27,94.29 62.27,96.48L62.27,110L32.38,110L32.38,78.05C32.38,75.86 31.51,72.5 30.45,70.58L20.11,51.79C18,47.71 18.38,45.18 19.5,42.4L22.48,36.09C23.97,32.93 26.46,32.1 30.15,33.6ZM44.83,83.19C47.32,85.68 49.02,90.17 49.61,94.65C50.21,90.17 51.91,86.18 54.3,83.99C51.31,82.19 47.82,82.19 44.83,83.19Z"
}
private val fistPath by lazy { PathParser().parsePathString(Fist.PATH).toPath() }

@Composable
internal fun Brand(compact: Boolean = false) {
    val icon = if (compact) 28.dp else 40.dp
    val subtitle = if (compact) 7.sp else 9.sp
    // The wordmark is a logo pinned to the icon's height, so the system text size does not scale it.
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)) {
            // The app icon's fist in ink on gold, a bit larger than in the icon so it reads at this size. The arm runs off the bottom edge.
            Canvas(Modifier.size(icon).clip(RoundedCornerShape(10.dp)).background(Palette.Gold)) {
                val s = size.width / 96f
                withTransform({ translate(size.width / 2 - 54f * s, size.height / 2 - 58f * s); scale(s, s, Offset.Zero) }) {
                    drawPath(fistPath, Palette.Ink)
                }
            }
            // The subtitle's capitals start at the icon's top edge and the wordmark stands on its bottom edge.
            Layout({
                Text(GameIdentity.SUBTITLE, color = Palette.Gold, fontSize = subtitle, letterSpacing = if (compact) 2.6.sp else 3.3.sp, fontWeight = FontWeight.Bold)
                Display(GameIdentity.WORDMARK, if (compact) 29 else 42)
            }, Modifier.height(icon)) { measurables, constraints ->
                val (top, word) = measurables.map { it.measure(Constraints()) }
                layout(maxOf(top.width, word.width), constraints.maxHeight) {
                    top.place(0, (subtitle.toPx() * .7f).roundToInt() - top[FirstBaseline])
                    word.place(0, constraints.maxHeight - word[FirstBaseline])
                }
            }
        }
    }
}

@Composable
internal fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, symbol: Symbol? = Symbol.Arrow) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 55.dp).clip(shape)
            .background(if (primary) if (enabled) Palette.Gold else Palette.Faint else Palette.Panel)
            .border(1.dp, if (primary) Color.Transparent else Palette.Line, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, Modifier.weight(1f), if (primary) Palette.Ink else Palette.Cream,
            fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        if (symbol != null) SymbolIcon(symbol, Modifier.size(19.dp), if (primary) Palette.Ink else Palette.Gold)
    }
}

@Composable
private fun WelcomeScreen(session: GameSession, strings: Strings) {
    var choosing by remember { mutableStateOf(false) }
    if (choosing) CityDialog(session, strings) { choosing = false }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artHeight = (maxHeight * .36f).coerceIn(200.dp, 300.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.padding(28.dp, 28.dp, 28.dp, 0.dp)) { Brand() }
            CityArtwork(session.city, Modifier.padding(top = 12.dp).fillMaxWidth().height(artHeight))
            Column(Modifier.padding(horizontal = 28.dp)) {
                Eyebrow(strings[Copy.CityAwake])
                Spacer(Modifier.height(14.dp))
                Display(strings[Copy.WelcomeTitle], 58)
                Spacer(Modifier.height(16.dp))
                Text(strings[Copy.WelcomeBody] + "\n" + strings[Copy.WelcomeHint], color = Palette.Muted, fontSize = 14.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(28.dp))
                ActionButton(strings[Copy.Enter], { choosing = true })
                Spacer(Modifier.height(40.dp))
                Footer(strings)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MenuScreen(session: GameSession, strings: Strings) {
    var choosing by remember { mutableStateOf(false) }
    if (choosing) CityDialog(session, strings) { choosing = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Brand()
            Column(horizontalAlignment = Alignment.End) {
                Text(strings[Copy.Menu], color = Palette.Faint, fontSize = 8.sp, letterSpacing = 1.sp)
                LanguageButton(session, strings)
            }
        }
        Spacer(Modifier.height(30.dp))
        Box(Modifier.fillMaxWidth().height(274.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Palette.Line, RoundedCornerShape(12.dp))) {
            CityArtwork(session.city, Modifier.fillMaxSize(), compact = true)
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Eyebrow("${strings[Copy.MissionNumber]}  /  ${strings[Copy.CityAwake]}", color = Palette.Mint)
                Spacer(Modifier.weight(1f))
                Display(strings[Copy.MissionTitle], 36)
                Spacer(Modifier.height(8.dp))
                Text(strings.mission(session.city), color = Palette.Muted, fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("72", strings[Copy.People])
            Box(Modifier.height(43.dp).width(1.dp).background(Palette.Line))
            Stat("3", strings[Copy.Minutes])
            Box(Modifier.height(43.dp).width(1.dp).background(Palette.Line))
            Stat("36", strings[Copy.ToSave])
        }
        ActionButton(strings[Copy.Start], { choosing = true }, symbol = Symbol.Shield)
        Spacer(Modifier.height(14.dp))
        MenuRow(strings[Copy.HowTo], Symbol.Guide) { session.screen = Screen.Guide }
        Spacer(Modifier.height(25.dp))
        if (session.best > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow(strings[Copy.Record], Palette.Faint)
                Text("${session.best} / 72", color = Palette.Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else Text(strings[Copy.NoRecord], Modifier.fillMaxWidth(), Palette.Faint, fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Footer(strings)
    }
}

@Composable
private fun LanguageButton(session: GameSession, strings: Strings) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(6.dp)).semantics { contentDescription = strings[Copy.ActiveLanguage] }
            .clickable(role = Role.Button) { open = true }.padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SymbolIcon(Symbol.Globe, Modifier.size(14.dp), Palette.Mint)
            Text(session.language.code.uppercase(), color = Palette.Mint, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.background(Palette.Panel).border(1.dp, Palette.Line, MaterialTheme.shapes.medium)) {
            Language.entries.forEach { language ->
                DropdownMenuItem(onClick = { session.select(language); open = false }) {
                    Text(language.nativeName, Modifier.weight(1f), Palette.Cream, fontSize = 13.sp)
                    Box(Modifier.padding(start = 16.dp).size(16.dp)) {
                        if (language == session.language) SymbolIcon(Symbol.Check, Modifier.fillMaxSize(), Palette.Mint)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Stat(value: String, label: String, color: Color = Palette.Cream) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Display(value, 32, color)
        Spacer(Modifier.height(5.dp))
        Text(label, color = Palette.Faint, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun MenuRow(title: String, symbol: Symbol, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 57.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        SymbolIcon(symbol, Modifier.size(21.dp), Palette.Muted)
        Text(title, Modifier.weight(1f), Palette.Cream, fontSize = 13.sp)
        SymbolIcon(Symbol.Arrow, Modifier.size(16.dp), Palette.Faint)
    }
    Divider(color = Palette.Line.copy(alpha = .7f))
}

@Composable
private fun Footer(strings: Strings) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(strings[Copy.Tagline], color = Palette.Faint, fontSize = 8.sp, letterSpacing = 1.8.sp)
        Text("v${GameIdentity.VERSION}", color = Palette.Faint, fontSize = 9.sp)
    }
}

@Composable
private fun BackBar(title: String, strings: Strings, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onBack).padding(top = 10.dp, bottom = 10.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SymbolIcon(Symbol.Back, Modifier.size(18.dp), Palette.Cream)
            Text(strings[Copy.Back], color = Palette.Cream, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Eyebrow(title.uppercase(), Palette.Faint)
    }
}

private const val NEXT_CITY_MAIL = "mailto:contact@katemariongames.com?subject=Protest%20Defender:%20next%20city"

/**
 * Every game starts here: the player picks the city, then the march begins. Every city fits a 360 × 700 dp phone without scrolling.
 * The [uriHandler] comes from outside the dialog. Inside, the dialog provides the platform's own handler, which a test cannot replace.
 */
@Composable
private fun CityDialog(session: GameSession, strings: Strings, uriHandler: UriHandler = LocalUriHandler.current, onDismiss: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
    val dialogShape = RoundedCornerShape(16.dp)
    Column(Modifier.widthIn(max = 400.dp).clip(dialogShape).background(Palette.Ink).border(1.dp, Palette.Line, dialogShape)
        .verticalScroll(rememberScrollState()).padding(22.dp, 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SymbolIcon(Symbol.Pin, Modifier.size(24.dp), Palette.Mint)
            Eyebrow(strings[Copy.CityEyebrow], modifier = Modifier.weight(1f))
            SymbolIcon(Symbol.Close, Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).semantics { contentDescription = strings[Copy.Back] }
                .clickable(role = Role.Button, onClick = onDismiss).padding(10.dp), Palette.Muted)
        }
        Spacer(Modifier.height(8.dp))
        Display(strings[Copy.CityHeading], 32)
        Spacer(Modifier.height(16.dp))
        City.entries.forEach { city ->
            val selected = city == session.city
            val shape = RoundedCornerShape(12.dp)
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape).background(Palette.Panel)
                .border(1.dp, if (selected) Palette.Mint.copy(alpha = .65f) else Palette.Line, shape)
                .selectable(selected, role = Role.RadioButton) { session.selectCity(city) }.padding(16.dp, 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Canvas(Modifier.size(18.dp, 12.dp).clip(RoundedCornerShape(2.dp))) { flag(city) }
                            Eyebrow(strings.country(city).uppercase(), Palette.Faint)
                        }
                        Spacer(Modifier.height(4.dp))
                        Display(strings.cityName(city), 26)
                    }
                    val best = session.best(city)
                    if (best > 0) Column(Modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.End) {
                        Eyebrow(strings[Copy.CityBest], Palette.Faint)
                        Text("$best / 72", color = Palette.Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    // The check keeps its place on every card, so the bests line up and stay put when the choice changes.
                    Box(Modifier.size(24.dp)) { if (selected) SymbolIcon(Symbol.Check, Modifier.size(24.dp), Palette.Mint) }
                }
                Spacer(Modifier.height(6.dp))
                Text("${city.template.stops.first().name} → ${city.template.stops.last().name}", color = Palette.Muted, fontSize = 12.sp)
            }
        }
        // The player's mail app opens a mail to the studio. Without a mail app, the tap does nothing.
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) { runCatching { uriHandler.openUri(NEXT_CITY_MAIL) } }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SymbolIcon(Symbol.Mail, Modifier.size(24.dp), Palette.Mint)
            Text(strings[Copy.ProposeCity], color = Palette.Mint, fontSize = 13.sp)
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .toggleable(session.cityLanguage, role = Role.Checkbox, onValueChange = session::switchLanguageWithCity).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(session.cityLanguage, onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = Palette.Mint, uncheckedColor = Palette.Faint, checkmarkColor = Palette.Ink))
            Text(strings[Copy.CityLanguage], color = Palette.Cream, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        ActionButton(strings[Copy.Start], { onDismiss(); session.start() }, symbol = Symbol.Shield)
    }
}

@Composable
private fun GuideScreen(strings: Strings, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        BackBar(strings[Copy.HowTo], strings, onBack)
        Spacer(Modifier.height(28.dp))
        Eyebrow(strings[Copy.Tagline])
        Spacer(Modifier.height(14.dp))
        Display(strings[Copy.GuideTitle], 44)
        Spacer(Modifier.height(30.dp))
        listOf(Copy.MoveTitle to Copy.MoveBody, Copy.DefendTitle to Copy.DefendBody,
            Copy.BombTitle to Copy.BombBody, Copy.GoalTitle to Copy.GoalBody).forEachIndexed { index, (title, body) ->
            Row(Modifier.padding(bottom = 25.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Display("0${index + 1}", 28, Palette.Gold, Modifier.width(29.dp))
                Column {
                    Text(strings[title], color = Palette.Cream, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(strings[body], color = Palette.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                }
            }
        }
        ActionButton(strings[Copy.GotIt], onBack, symbol = Symbol.Check)
        Spacer(Modifier.height(20.dp))
    }
}
