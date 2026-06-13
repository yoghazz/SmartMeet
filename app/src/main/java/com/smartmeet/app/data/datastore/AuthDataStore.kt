package com.smartmeet.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.authPrefs by preferencesDataStore(name = "smartmeet_auth")

@Singleton
class AuthDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
    }

    val accessTokenFlow: Flow<String?> = context.authPrefs.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.accessToken] }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.authPrefs.edit {
            it[Keys.accessToken] = accessToken
            it[Keys.refreshToken] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? = accessTokenFlow.firstOrNull()

    suspend fun getRefreshToken(): String? = context.authPrefs.data.map { it[Keys.refreshToken] }.firstOrNull()

    suspend fun clear() {
        context.authPrefs.edit { it.clear() }
    }
}
