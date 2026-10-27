package dev.oblac.kolona.ui

import dev.oblac.kolona.data.MemoryPreferenceStore
import dev.oblac.kolona.game.*
import dev.oblac.kolona.i18n.Language
import kotlin.test.*

class CitySessionTest {
    @Test fun belgradeIsTheDefaultCityAndTheBoxIsUnchecked() {
        val session = GameSession(MemoryPreferenceStore())
        assertEquals(City.Belgrade, session.city)
        assertFalse(session.cityLanguage)
    }

    @Test fun selectedCityIsSavedAndLoadedAgain() {
        val store = MemoryPreferenceStore()
        GameSession(store).selectCity(City.Paris)
        assertEquals("paris", store.get("city"))
        assertEquals(City.Paris, GameSession(store).city)
    }

    @Test fun uncheckedBoxKeepsTheLanguageWhenTheCityChanges() {
        val session = GameSession(MemoryPreferenceStore())
        session.selectCity(City.Paris)
        assertEquals(City.Paris, session.city)
        assertEquals(Language.English, session.language)
    }

    @Test fun checkedBoxSwitchesToTheCityLanguage() {
        val store = MemoryPreferenceStore().apply { put("city.language", "true") }
        val session = GameSession(store)
        assertTrue(session.cityLanguage)
        session.selectCity(City.Paris)
        assertEquals(Language.French, session.language)
        session.selectCity(City.Belgrade)
        assertEquals(Language.Serbian, session.language)
        assertEquals("sr", store.get("language"))
        session.selectCity(City.Milan)
        assertEquals(Language.Italian, session.language)
        assertEquals("it", store.get("language"))
    }

    @Test fun checkingTheBoxSwitchesToTheCurrentCityLanguageRightAway() {
        val store = MemoryPreferenceStore()
        val session = GameSession(store)
        session.selectCity(City.Paris)
        session.switchLanguageWithCity(true)
        assertTrue(session.cityLanguage)
        assertEquals(Language.French, session.language)
        assertEquals("true", store.get("city.language"))
    }

    @Test fun uncheckingTheBoxIsSavedAndKeepsTheLanguage() {
        val store = MemoryPreferenceStore()
        val session = GameSession(store)
        session.switchLanguageWithCity(true)
        session.switchLanguageWithCity(false)
        session.selectCity(City.Paris)
        assertEquals(Language.Serbian, session.language)
        assertEquals("false", store.get("city.language"))
        assertFalse(GameSession(store).cityLanguage)
    }

    @Test fun globeChoiceDoesNotUncheckTheBox() {
        val store = MemoryPreferenceStore()
        val session = GameSession(store)
        session.switchLanguageWithCity(true)
        session.select(Language.English)
        assertTrue(session.cityLanguage)
        assertEquals(Language.English, session.language)
        assertEquals("true", store.get("city.language"))
    }

    @Test fun bestRescueIsKeptPerCityAndLegacyBestBelongsToBelgrade() {
        val session = GameSession(MemoryPreferenceStore().apply { put("best", "50") })
        assertEquals(50, session.best)
        session.selectCity(City.Paris)
        assertEquals(0, session.best)
        session.selectCity(City.Belgrade)
        assertEquals(50, session.best)
    }

    @Test fun unknownSavedCityFallsBackToBelgradeAndBadBestIsIgnored() {
        val store = MemoryPreferenceStore().apply { put("city", "tokyo"); put("best", "abc"); put("best.paris", "999") }
        val session = GameSession(store)
        assertEquals(City.Belgrade, session.city)
        assertEquals(0, session.best)
        session.selectCity(City.Paris)
        assertEquals(72, session.best)
    }

    @Test fun changingCityDuringAPausedGameAffectsOnlyTheNextGame() {
        val session = GameSession(MemoryPreferenceStore())
        session.start()
        session.menu()
        session.selectCity(City.Paris)
        assertEquals(GameStatus.Paused, session.engine.status)
        assertEquals(City.Belgrade, session.engine.map.city)
        session.start()
        assertEquals(City.Paris, session.engine.map.city)
    }

    @Test fun aWinIsRecordedForTheCityOfItsGame() {
        val store = MemoryPreferenceStore()
        val session = GameSession(store, GameConfig(spawnEnemies = false))
        session.start()
        session.selectCity(City.Paris)
        repeat(3000) { if (session.engine.status == GameStatus.Playing) session.tick(.1f) }
        assertEquals(GameStatus.Won, session.engine.status)
        val survivors = session.engine.survivors
        assertEquals(survivors.toString(), store.get("best.belgrade"))
        assertNull(store.get("best.paris"))
        assertEquals(0, session.best)
        session.selectCity(City.Belgrade)
        assertEquals(survivors, session.best)
    }

    @Test fun startUsesTheSelectedCityAndAScaleWithinConfig() {
        val session = GameSession(MemoryPreferenceStore())
        session.selectCity(City.Paris)
        val scales = List(5) {
            session.start()
            assertEquals(City.Paris, session.engine.map.city)
            assertSame(session.config, session.engine.config)
            session.engine.map.scale
        }
        assertTrue(scales.all { it in 1f..session.config.maxMapScale }, "$scales")
        assertTrue(scales.toSet().size > 1, "Every game picks its own size: $scales")
        val small = GameSession(MemoryPreferenceStore(), GameConfig(maxMapScale = 1f))
        small.start()
        assertEquals(1f, small.engine.map.scale)
    }
}
