package com.smartmeet.app.ui.screens.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

object GoogleSignInHelper {
    // web_client_id akan terisi otomatis dari google-services.json setelah SHA-1 didaftarkan di Firebase
    // Format: <project_number>-<hash>.apps.googleusercontent.com
    private const val WEB_CLIENT_ID = "215439803494-default-web-client.apps.googleusercontent.com"

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent = getClient(context).signInIntent
}
