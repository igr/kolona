package dev.oblac.kolona.data

interface PreferenceStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

class MemoryPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
}
