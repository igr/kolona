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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
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

internal enum class Symbol { Arrow, Back, Shield, Guide, Globe, Pin, Pause, Check, Close, Bomb }

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

@Composable
internal fun Brand(strings: Strings, compact: Boolean = false) {
    val icon = if (compact) 30.dp else 44.dp
    val subtitle = if (compact) 7.sp else 9.sp
    // The wordmark is a logo pinned to the icon's height, so the system text size does not scale it.
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)) {
            Canvas(Modifier.size(icon).clip(RoundedCornerShape(10.dp)).background(Palette.Gold)) {
                val s = size.width / 44f
                withTransform({ scale(s, s, Offset.Zero) }) {
                    round(9f, 13f, 26f, 7f, Palette.Ink, 1f)
                    round(9f, 23f, 26f, 7f, Palette.Ink, 1f)
                    round(12f, 30f, 4f, 5f, Palette.Ink, 1f)
                    round(28f, 30f, 4f, 5f, Palette.Ink, 1f)
                    drawLine(Palette.Gold, Offset(20f, 13f), Offset(15f, 20f), 3f)
                    drawLine(Palette.Gold, Offset(28f, 23f), Offset(23f, 30f), 3f)
                }
            }
            // The subtitle's capitals start at the icon's top edge and the wordmark stands on its bottom edge.
            Layout({
                Text(strings[Copy.Subtitle], color = Palette.Gold, fontSize = subtitle, letterSpacing = if (compact) 2.2.sp else 3.3.sp, fontWeight = FontWeight.Bold)
                Display(strings[Copy.Wordmark], if (compact) 32 else 48)
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
            Row(Modifier.fillMaxWidth().padding(28.dp, 28.dp, 28.dp, 0.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Brand(strings)
                Text("01 / 72", color = Palette.Faint, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
            CityArtwork(session.city, Modifier.padding(top = 12.dp).fillMaxWidth().height(artHeight))
            Column(Modifier.padding(horizontal = 28.dp)) {
                Eyebrow(strings[Copy.CityAwake])
                Spacer(Modifier.height(14.dp))
                Display(strings[Copy.WelcomeTitle], 58)
                Spacer(Modifier.height(16.dp))
                Text(strings[Copy.WelcomeBody], color = Palette.Muted, fontSize = 14.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(28.dp))
                ActionButton(strings[Copy.Enter], { choosing = true })
                Spacer(Modifier.height(16.dp))
                Text(strings[Copy.WelcomeHint], Modifier.fillMaxWidth(), Palette.Faint, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
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
            Brand(strings)
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

/** Every game starts here: the player picks the city, then the march begins. Every city fits a 360 × 700 dp phone without scrolling. */
@Composable
private fun CityDialog(session: GameSession, strings: Strings, onDismiss: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
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
                    if (selected) SymbolIcon(Symbol.Check, Modifier.size(24.dp), Palette.Mint)
                }
                Spacer(Modifier.height(6.dp))
                Text("${city.template.stops.first().name} → ${city.template.stops.last().name}", color = Palette.Muted, fontSize = 12.sp)
            }
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
