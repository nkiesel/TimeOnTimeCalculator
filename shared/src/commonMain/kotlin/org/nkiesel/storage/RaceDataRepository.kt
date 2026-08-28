package org.nkiesel.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.nkiesel.model.RaceComparisonData
import org.nkiesel.service.JibesetBoat

class RaceDataRepository(
    private val storage: KeyValueStorage,
    private val json: Json = defaultJson
) {

    companion object {
        const val KEY_RACE_DATA = "saved_race_data"
        const val KEY_JIBESET_BOATS = "saved_jibeset_boats"

        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    fun saveRaceData(data: RaceComparisonData) {
        try {
            val jsonString = json.encodeToString(data)
            storage.setString(KEY_RACE_DATA, jsonString)
        } catch (_: Exception) {
        }
    }

    fun loadRaceData(): RaceComparisonData? {
        val jsonString = storage.getString(KEY_RACE_DATA) ?: return null
        return try {
            json.decodeFromString<RaceComparisonData>(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    fun saveBoats(boats: List<JibesetBoat>) {
        try {
            val jsonString = json.encodeToString(boats)
            storage.setString(KEY_JIBESET_BOATS, jsonString)
        } catch (_: Exception) {
        }
    }

    fun loadBoats(): List<JibesetBoat> {
        val jsonString = storage.getString(KEY_JIBESET_BOATS) ?: return emptyList()
        return try {
            json.decodeFromString<List<JibesetBoat>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
