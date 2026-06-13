package com.smartmeet.app.data.api

import com.smartmeet.app.data.api.models.CreateSessionRequest
import com.smartmeet.app.data.api.models.DocumentResponse
import com.smartmeet.app.data.api.models.GenerateDocRequest
import com.smartmeet.app.data.api.models.GoogleAuthRequest
import com.smartmeet.app.data.api.models.LoginRequest
import com.smartmeet.app.data.api.models.RefreshTokenRequest
import com.smartmeet.app.data.api.models.RegisterRequest
import com.smartmeet.app.data.api.models.SessionListResponse
import com.smartmeet.app.data.api.models.SessionResponse
import com.smartmeet.app.data.api.models.ShareLinkResponse
import com.smartmeet.app.data.api.models.TokenResponse
import com.smartmeet.app.data.api.models.UpdateSessionRequest
import com.smartmeet.app.data.api.models.UserResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface SmartMeetApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): UserResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): TokenResponse

    @POST("api/v1/auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): TokenResponse

    @GET("api/v1/auth/me")
    suspend fun getMe(): UserResponse

    @GET("api/v1/sessions")
    suspend fun getSessions(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
        @Query("category") category: String? = null
    ): List<SessionListResponse>

    @POST("api/v1/sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): SessionResponse

    @GET("api/v1/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): SessionResponse

    @PATCH("api/v1/sessions/{id}")
    suspend fun updateSession(@Path("id") id: String, @Body body: UpdateSessionRequest): SessionResponse

    @POST("api/v1/sessions/{id}/start")
    suspend fun startRecording(@Path("id") id: String): SessionResponse

    @POST("api/v1/sessions/{id}/stop")
    suspend fun stopRecording(@Path("id") id: String): SessionResponse

    @Multipart
    @POST("api/v1/sessions/{id}/upload-audio")
    suspend fun uploadAudio(@Path("id") id: String, @Part audio: MultipartBody.Part): SessionResponse

    @DELETE("api/v1/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): Response<Unit>

    @POST("api/v1/sessions/{id}/documents")
    suspend fun generateDocuments(@Path("id") id: String, @Body body: GenerateDocRequest): List<DocumentResponse>

    @GET("api/v1/sessions/{id}/documents")
    suspend fun getDocuments(@Path("id") id: String): List<DocumentResponse>

    @GET("api/v1/sessions/{id}/documents/{docId}")
    suspend fun getDocument(@Path("id") id: String, @Path("docId") docId: String): DocumentResponse

    @POST("api/v1/sessions/{id}/documents/{docId}/share")
    suspend fun createShareLink(@Path("id") id: String, @Path("docId") docId: String): ShareLinkResponse
}
