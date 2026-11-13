package dev.oblac.kolona

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.*
import dev.oblac.gamekit.IosAudio
import dev.oblac.gamekit.IosStore
import dev.oblac.kolona.ui.GameSession
import dev.oblac.kolona.ui.KolonaApp

/** SwiftUI forwards scene changes here, so a backgrounded game always pauses. */
class IosGame {
    private val session = GameSession(IosStore("kolona."), audio = IosAudio())
    fun viewController(): UIViewController = ComposeUIViewController { KolonaApp(session) }
    fun pause() = session.pause()
}
