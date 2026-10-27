package dev.oblac.kolona.game

import kotlin.math.*

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vec2(x * scale, y * scale)
    fun length() = sqrt(x * x + y * y)
    fun distanceTo(other: Vec2) = (this - other).length()
    fun normalized(): Vec2 = if (length() > 0.001f) this * (1f / length()) else Zero
    companion object { val Zero = Vec2(0f, 0f) }
}

/** A portrait viewport into a larger world. The only tracking input is the hero. */
class HeroCamera(val worldWidth: Float = 1400f, val worldHeight: Float = 1120f) {
    var center = Vec2(420f, 920f)
        private set
    var viewWidth = 520f
        private set
    var viewHeight = 780f
        private set
    /** World units the view may go up past the map's top edge, so the HUD over the top of the view does not cover the map. */
    var topInset = 0f

    fun resize(pixelWidth: Float, pixelHeight: Float) {
        if (pixelWidth > 0f && pixelHeight > 0f) {
            viewHeight = min(worldHeight, 520f * pixelHeight / pixelWidth)
            viewWidth = viewHeight * pixelWidth / pixelHeight
        }
    }

    fun follow(hero: Vec2, dt: Float, snap: Boolean = false) {
        val target = Vec2(
            hero.x.coerceIn(viewWidth / 2, worldWidth - viewWidth / 2),
            hero.y.coerceIn(viewHeight / 2 - topInset, worldHeight - viewHeight / 2),
        )
        center = if (snap) target else center + (target - center) * (1f - exp(-10f * dt))
    }

    fun screenToWorld(point: Vec2, pixelWidth: Float) =
        center + Vec2(point.x * viewWidth / pixelWidth - viewWidth / 2,
            point.y * viewWidth / pixelWidth - viewHeight / 2)

    fun worldToScreen(point: Vec2, pixelWidth: Float) =
        (point - center + Vec2(viewWidth / 2, viewHeight / 2)) * (pixelWidth / viewWidth)
}

data class RoutePoint(val position: Vec2, val distance: Float)
data class Building(val position: Vec2, val width: Float, val height: Float, val palette: Int)
data class Landmark(val name: String, val kind: LandmarkKind, val position: Vec2, val width: Float, val height: Float)
data class Tree(val position: Vec2, val radius: Float)
/** [position] is the middle of the footprint. [angle] (radians) points to the nearest street, so the prop's front faces the sidewalk. */
data class Prop(val kind: PropKind, val position: Vec2, val angle: Float)

class MarchRoute(points: List<Vec2>) {
    val samples: List<RoutePoint>
    val length: Float
    init {
        val result = mutableListOf(RoutePoint(points.first(), 0f))
        var distance = 0f
        for (i in 0 until points.lastIndex) {
            val a = points[max(0, i - 1)]
            val b = points[i]
            val c = points[i + 1]
            val d = points[min(points.lastIndex, i + 2)]
            for (j in 1..20) {
                val t = j / 20f
                val p = (b * 2f + (c - a) * t +
                    (a * 2f - b * 5f + c * 4f - d) * (t * t) +
                    (b * 3f - a - c * 3f + d) * (t * t * t)) * 0.5f
                distance += p.distanceTo(result.last().position)
                result += RoutePoint(p, distance)
            }
        }
        samples = result
        length = distance
    }

    fun at(distance: Float, offset: Float = 0f): Vec2 {
        val s = distance.coerceIn(0f, length)
        var lo = 1
        var hi = samples.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (samples[mid].distance < s) lo = mid + 1 else hi = mid
        }
        val a = samples[lo - 1]
        val b = samples[lo]
        val t = (s - a.distance) / (b.distance - a.distance).coerceAtLeast(0.001f)
        val direction = (b.position - a.position).normalized()
        return a.position + (b.position - a.position) * t + Vec2(-direction.y, direction.x) * offset
    }

    fun distanceFrom(point: Vec2) = samples.minOf { it.position.distanceTo(point) }
}
