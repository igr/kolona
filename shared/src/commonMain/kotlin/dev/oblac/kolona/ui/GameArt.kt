package dev.oblac.kolona.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import dev.oblac.kolona.game.*
import dev.oblac.kolona.i18n.*
import kotlin.math.*

internal fun Vec2.offset() = Offset(x, y)
private fun MarchRoute.path() = Path().apply {
    samples.forEachIndexed { i, sample ->
        if (i == 0) moveTo(sample.position.x, sample.position.y)
        else lineTo(sample.position.x, sample.position.y)
    }
}
internal class MapPaint(val map: CityMap) {
    val route = map.route.path()
    val streets = map.network.streets.map { it.line.path() }
    // Every width is fixed, so the strokes are built once, not every frame.
    val curbs = map.network.streets.map { road(it.width + 12f) }
    val lanes = map.network.streets.map { road(it.width - 4f) }
    val routeRoad = listOf(Color(0xFF172F39) to 143f, Color(0xFF869893) to 132f, Color(0xFF526E73) to 118f, Color(0xFF304E5B) to 103f)
        .map { (color, width) -> color to road(width) }
    val centerLine = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 16f)))
    // The minimap's route keeps its size on a larger map.
    val miniStreets = map.network.streets.map { road(it.width / 2) }
    val miniRoute = Stroke(16f * map.scale, cap = StrokeCap.Round)
    /** Each crosswalk's center and its angle in degrees. The first and the last stay outside the end squares. */
    val crosswalks = listOf(.04f, .36f, .66f, .92f).map { f ->
        val d = (f * map.route.length).coerceIn(map.stops.first().radius + 30f, map.route.length - map.stops.last().radius - 30f)
        val p = map.route.at(d)
        val next = map.route.at(d + 5)
        p.offset() to atan2(next.y - p.y, next.x - p.x) * 180f / PI.toFloat()
    }
    val names = map.labels.map { it.text.uppercase() }
}
private val blocks = Color(0xFF1A2F37)
private val lowRoof = Color(0xFF223C46)
private val curb = Color(0xFF5B7376)
private val asphalt = Color(0xFF2B4753)
private val plaza = Color(0xFF3A5761)
private val roofs = listOf(Color(0xFF597078), Color(0xFF6A7F7C), Color(0xFF466573), Color(0xFF7B8071), Color(0xFF3E6271))
// City colours are dimmed for the night, so the march, the threats, and the names stay brighter than any roof.
private val zinc = listOf(Color(0xFF5F6E7C), Color(0xFF687582), Color(0xFF586A7D), Color(0xFF6E7884), Color(0xFF526379))
private val stone = Color(0xFF7F7563)
private val tiles = Color(0xFF74544B)
private val plaster = Color(0xFF6F6357)
// Giallo Milano, terracotta, and pale ochre plaster.
private val milanFronts = listOf(Color(0xFF8E7647), Color(0xFF7F5141), Color(0xFF867A5E), Color(0xFF8A6446), Color(0xFF7D7357))
private val milanCourtyards = milanFronts.map { lerp(it, Palette.Deep, .3f) }
private val coppi = Color(0xFF7B4D3C)
private val coppiDark = Color(0xFF603B30)
private val coppiRidge = Color(0xFF9C6A55)
private val shutters = Color(0xFF3F5C46)
private val cortile = Color(0xFF1C282B)
private val flatRoof = Color(0xFF6A665E)
private val lit = Palette.Gold.copy(alpha = .45f)
private val shade = Palette.Deep.copy(alpha = .35f)
private val copper = Color(0xFF5D8478)
private val copperLight = Color(0xFF9CC2B0)
private val churchStone = Color(0xFF908D83)
private val churchLight = Color(0xFFA9A69B)
private val churchFace = Color(0xFF6A675F)
private val glass = Color(0xFF2A2827)
private val towerRoof = Color(0xFF221F1E)
private val towerEdge = Color(0xFF57514C)
private val hotelFront = Color(0xFF7E8C71)
private val ochre = Color(0xFFA48D5C)
private val slate = Color(0xFF4A5562)
private val museumStone = Color(0xFF8E8571)
private val museumLight = Color(0xFFB0A68C)
private val circusWall = Color(0xFF948A74)
private val frieze = Color(0xFF7A3934)
private val circusRoof = Color(0xFF77736F)
private val operaStone = Color(0xFF858B8B)
private val operaFace = Color(0xFF6B7272)
private val operaGlass = Color(0xFF47657A)
private val duomo = Color(0xFFB6B1A4)
private val duomoLight = Color(0xFFD2CDC0)
private val duomoFace = Color(0xFF8C887E)
private val duomoFacade = Color(0xFFB9ADA3)
private val duomoCrossing = lerp(duomo, duomoFace, .3f)
private val duomoSpireShade = lerp(duomoLight, duomoFace, .55f)
private val galleriaGlass = Color(0xFF8AA2A6)
private val galleriaIron = Color(0xFF4C5C60)
private val galleriaRoof = Color(0xFF7A7468)
private val galleriaFront = Color(0xFF978A72)
private val casello = Color(0xFFA89A88)
private val caselloFace = Color(0xFF857A6B)
// Milan's landmarks draw in their own unscaled units, so their shapes and strokes are built once, not every frame.
// The nave runs off the bottom: each layer clips it where the landmark's height ends it.
private val duomoPlan = polygon(14f, 1000f, 14f, 50f, 0f, 50f, 0f, 24f, 14f, 24f, 14f, 16f, 24f, 5f, 38f, 0f, 54f, 0f, 68f, 5f, 78f, 16f,
    78f, 24f, 92f, 24f, 92f, 50f, 78f, 50f, 78f, 1000f)
private val duomoFront = polygon(-32f, 0f, -32f, -28f, -21f, -33f, -10.5f, -38f, 0f, -46f, 10.5f, -38f, 21f, -33f, 32f, -28f, 32f, 0f)
private val spirePath = polygon(-1.3f, 0f, 1.3f, 0f, 0f, -7f)
private val smallSpirePath = polygon(-.91f, 0f, .91f, 0f, 0f, -4.9f)
private val mainSpireLit = polygon(-3f, 2f, 0f, 2f, 0f, -24f)
private val mainSpireShade = polygon(0f, 2f, 3f, 2f, 0f, -24f)
private val duomoColumns = floatArrayOf(-32f, -20f, 20f, 32f)
private val transeptSpires = floatArrayOf(3f, 10f, 82f, 89f)
private val apseSpires = floatArrayOf(14f, 22f, 24f, 11f, 38f, 7.5f, 54f, 7.5f, 68f, 11f, 78f, 22f)
private val duomoSteps = floatArrayOf(-32f, 4f, -21f, 9f, -10.5f, 14f, 10.5f, 14f, 21f, 9f, 32f, 4f)
private val duomoPortals = floatArrayOf(0f, 7f, -15.5f, 5f, 15.5f, 5f, -26.5f, 4f, 26.5f, 4f)
private val galleriaOctagon = polygon(*FloatArray(16) { i ->
    val a = ((i / 2) * 45f + 22.5f) * PI.toFloat() / 180f
    (if (i % 2 == 0) cos(a) else sin(a)) * 16f
})
private val domeRib = Stroke(.6f)
private val vaultEdge = Stroke(.8f)
private val ledge = Stroke(1.2f)
private val ridgeLine = Stroke(1.4f)
private val parapet = Stroke(1.6f)
// The rendering test finds each landmark by this colour, so keep it apart from the street and building colours.
internal val LandmarkKind.signature: Color get() = when (this) {
    LandmarkKind.Church -> churchStone
    LandmarkKind.Tower -> glass
    LandmarkKind.Hotel -> hotelFront
    LandmarkKind.Museum -> museumStone
    LandmarkKind.Circus -> circusRoof
    LandmarkKind.Opera -> operaStone
    LandmarkKind.Cathedral -> duomo
    LandmarkKind.Arcade -> galleriaRoof
    LandmarkKind.Tollhouse -> casello
}
private val coats = listOf(Palette.Mint, Palette.Gold, Palette.Cream, Color(0xFF88B7BC), Color(0xFFD2A8AD))
private val skins = listOf(Color(0xFFE0B293), Color(0xFFB38263), Color(0xFFD2A383), Color(0xFF8C644D))

internal fun DrawScope.round(x: Float, y: Float, w: Float, h: Float, color: Color, radius: Float = 3f) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(radius))

internal fun DrawScope.label(measurer: TextMeasurer, text: String, position: Offset, color: Color, fontSize: Float = 12f) {
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = (fontSize / density).sp, fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = Offset(position.x - layout.size.width / 2, position.y - layout.size.height / 2))
}

private fun Rect.meets(position: Vec2, width: Float, height: Float) =
    position.x < right && position.x + width > left && position.y < bottom && position.y + height > top
internal fun visibleBuildings(map: CityMap, visible: Rect): List<Building> {
    val area = visible.inflate(120f)
    return map.buildings.filter { area.meets(it.position, it.width, it.height) }
}
internal fun visibleProps(map: CityMap, visible: Rect): List<Prop> {
    val area = visible.inflate(120f)
    return map.props.filter { area.contains(it.position.offset()) }
}
internal fun visibleLandmarks(map: CityMap, visible: Rect): List<Landmark> {
    val area = visible.inflate(120f)
    return map.landmarks.filter { area.meets(it.position, it.width, it.height) }
}

private fun polygon(vararg points: Float) = Path().apply {
    moveTo(points[0], points[1])
    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
    close()
}

private fun DrawScope.building(b: Building, city: City) {
    val x = b.position.x
    val y = b.position.y
    val w = b.width
    val h = b.height
    round(x + 10, y + 18, w, h + 4, shade)
    when {
        city == City.Paris -> {
            round(x, y + 12, w, h, stone)
            for (j in 0 until ((w - 6) / 10).toInt()) round(x + 6 + j * 10, y + h + 2, 4f, 7f, lit, 1f)
            drawLine(Palette.Deep.copy(alpha = .6f), Offset(x + 2, y + h + 5), Offset(x + w - 2, y + h + 5), 1.2f)
            round(x, y, w, h, lerp(zinc[b.palette], Palette.Deep, .42f))
            for (s in 1 until (w / 4).toInt()) drawLine(Color(0xFFA9B4BA).copy(alpha = .14f), Offset(x + s * 4, y + h - 13), Offset(x + s * 4, y + h - 1), 1f)
            round(x + 5, y + 4, w - 10, h - 17, zinc[b.palette], 2f)
            drawLine(Color(0xFFA9B4BA).copy(alpha = .4f), Offset(x + 4, y + h - 13), Offset(x + w - 4, y + h - 13), 1.5f)
            for (j in 0 until ((w - 10) / 14).toInt()) {
                round(x + 8 + j * 14, y + h - 10, 7f, 8f, Color(0xFF7D8890), 1f)
                round(x + 10 + j * 14, y + h - 7, 3f, 4f, Palette.Gold.copy(alpha = .4f), .5f)
            }
            val stack = x + 7 + (b.palette * 11f) % (w - 34)
            round(stack, y + 2, 20f, 7f, Color(0xFF6F655B), 1f)
            repeat(5) { k -> round(stack + 1.5f + k * 3.8f, y, 2.4f, 3.5f, Color(0xFF9A6B55), .5f) }
        }
        city == City.Milan -> {
            val front = milanFronts[b.palette]
            round(x, y + 12, w, h, front)
            for (j in 0 until ((w - 6) / 12).toInt()) {
                val wx = x + 6 + j * 12
                round(wx, y + h + 3, 1.8f, 6.5f, shutters, .5f)
                round(wx + 2.3f, y + h + 3, 3.6f, 6.5f, lit, 1f)
                round(wx + 6.4f, y + h + 3, 1.8f, 6.5f, shutters, .5f)
            }
            if (b.palette == 4) roofTerrace(x, y, w, h, (x / 72).toInt() % 2 == 0) else cortileRoof(x, y, w, h, milanCourtyards[b.palette])
        }
        b.palette == 2 || b.palette == 4 -> {
            round(x, y + 12, w, h, plaster)
            for (j in 0 until min(4, ((w - 3) / 17).toInt())) round(x + 10 + j * 17, y + h + 3, 7f, 7f, lit, 1f)
            val inset = min(w, h) / 2
            val ridge = y + h * .45f
            drawRect(lerp(tiles, Palette.Deep, .15f), Offset(x, y), Size(w, h))
            drawPath(polygon(x, y, x + w, y, x + w - inset, ridge, x + inset, ridge), lerp(tiles, Palette.Deep, .3f))
            drawPath(polygon(x, y + h, x + w, y + h, x + w - inset, ridge, x + inset, ridge), tiles)
            for (k in 1..3) {
                val t = k / 4f
                val course = ridge + (y + h - ridge) * t
                drawLine(Palette.Deep.copy(alpha = .25f), Offset(x + inset * t, course), Offset(x + w - inset * t, course), 1f)
            }
            drawLine(Color(0xFFA77C66).copy(alpha = .7f), Offset(x + inset, ridge), Offset(x + w - inset, ridge), 2f)
        }
        else -> {
            round(x, y + 12, w, h, Color(0xFF29444E))
            for (j in 0 until min(4, ((w - 3) / 17).toInt())) round(x + 10 + j * 17, y + h + 3, 7f, 7f, lit, 1f)
            round(x, y, w, h, roofs[b.palette])
            drawRect(Color(0xFFB3C2B7).copy(alpha = .2f), Offset(x + 5, y + 5), Size(w - 10, h - 10), style = Stroke(2f))
            round(x + 12, y + 13, w * .4f, h * .32f, Color(0xFF304B56), 2f)
            drawLine(Color(0xFF9DAFAD).copy(alpha = .5f), Offset(x + 12, y + 13), Offset(x + 12 + w * .4f, y + 13), 2f)
            round(x + w - 27, y + h - 25, 16f, 14f, Color(0xFF85958A), 2f)
            drawCircle(Color(0xFF42606A), 4f, Offset(x + w - 19, y + h - 18))
            round(x + 10, y + h - 17, 26f, 6f, Color(0xFF304B56), 1f)
        }
    }
}

// A ring of tiled roof around a dark courtyard. The courtyard wall of the north wing faces the viewer.
private fun DrawScope.cortileRoof(x: Float, y: Float, w: Float, h: Float, courtyard: Color) {
    val ring = min(w, h) * .3f
    drawRect(coppi, Offset(x, y), Size(w, h))
    drawRect(coppiDark, Offset(x, y), Size(w, ring / 2))
    drawRect(coppiDark, Offset(x + ring, y + h - ring), Size(w - 2 * ring, ring / 2))
    repeat(4) { i ->
        val corner = Offset(if (i % 2 == 0) x else x + w, if (i < 2) y else y + h)
        drawLine(coppiRidge.copy(alpha = .5f), corner, corner + Offset(if (i % 2 == 0) ring else -ring, if (i < 2) ring else -ring), 1f)
    }
    drawRect(coppiRidge.copy(alpha = .8f), Offset(x + ring / 2, y + ring / 2), Size(w - ring, h - ring), style = ridgeLine)
    val cw = w - 2 * ring
    val ch = h - 2 * ring
    val wall = min(6f, ch * .45f)
    drawRect(courtyard, Offset(x + ring, y + ring), Size(cw, wall))
    for (i in 0 until ((cw - 2) / 5).toInt()) round(x + ring + 2 + i * 5, y + ring + 1.5f, 2f, 2.6f, lit, .5f)
    drawRect(cortile, Offset(x + ring, y + ring + wall), Size(cw, ch - wall))
}

private fun DrawScope.roofTerrace(x: Float, y: Float, w: Float, h: Float, left: Boolean) {
    drawRect(flatRoof, Offset(x, y), Size(w, h))
    drawRect(Color(0xFF8E897D).copy(alpha = .55f), Offset(x + 2, y + 2), Size(w - 4, h - 4), style = parapet)
    val dw = w * .5f
    val dh = h * .55f
    val dx = if (left) x + 5 else x + w - 5 - dw
    val dy = y + h - 5 - dh
    round(dx, dy, dw, dh, Color(0xFF6F5B45), 1f)
    for (i in 1 until (dw / 4).toInt()) drawLine(Palette.Deep.copy(alpha = .25f), Offset(dx + i * 4, dy), Offset(dx + i * 4, dy + dh), .6f)
    for (k in 0 until (dw / 7).toInt()) {
        drawCircle(Color(0xFF3F5E42), 2.6f, Offset(dx + 3.5f + k * 7, dy + 2.5f))
        drawCircle(Color(0xFF5E7E58), 1.4f, Offset(dx + 3f + k * 7, dy + 2f))
    }
    val c = Offset(dx + dw / 2, dy + dh * .62f)
    drawCircle(shade, 6f, c + Offset(2f, 3f))
    for (i in 0 until 8) drawArc(if (i % 2 == 0) Color(0xFFB9AE94) else Color(0xFF8E4A40), i * 45f, 45f, true, c - Offset(6f, 6f), Size(12f, 12f))
    drawCircle(Color(0xFF6E665A), 1f, c)
    round(if (left) x + w - 17 else x + 6, y + 6, 11f, 8f, Color(0xFF7C776C), 1f)
}

private fun DrawScope.dome(c: Offset, r: Float) {
    drawCircle(shade, r, c + Offset(r * .25f, r * .35f))
    drawCircle(copper, r, c)
    if (r >= 8f) for (i in 0 until 8) {
        val d = Offset(cos(i * PI.toFloat() / 4), sin(i * PI.toFloat() / 4))
        drawLine(Palette.Deep.copy(alpha = .25f), c + d * (r * .3f), c + d * r, r / 18)
    }
    drawCircle(copperLight.copy(alpha = .45f), r * .5f, c - Offset(r * .3f, r * .32f))
    drawCircle(copperLight, r * .18f, c)
}

// Clipped, so a landmark never covers a street.
private fun DrawScope.landmark(l: Landmark) {
    val x = l.position.x
    val y = l.position.y
    clipRect(x, y, x + l.width, y + l.height) {
        when (l.kind) {
            LandmarkKind.Church -> church(x, y, l.width, l.height)
            LandmarkKind.Tower -> tower(x, y, l.width, l.height)
            LandmarkKind.Hotel -> hotel(x, y, l.width, l.height)
            LandmarkKind.Museum -> museum(x, y, l.width, l.height)
            LandmarkKind.Circus -> circus(x, y, l.width, l.height)
            LandmarkKind.Opera -> opera(x, y, l.width, l.height)
            LandmarkKind.Cathedral -> cathedral(x, y, l.width, l.height)
            LandmarkKind.Arcade -> arcade(x, y, l.width, l.height)
            LandmarkKind.Tollhouse -> tollhouse(x, y, l.width, l.height)
        }
    }
}

private fun DrawScope.church(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 150f
    val bw = w - 8 * k
    val face = 14 * k
    val rh = h - 8 * k - face
    val a = bw * .4f
    val cx = x + bw / 2
    val cy = y + rh / 2
    fun cross(dx: Float, dy: Float, color: Color) {
        drawRect(color, Offset(x + dx, cy - a / 2 + dy), Size(bw, a))
        drawRect(color, Offset(cx - a / 2 + dx, y + dy), Size(a, rh))
    }
    cross(8 * k, face + 8 * k, shade)
    cross(0f, face, churchFace)
    cross(0f, 0f, churchStone)
    listOf(Offset(x + a * .3f, cy), Offset(x + bw - a * .3f, cy), Offset(cx, y + a * .3f), Offset(cx, y + rh - a * .3f)).forEach { dome(it, a * .22f) }
    for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
        val t = Offset(cx + sx * a / 2, cy + sy * a / 2)
        val s = a * .3f
        drawRect(churchFace, Offset(t.x - s / 2, t.y - s / 2 + 4 * k), Size(s, s))
        drawRect(churchLight, Offset(t.x - s / 2, t.y - s / 2), Size(s, s))
        dome(t, s * .36f)
    }
    val r = a * .62f
    drawCircle(churchFace, r + 4 * k, Offset(cx, cy + 3 * k))
    drawCircle(churchLight, r + 4 * k, Offset(cx, cy))
    dome(Offset(cx, cy - 2 * k), r)
    val top = Offset(cx, cy - 2 * k)
    drawLine(Palette.Gold, top, top - Offset(0f, 10 * k), 1.6f * k)
    drawLine(Palette.Gold, top - Offset(3.5f * k, 7 * k), top - Offset(-3.5f * k, 7 * k), 1.6f * k)
}

private fun DrawScope.tower(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 70f
    val bw = w - 18 * k
    val bh = h - 8 * k
    val roof = bh * .34f
    drawRect(Palette.Deep.copy(alpha = .42f), Offset(x + 18 * k, y + roof * .6f), Size(bw, h))
    drawRect(glass, Offset(x, y + roof), Size(bw, bh - roof))
    for (i in 0 until ((bw - 2 * k) / (6 * k)).toInt()) for (j in 0 until ((bh - roof - 4 * k) / (5 * k)).toInt()) {
        val on = (i * 7 + j * 13 + i * j) % 9 < 3
        drawRect(if (on) Palette.Gold.copy(alpha = .6f) else Color(0xFF3C3936), Offset(x + 3 * k + i * 6 * k, y + roof + 3 * k + j * 5 * k), Size(3 * k, 2.2f * k))
    }
    drawLine(towerEdge, Offset(x + k, y + roof), Offset(x + k, y + bh), 2 * k)
    drawRect(towerRoof, Offset(x, y), Size(bw, roof))
    drawRect(towerEdge, Offset(x + k, y + k), Size(bw - 2 * k, roof - 2 * k), style = Stroke(1.5f * k))
    drawRect(Color(0xFF151413), Offset(x + bw * .25f, y + roof * .22f), Size(bw * .5f, roof * .56f))
    drawCircle(Color(0xFF8FA1A4), 2.2f * k, Offset(x + bw / 2, y + roof / 2))
}

private fun DrawScope.hotel(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 90f
    val bw = w - 8 * k
    val bh = h - 6 * k
    val face = 28 * k
    val roof = bh - face
    drawRect(shade, Offset(x + 8 * k, y + 8 * k), Size(bw, bh - 2 * k))
    drawRect(hotelFront, Offset(x, y + roof), Size(bw, face))
    for (j in 0 until ((bw - 14 * k) / (8 * k)).toInt()) for (row in 0..2)
        round(x + 9 * k + j * 8 * k, y + roof + (5 + row * 7.5f) * k, 3.5f * k, 4.5f * k, lit, 1.7f * k)
    drawRect(ochre, Offset(x, y + roof), Size(bw, 2.5f * k))
    drawRect(ochre.copy(alpha = .7f), Offset(x, y + roof + face - 4 * k), Size(bw, 1.5f * k))
    val inset = roof / 2
    drawRect(lerp(slate, Palette.Deep, .25f), Offset(x, y), Size(bw, roof))
    drawPath(polygon(x, y + roof, x + bw, y + roof, x + bw - inset, y + roof / 2, x + inset, y + roof / 2), slate)
    drawLine(Color(0xFF8A96A0).copy(alpha = .5f), Offset(x + inset, y + roof / 2), Offset(x + bw - inset, y + roof / 2), 1.5f * k)
    dome(Offset(x + 6 * k, y + 6 * k), 4.5f * k)
    dome(Offset(x + bw - 6 * k, y + 6 * k), 4.5f * k)
    // The biggest dome crowns the corner of Terazije and Balkanska.
    for ((cx, r) in listOf(x + 7 * k to 6 * k, x + bw - 10 * k to 9 * k)) {
        drawRect(hotelFront, Offset(cx - r, y + roof), Size(2 * r, face))
        drawRect(ochre.copy(alpha = .6f), Offset(cx - r, y + roof), Size(2 * r, face), style = Stroke(k))
        dome(Offset(cx, y + roof), r)
        drawCircle(Palette.Gold.copy(alpha = .8f), r * .16f, Offset(cx, y + roof - r * .55f))
    }
}

private fun DrawScope.museum(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 120f
    val bw = w - 8 * k
    val bh = h - 6 * k
    val face = 18 * k
    val roof = bh - face
    val cx = x + bw / 2
    drawRect(shade, Offset(x + 8 * k, y + 8 * k), Size(bw, bh - 2 * k))
    drawRect(museumStone, Offset(x, y + roof), Size(bw, face))
    for (j in 0 until ((bw / 2 - 26 * k) / (8 * k)).toInt()) for (side in listOf(-1f, 1f))
        round(cx + side * (24 * k + j * 8 * k) - 2 * k, y + roof + 6 * k, 4 * k, 7 * k, lit, 1f)
    drawRect(Color(0xFF4F4A40), Offset(cx - 18 * k, y + roof + 3 * k), Size(36 * k, face - 4 * k))
    for (i in 0..6) drawLine(museumLight, Offset(cx - 16.5f * k + i * 5.5f * k, y + roof + 3 * k), Offset(cx - 16.5f * k + i * 5.5f * k, y + roof + face - k), 2 * k)
    drawRect(museumLight, Offset(x, y + roof), Size(bw, 2 * k))
    drawRect(museumStone, Offset(x, y), Size(bw, roof))
    drawRect(Color(0xFF5C625F), Offset(x + 5 * k, y + 5 * k), Size(bw - 10 * k, roof - 9 * k))
    drawPath(polygon(cx - 22 * k, y + roof, cx + 22 * k, y + roof, cx, y + roof - 12 * k), museumLight)
    val c = Offset(cx, y + roof * .45f)
    drawCircle(museumStone, 17 * k, c + Offset(0f, 3 * k))
    drawCircle(museumLight, 17 * k, c)
    dome(c - Offset(0f, 2 * k), 13 * k)
}

private fun DrawScope.circus(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 80f
    val wall = h * .14f
    val r = min((w - 8 * k) / 2, (h - wall - 6 * k) / 2)
    val c = Offset(x + r, y + r)
    fun corner(i: Int) = c + Offset(cos(i * PI.toFloat() / 10), sin(i * PI.toFloat() / 10)) * r
    fun drum(dx: Float, dy: Float) = Path().apply {
        for (i in 0 until 20) corner(i).let { if (i == 0) moveTo(it.x + dx, it.y + dy) else lineTo(it.x + dx, it.y + dy) }
        close()
    }
    drawPath(drum(8 * k, wall + 6 * k), shade)
    drawPath(drum(0f, wall), circusWall)
    drawRect(circusWall, Offset(c.x - r, c.y), Size(2 * r, wall))
    drawPath(drum(0f, 4 * k), frieze)
    for (i in 1 until 10) {
        val p = corner(i)
        drawLine(Color(0xFFC1B597), p + Offset(0f, 4 * k), p + Offset(0f, wall), 1.2f * k)
        val window = (p + corner(i - 1)) * .5f
        round(window.x - 1.2f * k, window.y + wall * .45f, 2.4f * k, 4 * k, if (i % 2 == 0) lit else Palette.Deep.copy(alpha = .5f), 1.2f * k)
    }
    drawPath(drum(0f, 0f), circusRoof)
    drawPath(Path().apply { moveTo(c.x, c.y); (10..15).forEach { corner(it).let { p -> lineTo(p.x, p.y) } }; close() }, Color.White.copy(alpha = .08f))
    for (i in 0 until 20) drawLine(Palette.Deep.copy(alpha = .3f), c + (corner(i) - c) * .16f, corner(i), k)
    drawCircle(Color(0xFFA9AFAF), r * .15f, c)
    drawCircle(Palette.Deep.copy(alpha = .5f), r * .15f, c, style = Stroke(k))
}

private fun DrawScope.opera(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 160f
    val bw = w - 8 * k
    val bh = h - 6 * k
    val face = 16 * k
    val roof = bh - face
    val rx = roof * .48f
    fun plan(dx: Float, dy: Float) = Path().apply {
        moveTo(x + rx + dx, y + dy)
        lineTo(x + bw + dx, y + dy)
        lineTo(x + bw + dx, y + roof + dy)
        lineTo(x + rx + dx, y + roof + dy)
        arcTo(Rect(x + dx, y + dy, x + 2 * rx + dx, y + roof + dy), 90f, 180f, false)
        close()
    }
    drawPath(plan(8 * k, face + 6 * k), shade)
    drawPath(plan(0f, face), operaFace)
    for (j in 0 until ((bw - rx - 8 * k) / (7 * k)).toInt()) round(x + rx + 4 * k + j * 7 * k, y + roof + 6 * k, 4 * k, 3.5f * k, lit, 1f)
    clipRect(x, y, x + rx, y + bh) {
        drawPath(plan(0f, face), operaGlass)
        drawRect(operaGlass, Offset(x, y + roof / 2), Size(rx, face))
        for (i in 1..8) {
            val angle = (90f + i * 10f) * PI.toFloat() / 180
            val p = Offset(x + rx + cos(angle) * rx, y + roof / 2 + sin(angle) * roof / 2)
            drawLine(Color(0xFF8DA7B3).copy(alpha = .6f), p, p + Offset(0f, face), k)
            if (i % 3 == 1) round(p.x + 1.5f * k, p.y + face * .35f, 3 * k, 4 * k, lit, 1f)
        }
    }
    drawPath(plan(0f, 0f), operaStone)
    for (i in 1..((bw - rx) / (16 * k)).toInt()) drawLine(Color.White.copy(alpha = .06f), Offset(x + rx + i * 16 * k, y), Offset(x + rx + i * 16 * k, y + roof), k)
    drawArc(operaGlass, 90f, 180f, false, Offset(x + 4 * k, y + 4 * k), Size(2 * rx - 8 * k, roof - 8 * k), style = Stroke(8 * k))
    for (i in 1..5) {
        val angle = (90f + i * 30f) * PI.toFloat() / 180
        val d = Offset(cos(angle) * rx, sin(angle) * roof / 2)
        val center = Offset(x + rx, y + roof / 2)
        drawLine(Color(0xFF8DA7B3).copy(alpha = .5f), center + d * .8f, center + d * .98f, k)
    }
    val stage = Offset(x + bw * .55f, y + 9 * k)
    drawRect(operaFace, stage + Offset(0f, 7 * k), Size(bw * .37f, roof * .52f))
    drawRect(lerp(operaStone, Palette.Cream, .2f), stage, Size(bw * .37f, roof * .52f))
    drawRect(Palette.Deep.copy(alpha = .3f), stage + Offset(bw * .05f, roof * .14f), Size(bw * .1f, roof * .12f))
}

// The Duomo turns its front to the viewer: a cross of marble roofs full of spires, and the pointed front over five portals.
private fun DrawScope.cathedral(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 100f
    withTransform({ translate(x, y); scale(k, k, Offset.Zero) }) {
        val bh = h / k - 6
        val roof = bh - 24
        translate(8f, 30f) { drawPath(duomoPlan, shade) }
        clipRect(right = 110f, bottom = bh) { translate(0f, 24f) { drawPath(duomoPlan, duomoFace) } }
        repeat(2) { s -> repeat(2) { j -> round(46 + (s * 2 - 1) * (37f + j * 6) - 1.2f, 56f, 2.4f, 9f, lit, 1.2f) } }
        clipRect(right = 110f, bottom = roof) { drawPath(duomoPlan, duomo) }
        drawRect(duomoLight.copy(alpha = .45f), Offset(37f, 14f), Size(18f, roof - 14))
        drawLine(duomoLight, Offset(46f, 14f), Offset(46f, roof), 1.2f)
        var row = 23f
        while (row < roof - 3) {
            for (dx in duomoColumns) if (row !in 24f..54f || abs(dx) < 32f) spire(46 + dx, row)
            row += 7
        }
        for (sx in transeptSpires) {
            spire(sx, 30f)
            spire(sx, 50f)
        }
        for (i in 0 until 6) spire(apseSpires[2 * i], apseSpires[2 * i + 1])
        drawCircle(duomoFace, 11f, Offset(46f, 39f))
        drawCircle(duomoCrossing, 11f, Offset(46f, 37f))
        drawCircle(duomoLight.copy(alpha = .5f), 11f, Offset(46f, 37f), style = ledge)
        for (i in 0 until 8) spire(46 + cos(i * PI.toFloat() / 4) * 8.5f, 40 + sin(i * PI.toFloat() / 4) * 8.5f, small = true)
        translate(46f, 37f) {
            drawPath(mainSpireLit, duomoLight)
            drawPath(mainSpireShade, duomoSpireShade)
        }
        drawCircle(Palette.Gold.copy(alpha = .3f), 3.2f, Offset(46f, 12f))
        drawCircle(Palette.Gold, 1.6f, Offset(46f, 12f))
        translate(46f, bh) { drawPath(duomoFront, duomoFacade) }
        for (i in 0 until 6) {
            val dx = duomoSteps[2 * i]
            val up = duomoSteps[2 * i + 1]
            drawRect(duomoLight, Offset(46 + dx - 1.2f, roof - up), Size(2.4f, 24 + up))
            spire(46 + dx, roof - up)
        }
        spire(46f, roof - 22)
        for (i in 0 until 5) {
            val dx = duomoPortals[2 * i]
            val dw = duomoPortals[2 * i + 1]
            drawRect(Color(0xFF3B3633), Offset(46 + dx - dw / 2, bh - dw * 1.3f), Size(dw, dw * 1.3f))
            drawArc(Color(0xFF3B3633), 180f, 180f, true, Offset(46 + dx - dw / 2, bh - dw * 1.8f), Size(dw, dw))
        }
        for (i in 1 until 5) round(46 + duomoPortals[2 * i] - 1.2f, bh - 16, 2.4f, 5f, lit, 1.2f)
        round(43.6f, bh - 21, 4.8f, 8f, lit, 2.4f)
    }
}

private fun DrawScope.spire(x: Float, y: Float, small: Boolean = false) {
    val s = if (small) .7f else 1f
    translate(x, y) { drawPath(if (small) smallSpirePath else spirePath, duomoLight) }
    drawLine(duomoFace, Offset(x + .3f * s, y - 6 * s), Offset(x + 1.1f * s, y), .6f)
}

private fun DrawScope.arcade(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 100f
    withTransform({ translate(x, y); scale(k, k, Offset.Zero) }) {
        val bh = h / k - 6
        val roof = bh - 16
        val cy = roof * .48f
        drawRect(shade, Offset(8f, 8f), Size(92f, bh - 2))
        drawRect(galleriaFront, Offset(0f, roof), Size(92f, 16f))
        repeat(2) { s ->
            for (j in 0 until ((92f / 2 - 9 - 6) / 6).toInt()) for (r in 0..1)
                round(46 + (s * 2 - 1) * (14f + j * 6) - 1.2f, roof + 3 + r * 6, 2.4f, 3.6f, lit, 1.2f)
        }
        drawRect(Color(0xFFC4B79C), Offset(0f, roof), Size(92f, 1.6f))
        drawRect(Color(0xFFC4B79C), Offset(35f, roof), Size(22f, 16f))
        drawRect(Color(0xFF3A342E), Offset(38.5f, roof + 6), Size(15f, 10f))
        drawArc(Color(0xFF3A342E), 180f, 180f, true, Offset(38.5f, roof + 1.5f), Size(15f, 9f))
        drawRect(Palette.Gold.copy(alpha = .35f), Offset(40.5f, roof + 6), Size(11f, 10f))
        drawRect(galleriaRoof, Offset.Zero, Size(92f, roof))
        repeat(4) { i ->
            drawRect(Color(0xFF999181).copy(alpha = .35f), Offset(if (i % 2 == 0) 2f else 57f, if (i < 2) 2f else cy + 11), Size(33f, roof / 2 - 13),
                style = thin)
        }
        drawRect(galleriaGlass, Offset(37f, 0f), Size(18f, roof))
        drawRect(galleriaGlass, Offset(0f, cy - 9), Size(92f, 18f))
        drawRect(Palette.Gold.copy(alpha = .14f), Offset(42.4f, 0f), Size(7.2f, roof))
        drawRect(Palette.Gold.copy(alpha = .14f), Offset(0f, cy - 3.6f), Size(92f, 7.2f))
        var r = 2f
        while (r < roof) { drawLine(galleriaIron, Offset(37f, r), Offset(55f, r), .6f); r += 4 }
        var c = 2f
        while (c < 92) { drawLine(galleriaIron, Offset(c, cy - 9), Offset(c, cy + 9), .6f); c += 4 }
        drawLine(galleriaIron, Offset(46f, 0f), Offset(46f, roof), 1f)
        drawLine(galleriaIron, Offset(0f, cy), Offset(92f, cy), 1f)
        drawRect(galleriaIron, Offset(37f, 0f), Size(18f, roof), style = vaultEdge)
        drawRect(galleriaIron, Offset(0f, cy - 9), Size(92f, 18f), style = vaultEdge)
        translate(46f, cy) { drawPath(galleriaOctagon, galleriaRoof) }
        drawCircle(galleriaIron, 13.5f, Offset(46f, cy))
        drawCircle(galleriaGlass, 12.5f, Offset(46f, cy))
        drawCircle(Palette.Gold.copy(alpha = .16f), 8f, Offset(46f, cy))
        for (i in 0 until 16) drawLine(galleriaIron, Offset(46f, cy), Offset(46 + cos(i * PI.toFloat() / 8) * 12.5f, cy + sin(i * PI.toFloat() / 8) * 12.5f), .6f)
        drawCircle(galleriaIron, 12.5f * .45f, Offset(46f, cy), style = domeRib)
        drawCircle(galleriaIron, 12.5f * .8f, Offset(46f, cy), style = domeRib)
        drawCircle(Color(0xFFB9C7C5), 2.4f, Offset(45.5f, cy - .5f))
    }
}

private fun DrawScope.tollhouse(x: Float, y: Float, w: Float, h: Float) {
    val k = w / 90f
    withTransform({ translate(x, y); scale(k, k, Offset.Zero) }) {
        val bh = h / k - 5
        val roof = bh - 14
        drawRect(Color(0xFF55605F), Offset(35f, 4f), Size(14f, bh - 4))
        for (j in 1..5) drawLine(Palette.Deep.copy(alpha = .3f), Offset(35f, 4 + j * (bh - 4) / 6), Offset(49f, 4 + j * (bh - 4) / 6), .6f)
        val trim = Color(0xFFC6BBA6)
        repeat(2) { i ->
            val px = i * 49f
            drawRect(shade, Offset(px + 5, 6f), Size(35f, bh - 2))
            drawRect(caselloFace, Offset(px, roof), Size(35f, 14f))
            drawRect(Color(0xFF3E3832), Offset(px + 2.5f, roof + 3), Size(30f, 9.5f))
            drawRect(lit, Offset(px + 15, roof + 5.5f), Size(5f, 7f))
            for (j in 0..3) drawRect(casello, Offset(px + 3 + j * 9, roof + 2.5f), Size(2f, 10.5f))
            drawRect(trim, Offset(px, roof), Size(35f, 2.5f))
            drawRect(caselloFace, Offset(px - .5f, roof + 12.8f), Size(36f, 1.2f))
            drawRect(casello, Offset(px, 0f), Size(35f, roof))
            drawRect(trim, Offset(px + 1.5f, 1.5f), Size(32f, roof - 3), style = ledge)
            drawRect(Color(0xFF968A75), Offset(px + 7, 7f), Size(21f, roof - 14))
            repeat(4) { j ->
                val sx = px + if (j % 2 == 0) 3.5f else 31.5f
                val sy = if (j < 2) 6f else roof - 1
                drawOval(shade, Offset(sx - 1.6f, sy - .6f), Size(3.4f, 1.4f))
                drawLine(Color(0xFFD8CFBF), Offset(sx, sy), Offset(sx, sy - 4.5f), 1.6f, StrokeCap.Round)
                drawCircle(Color(0xFFD8CFBF), 1f, Offset(sx, sy - 5.6f))
            }
        }
    }
}

// Street life is dimmed like the city. Its warm accents stay small, so the march and the threats are still the brightest things.
private val awning = Color(0xFF8E3832)
private val awningDark = Color(0xFF6A2A26)
private val awningEdge = Color(0xFFB0554A)
private val marble = Color(0xFFC4BFB1)
private val rattan = Color(0xFF86683F)
private val bakery = Color(0xFF2D4F7C)
private val linen = Color(0xFFD2C4A0)
private val crust = Color(0xFFD7A55C)
private val crumb = Color(0xFFF0D08E)
private val bake = Color(0xFFC98A3F)
private val wood = Color(0xFF6A4D35)
private val crepe = Color(0xFFE9CB8A)
private val mustard = Color(0xFFC29A45)
private val steel = Color(0xFF4A5257)
private val iron = Color(0xFF2A2C2E)
private val tyre = Color(0xFF1D2427)
private val parisGreen = Color(0xFF2E6A55)
private val parisGreenDark = Color(0xFF224D3F)
private val parisGreenLight = Color(0xFF5C987D)
private val amber = Color(0xFFF0A548)
private val accordionRed = Color(0xFFA73C3C)
private val bikeGreen = Color(0xFF5FA07E)
private val bikeBlue = Color(0xFF5B8DB8)
private val ember = Color(0xFFE5783A)
private val meat = Color(0xFF6E3B27)
private val char = Color(0xFF3B2016)
private val burek = Color(0xFFD59A48)
private val burekDark = Color(0xFFA06A33)
private val checks = Color(0xFFAE4239)
private val cloth = Color(0xFFDCD3C0)
private val trafikaBlue = Color(0xFF3C6FA2)
private val board = Color(0xFFD8CDAA)
private val brass = Color(0xFFCC9C3A)
private val pigeon = Color(0xFF969CB4)
private val pigeonWing = Color(0xFF767D93)
private val smoke = Color(0xFFB9C0C2)
private val gelatoPink = Color(0xFFC98894)
private val flavours = listOf(gelatoPink, Color(0xFFA9C28A), Color(0xFFE8D9A8), Color(0xFF7A4B33), Color(0xFFE9C46A), Color(0xFFF0EAD8))
private val pasticceriaGreen = Color(0xFF8DAE9C)
private val panettoneCrust = Color(0xFFB8702F)
private val panettonePaper = Color(0xFF8C5634)
private val espressoRed = Color(0xFF7D2F3B)
private val vedovellaGreen = Color(0xFF2F5E46)
private val vedovellaLight = Color(0xFF5E8C70)
private val vedovellaDark = Color(0xFF214433)
private val scooterBlue = Color(0xFF7E9EC0)
private val scooterBodies = listOf(scooterBlue, Color(0xFFD5CBB0), scooterBlue, Color(0xFFA44B43))
private val boutiqueWindow = Color(0xFFD6C49A)
private val dresses = listOf(Color(0xFFA33B3B), Color(0xFF2A2A2E), Color(0xFFB08A5A), Color(0xFFE2DCCD))
private val mannequinSpots = floatArrayOf(-15f, -8f, 8f, 15f)
private val mannequinDress = polygon(-1.6f, -12.5f, 1.6f, -12.5f, 2.8f, -3.2f, -2.8f, -3.2f)
private val scooterCowl = Path().apply { moveTo(-5.6f, -2.8f); quadraticTo(-6f, -6.8f, -1.4f, -6.4f); lineTo(-.2f, -2.8f); close() }
private val papers = listOf(Palette.Cream, Color(0xFFB0574A), Color(0xFF7C9AA8), Color(0xFFBFA46A))
private val folk = listOf(Color(0xFF5E6E7E), Color(0xFF7A5E4E), Color(0xFF55705F), Color(0xFF8A7D6A))
private val thin = Stroke(1f)
private val roll = Stroke(2.4f, cap = StrokeCap.Round)
private val crescent = Stroke(3.2f, cap = StrokeCap.Round)
private val letters = PathEffect.dashPathEffect(floatArrayOf(1.6f, 1f))
// Props are drawn 5/4 the size of the numbers below, so their people stand as tall as the marchers.
// Each kind's footprint in game/Cities.kt is 5/4 of its drawing, rounded up.
private const val PROP_SCALE = 1.25f
// The rendering test finds each prop by this colour, so keep it apart from the street, building, and landmark colours.
internal val PropKind.signature: Color get() = when (this) {
    PropKind.Boulangerie -> bakery
    PropKind.Crepes -> crepe
    PropKind.Cafe -> awning
    PropKind.MorrisColumn, PropKind.WallaceFountain, PropKind.Metro, PropKind.Newsstand -> parisGreen
    PropKind.Accordion -> accordionRed
    PropKind.Bikes -> bikeGreen
    PropKind.Grill -> ember
    PropKind.Pekara -> burek
    PropKind.Kafana -> checks
    PropKind.Trafika -> trafikaBlue
    PropKind.Chess -> board
    PropKind.BrassBand -> brass
    PropKind.Gelato -> gelatoPink
    PropKind.Pasticceria -> pasticceriaGreen
    PropKind.Espresso -> espressoRed
    PropKind.Vedovella -> vedovellaGreen
    PropKind.Scooters -> scooterBlue
    PropKind.Boutique -> boutiqueWindow
    PropKind.Pigeons -> pigeon
}

// In prop art, x runs along the street and y toward it. The plan turns to face the street; people, columns, and smoke stay upright.
private fun Prop.at(x: Float, y: Float) = Offset(position.x + x * sin(angle) + y * cos(angle), position.y - x * cos(angle) + y * sin(angle))
// A map direction in the turned frame, so shadows fall and walls rise the same way whichever way the prop faces.
private fun Prop.screen(dx: Float, dy: Float) = Offset(dx * sin(angle) - dy * cos(angle), dx * cos(angle) + dy * sin(angle))
// Figures lower on the screen are nearer, so a row of them is drawn from the back.
private fun Prop.backToFront(k: Int, count: Int) = if (cos(angle) < 0f) k else count - 1 - k
private inline fun DrawScope.turned(p: Prop, block: DrawScope.() -> Unit) =
    withTransform({ translate(p.position.x, p.position.y); rotate(p.angle * 180f / PI.toFloat() - 90f, Offset.Zero) }, block)

// What is drawn between the walls and the lid shows on the near wall only.
private fun DrawScope.walls(p: Prop, x: Float, y: Float, w: Float, h: Float, tall: Float, color: Color) {
    drawRect(shade, Offset(x, y) + p.screen(3f, 4f), Size(w, h))
    for (i in 0..3) drawRect(color, Offset(x, y) + p.screen(0f, -tall * i / 4f), Size(w, h))
}
private fun DrawScope.lid(p: Prop, x: Float, y: Float, w: Float, h: Float, tall: Float, color: Color) =
    drawRect(color, Offset(x, y) + p.screen(0f, -tall), Size(w, h))

private fun DrawScope.sitter(at: Offset, coat: Color, skin: Color) {
    drawOval(shade, at + Offset(-3.5f, -.5f), Size(8f, 3.5f))
    round(at.x - 2.6f, at.y - 7f, 5.2f, 7f, coat, 2f)
    drawCircle(skin, 2.3f, at + Offset(0f, -9f))
    drawArc(Color(0xFF3C3934), 180f, 180f, true, at + Offset(-2.4f, -11.6f), Size(4.8f, 4.6f))
}

private fun DrawScope.musician(at: Offset, coat: Color, skin: Color, hat: Color) {
    drawLine(Palette.Ink, at + Offset(-1.7f, -7f), at + Offset(-2f, 0f), 2.4f, StrokeCap.Round)
    drawLine(Palette.Ink, at + Offset(1.7f, -7f), at + Offset(2f, 0f), 2.4f, StrokeCap.Round)
    round(at.x - 3.8f, at.y - 16.5f, 7.6f, 10.5f, coat, 2.5f)
    drawCircle(skin, 3.1f, at + Offset(0f, -19.8f))
    round(at.x - 3.4f, at.y - 23.6f, 6.8f, 2.6f, hat, 1.2f)
}

private fun DrawScope.note(at: Offset, time: Float, phase: Float) {
    val t = (time * .45f + phase) % 1f
    val p = at + Offset(sin(t * 6f + phase * 9f) * 2f, -t * 8f)
    val color = Palette.Gold.copy(alpha = .55f * (1f - t))
    drawCircle(color, 1.2f, p)
    drawLine(color, p + Offset(1f, 0f), p + Offset(1f, -4f), .7f)
}

private fun DrawScope.prop(p: Prop, time: Float, hero: Vec2?, people: List<Marcher>) = scale(PROP_SCALE, PROP_SCALE, p.position.offset()) {
    when (p.kind) {
        PropKind.Boulangerie -> boulangerie(p)
        PropKind.Crepes -> crepes(p)
        PropKind.Cafe -> cafe(p)
        PropKind.MorrisColumn -> morrisColumn(p)
        PropKind.WallaceFountain -> wallaceFountain(p)
        PropKind.Metro -> metro(p)
        PropKind.Newsstand -> newsstand(p)
        PropKind.Accordion -> accordion(p, time)
        PropKind.Bikes -> bikes(p)
        PropKind.Grill -> grill(p, time)
        PropKind.Pekara -> pekara(p)
        PropKind.Kafana -> kafana(p)
        PropKind.Trafika -> trafika(p)
        PropKind.Chess -> chess(p)
        PropKind.BrassBand -> brassBand(p, time)
        PropKind.Gelato -> gelato(p)
        PropKind.Pasticceria -> pasticceria(p)
        PropKind.Espresso -> espresso(p)
        PropKind.Vedovella -> vedovella(p)
        PropKind.Scooters -> scooters(p)
        PropKind.Boutique -> boutique(p)
        PropKind.Pigeons -> pigeons(p, time, hero, people)
    }
}

private fun DrawScope.baguette(a: Offset, b: Offset) {
    drawLine(Color(0xFFA87538), a + Offset(.3f, .5f), b + Offset(.3f, .5f), 3.4f, StrokeCap.Round)
    drawLine(crust, a, b, 3.2f, StrokeCap.Round)
    drawLine(crumb, a + Offset(.2f, -.7f), b + Offset(-.2f, -.7f), 1f, StrokeCap.Round)
    for (i in 1..3) {
        val c = a + (b - a) * (i / 4f)
        drawLine(Color(0xFFA87538), c + Offset(-1f, .9f), c + Offset(1f, -.9f), .7f)
    }
}

private fun DrawScope.croissant(c: Offset) {
    drawArc(Color(0xFF9A5E28), 195f, 150f, false, c - Offset(3.4f, 2.8f), Size(6.8f, 6.8f), style = crescent)
    drawArc(bake, 200f, 140f, false, c - Offset(3.4f, 3.2f), Size(6.8f, 6.8f), style = crescent)
    drawLine(Color(0xFF9A5E28), c + Offset(-1.6f, -3.6f), c + Offset(-1f, -1.4f), .6f)
    drawLine(Color(0xFF9A5E28), c + Offset(1.6f, -3.6f), c + Offset(1f, -1.4f), .6f)
    drawCircle(crumb, .8f, c + Offset(0f, -3.2f))
}

private fun DrawScope.boulangerie(p: Prop) = turned(p) {
    drawOval(Palette.Gold.copy(alpha = .13f), Offset(-20f, -6f), Size(40f, 18f))
    drawRect(bakery, Offset(-20f, -12f), Size(40f, 5f))
    for (i in 0..6) drawRect(Palette.Gold.copy(alpha = .75f), Offset(-10f + i * 3f, -10f), Size(2f, 1.2f))
    for (i in 0..9) drawRect(if (i % 2 == 0) linen else bakery, Offset(-20f + i * 4f, -7f), Size(4f, 4.5f))
    for (i in 0..9) drawCircle(if (i % 2 == 0) linen else bakery, 2f, Offset(-18f + i * 4f, -2.5f))
    drawRect(shade, Offset(-15f, 2f) + p.screen(2f, 3f), Size(30f, 9f))
    round(-15f, 1.5f, 30f, 9.5f, wood, 1.5f)
    drawRect(Color(0xFF8A6A4A), Offset(-15f, 1.5f), Size(30f, 1.2f))
    drawOval(Color(0xFF8A6440), Offset(-14f, 2.5f), Size(13f, 7.5f))
    baguette(Offset(-13f, 8f), Offset(-2f, 3f))
    baguette(Offset(-12f, 9.5f), Offset(0f, 5f))
    croissant(Offset(5f, 8.5f))
    croissant(Offset(11.5f, 8.5f))
}

private fun DrawScope.crepes(p: Prop) = turned(p) {
    drawOval(ember.copy(alpha = .12f), Offset(-14f, -4f), Size(28f, 16f))
    walls(p, -13f, -11f, 26f, 9f, 5f, Color(0xFF8D826C))
    lid(p, -13f, -11f, 26f, 9f, 5f, mustard)
    for (i in 0..5) drawRect(Color(0xFFE6DCC0), Offset(-13f + i * 4.6f, -11f) + p.screen(0f, -5f), Size(2.2f, 9f))
    round(-14f, -2f, 28f, 11f, steel, 1.5f)
    for (i in 0..1) {
        drawCircle(iron, 5f, Offset(i * 13f - 6.5f, 3.5f))
        drawCircle(ember.copy(alpha = .35f), 5f, Offset(i * 13f - 6.5f, 3.5f), style = thin)
    }
    drawCircle(crepe, 4.4f, Offset(-6.5f, 3.5f))
    for (i in 0..3) drawCircle(Color(0xFFC79A55), .55f, Offset(-6.5f + cos(i * 1.7f) * 2.4f, 3.5f + sin(i * 1.7f) * 2.4f))
    drawArc(crepe, 200f, 100f, true, Offset(1.5f, -1.5f), Size(10f, 10f))
    drawArc(Color(0xFFC79A55), 200f, 100f, true, Offset(1.5f, -1.5f), Size(10f, 10f), style = thin)
    round(11.5f, 4f, 3.5f, 5f, Color(0xFF26302F), .6f)
    drawLine(Palette.Cream.copy(alpha = .5f), Offset(12.2f, 5.5f), Offset(14.2f, 5.5f), .6f)
    drawLine(Palette.Cream.copy(alpha = .5f), Offset(12.2f, 7f), Offset(14.2f, 7f), .6f)
}

private fun DrawScope.cafe(p: Prop) {
    turned(p) {
        drawRect(Palette.Gold.copy(alpha = .06f), Offset(-23f, -14f), Size(46f, 28f))
        drawOval(Palette.Gold.copy(alpha = .13f), Offset(-21f, -9f), Size(42f, 18f))
        for (i in -1..1) {
            val x = i * 14.5f
            drawCircle(shade, 4f, Offset(x, 6f) + p.screen(1.5f, 2f))
            for (k in 0..1) {
                val cx = x + k * 7.2f - 3.6f
                round(cx - 1.8f, -1.5f, 3.6f, 3.6f, rattan, 1f)
                drawLine(Color(0xFF5E4729), Offset(cx - 1.8f, -1.8f), Offset(cx + 1.8f, -1.8f), 1.2f)
            }
            drawCircle(Color(0xFF8E897D), 3.8f, Offset(x, 5f))
            drawCircle(marble, 3.1f, Offset(x, 5f))
        }
        drawRect(awningDark, Offset(-23f, -14f), Size(46f, 2.5f))
        drawRect(awning, Offset(-23f, -11.5f), Size(46f, 6.5f))
        for (i in 0..8) drawRect(Palette.Cream.copy(alpha = .7f), Offset(-8.5f + i * 2f, -8.6f), Size(1.3f, 1.2f))
        for (i in 0..11) drawCircle(awningEdge, 2f, Offset(-21f + i * 3.82f, -5f))
        round(18f, 7.5f, 4f, 5f, Color(0xFF26302F), .6f)
        drawLine(Palette.Cream.copy(alpha = .5f), Offset(18.7f, 9f), Offset(21.3f, 9f), .6f)
        drawLine(Palette.Cream.copy(alpha = .5f), Offset(18.7f, 10.5f), Offset(21.3f, 10.5f), .6f)
    }
    sitter(p.at(-18.1f, 1f), folk[0], skins[0])
    sitter(p.at(10.9f, 1f), Color(0xFF8C6A5C), skins[2])
}

private fun DrawScope.morrisColumn(p: Prop) {
    val c = p.position.offset()
    drawOval(shade, c + Offset(-6f, -2.5f), Size(13.5f, 5.5f))
    round(c.x - 6f, c.y - 3.5f, 12f, 3.5f, parisGreenDark, 1.5f)
    drawRect(parisGreen, Offset(c.x - 5f, c.y - 25f), Size(10f, 22f))
    drawRect(parisGreenLight, Offset(c.x - 3.8f, c.y - 25f), Size(1.6f, 22f))
    for (i in 0..2) drawRect(papers[i + 1], Offset(c.x - 5f + i * 3.34f, c.y - 20f), Size(3.34f, 11f))
    for (i in 0..2) for (j in 0..2) drawLine(Palette.Ink.copy(alpha = .35f), Offset(c.x - 4.3f + i * 3.34f, c.y - 18f + j * 3f),
        Offset(c.x - 2.4f + i * 3.34f, c.y - 18f + j * 3f), .6f)
    drawRect(parisGreenLight, Offset(c.x - 6.5f, c.y - 27f), Size(13f, 2.2f))
    drawArc(parisGreen, 180f, 180f, true, Offset(c.x - 5.5f, c.y - 32.5f), Size(11f, 11f))
    drawArc(parisGreenLight, 200f, 50f, false, Offset(c.x - 4f, c.y - 31f), Size(8f, 8f), style = thin)
    drawCircle(parisGreenLight, 1.3f, Offset(c.x, c.y - 33.4f))
}

private fun DrawScope.wallaceFountain(p: Prop) {
    val c = p.position.offset()
    drawOval(shade, c + Offset(-5f, -2f), Size(11f, 4.5f))
    round(c.x - 5f, c.y - 3.5f, 10f, 3.5f, parisGreenDark, 1f)
    round(c.x - 4f, c.y - 6f, 8f, 2.8f, parisGreen, 1f)
    drawLine(parisGreenDark, Offset(c.x, c.y - 6f), Offset(c.x, c.y - 14.5f), 1.6f)
    drawLine(Color(0xFF9CC7D6).copy(alpha = .8f), Offset(c.x + 1f, c.y - 15f), Offset(c.x + 1f, c.y - 6f), .8f)
    for (i in 0..1) {
        val k = i * 2f - 1f
        drawLine(parisGreen, Offset(c.x + k * 2.9f, c.y - 6f), Offset(c.x + k * 2.6f, c.y - 14.5f), 2.2f, StrokeCap.Round)
        drawCircle(parisGreenLight, .9f, Offset(c.x + k * 2.6f, c.y - 15.6f))
    }
    round(c.x - 5.5f, c.y - 17.8f, 11f, 1.9f, parisGreenLight, .6f)
    drawArc(parisGreen, 180f, 180f, true, Offset(c.x - 4.5f, c.y - 21.6f), Size(9f, 8f))
    drawLine(parisGreen, Offset(c.x, c.y - 21f), Offset(c.x, c.y - 23.6f), 1.2f)
    drawCircle(parisGreenLight, .9f, Offset(c.x, c.y - 23.8f))
}

private fun DrawScope.metro(p: Prop) {
    turned(p) {
        drawRect(Color(0xFF121A1E), Offset(-8f, -13f), Size(16f, 24f))
        // The stairs go down away from the street, so the steps fade into the dark.
        for (i in 0..6) drawLine(Color(0xFF6E7F80).copy(alpha = .08f + i * .06f), Offset(-7.5f, -10.5f + i * 3.3f), Offset(7.5f, -10.5f + i * 3.3f), 1.1f)
        for (k in 0..1) {
            val x = k * 19f - 9.5f
            drawLine(parisGreen, Offset(x, -14.5f), Offset(x, 12f), 2.4f)
            for (i in 0..7) drawCircle(parisGreenLight, .7f, Offset(x, -13f + i * 3.4f))
        }
        drawLine(parisGreen, Offset(-9.5f, -14.5f), Offset(9.5f, -14.5f), 2.4f)
        for (i in 0..5) drawCircle(parisGreenLight, .7f, Offset(-7f + i * 2.8f, -14.5f))
    }
    val left = p.at(-9.5f, 12f)
    val right = p.at(9.5f, 12f)
    lamp(left)
    lamp(right)
    drawLine(Color(0xFFE6D9B0), left - Offset(0f, 11.5f), right - Offset(0f, 11.5f), 3.2f)
    drawLine(Palette.Ink.copy(alpha = .55f), left - Offset(0f, 11.5f), right - Offset(0f, 11.5f), 1f, pathEffect = letters)
}

private fun DrawScope.lamp(post: Offset) {
    drawOval(shade, post + Offset(-1.5f, -.8f), Size(4.5f, 2f))
    drawLine(parisGreen, post, post - Offset(0f, 13f), 1.6f)
    drawCircle(amber.copy(alpha = .22f), 4.2f, post - Offset(0f, 14.5f))
    drawCircle(amber, 2.1f, post - Offset(0f, 14.5f))
}

private fun DrawScope.newsstand(p: Prop) = turned(p) {
    drawOval(Palette.Gold.copy(alpha = .12f), Offset(-13f, 1f), Size(26f, 11f))
    for (i in 0..6) round(-11.5f + i * 3.3f, 7.2f, 2.8f, 3.4f, papers[i % 4], .4f)
    walls(p, -10f, -8f, 20f, 15f, 7f, parisGreenDark)
    for (i in 0..4) round(-8.6f + i * 3.6f, 3.6f, 2.8f, 3f, papers[(i + 2) % 4], .3f)
    val roof = p.screen(0f, -7.5f)
    drawRoundRect(parisGreen, Offset(-12f, -10f) + roof, Size(24f, 19f), CornerRadius(4f))
    for (i in 0..3) drawLine(parisGreenLight.copy(alpha = .55f), Offset(0f, -.5f) + roof,
        Offset(if (i % 2 == 0) -10.5f else 10.5f, if (i < 2) -8.5f else 7.5f) + roof, .8f)
    drawCircle(parisGreenLight, 3.4f, Offset(0f, -.5f) + roof)
    drawCircle(parisGreen, 2.2f, Offset(0f, -.5f) + roof)
    drawCircle(amber, 1.1f, Offset(0f, -.5f) + roof)
}

private fun DrawScope.accordion(p: Prop, time: Float) {
    turned(p) {
        round(-1f, 1f, 9f, 5f, Color(0xFF2B2420), 1f)
        round(-.2f, 1.8f, 7.4f, 3.4f, accordionRed, .8f)
        for (i in 0..2) drawCircle(Palette.Gold, .6f, Offset(1.5f + i * 2f, 3.2f + (i % 2) * .9f))
    }
    val feet = p.at(-3.5f, -1f)
    drawOval(shade, feet + Offset(-5f, -1.5f), Size(11f, 4.5f))
    rotate(sin(time * 2.6f) * 5f, feet) {
        musician(feet, Color(0xFF2E3B55), skins[0], Color(0xFF26201E))
        for (i in 0..2) drawLine(Palette.Cream.copy(alpha = .8f), feet + Offset(-3.6f, -14.5f + i * 3f), feet + Offset(3.6f, -14.5f + i * 3f), .9f)
        val squeeze = 7f + sin(time * 3.2f) * 1.6f
        round(feet.x - squeeze / 2, feet.y - 15f, squeeze, 7f, accordionRed, 1f)
        for (i in 1..3) drawLine(Color(0xFF6E2226), feet + Offset(-squeeze / 2 + i * squeeze / 4, -15f), feet + Offset(-squeeze / 2 + i * squeeze / 4, -8f), .7f)
        drawRect(Palette.Cream, Offset(feet.x + squeeze / 2 - 1.2f, feet.y - 15f), Size(1.6f, 7f))
    }
    note(feet + Offset(4f, -22f), time, 0f)
    note(feet + Offset(-2f, -24f), time, .5f)
}

private fun DrawScope.bikes(p: Prop) {
    turned(p) { drawLine(Color(0xFF59656B), Offset(-16f, -4f), Offset(17f, -4f), 1.4f) }
    val post = p.at(-18f, -4f)
    drawOval(shade, post + Offset(-1.5f, -.8f), Size(5f, 2f))
    round(post.x - 1.6f, post.y - 9f, 3.2f, 9f, Color(0xFF59656B), 1f)
    drawRect(Color(0xFF8FB8D9).copy(alpha = .8f), Offset(post.x - 1f, post.y - 8f), Size(2f, 2.4f))
    for (k in 0..3) {
        val i = p.backToFront(k, 4)
        bike(p.at(-10.5f + i * 7.5f, 1f), if (i == 2) bikeBlue else bikeGreen)
    }
}

private fun DrawScope.bike(at: Offset, frame: Color) {
    drawOval(shade, at + Offset(-5.5f, -.9f), Size(12f, 2.6f))
    drawCircle(tyre, 2.5f, at + Offset(-3.4f, -2.5f), style = thin)
    drawCircle(tyre, 2.5f, at + Offset(3.4f, -2.5f), style = thin)
    drawLine(frame, at + Offset(-3.4f, -2.5f), at + Offset(-.2f, -2.6f), 1.3f)
    drawLine(frame, at + Offset(-.2f, -2.6f), at + Offset(2.4f, -6.4f), 1.4f)
    drawLine(frame, at + Offset(2.4f, -6.4f), at + Offset(3.4f, -2.5f), 1.2f)
    drawLine(frame, at + Offset(-.2f, -2.6f), at + Offset(-1.1f, -6.2f), 1.2f)
    drawLine(tyre, at + Offset(-2.2f, -6.6f), at + Offset(0f, -6.6f), 1.2f, StrokeCap.Round)
    drawLine(tyre, at + Offset(2.4f, -6.4f), at + Offset(2f, -7.8f), 1f)
    drawRect(frame, Offset(at.x + 2.6f, at.y - 8.6f), Size(3.2f, 2.4f))
}

private fun DrawScope.grill(p: Prop, time: Float) {
    // With the street up the screen, the counter is behind the kiosk and goes down first.
    val behind = sin(p.angle) < 0f
    turned(p) {
        drawOval(ember.copy(alpha = .14f), Offset(-13f, -1f), Size(26f, 13f))
        if (!behind) grillKiosk(p)
        round(-12f, 1.5f, 24f, 9f, iron, 1.5f)
        drawRect(ember, Offset(-10.8f, 2.7f), Size(21.6f, 6.6f))
        for (i in 0..4) drawLine(iron, Offset(-8.8f + i * 4.4f, 2.7f), Offset(-8.8f + i * 4.4f, 9.3f), .6f)
        for (k in 0..1) {
            val x = k * 6f - 6.5f
            drawCircle(meat, 2.9f, Offset(x, 6f))
            drawLine(char, Offset(x - 1.6f, 4.9f), Offset(x + 1.2f, 7.4f), .7f)
            drawLine(char, Offset(x - .2f, 4.1f), Offset(x + 2f, 6.3f), .7f)
        }
        for (i in 0..3) drawLine(Color(0xFF7B4529), Offset(4.6f + i * 1.7f, 3.9f), Offset(4.6f + i * 1.7f, 8.1f), 1.3f, StrokeCap.Round)
        if (behind) grillKiosk(p)
    }
    val from = p.at(-2f, 6f)
    for (k in 0..3) {
        val t = (time * .3f + k / 4f) % 1f
        drawCircle(smoke.copy(alpha = .22f * (1f - t) * min(1f, t * 5f + .4f)), 2.2f + t * 4f, from + Offset(sin(t * 5f + k) * 2.2f + t * 3.5f, -6f - t * 14f))
    }
}

private fun DrawScope.grillKiosk(p: Prop) {
    walls(p, -12f, -12f, 24f, 9f, 7f, Color(0xFF6B4A35))
    drawRect(Palette.Gold.copy(alpha = .55f), Offset(-8f, -8.5f), Size(16f, 4f))
    lid(p, -12f, -12f, 24f, 9f, 7f, Color(0xFF867E73))
    drawRect(Color(0xFFA4443A), Offset(-12f, -5f) + p.screen(0f, -7f), Size(24f, 2f))
}

private fun DrawScope.burekPan(c: Offset, cut: Boolean) {
    drawCircle(shade, 7f, c + Offset(.8f, 1f))
    drawCircle(steel, 7f, c)
    drawCircle(burek, 6.1f, c)
    for (i in 1..3) {
        val r = i * 1.55f
        drawArc(burekDark, r * 40f, 290f, false, c - Offset(r, r), Size(2 * r, 2 * r), style = thin)
    }
    if (cut) drawArc(steel, -40f, 55f, true, c - Offset(6.1f, 6.1f), Size(12.2f, 12.2f))
}

private fun DrawScope.pekara(p: Prop) = turned(p) {
    drawOval(Palette.Gold.copy(alpha = .13f), Offset(-19f, -6f), Size(38f, 17f))
    drawRect(Color(0xFF2E5961), Offset(-19f, -12f), Size(38f, 5.5f))
    for (i in 0..5) drawRect(Palette.Cream.copy(alpha = .6f), Offset(-8.5f + i * 3f, -10f), Size(2f, 1.3f))
    drawRect(Color(0xFF8FA7AE).copy(alpha = .3f), Offset(-18f, -6.5f), Size(36f, 6.5f))
    drawRect(Color(0xFF8FA7AE).copy(alpha = .6f), Offset(-18f, -6.5f), Size(36f, 6.5f), style = thin)
    for (i in 0..4) drawArc(crumb, 200f, 200f, false, Offset(-16f + i * 3.4f, -5f), Size(2.6f, 2.6f), style = thin)
    for (i in 0..4) drawCircle(burek, 1.1f, Offset(2.5f + i * 3.2f, -3.2f))
    burekPan(Offset(-9f, 5f), true)
    burekPan(Offset(7f, 5f), false)
    for (i in 0..2) drawCircle(Color(0xFFE6E2D6), 1.3f, Offset(15.5f + (i % 2) * 1.6f, 2.5f + i * 2.4f))
}

private fun DrawScope.kafana(p: Prop) {
    turned(p) {
        drawRect(Color(0xFF3A3128).copy(alpha = .6f), Offset(-23f, -14f), Size(46f, 28f))
        for (i in 1..5) drawLine(Palette.Deep.copy(alpha = .25f), Offset(-23f, -14f + i * 4.6f), Offset(23f, -14f + i * 4.6f), .6f)
        drawOval(Palette.Gold.copy(alpha = .1f), Offset(-22f, -12f), Size(44f, 18f))
        drawLine(Color(0xFF5B5146), Offset(-22f, -11.5f), Offset(22f, -11.5f), .6f)
        for (i in 0..8) {
            drawCircle(Palette.Gold.copy(alpha = .16f), 2.6f, Offset(-21f + i * 5.25f, -11.2f))
            drawCircle(Palette.Gold.copy(alpha = .85f), 1f, Offset(-21f + i * 5.25f, -11.2f))
        }
        for (i in -1..1) {
            val x = i * 14.5f
            for (k in 0..1) round(x + k * 12.8f - 8f, -.2f, 3.2f, 3.6f, Color(0xFF5A4130), .8f)
            drawRect(shade, Offset(x - 4.5f, -2.5f) + p.screen(1.5f, 2f), Size(9f, 9f))
            drawRect(cloth, Offset(x - 4.5f, -2.5f), Size(9f, 9f))
            for (r in 0..2) for (c in 0..2) if ((r + c) % 2 == 0) drawRect(checks, Offset(x - 4.5f + c * 3f, -2.5f + r * 3f), Size(3f, 3f))
        }
        drawRect(Color(0xFF4A3A2C), Offset(-22f, 10.5f), Size(44f, 3.5f))
        for (i in 0..10) drawCircle(if (i % 2 == 0) Color(0xFF4E7A55) else Color(0xFF3F6648), 2.1f, Offset(-21f + i * 4.2f, 10.6f))
    }
    sitter(p.at(-20.9f, 1.6f), folk[1], skins[1])
    sitter(p.at(8.1f, 1.6f), folk[2], skins[3])
}

private fun DrawScope.trafika(p: Prop) = turned(p) {
    drawOval(Palette.Gold.copy(alpha = .14f), Offset(-10f, 1f), Size(20f, 9f))
    for (i in 0..4) round(-9.5f + i * 3.9f, 6f, 3.2f, 3.6f, papers[(i + 1) % 4], .4f)
    walls(p, -9f, -8f, 18f, 13f, 8f, Color(0xFFB3AB95))
    drawRect(Palette.Gold.copy(alpha = .6f), Offset(-5.5f, -1.5f), Size(11f, 4.5f))
    drawRect(Color(0xFF6B5E48), Offset(-5.5f, -1.5f), Size(11f, 4.5f), style = thin)
    lid(p, -9.5f, -8.5f, 19f, 14f, 8f, trafikaBlue)
    val roof = p.screen(0f, -8f)
    drawRect(Color(0xFFB5573C), Offset(-9.5f, 4f) + roof, Size(19f, 1.5f))
    drawRect(Color(0xFF6E94BC).copy(alpha = .6f), Offset(-9.5f, -8.5f) + roof, Size(19f, 14f), style = thin)
}

private fun DrawScope.chess(p: Prop) {
    turned(p) {
        for (t in 0..1) {
            val x = t * 19f - 9.5f
            drawCircle(Color(0xFF6F6D66), 2.6f, Offset(x + (t * 2 - 1) * 8f, 0f))
            drawRect(shade, Offset(x - 6f, -6f) + p.screen(2f, 3f), Size(12f, 12f))
            round(x - 6f, -6f, 12f, 12f, Color(0xFF8C8A82), 1f)
            drawRect(Color(0xFF3D3A35), Offset(x - 5f, -5f), Size(10f, 10f))
            for (r in 0..3) for (c in 0..3) if ((r + c) % 2 == 0) drawRect(board, Offset(x - 5f + c * 2.5f, -5f + r * 2.5f), Size(2.5f, 2.5f))
            for (i in 0..2) {
                drawCircle(Palette.Cream, .6f, Offset(x - 3.75f + i * 2.5f, 3.75f - (i % 2) * 2.5f))
                drawCircle(Palette.Ink, .6f, Offset(x - 1.25f + i * 2.5f, -3.75f + (i % 2) * 2.5f))
            }
        }
    }
    sitter(p.at(-17.5f, 0f), folk[3], skins[1])
    sitter(p.at(17.5f, 0f), folk[0], skins[2])
    sitter(p.at(0f, 0f), folk[1], skins[0])
}

private fun DrawScope.brassBand(p: Prop, time: Float) {
    for (k in 0..3) {
        val i = p.backToFront(k, 4)
        val feet = p.at(-14f + i * 9.3f, 1f)
        drawOval(shade, feet + Offset(-5f, -1.5f), Size(11f, 4.5f))
        rotate(sin(time * 3f + i * 1.3f) * 4f, feet) {
            musician(feet, if (i == 2) Color(0xFF3E3A36) else Color(0xFF9A3A31), skins[i % skins.size], Color(0xFF5D5B46))
            drawLine(Color(0xFFD8D2C4), feet + Offset(0f, -16f), feet + Offset(0f, -7f), 1.3f)
            when (i) {
                1 -> {
                    drawCircle(brass, 5.6f, feet + Offset(0f, -11f), style = roll)
                    drawCircle(brass, 3.4f, feet + Offset(3.6f, -20.5f))
                    drawCircle(Color(0xFF7A5A22), 2f, feet + Offset(3.6f, -20.5f))
                }
                3 -> {
                    drawOval(Color(0xFFCDBF9D), feet + Offset(-4.5f, -14f), Size(9f, 6f))
                    drawOval(Color(0xFF7A4A2E), feet + Offset(-4.5f, -14f), Size(9f, 6f), style = thin)
                    drawLine(Color(0xFFB59A69), feet + Offset(2f, -15f), feet + Offset(6f, -19f), 1f)
                }
                else -> {
                    drawLine(brass, feet + Offset(1.5f, -19f), feet + Offset(8f, -18f), 1.6f)
                    drawCircle(brass, 2.3f, feet + Offset(8.5f, -18f))
                    drawRect(brass, Offset(feet.x + 1.5f, feet.y - 17.6f), Size(3f, 2.2f))
                }
            }
        }
    }
    note(p.at(-6f, 0f) + Offset(0f, -26f), time, .2f)
    note(p.at(10f, 0f) + Offset(0f, -27f), time, .7f)
}

// The vendor stands behind the cart, so with the street up the screen he is nearer and goes down last.
private fun DrawScope.gelato(p: Prop) {
    val vendor = p.at(-3f, -6.5f)
    fun gelataio() {
        drawOval(shade, vendor + Offset(-4f, -1.2f), Size(9f, 3.6f))
        musician(vendor, Color(0xFFE6E0D2), skins[2], Palette.Cream)
    }
    if (sin(p.angle) >= 0f) gelataio()
    turned(p) {
        drawOval(Palette.Gold.copy(alpha = .12f), Offset(-13f, -5f), Size(26f, 15f))
        walls(p, -10f, -4f, 20f, 9f, 6f, Color(0xFF8C8C86))
        lid(p, -10f, -4f, 20f, 9f, 6f, Color(0xFFB8B8B0))
        val top = p.screen(0f, -6f)
        for (i in 0..5) (Offset(-8.8f + (i % 3) * 6f, -3.2f + (i / 3) * 4f) + top).let { round(it.x, it.y, 5.2f, 3.2f, flavours[i], 1.2f) }
    }
    if (sin(p.angle) < 0f) gelataio()
    repeat(2) { i ->
        val wheel = p.at(i * 14f - 7f, 5f) - Offset(0f, 2.4f)
        drawCircle(tyre, 2.4f, wheel)
        drawCircle(Color(0xFF8A8A84), .9f, wheel)
    }
    val pole = p.at(0f, -.5f)
    drawLine(Color(0xFF9A9A92), pole - Offset(0f, 6f), pole - Offset(0f, 24f), .9f)
    for (i in 0 until 6) drawArc(if (i % 2 == 0) gelatoPink else Palette.Cream, 180f + i * 30f, 30f, true, pole - Offset(12f, 28f), Size(24f, 9f))
    for (i in 0..5) drawCircle(if (i % 2 == 0) gelatoPink else Palette.Cream, 1.3f, pole + Offset(-10f + i * 4f, -23.4f))
    drawCircle(Color(0xFF9A9A92), .8f, pole - Offset(0f, 28.4f))
}

private fun DrawScope.panettone(at: Offset) {
    drawOval(shade, at + Offset(-3.4f, -1f), Size(7.4f, 2.4f))
    round(at.x - 2.8f, at.y - 4.4f, 5.6f, 4.4f, panettonePaper, .8f)
    drawRect(Palette.Gold.copy(alpha = .6f), Offset(at.x - 2.8f, at.y - 3f), Size(5.6f, .8f))
    drawArc(Color(0xFF8A4F22), 180f, 180f, true, at + Offset(-3.4f, -8.4f), Size(6.8f, 8f))
    drawArc(panettoneCrust, 180f, 180f, true, at + Offset(-3.2f, -8.2f), Size(6.4f, 7.4f))
    drawCircle(Color(0xFFE2B26C).copy(alpha = .7f), 1f, at + Offset(-1.2f, -6.4f))
    drawLine(Color(0xFF7A4520), at + Offset(-1.3f, -7.4f), at + Offset(1.3f, -7.4f), .5f)
}

private fun DrawScope.pasticceria(p: Prop) {
    turned(p) {
        drawOval(Palette.Gold.copy(alpha = .13f), Offset(-19f, -6f), Size(38f, 18f))
        drawRect(pasticceriaGreen, Offset(-20f, -12f), Size(40f, 5f))
        for (i in 0..7) drawRect(Palette.Gold.copy(alpha = .75f), Offset(-9.5f + i * 2.6f, -10.2f), Size(1.6f, 1.2f))
        for (i in 0..9) drawRect(if (i % 2 == 0) pasticceriaGreen else Palette.Cream, Offset(-20f + i * 4f, -7f), Size(4f, 3.5f))
        for (i in 0..9) drawCircle(if (i % 2 == 0) pasticceriaGreen else Palette.Cream, 2f, Offset(-18f + i * 4f, -3.5f))
        drawRect(shade, Offset(-16f, 0f) + p.screen(2f, 3f), Size(32f, 7f))
        round(-16f, -.5f, 32f, 7.5f, wood, 1.5f)
        drawRect(Color(0xFF8A6A4A), Offset(-16f, -.5f), Size(32f, 1.2f))
    }
    for (k in 0..3) panettone(p.at(-11.5f + p.backToFront(k, 4) * 7.7f, 4f))
}

private fun DrawScope.espresso(p: Prop) {
    turned(p) {
        drawRect(Palette.Gold.copy(alpha = .06f), Offset(-20f, -13f), Size(40f, 26f))
        drawOval(Palette.Gold.copy(alpha = .13f), Offset(-18f, -8f), Size(36f, 16f))
        round(-14f, -4.5f, 28f, 3.5f, Color(0xFF5A3A28), 1f)
        round(-3.5f, -5f, 7f, 3f, Color(0xFFB9BDB8), .8f)
        for (i in 0..1) {
            val x = i * 17f - 8.5f
            drawCircle(shade, 3.4f, Offset(x, 5.5f) + p.screen(1.2f, 1.6f))
            repeat(2) { s -> round(x + (s * 2 - 1) * 5.4f - 1.5f, 3.5f, 3f, 3f, Color(0xFF3C3C3A), .8f) }
            drawCircle(Color(0xFF8E897D), 3.2f, Offset(x, 5f))
            drawCircle(marble, 2.6f, Offset(x, 5f))
            repeat(2) { d ->
                drawCircle(Palette.Cream, .9f, Offset(x + (d * 2 - 1) * 1.1f, 4.6f))
                drawCircle(Color(0xFF3B2416), .45f, Offset(x + (d * 2 - 1) * 1.1f, 4.6f))
            }
        }
        drawRect(Color(0xFF5C1F28), Offset(-20f, -13f), Size(40f, 2.5f))
        drawRect(espressoRed, Offset(-20f, -10.5f), Size(40f, 5f))
        for (i in 0..7) drawRect(Palette.Cream.copy(alpha = .7f), Offset(-7.5f + i * 2f, -8.6f), Size(1.3f, 1.2f))
        for (i in 0..10) drawCircle(Color(0xFF9A4450), 1.8f, Offset(-18.5f + i * 3.7f, -5.5f))
        repeat(2) { s ->
            round((s * 2 - 1) * 17.5f - 2f, 8.5f, 4f, 3.5f, Color(0xFF4A3A2C), .8f)
            drawCircle(Color(0xFF4E7A55), 2.2f, Offset((s * 2 - 1) * 17.5f, 9f))
        }
    }
    sitter(p.at(-13.9f, 4.5f), folk[2], skins[1])
    sitter(p.at(3.1f, 4.5f), Color(0xFF8C6A5C), skins[3])
}

private fun DrawScope.vedovella(p: Prop) {
    val c = p.position.offset()
    val f = Offset(cos(p.angle), sin(p.angle))
    val drain = c + Offset(f.x * 5f, f.y * 2.5f)
    val spout = c + Offset(f.x * 3.8f, -7.5f + f.y)
    fun water() {
        drawLine(vedovellaDark, c + Offset(0f, -7.5f), spout, 1.3f, StrokeCap.Round)
        drawCircle(vedovellaGreen, 1.1f, spout)
        drawLine(Color(0xFF9CC7D6).copy(alpha = .8f), spout + Offset(0f, .6f), drain, .7f)
    }
    drawOval(shade, c + Offset(-4.5f, -1.8f), Size(10f, 4f))
    drawOval(iron, drain + Offset(-2.6f, -1.1f), Size(5.2f, 2.2f))
    for (i in -1..1) drawLine(Color(0xFF55595B), drain + Offset(i * 1.2f, -.9f), drain + Offset(i * 1.2f, .9f), .4f)
    if (f.y < 0f) water()
    round(c.x - 4.2f, c.y - 2.6f, 8.4f, 2.6f, vedovellaDark, 1f)
    round(c.x - 3.3f, c.y - 16f, 6.6f, 14f, vedovellaGreen, 1.5f)
    drawRect(vedovellaLight, Offset(c.x - 2.3f, c.y - 15f), Size(1.1f, 12f))
    drawRect(vedovellaDark, Offset(c.x + 1.6f, c.y - 15.5f), Size(1.4f, 13f))
    round(c.x - 3.9f, c.y - 16.8f, 7.8f, 1.6f, vedovellaDark, .6f)
    drawArc(vedovellaGreen, 180f, 180f, true, Offset(c.x - 3.3f, c.y - 20.4f), Size(6.6f, 7f))
    drawCircle(vedovellaLight, .9f, Offset(c.x, c.y - 20.6f))
    round(c.x - 1.3f, c.y - 12.6f, 2.6f, 3.2f, Color(0xFFD9D4C6), .7f)
    drawLine(Color(0xFFB0403A), Offset(c.x, c.y - 12.4f), Offset(c.x, c.y - 9.7f), .6f)
    drawLine(Color(0xFFB0403A), Offset(c.x - 1.1f, c.y - 11.3f), Offset(c.x + 1.1f, c.y - 11.3f), .6f)
    if (f.y >= 0f) water()
}

private fun DrawScope.scooters(p: Prop) {
    for (k in 0..3) {
        val i = p.backToFront(k, 4)
        scooter(p.at(-13.5f + i * 9f, 0f), scooterBodies[i])
    }
}

private fun DrawScope.scooter(at: Offset, body: Color) {
    drawOval(shade, at + Offset(-5.5f, -1f), Size(12f, 2.8f))
    repeat(2) { i ->
        val hub = at + Offset(if (i == 0) -3.6f else 3.8f, -1.8f)
        drawCircle(tyre, 1.8f, hub)
        drawCircle(Color(0xFF8A9093), .6f, hub)
    }
    translate(at.x, at.y) { drawPath(scooterCowl, body) }
    round(at.x - 5.2f, at.y - 7.8f, 4.8f, 1.7f, Color(0xFF2E2A28), .8f)
    drawLine(body, at + Offset(-1f, -2.9f), at + Offset(2.6f, -2.9f), 1.5f)
    drawLine(body, at + Offset(2.4f, -2.6f), at + Offset(3.4f, -7.6f), 2.2f, StrokeCap.Round)
    drawLine(Color(0xFF8A9093), at + Offset(3.4f, -7.6f), at + Offset(3.2f, -9f), .8f)
    drawLine(iron, at + Offset(2f, -9f), at + Offset(4.6f, -9f), .9f, StrokeCap.Round)
    drawCircle(amber.copy(alpha = .75f), .7f, at + Offset(4f, -8.3f))
    drawArc(body, 180f, 180f, false, at + Offset(2f, -3.8f), Size(3.6f, 3.6f), style = thin)
}

private fun DrawScope.boutique(p: Prop) {
    turned(p) {
        drawOval(Palette.Gold.copy(alpha = .15f), Offset(-19f, -5f), Size(38f, 14f))
        drawRect(Color(0xFF1E1C1B), Offset(-20f, -12f), Size(40f, 11f))
        for (i in 0..8) drawRect(Palette.Gold.copy(alpha = .85f), Offset(-8.2f + i * 1.9f, -10.6f), Size(1.1f, 1.6f))
        drawRect(boutiqueWindow, Offset(-18.5f, -7.5f), Size(14.5f, 6f))
        drawRect(boutiqueWindow, Offset(4f, -7.5f), Size(14.5f, 6f))
        drawRect(Color(0xFF3A302A), Offset(-2.5f, -7.5f), Size(5f, 6.5f))
        repeat(2) { s ->
            round((s * 2 - 1) * 5.2f - 1.8f, .5f, 3.6f, 3f, Color(0xFF4A4642), .6f)
            drawCircle(Color(0xFF3F5E42), 1.9f, Offset((s * 2 - 1) * 5.2f, 1f))
        }
    }
    for (k in 0..3) {
        val i = p.backToFront(k, 4)
        mannequin(p.at(mannequinSpots[i], -4f), dresses[i])
    }
}

private fun DrawScope.mannequin(at: Offset, dress: Color) {
    drawOval(Palette.Deep.copy(alpha = .3f), at + Offset(-1.8f, -.7f), Size(3.6f, 1.4f))
    drawLine(iron, at, at - Offset(0f, 3.4f), .7f)
    translate(at.x, at.y) { drawPath(mannequinDress, dress) }
    drawLine(Color(0xFFB9B4AA), at - Offset(0f, 12.5f), at - Offset(0f, 13.6f), .8f)
    drawCircle(Color(0xFFC9C4BA), 1.5f, at - Offset(0f, 14.8f))
}

// No state: in each frame, the nearest walker's distance alone decides how far up and away a bird is.
private fun DrawScope.pigeons(p: Prop, time: Float, hero: Vec2?, people: List<Marcher>) {
    var nearest = Float.MAX_VALUE
    var fromX = 0f
    var fromY = 0f
    for (i in -1 until people.size) {
        val walker = if (i < 0) hero else people[i].takeIf { it.alive && !it.arrived }?.position
        if (walker == null) continue
        val d = (walker.x - p.position.x) * (walker.x - p.position.x) + (walker.y - p.position.y) * (walker.y - p.position.y)
        if (d < nearest) { nearest = d; fromX = walker.x; fromY = walker.y }
    }
    val seed = abs(p.position.x.toInt() * 73 + p.position.y.toInt() * 37)
    for (i in 0 until 3 + seed % 4) {
        val a = seed * .37f + i * 2.4f
        val bx = p.position.x + cos(a) * (4f + i * 7 % 10)
        val by = p.position.y + sin(a) * (3f + i * 5 % 7)
        val d = sqrt((bx - fromX) * (bx - fromX) + (by - fromY) * (by - fromY))
        val lift = if (nearest == Float.MAX_VALUE) 0f else ((60f - d) / 30f).coerceIn(0f, 1f)
        val ax = if (d > .01f) (bx - fromX) / d else cos(a)
        val ay = if (d > .01f) (by - fromY) / d else sin(a)
        val gx = bx + ax * lift * 48f
        val gy = by + ay * lift * 48f
        val face = if (lift > 0f) (if (ax >= 0f) 1f else -1f) else if ((seed + i) % 2 == 0) 1f else -1f
        drawOval(shade.copy(alpha = .3f * (1f - lift * .7f)), Offset(gx - 3f, gy - 1f), Size(6f, 2.2f))
        val x = gx
        val y = gy - lift * 44f
        if (lift > 0f) {
            val flap = sin(time * 26f + i * 1.3f) * 3f
            val fade = 1f - lift * .55f
            drawLine(pigeonWing.copy(alpha = fade), Offset(x, y - 2f), Offset(x - 5f, y - 3.5f - flap), 1.6f, StrokeCap.Round)
            drawLine(pigeonWing.copy(alpha = fade), Offset(x, y - 2f), Offset(x + 5f, y - 3.5f - flap), 1.6f, StrokeCap.Round)
            drawOval(pigeon.copy(alpha = fade), Offset(x - 2.6f, y - 3.4f), Size(5.2f, 3f))
            drawCircle(pigeon.copy(alpha = fade), 1.3f, Offset(x + face * 2.6f, y - 3.2f))
        } else {
            val peck = max(0f, sin(time * 3.3f + i * 1.9f) - .55f) * 5f
            drawLine(Color(0xFFB07C78), Offset(x - .8f, y - 1f), Offset(x - .8f, y), .6f)
            drawLine(Color(0xFFB07C78), Offset(x + .8f, y - 1f), Offset(x + .8f, y), .6f)
            drawLine(pigeonWing, Offset(x - face * 2.4f, y - 2.6f), Offset(x - face * 4.6f, y - 1.8f), 1.5f, StrokeCap.Round)
            drawOval(pigeon, Offset(x - 3f, y - 4.4f), Size(6f, 3.6f))
            drawOval(pigeonWing, Offset(x - 2.2f - face * .6f, y - 4.1f), Size(3.8f, 2f))
            val hx = x + face * (2.7f + peck * .3f)
            val hy = y - 4.8f + peck
            drawCircle(Color(0xFF6F8F86), 1f, Offset(hx - face * .7f, hy + 1f))
            drawCircle(pigeon, 1.5f, Offset(hx, hy))
            drawLine(Color(0xFFC9B9A0), Offset(hx + face * 1.2f, hy + .2f), Offset(hx + face * 2.3f, hy + .6f), .8f)
            drawCircle(Palette.Ink, .35f, Offset(hx + face * .5f, hy - .3f))
        }
    }
}

internal val postcard = Color(0xFFEBE0C6)
private val croissantBody = Color(0xFFCF8A3E)
private val patty = Color(0xFF6E3B26)
internal val City.souvenirColor: Color get() = when (this) { City.Belgrade -> patty; City.Paris -> croissantBody; City.Milan -> panettoneCrust }

internal fun DrawScope.souvenir(city: City) {
    val k = size.width / 120f
    scale(k, k, Offset.Zero) {
        val h = size.height / k
        round(0f, 0f, 120f, h, postcard, 4f)
        drawRect(Color(0xFFCBB993), Offset(4f, 4f), Size(112f, h - 8f), style = thin)
        val stamp = Offset(95f, 7f)
        round(stamp.x, stamp.y, 18f, 22f, Color(0xFFF7F2E4), .5f)
        translate(stamp.x + 2.5f, stamp.y + 3f) { flag(city, Size(13f, 9f)) }
        drawRect(Color(0xFF8A7B5E), Offset(stamp.x + 2.5f, stamp.y + 13.5f), Size(13f, 1.2f))
        drawRect(Color(0xFF8A7B5E), Offset(stamp.x + 2.5f, stamp.y + 16.5f), Size(9f, 1.2f))
        for (i in 0..5) {
            drawCircle(postcard, .9f, Offset(stamp.x + 1.5f + i * 3f, stamp.y))
            drawCircle(postcard, .9f, Offset(stamp.x + 1.5f + i * 3f, stamp.y + 22f))
        }
        for (i in 0..7) {
            drawCircle(postcard, .9f, Offset(stamp.x, stamp.y + 1f + i * 3f))
            drawCircle(postcard, .9f, Offset(stamp.x + 18f, stamp.y + 1f + i * 3f))
        }
        val ink = Color(0xFF6D6250).copy(alpha = .5f)
        drawCircle(ink, 8f, Offset(93f, 22f), style = thin)
        for (i in 0..2) drawArc(ink, 200f, 140f, false, Offset(70f, 15f + i * 3.5f), Size(14f, 6f), style = thin)
        when (city) {
            City.Paris -> croissantCard()
            City.Milan -> panettoneCard()
            City.Belgrade -> pljeskavicaCard()
        }
    }
}

// The tips go first, so each layer overlaps the next.
private fun DrawScope.croissantCard() {
    drawOval(Color(0xFF6D5A3A).copy(alpha = .22f), Offset(24f, 58f), Size(72f, 12f))
    val center = Offset(59f, 78f)
    for ((angle, radial, along, color) in listOf(
        Segment(-160f, 9f, 11f, Color(0xFFA9662C)), Segment(-20f, 9f, 11f, Color(0xFFA9662C)),
        Segment(-140f, 15f, 15f, Color(0xFFBB7633)), Segment(-40f, 15f, 15f, Color(0xFFBB7633)),
        Segment(-117f, 21f, 17f, Color(0xFFC6823A)), Segment(-63f, 21f, 17f, Color(0xFFC6823A)),
        Segment(-90f, 27f, 20f, croissantBody),
    )) {
        val rad = angle * PI.toFloat() / 180f
        val c = center + Offset(cos(rad), sin(rad)) * 34f
        rotate(angle + 90f, c) {
            drawOval(Color(0xFF7E4A1F), c - Offset(along / 2 + 1.2f, radial / 2 + 1.2f), Size(along + 2.4f, radial + 2.4f))
            drawOval(color, c - Offset(along / 2, radial / 2), Size(along, radial))
            drawArc(Color(0xFF8E5523), 20f, 140f, false, c - Offset(along / 2 - 1f, radial / 2 - 1f), Size(along - 2f, radial - 2f), style = thin)
            drawOval(Color(0xFFEDBE6E).copy(alpha = .8f), c - Offset(along * .36f, radial * .4f), Size(along * .3f, radial * .32f))
        }
    }
}
private data class Segment(val angle: Float, val radial: Float, val along: Float, val color: Color)

private fun DrawScope.pljeskavicaCard() {
    drawOval(Color(0xFF6D5A3A).copy(alpha = .22f), Offset(16f, 58f), Size(88f, 12f))
    drawOval(Color(0xFFB27A43), Offset(22f, 13f), Size(76f, 30f))
    drawOval(Color(0xFFCB955A), Offset(23f, 12f), Size(74f, 26f))
    drawOval(Color(0xFFE2BC86).copy(alpha = .7f), Offset(34f, 14f), Size(30f, 7f))
    drawOval(Color(0xFFEAD3A6), Offset(28f, 24f), Size(64f, 15f))
    for (i in 0..6) drawCircle(Color(0xFFC9AA78), .9f, Offset(36f + i * 7.5f, 29f + (i % 3) * 2.2f))
    drawOval(Color(0xFFA97B4A), Offset(24f, 50f), Size(70f, 15f))
    drawOval(Color(0xFFCFA06A), Offset(24f, 47f), Size(70f, 14f))
    drawOval(Color(0xFF4A2718), Offset(16f, 35f), Size(88f, 25f))
    drawOval(patty, Offset(16f, 31f), Size(88f, 24f))
    for (x in listOf(30f, 82f)) drawLine(char, Offset(x - 5f, 36f), Offset(x + 3f, 50f), 2.2f, StrokeCap.Round)
    for ((x, y) in listOf(25f to 42f, 92f to 40f, 48f to 50f, 72f to 50f, 40f to 34f, 78f to 34f))
        drawOval(Color(0xFF8A4E33), Offset(x - 3f, y - 1.5f), Size(6f, 3f))
    for ((x, y) in listOf(38f to 39f, 44f to 46f, 50f to 36f, 70f to 37f, 76f to 45f, 66f to 49f, 34f to 47f, 86f to 44f, 56f to 51f))
        round(x - 1.8f, y - 1.4f, 3.6f, 2.8f, Color(0xFFEEE7D6), .8f)
    for ((x, y) in listOf(44f to 46f, 76f to 45f, 56f to 51f)) drawRect(Color(0xFFCDB0C6), Offset(x - 1.8f, y + .6f), Size(3.6f, .8f))
}

private fun DrawScope.panettoneCard() {
    drawOval(Color(0xFF6D5A3A).copy(alpha = .22f), Offset(27f, 64f), Size(66f, 11f))
    drawOval(Color(0xFF6E4127), Offset(38f, 64f), Size(44f, 8f))
    drawRect(panettonePaper, Offset(38f, 40f), Size(44f, 28f))
    drawOval(panettonePaper, Offset(38f, 63.5f), Size(44f, 8f))
    for (i in 0..11) drawLine(Color(0xFFAA7550), Offset(40f + i * 3.6f, 42f), Offset(39.6f + i * 3.6f, 70.5f), .8f)
    drawRect(Palette.Gold.copy(alpha = .75f), Offset(38f, 53f), Size(44f, 3f))
    drawOval(Color(0xFF7E4519), Offset(32f, 8f), Size(56f, 41f))
    drawOval(panettoneCrust, Offset(33f, 8.5f), Size(54f, 38f))
    drawOval(Color(0xFFD9944A).copy(alpha = .55f), Offset(39f, 12f), Size(17f, 7f))
    for ((x, y) in listOf(38f to 30f, 45f to 37f, 76f to 36f, 82f to 27f, 70f to 14f)) drawOval(Color(0xFF5A2E14), Offset(x - 1.6f, y - 1f), Size(3.2f, 2.2f))
    drawLine(Color(0xFF7E4519), Offset(60f, 12f), Offset(60f, 27f), 4f, StrokeCap.Round)
    drawLine(Color(0xFF7E4519), Offset(49f, 19.5f), Offset(71f, 19.5f), 4f, StrokeCap.Round)
    drawLine(Color(0xFFEBC982), Offset(60f, 13f), Offset(60f, 26f), 2f, StrokeCap.Round)
    drawLine(Color(0xFFEBC982), Offset(50f, 19.5f), Offset(70f, 19.5f), 2f, StrokeCap.Round)
    drawCircle(Color(0xFFF2DCA0), 2f, Offset(60f, 19.5f))
}

private fun road(width: Float) = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Blocks full of low roofs, and the faint grid. Streets and squares are drawn over them. */
private fun DrawScope.ground(rect: Rect) {
    drawRect(blocks, rect.topLeft, rect.size)
    val columns = floor(rect.left / 70).toInt()..floor(rect.right / 70).toInt()
    val rows = floor(rect.top / 70).toInt()..floor(rect.bottom / 70).toInt()
    for (row in rows) for (column in columns) {
        val x = column * 70f
        val y = row * 70f
        when ((column * 7 + row * 11 + column * row).mod(5)) {
            0, 1 -> round(x + 5, y + 5, 60f, 60f, lowRoof)
            2, 3 -> { round(x + 5, y + 5, 27f, 60f, lowRoof); round(x + 38, y + 5, 27f, 60f, lowRoof) }
            else -> { round(x + 5, y + 5, 60f, 27f, lowRoof); round(x + 5, y + 38, 60f, 27f, lowRoof) }
        }
    }
    for (column in columns) drawLine(Color.White.copy(alpha = .022f), Offset(column * 70f, rect.top), Offset(column * 70f, rect.bottom), 1f)
    for (row in rows) drawLine(Color.White.copy(alpha = .022f), Offset(rect.left, row * 70f), Offset(rect.right, row * 70f), 1f)
}

/** [time], [hero], and [people] only animate street life; without them the props stand still. */
internal fun DrawScope.city(paint: MapPaint, measurer: TextMeasurer? = null, visible: Rect? = null, time: Float = 0f, hero: Vec2? = null,
                            people: List<Marcher> = listOf()) {
    val map = paint.map
    val area = visible?.inflate(120f)
    ground(visible ?: Rect(0f, 0f, map.width, map.height))
    // Every edge goes down before any asphalt, so crossings and the mouths of the squares stay open.
    paint.streets.forEachIndexed { i, street -> drawPath(street, curb, style = paint.curbs[i]) }
    map.network.squares.forEach { drawCircle(curb, it.radius + 3f, it.center.offset(), style = Stroke(6f)) }
    paint.streets.forEachIndexed { i, street -> drawPath(street, asphalt, style = paint.lanes[i]) }
    // Spokes start at the square center, so the plaza goes over their asphalt.
    map.network.squares.forEach { drawCircle(plaza, it.radius, it.center.offset()) }
    paint.routeRoad.forEach { (color, stroke) -> drawPath(paint.route, color, style = stroke) }
    drawPath(paint.route, Color(0xFF91A6A0).copy(alpha = .4f), style = paint.centerLine)
    paint.crosswalks.forEach { (p, angle) ->
        rotate(angle, p) { repeat(7) { j -> round(p.x - 15, p.y - 41 + j * 12, 30f, 5f, Color(0xFF90A7A3).copy(alpha = .34f), 1f) } }
    }
    // The route runs through every stop's center, so the monuments go on top of it.
    map.network.squares.forEach { square ->
        val c = square.center.offset()
        when (square.kind) {
            SquareKind.Roundabout -> {
                drawCircle(Color(0xFF3E655C), square.radius * .4f, c)
                drawCircle(curb, square.radius * .4f, c, style = Stroke(4f))
            }
            SquareKind.Round -> {
                drawCircle(Palette.Deep.copy(alpha = .35f), 13f, c + Offset(5f, 8f))
                drawCircle(Color(0xFF9DAFAD), 12f, c)
                drawCircle(Palette.Gold, 5f, c)
            }
            else -> {}
        }
    }
    (if (visible == null) map.buildings else visibleBuildings(map, visible)).forEach { building(it, map.city) }
    (if (visible == null) map.landmarks else visibleLandmarks(map, visible)).forEach { landmark(it) }
    map.trees.forEach { t ->
        if (area != null && !area.contains(t.position.offset())) return@forEach
        drawCircle(Palette.Deep.copy(alpha = .28f), t.radius, t.position.offset() + Offset(5f, 9f))
        drawCircle(Color(0xFF3E655C), t.radius, t.position.offset())
        drawCircle(Color(0xFF527A67), t.radius * .72f, t.position.offset() + Offset(-3f, -4f))
        drawCircle(Color(0xFF6E8D70).copy(alpha = .45f), t.radius * .35f, t.position.offset() + Offset(-5f, -7f))
    }
    (if (visible == null) map.props else visibleProps(map, visible)).forEach { prop(it, time, hero, people) }
    if (measurer != null) map.labels.forEachIndexed { i, place ->
        val p = place.position.offset()
        if (area == null || area.contains(p)) rotate(place.angle * 180f / PI.toFloat(), p) { label(measurer, paint.names[i], p, Palette.Cream, 10f) }
    }
    listOf(map.route.samples.first().position to "A", map.route.samples.last().position to "B").forEach { (p, name) ->
        drawCircle(Palette.Ink, 30f, p.offset())
        drawCircle(if (name == "A") Palette.Gold else Palette.Mint, 30f, p.offset(), style = Stroke(2f))
        if (measurer != null) label(measurer, name, p.offset(), if (name == "A") Palette.Gold else Palette.Mint, 26f)
    }
}

private fun DrawScope.person(p: Vec2, coat: Color, id: Int, step: Float, sign: Boolean = false) {
    translate(p.x, p.y) {
        val walk = sin(step) * 2.7f
        drawOval(Palette.Deep.copy(alpha = .35f), Offset(-6f, 5f), Size(15f, 7f))
        drawLine(Palette.Ink, Offset(-3f, 2f), Offset(-3f + walk, 9f), 3.5f, StrokeCap.Round)
        drawLine(Palette.Ink, Offset(3f, 2f), Offset(3f - walk, 9f), 3.5f, StrokeCap.Round)
        drawLine(coat, Offset(-5f, -4f), Offset(-8f, 2f), 3.5f, StrokeCap.Round)
        drawLine(coat, Offset(5f, -4f), Offset(8f, 1f), 3.5f, StrokeCap.Round)
        round(-5f, -8f, 10f, 12f, coat)
        if (id % 6 == 4) drawPath(Path().apply { moveTo(-5f, -1f); lineTo(-8f, 7f); lineTo(8f, 7f); lineTo(5f, -1f); close() }, coat)
        drawCircle(skins[id % skins.size], 4.8f, Offset(0f, -11f))
        round(-4.5f, -16f, 9f, 4f, if (id % 3 == 0) Color(0xFFB59A69) else Color(0xFF3C3934), 2f)
        if (sign) {
            drawLine(Color(0xFFB1A17C), Offset(7f, -1f), Offset(7f, -33f), 1.5f)
            round(-5f, -34f, 27f, 14f, Palette.Cream, 1f)
            drawLine(Palette.Ground, Offset(0f, -29f), Offset(17f, -29f), 2f)
            drawLine(Color(0xFFB36D5B), Offset(3f, -25f), Offset(14f, -25f), 2f)
        }
    }
}

internal fun DrawScope.defender(position: Offset, barricade: Float, step: Float = 0f, angle: Float = 0f) {
    translate(position.x, position.y) {
        // Translation leaves DrawScope.center at the canvas center; use the sprite's local origin.
        drawCircle(Palette.Gold.copy(alpha = .1f), 27f, center = Offset.Zero)
        drawCircle(Palette.Gold.copy(alpha = .8f), 25f, center = Offset.Zero, style = Stroke(1.5f))
        drawOval(Palette.Deep.copy(alpha = .4f), Offset(-17f, 8f), Size(38f, 14f))
        drawLine(Palette.Deep, Offset(-6f, 4f), Offset(-6f + sin(step)*4, 16f), 6f, StrokeCap.Round)
        drawLine(Palette.Deep, Offset(6f, 4f), Offset(6f - sin(step)*4, 16f), 6f, StrokeCap.Round)
        round(-13f, -13f, 26f, 27f, Color(0xFFB85946), 5f)
        drawLine(Color(0xFFCC7058), Offset(-13f, -9f), Offset(-16f, 7f), 6f, StrokeCap.Round)
        drawLine(Color(0xFFCC7058), Offset(13f, -9f), Offset(16f, 7f), 6f, StrokeCap.Round)
        drawLine(Palette.Ink, Offset(-7f, -11f), Offset(-7f, 9f), 4f)
        drawLine(Palette.Ink, Offset(7f, -11f), Offset(7f, 9f), 4f)
        drawCircle(Color(0xFF403A34), 11f, Offset(0f, -22f))
        drawCircle(Color(0xFFDDB28B), 10f, Offset(0f, -20f))
        drawArc(Color(0xFF483B32), 0f, 180f, true, Offset(-10f, -28f), Size(20f, 20f))
        drawArc(Color(0xFF483B32), 180f, 180f, true, Offset(-11f, -33f), Size(22f, 17f))
        round(-10f, -25f, 9f, 6f, Palette.Deep, 2f)
        round(1f, -25f, 9f, 6f, Palette.Deep, 2f)
        drawLine(Palette.Deep, Offset(-1f, -23f), Offset(2f, -23f), 2f)
        drawLine(Color(0xFF8BA5A8), Offset(-8f, -24f), Offset(-4f, -24f), 1f)
        rotate(angle * 180f / PI.toFloat(), pivot = Offset.Zero) {
            round(10f, -5f, 23f, 6f, Color(0xFFAAC1C1), 2f)
            round(13f, -2f, 7f, 7f, Palette.Ink, 1f)
        }
        if (barricade > 0f) scale(barricade, barricade, Offset.Zero) {
            round(-34f, -9f, 7f, 48f, Color(0xFF998564), 2f)
            round(27f, -9f, 7f, 48f, Color(0xFF998564), 2f)
            for (j in 0..2) {
                round(-32f, 15f + j * 7f, 64f, 6f, Color(0xFFC1AE77), 1f)
                for (k in 0..3) drawLine(Color(0xFF61594B), Offset(-27f + k * 16, 15f + j * 7), Offset(-21f + k * 16, 21f + j * 7), 4f)
            }
        }
    }
}

private fun DrawScope.enemy(enemy: Enemy, measurer: TextMeasurer) {
    val color = when (enemy.kind) { EnemyKind.Thug -> Palette.Red; EnemyKind.Police -> Color(0xFF8FB4D1); EnemyKind.Car -> Color(0xFFE9AD6F) }
    val p = enemy.position.offset()
    if (enemy.warning > 0) {
        drawCircle(color.copy(alpha = .3f + .25f * sin(enemy.age * 12)), 28f + enemy.warning * 10, p, style = Stroke(2f))
        label(measurer, "!", p + Offset(0f, -37f), color, 22f)
    }
    if (enemy.kind == EnemyKind.Car) {
        val direction = enemy.path.firstOrNull()?.minus(enemy.position)
        val angle = if (direction != null) atan2(direction.y, direction.x) * 180f / PI.toFloat() else 0f
        rotate(angle, p) {
            round(p.x - 23, p.y - 14, 46f, 28f, if (enemy.hit > 0) Palette.Cream else color, 6f)
            round(p.x - 5, p.y - 11, 14f, 22f, Palette.Ink)
            round(p.x - 12, p.y - 16, 10f, 4f, Palette.Deep, 1f)
            round(p.x - 12, p.y + 12, 10f, 4f, Palette.Deep, 1f)
            round(p.x + 19, p.y - 11, 3f, 7f, Palette.Gold, 1f)
            round(p.x + 19, p.y + 4, 3f, 7f, Palette.Gold, 1f)
        }
    } else {
        person(enemy.position, if (enemy.hit > 0) Palette.Cream else if (enemy.kind == EnemyKind.Thug) Color(0xFF735458) else Color(0xFF547D9C), enemy.id, enemy.age * 11)
        round(p.x - 7, p.y - 17, 14f, 7f, Palette.Ink, 3f)
        if (enemy.kind == EnemyKind.Police) {
            round(p.x + 4, p.y - 3, 13f, 18f, color, 3f)
            drawLine(Palette.Ink, p + Offset(7f, 4f), p + Offset(14f, 4f), 2f)
        } else drawLine(Color(0xFFC7AC86), p + Offset(8f, -1f), p + Offset(16f, -23f), 4f, StrokeCap.Round)
    }
    if (enemy.health < enemy.kind.health) {
        round(p.x - 12, p.y - 28, 24f, 3f, Palette.Ink, 1f)
        round(p.x - 12, p.y - 28, 24f * max(0, enemy.health) / enemy.kind.health, 3f, color, 1f)
    }
}

// The preview seed is fixed, so each city's preview map is generated once.
private val previews = mutableMapOf<City, MapPaint>()

@Composable
fun CityArtwork(city: City = City.Belgrade, modifier: Modifier = Modifier, hero: Boolean = true, compact: Boolean = false) {
    val paint = previews.getOrPut(city) { MapPaint(generateMap(city, 2026, 1f)) }
    val map = paint.map
    Canvas(modifier.clipToBounds()) {
        // The start square and the crowd go where the card text and the hero leave room, even past the map edge.
        val start = map.route.samples.first().position
        val head = map.route.at(220f)
        val subject = Rect(min(start.x, head.x) - 36f, min(start.y, head.y) - 36f, max(start.x, head.x) + 36f, max(start.y, head.y) + 36f)
        val room = if (compact) Rect(.04f, .16f, .52f, .6f) else Rect(.03f, .08f, .45f, .66f)
        val scale = minOf(size.width / 760f, room.width * size.width / subject.width, room.height * size.height / subject.height)
        val left = subject.center.x - room.center.x * size.width / scale
        val top = subject.center.y - room.center.y * size.height / scale
        withTransform({ scale(scale, scale, Offset.Zero); translate(-left, -top) }) {
            city(paint, visible = Rect(left, top, left + size.width / scale, top + size.height / scale))
            repeat(44) { id ->
                val p = map.route.at(220f - (id / 4) * 14f, ((id % 4) - 1.5f) * 13f)
                person(p, coats[id % coats.size], id, 0f, id % 13 == 0)
            }
        }
        drawRect(Brush.verticalGradient(listOf(Palette.Ink.copy(alpha = .08f), Color.Transparent, Palette.Ink.copy(alpha = .9f))))
        if (hero) {
            val p = Offset(size.width * (if (compact) .78f else .68f), size.height * (if (compact) .43f else .63f))
            drawCircle(Palette.Gold.copy(alpha = .09f), size.width * .24f, p)
            drawCircle(Palette.Gold.copy(alpha = .18f), size.width * .23f, p, style = Stroke(1f))
            drawCircle(Palette.Gold.copy(alpha = .1f), size.width * .29f, p, style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))))
            val heroScale = size.width / (if (compact) 260f else 180f)
            scale(heroScale, heroScale, p) { defender(p, 1f) }
        }
        // The menu card's eyebrow sits at the top, sometimes over a landmark.
        if (compact) drawRect(Brush.verticalGradient(0f to Palette.Ink.copy(alpha = .6f), .24f to Color.Transparent, .42f to Color.Transparent,
            .76f to Palette.Ink.copy(alpha = .8f), 1f to Palette.Ink.copy(alpha = .96f)))
    }
}

@Composable
fun WorldCanvas(engine: GameEngine, camera: HeroCamera, strings: Strings, revision: Int, modifier: Modifier) {
    // A frame lays out the street names plus the sprite labels; the default cache of 8 would miss every time.
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val paint = remember(engine.map) { MapPaint(engine.map) }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val frame = revision
        val scale = size.width / camera.viewWidth
        withTransform({
            scale(scale, scale, Offset.Zero)
            translate(camera.viewWidth / 2 - camera.center.x, camera.viewHeight / 2 - camera.center.y)
        }) {
            city(paint, measurer, Rect(Offset(camera.center.x - camera.viewWidth / 2, camera.center.y - camera.viewHeight / 2),
                Size(camera.viewWidth, camera.viewHeight)), engine.elapsed, engine.hero.position, engine.people)
            if (!engine.hero.moving) drawCircle(Palette.Gold.copy(alpha = .045f), GameEngine.FIRE_RANGE, engine.hero.position.offset())
            if (engine.hero.moving) {
                val path = Path().apply {
                    moveTo(engine.hero.position.x, engine.hero.position.y)
                    engine.hero.path.forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, Palette.Gold.copy(alpha = .65f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 8f))))
                engine.hero.path.lastOrNull()?.let {
                    drawCircle(Palette.Gold, 12f, it.offset(), style = Stroke(2f))
                    drawLine(Palette.Gold, it.offset() - Offset(18f, 0f), it.offset() + Offset(18f, 0f), 1f)
                    drawLine(Palette.Gold, it.offset() - Offset(0f, 18f), it.offset() + Offset(0f, 18f), 1f)
                }
            }
            engine.people.filter { it.alive && !it.arrived }.sortedBy { it.position.y }.forEach { p ->
                person(p.position, if (p.hit > 0) Palette.Cream else coats[p.id % coats.size], p.id,
                    if (engine.speechRemaining > 0) 0f else engine.elapsed * 9 + p.id, p.id % 11 == 0)
            }
            engine.people.firstOrNull { it.alive && !it.arrived }?.let { lead ->
                val p = lead.position.offset() + Offset(0f, -49f)
                round(p.x - 42, p.y - 8, 84f, 16f, Palette.Cream, 2f)
                label(measurer, strings[Copy.Banner], p, Palette.Ground, 7f)
            }
            engine.enemies.forEach { enemy(it, measurer) }
            defender(engine.hero.position.offset(), if (engine.hero.moving) 0f else engine.hero.barricade,
                if (engine.hero.moving) engine.elapsed * 22 else 0f, engine.hero.angle)
            label(measurer, strings[Copy.You], engine.hero.position.offset() + Offset(0f, -47f), Palette.Gold, 10f)
            if (engine.speechRemaining > 0) {
                val center = engine.crowdCenter.offset()
                drawCircle(Palette.Mint.copy(alpha = .4f), 65f + sin(engine.elapsed * 3) * 5, center, style = Stroke(2f))
                label(measurer, strings[Copy.Speech], center + Offset(0f, -85f), Palette.Mint, 12f)
            }
            engine.shots.forEach { shot ->
                drawCircle(Palette.Gold.copy(alpha = .2f), 7f, shot.position.offset())
                drawCircle(Palette.Cream, 2.5f, shot.position.offset())
            }
            engine.bombs.forEach { bomb ->
                val t = (bomb.age / .6f).coerceIn(0f, 1f)
                val p = bomb.origin + (bomb.target - bomb.origin) * t + Vec2(0f, -sin(t * PI.toFloat()) * 100f)
                drawCircle(Palette.Gold.copy(alpha = .5f), GameEngine.BOMB_RADIUS, bomb.target.offset(), style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
                drawCircle(Palette.Deep, 7f, p.offset())
                drawCircle(Palette.Gold, 3f, p.offset() + Offset(4f, -6f))
            }
            engine.effects.forEach { effect ->
                val t = if (effect.blast) effect.life / .6f else effect.life / .3f
                drawCircle(Palette.Gold.copy(alpha = t * .32f), if (effect.blast) (1 - t) * 120 else (1 - t) * 15, effect.position.offset())
                if (effect.blast) drawCircle(Palette.Gold.copy(alpha = t), (1 - t) * 110, effect.position.offset(), style = Stroke(3f))
            }
        }
        // Indicators are overlays, never inputs to camera tracking.
        fun edge(point: Vec2, color: Color, text: String) {
            val p = camera.worldToScreen(point, size.width).offset()
            val inset = 20f * density
            if (p.x in inset..size.width - inset && p.y in inset..size.height - inset) return
            val pos = Offset(p.x.coerceIn(inset, size.width - inset), p.y.coerceIn(78f * density, size.height - 36f * density))
            drawCircle(Palette.Ink, 12f * density, pos)
            drawCircle(color, 12f * density, pos, style = Stroke(1.2f * density))
            label(measurer, text, pos, color, 11f * density)
        }
        edge(engine.crowdCenter, Palette.Mint, "●")
        engine.enemies.filter { it.warning <= 0 }.groupBy { ((it.position.x - camera.center.x) / 160).toInt() to ((it.position.y - camera.center.y) / 160).toInt() }
            .values.forEach { edge(it.first().position, Palette.Red, "!") }
    }
}

@Composable
fun RouteMiniature(camera: HeroCamera, engine: GameEngine, revision: Int, modifier: Modifier) {
    val paint = remember(engine.map) { MapPaint(engine.map) }
    Canvas(modifier) {
        @Suppress("UNUSED_VARIABLE") val frame = revision
        val s = min(size.width / engine.map.width, size.height / engine.map.height)
        val k = engine.map.scale // the dots keep their size on a larger map
        scale(s, s, Offset.Zero) {
            paint.streets.forEachIndexed { i, street -> drawPath(street, Palette.Muted.copy(alpha = .1f), style = paint.miniStreets[i]) }
            drawPath(paint.route, Palette.Muted.copy(alpha = .45f), style = paint.miniRoute)
            drawRect(Palette.Cream.copy(alpha = .12f), (camera.center - Vec2(camera.viewWidth / 2, camera.viewHeight / 2)).offset(), Size(camera.viewWidth, camera.viewHeight))
            drawCircle(Palette.Mint, 29f * k, engine.crowdCenter.offset())
            drawCircle(Palette.Gold, 32f * k, engine.hero.position.offset())
        }
    }
}
