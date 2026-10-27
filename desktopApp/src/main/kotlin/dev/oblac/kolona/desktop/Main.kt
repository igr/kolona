package dev.oblac.kolona.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import dev.oblac.kolona.data.DesktopPreferenceStore
import dev.oblac.kolona.i18n.Copy
import dev.oblac.kolona.i18n.Strings
import dev.oblac.kolona.ui.GameSession
import dev.oblac.kolona.ui.KolonaApp

fun main() = application {
    val session = remember { GameSession(DesktopPreferenceStore()) }
    Window(
        onCloseRequest = ::exitApplication,
        title = Strings(session.language)[Copy.GameName],
        state = rememberWindowState(width = 430.dp, height = 870.dp),
        resizable = false,
    ) {
        DisposableEffect(window) {
            val listener = object : WindowAdapter() {
                override fun windowDeactivated(e: WindowEvent?) { session.pause() }
                override fun windowIconified(e: WindowEvent?) { session.pause() }
            }
            window.addWindowListener(listener)
            onDispose { window.removeWindowListener(listener) }
        }
        KolonaApp(session)
    }
}
