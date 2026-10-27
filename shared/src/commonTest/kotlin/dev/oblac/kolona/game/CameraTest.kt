package dev.oblac.kolona.game

import kotlin.test.*

class CameraTest {
    @Test fun worldIsWiderThanPhoneViewportAndCameraTracksOnlyHero() {
        val camera = HeroCamera(1400f, 1120f)
        camera.resize(390f, 680f)
        val hero = Vec2(700f, 560f)
        camera.follow(hero, 0f, snap = true)
        assertTrue(camera.worldWidth > camera.viewWidth * 2)
        assertEquals(hero, camera.center)
        val game = GameEngine(1, GameConfig(spawnEnemies = false))
        repeat(180) { game.advance(1f / 60) }
        camera.follow(hero, .1f)
        assertEquals(hero, camera.center)
    }

    @Test fun cameraClampsAtWorldEdgesAndCoordinateMappingRoundTrips() {
        val camera = HeroCamera(1400f, 1120f)
        camera.resize(390f, 700f)
        camera.follow(Vec2(0f, 0f), 0f, snap = true)
        assertEquals(camera.viewWidth / 2, camera.center.x)
        assertEquals(camera.viewHeight / 2, camera.center.y)
        camera.follow(Vec2(1400f, 1120f), 0f, snap = true)
        assertEquals(1400f - camera.viewWidth / 2, camera.center.x)
        assertEquals(1120f - camera.viewHeight / 2, camera.center.y)
        val point = Vec2(1234f, 789f)
        val restored = camera.screenToWorld(camera.worldToScreen(point, 390f), 390f)
        assertTrue(restored.distanceTo(point) < .001f)
    }

    @Test fun cameraClampsToALargerMap() {
        val camera = HeroCamera(2800f, 2240f)
        camera.resize(390f, 700f)
        camera.follow(Vec2(2800f, 2240f), 0f, snap = true)
        assertEquals(Vec2(2800f - camera.viewWidth / 2, 2240f - camera.viewHeight / 2), camera.center)
    }

    // The phase box and the minimap cover the top of the view, so the camera may go up past the map's top edge.
    @Test fun topInsetShowsTheMapTopBelowTheHud() {
        val camera = HeroCamera(1400f, 1120f)
        camera.resize(390f, 700f)
        camera.topInset = 110f
        camera.follow(Vec2(700f, 0f), 0f, snap = true)
        assertEquals(camera.viewHeight / 2 - 110f, camera.center.y)
        camera.follow(Vec2(700f, 1120f), 0f, snap = true)
        assertEquals(1120f - camera.viewHeight / 2, camera.center.y)
    }

    @Test fun cameraSmoothingDoesNotOvershoot() {
        val camera = HeroCamera(1400f, 1120f)
        camera.resize(390f, 650f)
        camera.follow(Vec2(600f, 550f), 0f, snap = true)
        camera.follow(Vec2(800f, 550f), .016f)
        assertTrue(camera.center.x in 600f..800f)
        assertTrue(camera.center.x > 600f)
    }
}
