package dev.oblac.kolona

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.oblac.kolona.data.PreferenceStore
import dev.oblac.kolona.ui.*

class MainActivity : ComponentActivity() {
    private lateinit var session: GameSession
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        session = GameSession(AndroidPreferenceStore(getSharedPreferences("kolona", MODE_PRIVATE)))
        setContent {
            KolonaApp(session)
            BackHandler(enabled = session.screen != Screen.Welcome) {
                when (session.screen) {
                    Screen.Game -> session.pause()
                    Screen.Menu -> session.screen = Screen.Welcome
                    else -> session.screen = Screen.Menu
                }
            }
        }
    }
    override fun onPause() {
        if (::session.isInitialized) session.pause()
        super.onPause()
    }
}

private class AndroidPreferenceStore(private val preferences: SharedPreferences) : PreferenceStore {
    override fun get(key: String) = preferences.getString(key, null)
    override fun put(key: String, value: String) { preferences.edit().putString(key, value).apply() }
}
