package dev.oblac.kolona

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSProcessInfo
import platform.UIKit.*
import dev.oblac.gamekit.IosAudio
import dev.oblac.gamekit.IosStore
import dev.oblac.kolona.game.GameConfig
import dev.oblac.kolona.ui.GameSession
import dev.oblac.kolona.ui.KolonaApp
import kotlin.experimental.ExperimentalNativeApi

/** SwiftUI forwards scene changes here, so a backgrounded game always pauses. */
@OptIn(ExperimentalNativeApi::class)
class IosGame {
    // A Debug build launched with -calm sends no attackers, so a game reaches the square for the store screenshot of a win.
    private val calm = Platform.isDebugBinary && "-calm" in NSProcessInfo.processInfo.arguments
    private val session = GameSession(IosStore("kolona."), GameConfig(spawnEnemies = !calm), IosAudio())
    fun viewController(): UIViewController = ComposeUIViewController { KolonaApp(session) }
    fun pause() = session.pause()
}
