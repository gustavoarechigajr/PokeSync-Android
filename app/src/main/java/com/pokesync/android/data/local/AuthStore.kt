package com.pokesync.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
    }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "pokesync_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var token: String?
        get() = encryptedPrefs.getString("jwt_token", null)
        set(value) = encryptedPrefs.edit().apply {
            if (value != null) putString("jwt_token", value) else remove("jwt_token")
        }.apply()

    var userId: String?
        get() = encryptedPrefs.getString("user_id", null)
        set(value) = encryptedPrefs.edit().apply {
            if (value != null) putString("user_id", value) else remove("user_id")
        }.apply()

    var username: String?
        get() = encryptedPrefs.getString("username", null)
        set(value) = encryptedPrefs.edit().apply {
            if (value != null) putString("username", value) else remove("username")
        }.apply()

    val serverUrl: Flow<String?> = dataStore.data.map { it[KEY_SERVER_URL] }

    suspend fun saveServerUrl(url: String) {
        dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    fun isLoggedIn(): Boolean = token != null

    suspend fun logout() {
        token = null
        userId = null
        username = null
    }
}
