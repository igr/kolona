package dev.oblac.kolona.i18n

import dev.oblac.kolona.data.MemoryPreferenceStore
import dev.oblac.kolona.game.City
import dev.oblac.kolona.ui.GameSession
import kotlin.test.*

class LanguageTest {
    @Test fun englishIsTheDefaultWhenNothingIsSaved() {
        assertEquals(Language.English, GameSession(MemoryPreferenceStore()).language)
    }

    @Test fun everyLanguageCanBeSelectedAndIsLoadedAgain() {
        Language.entries.forEach { language ->
            val store = MemoryPreferenceStore()
            val session = GameSession(store)
            session.select(language)
            assertEquals(language, session.language)
            assertEquals(language.code, store.get("language"))
            assertEquals(language, GameSession(store).language)
        }
    }

    @Test fun savedSerbianStillLoads() {
        val store = MemoryPreferenceStore().apply { put("language", "sr") }
        assertEquals(Language.Serbian, GameSession(store).language)
    }

    @Test fun unknownSavedCodeFallsBackToEnglish() {
        val store = MemoryPreferenceStore().apply { put("language", "de") }
        assertEquals(Language.English, GameSession(store).language)
    }

    @Test fun stringsFollowTheirLanguage() {
        assertEquals("WE KEEP GOING", Strings(Language.English)[Copy.Banner])
        assertEquals("NE ODUSTAJEMO", Strings(Language.Serbian)[Copy.Banner])
        assertEquals("ON NE LÂCHE RIEN", Strings(Language.French)[Copy.Banner])
        assertEquals("NON MOLLIAMO", Strings(Language.Italian)[Copy.Banner])
    }

    @Test fun gameNameFollowsTheLanguage() {
        val names = listOf("Čuvar Kolone", "Guardian of the March", "Gardien du Cortège", "Guardiano del Corteo")
        listOf(Language.Serbian, Language.English, Language.French, Language.Italian).zip(names).forEach { (language, name) ->
            val strings = Strings(language)
            assertEquals(name, strings[Copy.GameName])
            assertEquals(name.uppercase(), "${strings[Copy.Subtitle]} ${strings[Copy.Wordmark]}", "$language: the wordmark spells the name")
        }
    }

    @Test fun souvenirLineNamesTheCityOfTheGame() {
        val lines = mapOf(
            City.Belgrade to listOf("SUVENIR IZ BEOGRADA", "A SOUVENIR FROM BELGRADE", "SOUVENIR DE BELGRADE", "SOUVENIR DA BELGRADO"),
            City.Paris to listOf("SUVENIR IZ PARIZA", "A SOUVENIR FROM PARIS", "SOUVENIR DE PARIS", "SOUVENIR DA PARIGI"),
            City.Milan to listOf("SUVENIR IZ MILANA", "A SOUVENIR FROM MILAN", "SOUVENIR DE MILAN", "SOUVENIR DA MILANO"),
        )
        for ((city, expected) in lines) listOf(Language.Serbian, Language.English, Language.French, Language.Italian).zip(expected).forEach { (language, line) ->
            assertEquals(line, Strings(language).souvenir(city), "$city in $language")
        }
    }

    @Test fun everyMenuAndInGameLabelHasAllTranslations() {
        Copy.entries.forEach { copy ->
            Language.entries.forEach { language ->
                assertTrue(Strings(language)[copy].isNotBlank(), "Missing $language: $copy")
            }
        }
    }
}
