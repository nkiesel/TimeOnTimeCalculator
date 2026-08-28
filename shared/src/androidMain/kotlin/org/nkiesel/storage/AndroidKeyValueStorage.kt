package org.nkiesel.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidKeyValueStorage(context: Context) : KeyValueStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences("tot_race_data", Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun setString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) {
                remove(key)
            } else {
                putString(key, value)
            }
            apply()
        }
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

@Composable
actual fun rememberKeyValueStorage(): KeyValueStorage {
    val context = LocalContext.current
    return remember(context) { AndroidKeyValueStorage(context) }
}
