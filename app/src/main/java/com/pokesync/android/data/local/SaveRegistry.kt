package com.pokesync.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pokesync.android.domain.model.RegisteredSave
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class RegisteredSaveJson(
    val id: String,
    val absolutePath: String,
    val displayName: String,
    val emulator: String,
    val gameVersion: String?,
    val generation: Int?,
    val trainerName: String?,
    val addedAt: Long,
    val lastRefreshedAt: Long?,
    val lastSaveId: String?,
)

@Singleton
class SaveRegistry @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi,
) {
    companion object {
        private val KEY_SAVES = stringPreferencesKey("registered_saves")
    }

    private val adapter by lazy {
        val type = Types.newParameterizedType(List::class.java, RegisteredSaveJson::class.java)
        moshi.adapter<List<RegisteredSaveJson>>(type)
    }

    val saves: Flow<List<RegisteredSave>> = dataStore.data.map { prefs ->
        val json = prefs[KEY_SAVES] ?: return@map emptyList()
        adapter.fromJson(json)?.map { it.toDomain() } ?: emptyList()
    }

    suspend fun add(save: RegisteredSave) {
        dataStore.edit { prefs ->
            val current = readList(prefs).toMutableList()
            current.removeAll { it.id == save.id }
            current.add(save.toJson())
            prefs[KEY_SAVES] = adapter.toJson(current)
        }
    }

    suspend fun update(save: RegisteredSave) = add(save)

    suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            val current = readList(prefs).toMutableList()
            current.removeAll { it.id == id }
            prefs[KEY_SAVES] = adapter.toJson(current)
        }
    }

    private fun readList(prefs: Preferences): List<RegisteredSaveJson> {
        val json = prefs[KEY_SAVES] ?: return emptyList()
        return adapter.fromJson(json) ?: emptyList()
    }

    private fun RegisteredSaveJson.toDomain() = RegisteredSave(
        id, absolutePath, displayName, emulator, gameVersion,
        generation, trainerName, addedAt, lastRefreshedAt, lastSaveId,
    )

    private fun RegisteredSave.toJson() = RegisteredSaveJson(
        id, absolutePath, displayName, emulator, gameVersion,
        generation, trainerName, addedAt, lastRefreshedAt, lastSaveId,
    )
}
