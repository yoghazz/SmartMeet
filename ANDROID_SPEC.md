# SmartMeet Android Client — Spesifikasi Teknis

> Dokumen ini adalah spesifikasi lengkap untuk membangun Android client SmartMeet.
> Backend API sudah live di `https://api.sityreq.online`

---

## 1. Tech Stack

| Komponen | Library / Tool |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Navigation | Compose Navigation |
| Network (REST) | Retrofit 2 + OkHttp 4 |
| Network (WebSocket) | OkHttp WebSocket |
| Local DB | Room |
| Auth Storage | DataStore (Encrypted) |
| Audio Recording | Android AudioRecord API |
| Background Service | Android Foreground Service |
| Image Loading | Coil |
| Animations | Lottie |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## 2. Design System

### 2.1 Color Palette

```kotlin
// Light Theme
val PrimaryBlue = Color(0xFF1A56A6)       // #1A56A6
val SecondaryBlue = Color(0xFF2E86AB)     // #2E86AB
val AccentOrange = Color(0xFFF4A261)      // #F4A261
val Background = Color(0xFFF8FAFF)
val Surface = Color(0xFFFFFFFF)
val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6B7280)
val Divider = Color(0xFFE5E7EB)
val Error = Color(0xFFDC2626)
val Success = Color(0xFF16A34A)
val RecordingRed = Color(0xFFEF4444)

// Dark Theme
val PrimaryBlueDark = Color(0xFF3B82F6)
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val OnBackgroundDark = Color(0xFFF1F5F9)
```

### 2.2 Typography

```kotlin
// Font families
// Poppins — headings (download dari Google Fonts)
// Inter — body text (download dari Google Fonts)

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = Poppins, fontWeight = Bold, fontSize = 32.sp),
    displayMedium = TextStyle(fontFamily = Poppins, fontWeight = Bold, fontSize = 28.sp),
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = SemiBold, fontSize = 24.sp),
    headlineMedium= TextStyle(fontFamily = Poppins, fontWeight = SemiBold, fontSize = 20.sp),
    titleLarge    = TextStyle(fontFamily = Poppins, fontWeight = Medium, fontSize = 18.sp),
    titleMedium   = TextStyle(fontFamily = Inter, fontWeight = Medium, fontSize = 16.sp),
    bodyLarge     = TextStyle(fontFamily = Inter, fontWeight = Normal, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = Inter, fontWeight = Normal, fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = Inter, fontWeight = Normal, fontSize = 12.sp),
    labelLarge    = TextStyle(fontFamily = Inter, fontWeight = Medium, fontSize = 14.sp),
)
```

### 2.3 Shape & Elevation

```kotlin
val Shapes = Shapes(
    small  = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large  = RoundedCornerShape(24.dp),
)
// Card elevation: 2.dp default, 8.dp pressed
// FAB: CircleShape dengan shadow
```

### 2.4 Spacing

```
xs = 4.dp
sm = 8.dp
md = 16.dp
lg = 24.dp
xl = 32.dp
xxl = 48.dp
```

---

## 3. Project Structure

```
app/
├── src/main/
│   ├── java/com/smartmeet/app/
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   ├── SmartMeetApi.kt          # Retrofit interface
│   │   │   │   ├── WebSocketManager.kt      # OkHttp WebSocket
│   │   │   │   └── models/                  # Request/Response DTOs
│   │   │   ├── db/
│   │   │   │   ├── SmartMeetDatabase.kt
│   │   │   │   ├── dao/SessionDao.kt
│   │   │   │   └── entities/SessionEntity.kt
│   │   │   ├── repository/
│   │   │   │   ├── AuthRepository.kt
│   │   │   │   ├── SessionRepository.kt
│   │   │   │   └── DocumentRepository.kt
│   │   │   └── datastore/
│   │   │       └── AuthDataStore.kt         # Token storage
│   │   ├── domain/
│   │   │   ├── model/                       # Domain models
│   │   │   └── usecase/                     # Business logic
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Typography.kt
│   │   │   │   ├── Shape.kt
│   │   │   │   └── Theme.kt
│   │   │   ├── components/                  # Reusable Composables
│   │   │   │   ├── AudioWaveform.kt
│   │   │   │   ├── SessionCard.kt
│   │   │   │   ├── LoadingButton.kt
│   │   │   │   └── StatusChip.kt
│   │   │   ├── screens/
│   │   │   │   ├── splash/SplashScreen.kt
│   │   │   │   ├── auth/
│   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   └── RegisterScreen.kt
│   │   │   │   ├── dashboard/DashboardScreen.kt
│   │   │   │   ├── session/
│   │   │   │   │   ├── NewSessionScreen.kt
│   │   │   │   │   ├── RecordingScreen.kt
│   │   │   │   │   └── SessionDetailScreen.kt
│   │   │   │   ├── document/DocumentScreen.kt
│   │   │   │   ├── library/LibraryScreen.kt
│   │   │   │   └── settings/SettingsScreen.kt
│   │   │   └── navigation/
│   │   │       └── NavGraph.kt
│   │   ├── service/
│   │   │   └── RecordingForegroundService.kt
│   │   ├── di/
│   │   │   ├── NetworkModule.kt
│   │   │   ├── DatabaseModule.kt
│   │   │   └── RepositoryModule.kt
│   │   └── MainActivity.kt
│   └── res/
│       ├── raw/                             # Lottie JSON files
│       └── font/                            # Inter + Poppins ttf
```

---

## 4. Gradle Dependencies (build.gradle.kts — app)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Security (EncryptedDataStore)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
}
```

---

## 5. API Base URL & Config

```kotlin
// NetworkModule.kt
const val BASE_URL = "https://api.sityreq.online/"

// Retrofit
val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .client(okHttpClient)
    .build()

// OkHttpClient dengan auth interceptor
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val token = runBlocking { authDataStore.getAccessToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        chain.proceed(request)
    }
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()
```

---

## 6. API Contract (Retrofit Interface)

```kotlin
interface SmartMeetApi {

    // ── Auth ──────────────────────────────────────────────
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

    // ── Sessions ──────────────────────────────────────────
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
    suspend fun updateSession(
        @Path("id") id: String,
        @Body body: UpdateSessionRequest
    ): SessionResponse

    @POST("api/v1/sessions/{id}/start")
    suspend fun startRecording(@Path("id") id: String): SessionResponse

    @POST("api/v1/sessions/{id}/stop")
    suspend fun stopRecording(@Path("id") id: String): SessionResponse

    @Multipart
    @POST("api/v1/sessions/{id}/upload-audio")
    suspend fun uploadAudio(
        @Path("id") id: String,
        @Part audio: MultipartBody.Part
    ): SessionResponse

    @DELETE("api/v1/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): Response<Unit>

    // ── Documents ─────────────────────────────────────────
    @POST("api/v1/sessions/{id}/documents")
    suspend fun generateDocuments(
        @Path("id") id: String,
        @Body body: GenerateDocRequest
    ): List<DocumentResponse>

    @GET("api/v1/sessions/{id}/documents")
    suspend fun getDocuments(@Path("id") id: String): List<DocumentResponse>

    @GET("api/v1/sessions/{id}/documents/{docId}")
    suspend fun getDocument(
        @Path("id") id: String,
        @Path("docId") docId: String
    ): DocumentResponse

    @POST("api/v1/sessions/{id}/documents/{docId}/share")
    suspend fun createShareLink(
        @Path("id") id: String,
        @Path("docId") docId: String
    ): ShareLinkResponse
}
```

---

## 7. Data Models (DTOs)

```kotlin
// Request models
data class RegisterRequest(val email: String, val full_name: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class RefreshTokenRequest(val refresh_token: String)
data class GoogleAuthRequest(val id_token: String)
data class CreateSessionRequest(
    val title: String,
    val category: String,        // meeting|seminar|lecture|discussion|interview|other
    val language: String,        // id|en
    val participants: List<Participant>?,
    val recording_mode: String   // realtime|batch|hybrid
)
data class UpdateSessionRequest(
    val title: String? = null,
    val manual_notes: String? = null,
    val transcript: String? = null
)
data class GenerateDocRequest(
    val formats: List<String>,   // ["pdf","docx","pptx"]
    val theme: String = "professional"  // professional|modern|minimalis
)
data class Participant(val name: String, val email: String?)

// Response models
data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String
)
data class UserResponse(
    val id: String,
    val email: String,
    val full_name: String,
    val is_active: Boolean,
    val subscription_plan: String,  // free|pro|enterprise
    val auth_provider: String,
    val created_at: String
)
data class SessionListResponse(
    val id: String,
    val title: String,
    val category: String,
    val status: String,             // created|recording|paused|processing|completed|failed
    val language: String,
    val audio_duration_seconds: Int?,
    val started_at: String?,
    val ended_at: String?,
    val created_at: String
)
data class SessionResponse(
    val id: String,
    val owner_id: String,
    val title: String,
    val category: String,
    val language: String,
    val participants: List<Participant>?,
    val recording_mode: String,
    val status: String,
    val audio_duration_seconds: Int?,
    val summary: String?,
    val key_points: List<String>?,
    val action_items: List<ActionItem>?,
    val conclusions: String?,
    val sentiment: SentimentData?,
    val manual_notes: String?,
    val started_at: String?,
    val ended_at: String?,
    val created_at: String,
    val updated_at: String
)
data class ActionItem(val task: String, val assignee: String?, val due_date: String?)
data class SentimentData(val overall: String, val score: Float, val topics: Map<String, String>?)
data class DocumentResponse(
    val id: String,
    val session_id: String,
    val format: String,          // pdf|docx|pptx
    val status: String,          // pending|generating|completed|failed
    val theme: String,
    val file_size_bytes: Int?,
    val download_url: String?,
    val share_token: String?,
    val share_expires_at: String?,
    val error_message: String?,
    val created_at: String
)
data class ShareLinkResponse(val share_url: String, val expires_at: String)
```

---

## 8. WebSocket (Real-time Audio Streaming)

**Endpoint:** `wss://api.sityreq.online/ws/sessions/{session_id}/stream?token={JWT}`

```kotlin
// WebSocketManager.kt
class WebSocketManager @Inject constructor(private val client: OkHttpClient) {

    private var webSocket: WebSocket? = null
    val transcriptFlow = MutableSharedFlow<TranscriptChunk>()
    val statusFlow = MutableSharedFlow<String>()

    fun connect(sessionId: String, token: String) {
        val url = "wss://api.sityreq.online/ws/sessions/$sessionId/stream?token=$token"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = Gson().fromJson(text, WebSocketMessage::class.java)
                when (msg.type) {
                    "transcript_chunk" -> transcriptFlow.tryEmit(TranscriptChunk(msg.text, msg.is_final))
                    "status" -> statusFlow.tryEmit(msg.status)
                }
            }
        })
    }

    // Kirim audio chunk (PCM 16kHz 16-bit mono)
    fun sendAudioChunk(pcmData: ByteArray) {
        webSocket?.send(pcmData.toByteString())
    }

    fun sendStop() {
        webSocket?.send("""{"type":"stop"}""")
    }

    fun disconnect() {
        webSocket?.close(1000, "Recording stopped")
    }
}

data class TranscriptChunk(val text: String, val isFinal: Boolean)
data class WebSocketMessage(
    val type: String,
    val text: String? = null,
    val is_final: Boolean = false,
    val status: String? = null
)
```

---

## 9. Recording Service

```kotlin
// RecordingForegroundService.kt
// - Jalankan sebagai Foreground Service agar tidak mati saat layar off
// - AudioRecord config: 16kHz, MONO, PCM_16BIT
// - Chunk size: 16000 * 2 * 5 bytes (5 detik audio)
// - Kirim chunk via WebSocket (mode realtime) atau buffer lokal (mode batch)
// - Tampilkan persistent notification saat recording

class RecordingForegroundService : Service() {
    private val SAMPLE_RATE = 16000
    private val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    private val FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val CHUNK_SECONDS = 5

    // Permissions yang dibutuhkan: RECORD_AUDIO, FOREGROUND_SERVICE
    // Notification channel: "recording_channel" dengan IMPORTANCE_LOW
    // Notification action: "Stop Recording" button
}
```

---

## 10. Screen Specifications

### 10.1 Splash Screen

- Logo SmartMeet di tengah dengan animasi Lottie (fade in + scale)
- Background: gradient dari `PrimaryBlue` ke `SecondaryBlue`
- Auto-navigate: cek token → Dashboard (jika login) atau Login (jika belum)
- Duration: 2 detik

---

### 10.2 Login Screen

**Layout:**
- Top: Logo SmartMeet kecil + tagline "AI Meeting Recorder"
- Card putih di tengah dengan shadow:
  - TextField: Email (keyboardType = Email)
  - TextField: Password (visualTransformation = Password) + eye icon toggle
  - `LoadingButton` "Masuk" → POST `/auth/login`
  - Divider "atau"
  - `GoogleSignInButton` → POST `/auth/google`
- Bottom: "Belum punya akun? Daftar" → navigate ke Register

**State:**
- Loading state pada button saat request
- Error snackbar jika credentials salah
- Simpan token ke DataStore setelah berhasil

---

### 10.3 Register Screen

**Layout:**
- TextField: Nama Lengkap
- TextField: Email
- TextField: Password + konfirmasi password
- `LoadingButton` "Daftar"
- "Sudah punya akun? Masuk"

**Validasi:**
- Email format valid
- Password min 8 karakter
- Konfirmasi password harus sama

---

### 10.4 Dashboard Screen

**Layout:**
- Top App Bar: "SmartMeet" + avatar user (klik → Settings)
- Stats row: total sesi, jam rekaman bulan ini
- Section "Sesi Terbaru": horizontal scroll `SessionCard` list (5 item terbaru)
- Section "Sedang Diproses": list sesi dengan status `processing`
- FAB besar di bottom center: "⏺ Rekam" → navigate ke NewSession
- Bottom Navigation Bar:
  - Dashboard (home icon)
  - Library (archive icon)
  - Settings (gear icon)

**`SessionCard` component:**
```
┌─────────────────────────────────┐
│ [Kategori chip]    [Status chip] │
│ Judul Sesi                       │
│ 📅 Tanggal  ⏱ Durasi            │
│ [PDF] [DOCX] [PPTX] (jika ada)  │
└─────────────────────────────────┘
```

---

### 10.5 New Session Screen

**Layout:**
- App Bar: "Sesi Baru" + tombol back
- Form:
  - `OutlinedTextField` Nama Rapat (required)
  - `ExposedDropdownMenu` Kategori: Rapat, Seminar, Kuliah, Diskusi, Wawancara
  - `SegmentedButton` Bahasa: Indonesia / English
  - `SegmentedButton` Mode: Real-time / Batch / Hybrid
  - Chip group peserta: input nama + add button, tampil sebagai removable chips
- Bottom: `LoadingButton` "Mulai Rekam" → create session → navigate ke Recording

---

### 10.6 Recording Screen

**Layout (landscape-friendly):**

```
┌──────────────────────────────────┐
│ ← [Nama Sesi]        [Pause] [⏹]│
├──────────────────────────────────┤
│                                  │
│    🎙️ [Audio Waveform Animasi]   │
│         00:05:32                 │
│                                  │
├──────────────────────────────────┤
│ Live Transcript                  │
│ ┌──────────────────────────────┐ │
│ │ Speaker 1: "Baik, mari kita  │ │
│ │ mulai rapat hari ini..."     │ │
│ │ Speaker 2: "Setuju..."       │ │
│ └──────────────────────────────┘ │
├──────────────────────────────────┤
│ 📝 Tambah catatan...  [Kirim]   │
└──────────────────────────────────┘
```

**Audio Waveform:**
- Custom Composable: Canvas dengan bar vertikal yang beranimasi mengikuti amplitude
- Bar count: 40 bar
- Warna aktif: `AccentOrange`, inactive: `Divider`
- Animasi: `animateFloatAsState` per bar

**Behavior:**
- Mode Realtime: AudioRecord → WebSocket → live transcript muncul
- Mode Batch: AudioRecord → simpan lokal → upload saat Stop
- Tombol Pause: suspend recording, tampil tombol Resume
- Tombol Stop: konfirmasi dialog → stop → upload audio → navigate ke SessionDetail
- Persistent notification dengan "Stop Recording" action

---

### 10.7 Session Detail Screen

**Layout:**
- Top App Bar: judul sesi + status chip + share button
- TabRow: Ringkasan | Transkripsi | Dokumen

**Tab Ringkasan:**
```
┌── Rangkuman Eksekutif ──────────┐
│ [Teks rangkuman paragraf]       │
└─────────────────────────────────┘
┌── Poin-Poin Kunci ──────────────┐
│ • Poin 1                        │
│ • Poin 2                        │
└─────────────────────────────────┘
┌── Action Items ─────────────────┐
│ ☐ Task A — Budi (30 Jun)        │
│ ☐ Task B — Ani (TBD)            │
└─────────────────────────────────┘
┌── Kesimpulan ───────────────────┐
│ [Teks kesimpulan]               │
└─────────────────────────────────┘
┌── Sentimen ─────────────────────┐
│ 😊 Positif (score: 0.75)        │
└─────────────────────────────────┘
```

**Tab Transkripsi:**
- List segment dengan label Speaker + timestamp
- Editable teks per segment
- "Simpan perubahan" button

**Tab Dokumen:**
- Jika belum generate: tombol "Generate Laporan" → DocumentScreen
- Jika sudah: list dokumen dengan status + download button + share button
- Pull-to-refresh untuk cek status dokumen yang masih generating

---

### 10.8 Document Generation Screen

**Layout:**
- App Bar: "Generate Laporan"
- Section "Format Output":
  - CheckBox: PDF (selalu tersedia)
  - CheckBox: Word (.docx) — locked untuk Free user
  - CheckBox: PowerPoint (.pptx) — locked untuk Free user
  - Badge "Pro" untuk fitur yang locked
- Section "Tema Desain":
  - 3 pilihan visual (card dengan preview):
    - Professional (biru formal)
    - Modern (gradien kontemporer)
    - Minimalis (putih bersih)
- `LoadingButton` "Generate" → POST `/sessions/{id}/documents`
- Setelah submit: tampil progress dengan polling setiap 3 detik
- Setelah selesai: tombol Download + Share per format

---

### 10.9 Library Screen

**Layout:**
- Search bar di atas
- Filter chips: Semua | Selesai | Diproses | Gagal
- Sort: Terbaru | Terlama | Terpanjang
- LazyColumn `SessionCard` list
- Swipe-to-delete dengan konfirmasi
- Empty state: ilustrasi + "Belum ada rekaman"

---

### 10.10 Settings Screen

**Layout:**
- Section Profil: avatar, nama, email, subscription badge
- Section Rekaman:
  - Kualitas audio: Standar (16kHz) / Tinggi (48kHz)
  - Mode default: Real-time / Batch / Hybrid
  - Auto-pause saat telepon masuk: toggle
- Section API:
  - TextField: Base URL (default: `https://api.sityreq.online`) — untuk ganti server
- Section Akun:
  - Kelola langganan
  - Logout
  - Hapus akun

---

## 11. Navigation Graph

```kotlin
// NavGraph.kt
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object NewSession : Screen("new_session")
    object Recording : Screen("recording/{sessionId}") {
        fun createRoute(sessionId: String) = "recording/$sessionId"
    }
    object SessionDetail : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
    object Documents : Screen("documents/{sessionId}") {
        fun createRoute(sessionId: String) = "documents/$sessionId"
    }
    object Library : Screen("library")
    object Settings : Screen("settings")
}

// Flow:
// Splash → (token ada) Dashboard
//        → (token kosong) Login → Register
// Dashboard → NewSession → Recording → SessionDetail → Documents
// Dashboard → Library → SessionDetail
// Dashboard → Settings
```

---

## 12. Local Database (Room)

```kotlin
// SessionEntity.kt
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val status: String,
    val language: String,
    val audioDurationSeconds: Int?,
    val summary: String?,
    val keyPointsJson: String?,    // JSON array
    val actionItemsJson: String?,  // JSON array
    val conclusions: String?,
    val manualNotes: String?,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// Dao
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)
}
```

---

## 13. Auth DataStore

```kotlin
// AuthDataStore.kt
class AuthDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? =
        dataStore.data.first()[ACCESS_TOKEN_KEY]

    suspend fun getRefreshToken(): String? =
        dataStore.data.first()[REFRESH_TOKEN_KEY]

    suspend fun clearTokens() {
        dataStore.edit { it.clear() }
    }

    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val Context.dataStore by preferencesDataStore(name = "auth_prefs")
    }
}
```

---

## 14. AndroidManifest Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Foreground Service -->
<service
    android:name=".service.RecordingForegroundService"
    android:foregroundServiceType="microphone"
    android:exported="false" />
```

---

## 15. Key UX Details

1. **Offline mode**: Saat tidak ada internet, tampilkan banner "Mode Offline" — rekaman tetap bisa dilakukan, auto-upload saat koneksi pulih
2. **Auto-refresh token**: Jika API return 401, otomatis panggil `/auth/refresh` dan retry request
3. **Pull-to-refresh**: Di Dashboard dan Library
4. **Loading skeleton**: Tampilkan shimmer loading di SessionCard saat fetch data
5. **Error handling**: Setiap error API tampilkan Snackbar dengan pesan yang user-friendly (bukan raw error)
6. **Haptic feedback**: Saat tombol Record ditekan (START/STOP)
7. **Background recording**: Pastikan RecordingForegroundService tidak mati saat layar off atau app di-minimize
8. **Dark mode**: Semua screen support dark mode, follow system setting
9. **Tablet support**: Layout adaptif untuk layar ≥ 7 inch (gunakan WindowSizeClass)
10. **Audio format untuk upload**: Compress ke M4A/AAC sebelum upload (hemat bandwidth)

---

## 16. Free vs Pro Enforcement

```kotlin
// Di DocumentScreen, cek subscription:
if (user.subscription_plan == "free") {
    // Disable DOCX dan PPTX checkbox
    // Tampilkan overlay "Pro" badge
    // Klik → tampilkan UpgradeDialog
}

// Batas rekaman Free: 180 menit/bulan
// Tampilkan progress bar di Settings: "X/180 menit terpakai"
```

---

## 17. Checklist Implementasi

- [ ] Setup project Android (Kotlin + Compose + Hilt)
- [ ] Tambah semua dependencies di build.gradle.kts
- [ ] Buat design system (Color, Typography, Theme)
- [ ] Implementasi AuthDataStore
- [ ] Implementasi Retrofit + OkHttp (NetworkModule)
- [ ] Buat semua DTO data classes
- [ ] Implementasi SmartMeetApi interface
- [ ] Buat Room database + DAO
- [ ] Buat AuthRepository
- [ ] Buat SessionRepository
- [ ] Buat DocumentRepository
- [ ] Implementasi WebSocketManager
- [ ] Buat RecordingForegroundService
- [ ] Splash Screen
- [ ] Login + Register Screen + ViewModel
- [ ] Dashboard Screen + ViewModel
- [ ] NewSession Screen + ViewModel
- [ ] Recording Screen + ViewModel (AudioRecord + WebSocket)
- [ ] SessionDetail Screen + ViewModel (3 tabs)
- [ ] Document Generation Screen + ViewModel
- [ ] Library Screen + ViewModel
- [ ] Settings Screen + ViewModel
- [ ] Navigation Graph
- [ ] AndroidManifest permissions + service
- [ ] Test di device fisik Android 8.0+
