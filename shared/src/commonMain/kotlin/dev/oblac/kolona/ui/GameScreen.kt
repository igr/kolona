package dev.oblac.kolona.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import dev.oblac.kolona.game.*
import dev.oblac.kolona.i18n.*
import kotlin.math.ceil

@Composable
internal fun GameScreen(session: GameSession, strings: Strings) {
    val engine = session.engine
    val frame = session.revision
    val status = engine.status
    val camera = remember(engine) { HeroCamera(engine.map.width, engine.map.height).apply { follow(engine.hero.position, 0f, snap = true) } }
    // The phase box and the minimap cover the top of the map, so the camera may go that far past the map's top edge.
    var mapWidth by remember { mutableIntStateOf(0) }
    var hudHeight by remember { mutableIntStateOf(0) }
    fun fitCamera() {
        if (mapWidth > 0) camera.topInset = hudHeight * camera.viewWidth / mapWidth
        camera.follow(engine.hero.position, 0f, snap = true)
    }
    LaunchedEffect(engine, status) {
        if (status != GameStatus.Playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (isActive && engine.status == GameStatus.Playing) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0f, .1f)
                last = now
                session.tick(dt)
                camera.follow(engine.hero.position, dt)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(17.dp, 15.dp, 12.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Brand(strings, compact = true)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings[Copy.InMarch], color = Palette.Faint, fontSize = 8.sp, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Display(engine.survivors.toString(), 29, if (engine.survivors < 45) Palette.Red else Palette.Cream)
                        Text(" / 72", Modifier.padding(bottom = 2.dp), Palette.Faint, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.width(15.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings[Copy.Defended], color = Palette.Faint, fontSize = 8.sp, letterSpacing = .5.sp)
                    Display(engine.defended.toString(), 29, Palette.Gold)
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Palette.Line, RoundedCornerShape(8.dp))
                    .semantics { contentDescription = strings[Copy.Pause] }.clickable(role = Role.Button) { session.pause() }, contentAlignment = Alignment.Center) {
                    SymbolIcon(Symbol.Pause, Modifier.size(16.dp), Palette.Cream)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("A", color = Palette.Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Line)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(engine.progress).background(Palette.Mint))
                }
                Text("B", color = Palette.Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${(engine.progress * 100).toInt()}%", Modifier.width(34.dp), Palette.Mint, fontSize = 10.sp, textAlign = TextAlign.End)
            }
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp, 14.dp, 0.dp, 0.dp))) {
                WorldCanvas(engine, camera, strings, frame, Modifier.fillMaxSize()
                    .semantics { contentDescription = strings[Copy.MoveBody] }
                    .onSizeChanged { camera.resize(it.width.toFloat(), it.height.toFloat()); mapWidth = it.width; fitCamera() }
                    .pointerInput(engine) {
                        detectTapGestures { p -> engine.command(camera.screenToWorld(Vec2(p.x, p.y), size.width.toFloat())) }
                    })
                Row(Modifier.fillMaxWidth().onSizeChanged { hudHeight = it.height; fitCamera() }.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.clip(RoundedCornerShape(6.dp)).background(Palette.Ink.copy(alpha = .91f)).padding(10.dp, 8.dp)) {
                        Text(if (engine.speechRemaining > 0) "${strings[Copy.Speech]} · ${ceil(engine.speechRemaining).toInt()} s" else strings.phase(engine.phase),
                            color = if (engine.speechRemaining > 0) Palette.Mint else Palette.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
                        Text("${strings[Copy.MissionNumber]}  ·  ${formatTime(engine.elapsed)}", Modifier.padding(top = 4.dp), Palette.Faint, fontSize = 8.sp)
                    }
                    RouteMiniature(camera, engine, frame, Modifier.size(82.dp, 66.dp).clip(RoundedCornerShape(6.dp)).background(Palette.Ink.copy(alpha = .85f)).padding(5.dp))
                }
                Row(Modifier.align(Alignment.BottomCenter).padding(10.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Ink.copy(alpha = .92f))
                    .border(1.dp, Palette.Line, RoundedCornerShape(20.dp)).padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(if (engine.hero.moving) Palette.Mint else Palette.Gold))
                    Text(strings[when { engine.hero.moving && engine.hero.slowedByCrowd -> Copy.Slowed; engine.hero.moving -> Copy.Moving; engine.hero.barricade < .7f -> Copy.Deploying; else -> Copy.Barricade }],
                        color = Palette.Cream, fontSize = 8.sp, letterSpacing = .5.sp)
                }
            }
            Row(Modifier.fillMaxWidth().background(Palette.Panel).padding(18.dp, 14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SymbolIcon(Symbol.Bomb, Modifier.size(27.dp), if (engine.bombCooldown > 0) Palette.Faint else Palette.Gold)
                Column(Modifier.weight(1f)) {
                    Text(strings[if (engine.bombCooldown > 0) Copy.BombWait else Copy.BombReady], color = if (engine.bombCooldown > 0) Palette.Muted else Palette.Gold,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(strings[Copy.BombHint], color = Palette.Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Display(if (engine.bombCooldown > 0) "${ceil(engine.bombCooldown).toInt()}s" else "4+", 29, if (engine.bombCooldown > 0) Palette.Faint else Palette.Gold)
            }
            Row(Modifier.fillMaxWidth().padding(16.dp, 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Legend(strings[Copy.Crowd], Palette.Mint)
                Legend(strings[Copy.Threat], Palette.Red)
                Legend(strings[Copy.You], Palette.Gold)
            }
        }
        if (status != GameStatus.Playing) {
            Box(Modifier.fillMaxSize().background(Palette.Deep.copy(alpha = .83f)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}, contentAlignment = Alignment.Center) {
                Column(Modifier.padding(22.dp).widthIn(max = 380.dp).verticalScroll(rememberScrollState()).clip(RoundedCornerShape(16.dp))
                    .background(Palette.Ink).border(1.dp, Palette.Line, RoundedCornerShape(16.dp)).padding(24.dp)) {
                    if (status == GameStatus.Paused) {
                        SymbolIcon(Symbol.Pause, Modifier.size(26.dp))
                        Spacer(Modifier.height(24.dp))
                        Eyebrow(strings[Copy.PausedEyebrow])
                        Spacer(Modifier.height(13.dp))
                        Display(strings[Copy.PausedTitle], 48)
                        Spacer(Modifier.height(14.dp))
                        Text(strings[Copy.PausedBody], color = Palette.Muted, fontSize = 13.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(28.dp))
                        ActionButton(strings[Copy.Resume], session::resume)
                        Spacer(Modifier.height(10.dp))
                        ActionButton(strings[Copy.Restart], session::start, primary = false)
                    } else {
                        val won = status == GameStatus.Won
                        // A win adds the souvenir, so its dialog is tighter to fit a 360 × 700 dp phone without scrolling.
                        val gap = if (won) .5f else 1f
                        SymbolIcon(Symbol.Shield, Modifier.size(34.dp), if (won) Palette.Mint else Palette.Red)
                        Spacer(Modifier.height(20.dp * gap))
                        Eyebrow(strings[if (won) Copy.WonEyebrow else Copy.LostEyebrow], if (won) Palette.Mint else Palette.Red)
                        Spacer(Modifier.height(13.dp * gap))
                        Display(strings[if (won) Copy.WonTitle else Copy.LostTitle], 43)
                        Spacer(Modifier.height(14.dp * gap))
                        Text(if (won) strings.won(engine.map.city) else strings[Copy.LostBody], color = Palette.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                        Spacer(Modifier.height(24.dp * gap))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                            Display(engine.survivors.toString(), if (won) 58 else 76, if (won) Palette.Mint else Palette.Red)
                            Text(" / 72", Modifier.padding(bottom = 8.dp), Palette.Faint, fontSize = 20.sp)
                        }
                        Text(strings[Copy.Saved], Modifier.fillMaxWidth(), Palette.Faint, fontSize = 9.sp, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(23.dp * gap))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Stat(engine.defended.toString(), strings[Copy.Defended])
                            Stat(formatTime(engine.elapsed), strings[Copy.Time])
                        }
                        if (won) {
                            Spacer(Modifier.height(10.dp))
                            Souvenir(engine.map.city, strings)
                        }
                        Spacer(Modifier.height(25.dp * gap))
                        ActionButton(strings[if (won) Copy.Again else Copy.TryAgain], session::start)
                    }
                    Spacer(Modifier.height(10.dp))
                    ActionButton(strings[Copy.ReturnMenu], session::menu, primary = false, symbol = Symbol.Back)
                }
            }
        }
    }
}

private val City.souvenirName get() = when (this) { City.Belgrade -> "Pljeskavica"; City.Paris -> "Croissant"; City.Milan -> "Panettone" }

@Composable
private fun Souvenir(city: City, strings: Strings) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Canvas(Modifier.size(66.dp, 44.dp).rotate(-4f)) { souvenir(city) }
        Column(Modifier.weight(1f)) {
            Text(strings.souvenir(city), color = Palette.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Display(city.souvenirName, 24)
        }
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, color = Palette.Faint, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

private fun formatTime(seconds: Float): String {
    val time = seconds.toInt()
    return "${time / 60}:${(time % 60).toString().padStart(2, '0')}"
}
