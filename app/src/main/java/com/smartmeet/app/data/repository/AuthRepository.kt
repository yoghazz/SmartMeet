package com.smartmeet.app.data.repository

import com.smartmeet.app.data.api.SmartMeetApi
import com.smartmeet.app.data.api.models.GoogleAuthRequest
import com.smartmeet.app.data.api.models.LoginRequest
import com.smartmeet.app.data.api.models.RegisterRequest
import com.smartmeet.app.data.api.models.TokenResponse
import com.smartmeet.app.data.api.models.UserResponse
import com.smartmeet.app.data.datastore.AuthDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: SmartMeetApi,
    private val authDataStore: AuthDataStore
) {
    suspend fun login(email: String, password: String): TokenResponse {
        val token = api.login(LoginRequest(email, password))
        authDataStore.saveTokens(token.accessToken, token.refreshToken)
        return token
    }

    suspend fun register(fullName: String, email: String, password: String): UserResponse {
        return api.register(RegisterRequest(email = email, fullName = fullName, password = password))
    }

    suspend fun loginWithGoogle(idToken: String): TokenResponse {
        val token = api.googleAuth(GoogleAuthRequest(idToken))
        authDataStore.saveTokens(token.accessToken, token.refreshToken)
        return token
    }

    suspend fun me(): UserResponse = api.getMe()
    suspend fun isLoggedIn(): Boolean = !authDataStore.getAccessToken().isNullOrBlank()
    suspend fun logout() = authDataStore.clear()
    suspend fun getAccessToken(): String? = authDataStore.getAccessToken()
}
