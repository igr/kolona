package dev.oblac.kolona

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.oblac.gamekit.AndroidAudio
import dev.oblac.gamekit.AndroidStore
import dev.oblac.kolona.ui.*

class MainActivity : ComponentActivity() {
    private lateinit var session: GameSession
    private lateinit var audio: AndroidAudio
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audio = AndroidAudio(this)
        session = GameSession(AndroidStore(getSharedPreferences("kolona", MODE_PRIVATE)), audio = audio)
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
    override fun onDestroy() {
        if (::audio.isInitialized) audio.release()
        super.onDestroy()
    }
}
