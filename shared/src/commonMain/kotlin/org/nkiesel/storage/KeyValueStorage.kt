package org.nkiesel.storage

import androidx.compose.runtime.Composable

/**
 * Key-value storage abstraction for persisting settings and race data.
 */
interface KeyValueStorage {
    fun getString(key: String, defaultValue: String? = null): String?
    fun setString(key: String, value: String?)
    fun remove(key: String)
}

/**
 * In-memory implementation of KeyValueStorage useful for testing and preview.
 */
class InMemoryKeyValueStorage(
    private val map: MutableMap<String, String> = mutableMapOf()
) : KeyValueStorage {
    override fun getString(key: String, defaultValue: String?): String? = map[key] ?: defaultValue

    override fun setString(key: String, value: String?) {
        if (value == null) {
            map.remove(key)
        } else {
            map[key] = value
        }
    }

    override fun remove(key: String) {
        map.remove(key)
    }
}

/**
 * Provides a platform-specific KeyValueStorage instance in Compose.
 */
@Composable
expect fun rememberKeyValueStorage(): KeyValueStorage
