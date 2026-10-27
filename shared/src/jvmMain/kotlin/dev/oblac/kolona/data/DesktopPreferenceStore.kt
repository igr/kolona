package dev.oblac.kolona.data

import java.util.prefs.Preferences

class DesktopPreferenceStore : PreferenceStore {
    private val preferences = Preferences.userRoot().node("dev/oblac/kolona")
    override fun get(key: String): String? = preferences.get(key, null)
    override fun put(key: String, value: String) { preferences.put(key, value) }
}
