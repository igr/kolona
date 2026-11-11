package dev.oblac.kolona.i18n

import dev.oblac.gamekit.MemoryStore
import dev.oblac.kolona.game.City
import dev.oblac.kolona.ui.GameSession
import kotlin.test.*

class LanguageTest {
    @Test fun englishIsTheDefaultWhenNothingIsSaved() {
        assertEquals(Language.English, GameSession(MemoryStore()).language)
    }

    @Test fun everyLanguageCanBeSelectedAndIsLoadedAgain() {
        Language.entries.forEach { language ->
            val store = MemoryStore()
            val session = GameSession(store)
            session.select(language)
            assertEquals(language, session.language)
            assertEquals(language.code, store.string("language"))
            assertEquals(language, GameSession(store).language)
        }
    }

    @Test fun savedSerbianStillLoads() {
        val store = MemoryStore().apply { put("language", "sr") }
        assertEquals(Language.Serbian, GameSession(store).language)
    }

    @Test fun unknownSavedCodeFallsBackToEnglish() {
        val store = MemoryStore().apply { put("language", "de") }
        assertEquals(Language.English, GameSession(store).language)
    }

    @Test fun stringsFollowTheirLanguage() {
        assertEquals("WE KEEP GOING", Strings(Language.English)[Copy.ShoutKeepGoing])
        assertEquals("NE ODUSTAJEMO", Strings(Language.Serbian)[Copy.ShoutKeepGoing])
        assertEquals("ON NE LÂCHE RIEN", Strings(Language.French)[Copy.ShoutKeepGoing])
        assertEquals("NON MOLLIAMO", Strings(Language.Italian)[Copy.ShoutKeepGoing])
    }

    // French puts a space before the exclamation marks, as in "GARE AUX VOITURES !".
    @Test fun aShoutEndsInExclamationMarks() {
        assertEquals("FREEDOM!", Strings(Language.English).shout(Copy.ShoutFreedom, 1))
        assertEquals("SLOBODA!!", Strings(Language.Serbian).shout(Copy.ShoutFreedom, 2))
        assertEquals("LIBERTÉ !!!", Strings(Language.French).shout(Copy.ShoutFreedom, 3))
        assertEquals("LIBERTÀ!!", Strings(Language.Italian).shout(Copy.ShoutFreedom, 2))
    }

    @Test fun theWelcomeTextNamesTheProtestersWithoutCountingThem() {
        val words = mapOf(Language.Serbian to "demonstranata", Language.English to "protesters", Language.French to "manifestants", Language.Italian to "manifestanti")
        for ((language, word) in words) {
            val text = Strings(language)[Copy.WelcomeBody]
            assertTrue(word in text && text.none(Char::isDigit), "$language: $text")
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
