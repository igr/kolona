package dev.oblac.kolona

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSUserDefaults
import platform.UIKit.*
import dev.oblac.kolona.data.PreferenceStore
import dev.oblac.kolona.ui.GameSession
import dev.oblac.kolona.ui.KolonaApp

private class IosPreferenceStore : PreferenceStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun get(key: String): String? = defaults.stringForKey("kolona.$key")
    override fun put(key: String, value: String) { defaults.setObject(value, "kolona.$key") }
}

/** SwiftUI forwards scene changes here, so a backgrounded game always pauses. */
class IosGame {
    private val session = GameSession(IosPreferenceStore())
    fun viewController(): UIViewController = ComposeUIViewController { KolonaApp(session) }
    fun pause() = session.pause()
}
