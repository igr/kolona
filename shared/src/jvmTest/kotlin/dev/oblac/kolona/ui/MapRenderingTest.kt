package dev.oblac.kolona.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.oblac.kolona.game.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Streets, squares, and blocks are drawn under the route; the march road must still show on top. */
class MapRenderingTest {
    private val asphalt = Color(0xFF304E5B)
    // Clear of the crosswalks (.36, .66, and next to the end squares) and of the A/B markers at the ends.
    private val fractions = listOf(.13f, .17f, .24f, .3f, .45f, .52f, .58f, .75f, .81f, .87f)

    @Test
    fun everyCityDrawsItsRouteRoad() {
        for (city in City.entries) for (scale in listOf(1f, 2f)) {
            val map = generateMap(city, 1, scale)
            val pixels = render(map).toPixelMap()
            for (f in fractions) {
                val p = map.route.at(f * map.route.length, 30f)
                val pixel = pixels[(p.x * PX).toInt(), (p.y * PX).toInt()]
                val channels = listOf(pixel.red - asphalt.red, pixel.green - asphalt.green, pixel.blue - asphalt.blue)
                assertTrue(channels.all { abs(it) * 255 <= 24 }, "$city ${scale}x: $pixel at $f of the route")
            }
        }
    }

    // No street, block, or building uses a landmark's main colour: drawn, it fills at least 5% of the footprint (10% measured); not drawn, none.
    @Test
    fun landmarksAreDrawn() {
        for (city in City.entries) {
            val map = generateMap(city, 1, 1f)
            assertTrue(map.landmarks.isNotEmpty(), "$city")
            val pixels = render(map).toPixelMap()
            map.landmarks.forEach { landmark ->
                val xs = (landmark.position.x * PX).toInt() until ((landmark.position.x + landmark.width) * PX).toInt()
                val ys = (landmark.position.y * PX).toInt() until ((landmark.position.y + landmark.height) * PX).toInt()
                val color = landmark.kind.signature
                val hits = xs.sumOf { x ->
                    ys.count { y ->
                        val pixel = pixels[x, y]
                        listOf(pixel.red - color.red, pixel.green - color.green, pixel.blue - color.blue).all { abs(it) * 255 <= 8 }
                    }
                }
                val area = xs.count() * ys.count()
                assertTrue(hits * 20 >= area, "$city: ${landmark.name} has $hits of $area pixels in its colour")
            }
        }
    }

    // No street, block, building, landmark, or tree uses a prop kind's colour: drawn, it shows at least 12 pixels in each footprint (16 measured); not drawn, none.
    @Test
    fun propsAreDrawn() {
        for (city in City.entries) {
            val map = generateMap(city, 1, 1f)
            assertTrue(map.props.isNotEmpty(), "$city")
            val pixels = render(map, 1f).toPixelMap()
            map.props.forEach { prop ->
                val facing = Vec2(cos(prop.angle), sin(prop.angle))
                val side = Vec2(-facing.y, facing.x)
                val corners = listOf(-1f, 1f).flatMap { i -> listOf(-1f, 1f).flatMap { j -> listOf(0f, prop.kind.height).map { up ->
                    prop.position + side * (i * prop.kind.width / 2) + facing * (j * prop.kind.depth / 2) - Vec2(0f, up)
                } } }
                val xs = corners.minOf { it.x }.toInt().coerceAtLeast(0)..corners.maxOf { it.x }.toInt().coerceAtMost(pixels.width - 1)
                val ys = corners.minOf { it.y }.toInt().coerceAtLeast(0)..corners.maxOf { it.y }.toInt().coerceAtMost(pixels.height - 1)
                val color = prop.kind.signature
                val hits = xs.sumOf { x ->
                    ys.count { y ->
                        val pixel = pixels[x, y]
                        listOf(pixel.red - color.red, pixel.green - color.green, pixel.blue - color.blue).all { abs(it) * 255 <= 8 }
                    }
                }
                assertTrue(hits >= 12, "$city: ${prop.kind} at ${prop.position} has $hits pixels in its colour")
            }
        }
    }

    // Pigeons keep no state: the hero on the flock sends the birds up, and the hero far away leaves them as if alone.
    @Test
    fun pigeonsFlyUpWhenSomeoneComesClose() {
        val map = generateMap(City.Belgrade, 1, 1f)
        val flock = map.props.first { it.kind == PropKind.Pigeons }.position
        fun render(hero: Vec2?) = ImageBitmap(160, 160).also {
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(it), Size(160f, 160f)) {
                translate(80f - flock.x, 80f - flock.y) { city(MapPaint(map), visible = Rect(flock.x - 80f, flock.y - 80f, flock.x + 80f, flock.y + 80f), hero = hero) }
            }
        }.toPixelMap().buffer
        val alone = render(null)
        assertTrue(alone.contentEquals(render(flock + Vec2(400f, 0f))), "A hero 400 units away moved the pigeons")
        val near = render(flock)
        val changed = alone.indices.count { alone[it] != near[it] }
        assertTrue(changed >= 30, "A hero on the flock changed only $changed pixels")
    }

    @Test
    fun crosswalksStayOutsideTheSquares() {
        for (city in City.entries) for (scale in listOf(1f, 2f)) for (seed in 1..3) {
            val map = generateMap(city, seed, scale)
            MapPaint(map).crosswalks.forEach { (p, _) ->
                // The stripes reach 15 units along the route from the center.
                val stop = map.stops.minBy { it.center.distanceTo(Vec2(p.x, p.y)) - it.radius }
                assertTrue(stop.center.distanceTo(Vec2(p.x, p.y)) >= stop.radius + 15f, "$city ${scale}x seed $seed: crosswalk at $p is in ${stop.name}")
            }
        }
    }

    @Test
    fun cullingKeepsOnlyBuildingsLandmarksAndPropsInView() {
        val map = generateMap(City.Paris, 1, 2f)
        val view = Rect(600f, 600f, 1120f, 1380f)
        val area = view.inflate(120f)
        val expected = map.buildings.filter {
            it.position.x < area.right && it.position.x + it.width > area.left &&
                it.position.y < area.bottom && it.position.y + it.height > area.top
        }
        assertTrue(expected.isNotEmpty() && expected.size < map.buildings.size, "${expected.size} of ${map.buildings.size}")
        assertEquals(expected, visibleBuildings(map, view))
        val props = map.props.filter { area.contains(Offset(it.position.x, it.position.y)) }
        assertTrue(props.isNotEmpty() && props.size < map.props.size, "${props.size} of ${map.props.size}")
        assertEquals(props, visibleProps(map, view))
        // Cirque d'Hiver is near République, far from Opéra Bastille.
        val circus = map.landmarks.first()
        assertEquals(listOf(circus), visibleLandmarks(map, Rect(circus.position.x, circus.position.y, circus.position.x + 520f, circus.position.y + 780f)))
    }

    private fun render(map: CityMap, px: Float = PX): ImageBitmap {
        val bitmap = ImageBitmap((map.width * px).toInt(), (map.height * px).toInt())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap),
            Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
            scale(px, px, Offset.Zero) { city(MapPaint(map)) }
        }
        return bitmap
    }

    private companion object { const val PX = .25f }
}
