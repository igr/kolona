package dev.oblac.kolona.ui

import dev.oblac.gamekit.MemoryStore
import dev.oblac.kolona.game.*
import dev.oblac.kolona.i18n.Language
import kotlin.test.*

class CitySessionTest {
    @Test fun belgradeIsTheDefaultCityAndTheBoxIsUnchecked() {
        val session = GameSession(MemoryStore())
        assertEquals(City.Belgrade, session.city)
        assertFalse(session.cityLanguage)
    }

    @Test fun selectedCityIsSavedAndLoadedAgain() {
        val store = MemoryStore()
        GameSession(store).selectCity(City.Paris)
        assertEquals("paris", store.string("city"))
        assertEquals(City.Paris, GameSession(store).city)
    }

    @Test fun uncheckedBoxKeepsTheLanguageWhenTheCityChanges() {
        val session = GameSession(MemoryStore())
        session.selectCity(City.Paris)
        assertEquals(City.Paris, session.city)
        assertEquals(Language.English, session.language)
    }

    @Test fun checkedBoxSwitchesToTheCityLanguage() {
        val store = MemoryStore().apply { put("city.language", "true") }
        val session = GameSession(store)
        assertTrue(session.cityLanguage)
        session.selectCity(City.Paris)
        assertEquals(Language.French, session.language)
        session.selectCity(City.Belgrade)
        assertEquals(Language.Serbian, session.language)
        assertEquals("sr", store.string("language"))
        session.selectCity(City.Milan)
        assertEquals(Language.Italian, session.language)
        assertEquals("it", store.string("language"))
    }

    @Test fun checkingTheBoxSwitchesToTheCurrentCityLanguageRightAway() {
        val store = MemoryStore()
        val session = GameSession(store)
        session.selectCity(City.Paris)
        session.switchLanguageWithCity(true)
        assertTrue(session.cityLanguage)
        assertEquals(Language.French, session.language)
        assertEquals("true", store.string("city.language"))
    }

    @Test fun uncheckingTheBoxIsSavedAndKeepsTheLanguage() {
        val store = MemoryStore()
        val session = GameSession(store)
        session.switchLanguageWithCity(true)
        session.switchLanguageWithCity(false)
        session.selectCity(City.Paris)
        assertEquals(Language.Serbian, session.language)
        assertEquals("false", store.string("city.language"))
        assertFalse(GameSession(store).cityLanguage)
    }

    @Test fun globeChoiceDoesNotUncheckTheBox() {
        val store = MemoryStore()
        val session = GameSession(store)
        session.switchLanguageWithCity(true)
        session.select(Language.English)
        assertTrue(session.cityLanguage)
        assertEquals(Language.English, session.language)
        assertEquals("true", store.string("city.language"))
    }

    @Test fun bestRescueIsKeptPerCityAndLegacyBestBelongsToBelgrade() {
        val session = GameSession(MemoryStore().apply { put("best", "50") })
        assertEquals(50, session.best)
        session.selectCity(City.Paris)
        assertEquals(0, session.best)
        session.selectCity(City.Belgrade)
        assertEquals(50, session.best)
    }

    // The city dialog shows every city's best, not only the selected one's.
    @Test fun everyCitysBestIsReadableWithoutSelectingIt() {
        val session = GameSession(MemoryStore().apply { put("best.belgrade", "50"); put("best.paris", "64") })
        assertEquals(City.Belgrade, session.city)
        assertEquals(listOf(50, 64, 0), City.entries.map { session.best(it) })
    }

    @Test fun unknownSavedCityFallsBackToBelgradeAndBadBestIsIgnored() {
        val store = MemoryStore().apply { put("city", "tokyo"); put("best", "abc"); put("best.paris", "999") }
        val session = GameSession(store)
        assertEquals(City.Belgrade, session.city)
        assertEquals(0, session.best)
        session.selectCity(City.Paris)
        assertEquals(72, session.best)
    }

    @Test fun changingCityDuringAPausedGameAffectsOnlyTheNextGame() {
        val session = GameSession(MemoryStore())
        session.start()
        session.menu()
        session.selectCity(City.Paris)
        assertEquals(GameStatus.Paused, session.engine.status)
        assertEquals(City.Belgrade, session.engine.map.city)
        session.start()
        assertEquals(City.Paris, session.engine.map.city)
    }

    @Test fun aWinIsRecordedForTheCityOfItsGame() {
        val store = MemoryStore()
        val session = GameSession(store, GameConfig(spawnEnemies = false))
        session.start()
        session.selectCity(City.Paris)
        repeat(3000) { if (session.engine.status == GameStatus.Playing) session.tick(.1f) }
        assertEquals(GameStatus.Won, session.engine.status)
        val survivors = session.engine.survivors
        assertEquals(survivors.toString(), store.string("best.belgrade"))
        assertNull(store.string("best.paris"))
        assertEquals(0, session.best)
        session.selectCity(City.Belgrade)
        assertEquals(survivors, session.best)
    }

    @Test fun startUsesTheSelectedCityAndAScaleWithinConfig() {
        val session = GameSession(MemoryStore())
        session.selectCity(City.Paris)
        val scales = List(5) {
            session.start()
            assertEquals(City.Paris, session.engine.map.city)
            assertSame(session.config, session.engine.config)
            session.engine.map.scale
        }
        assertTrue(scales.all { it in 1f..session.config.maxMapScale }, "$scales")
        assertTrue(scales.toSet().size > 1, "Every game picks its own size: $scales")
        val small = GameSession(MemoryStore(), GameConfig(maxMapScale = 1f))
        small.start()
        assertEquals(1f, small.engine.map.scale)
    }
}
