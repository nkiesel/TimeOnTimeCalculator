package org.nkiesel.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

class IosKeyValueStorage : KeyValueStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, defaultValue: String?): String? {
        val value = defaults.stringForKey(key)
        return value ?: defaultValue
    }

    override fun setString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

@Composable
actual fun rememberKeyValueStorage(): KeyValueStorage {
    return remember { IosKeyValueStorage() }
}
