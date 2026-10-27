package dev.oblac.kolona.ui

import androidx.compose.runtime.*
import dev.oblac.kolona.data.*
import dev.oblac.kolona.game.*
import dev.oblac.kolona.i18n.*
import kotlin.random.Random

enum class Screen { Welcome, Menu, Guide, Game }

val City.language: Language get() = when (this) {
    City.Belgrade -> Language.Serbian
    City.Paris -> Language.French
    City.Milan -> Language.Italian
}

class GameSession(val store: PreferenceStore, val config: GameConfig = GameConfig()) {
    var screen by mutableStateOf(Screen.Welcome)
    var language by mutableStateOf(Language.fromCode(store.get("language")))
        private set
    var city by mutableStateOf(City.fromCode(store.get("city")))
        private set
    var cityLanguage by mutableStateOf(store.get("city.language") == "true")
        private set
    private var game by mutableStateOf<GameEngine?>(null)
    // start() makes each game's engine, so launching the app generates no map.
    val engine: GameEngine get() = game ?: GameEngine(config = config).also { game = it }
    var revision by mutableIntStateOf(0)
        private set
    private val bests = City.entries.map { it to savedBest(it) }.toMutableStateMap()
    val best: Int get() = bests.getValue(city)
    private var recorded = false

    // Before cities there was one best, and it was Belgrade's.
    private fun savedBest(city: City) = (store.get("best.${city.code}") ?: if (city == City.Belgrade) store.get("best") else null)
        ?.toIntOrNull()?.coerceIn(0, 72) ?: 0

    fun start() {
        val seed = Random.nextInt()
        game = GameEngine(seed, config, generateMap(city, seed, mapScale(seed, config.maxMapScale)))
        recorded = false
        screen = Screen.Game
        revision++
    }
    // Focus loss calls pause() before the first game too, and that must not generate a map.
    fun pause() { game?.pause(); revision++ }
    fun resume() { game?.resume(); revision++ }
    fun menu() { pause(); screen = Screen.Menu }
    fun select(language: Language) {
        this.language = language
        store.put("language", language.code)
        revision++
    }
    fun selectCity(city: City) {
        this.city = city
        store.put("city", city.code)
        if (cityLanguage) select(city.language)
    }
    fun switchLanguageWithCity(on: Boolean) {
        cityLanguage = on
        store.put("city.language", on.toString())
        if (on) select(city.language)
    }
    fun tick(dt: Float) {
        engine.advance(dt)
        if (!recorded && engine.status == GameStatus.Won) {
            recorded = true
            val played = engine.map.city
            if (engine.survivors > bests.getValue(played)) {
                bests[played] = engine.survivors
                store.put("best.${played.code}", engine.survivors.toString())
            }
        }
        revision++
    }
}
