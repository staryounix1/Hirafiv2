package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.remote.SupabaseConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GoogleAuthAccount(
    val idToken: String,
    val displayName: String,
    val email: String,
    val nonce: String = ""
)

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun launchGoogleSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (GoogleAuthAccount) -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit,
        onLaunchedBrowserOAuth: () -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            if (SupabaseConfig.isConfigured && launchBrowserGoogleOAuth(context)) {
                onLaunchedBrowserOAuth()
            } else {
                onAuthError("تعذر فتح نافذة اختيار حساب غوغل")
            }
            return
        }

        val clientId = runCatching {
            context.getString(R.string.default_web_client_id).trim()
        }.getOrNull().orEmpty()

        if (clientId.isBlank()) {
            if (SupabaseConfig.isConfigured && launchBrowserGoogleOAuth(activity)) {
                onLaunchedBrowserOAuth()
            } else {
                onAuthError("تعذر العثور على إعدادات تسجيل الدخول بواسطة غوغل")
            }
            return
        }

        scope.launch {
            val rawNonce = java.util.UUID.randomUUID().toString()
            val hashedNonce = sha256Hex(rawNonce)

            // 1. Primary native flow: GetSignInWithGoogleOption (explicit in-app Google Sign-In sheet)
            val buttonOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
                .setNonce(hashedNonce)
                .build()
            val primaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(buttonOption)
                .build()

            when (val primary = tryCredentialRequest(activity, credentialManager, primaryRequest, rawNonce)) {
                is CredentialAttempt.Success -> {
                    onAuthSuccess(primary.account)
                    return@launch
                }
                CredentialAttempt.Cancelled -> {
                    onAuthCancelled()
                    return@launch
                }
                is CredentialAttempt.Failed -> {
                    Log.w(TAG, "GetSignInWithGoogleOption unavailable (${primary.message}); trying GetGoogleIdOption")
                }
            }

            // 2. Secondary native flow: GetGoogleIdOption without authorized-accounts filter
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()
            val secondaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            when (val secondary = tryCredentialRequest(activity, credentialManager, secondaryRequest, rawNonce)) {
                is CredentialAttempt.Success -> {
                    onAuthSuccess(secondary.account)
                    return@launch
                }
                CredentialAttempt.Cancelled -> {
                    onAuthCancelled()
                    return@launch
                }
                is CredentialAttempt.Failed -> {
                    Log.w(TAG, "GetGoogleIdOption unavailable (${secondary.message}); falling back to Supabase Google OAuth browser flow")
                    if (SupabaseConfig.isConfigured && launchBrowserGoogleOAuth(activity)) {
                        onLaunchedBrowserOAuth()
                    } else {
                        onAuthError(secondary.message.ifBlank { "تسجيل الدخول عبر غوغل غير متاح على هذا الجهاز حالياً" })
                    }
                }
            }
        }
    }

    fun launchBrowserGoogleOAuth(context: Context): Boolean {
        if (!SupabaseConfig.isConfigured) return false
        return runCatching {
            val pkceVerifier = SupabaseConfig.generatePkceVerifier()
            context.applicationContext
                .getSharedPreferences(
                    com.example.data.local.UserPreferencesDataStore.PKCE_PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                    com.example.data.local.UserPreferencesDataStore.KEY_PKCE_VERIFIER,
                    pkceVerifier
                )
                .commit()
            val pkceChallenge = SupabaseConfig.generatePkceChallenge(pkceVerifier)
            val authUri = Uri.parse(SupabaseConfig.googleAuthorizeUrl(codeChallenge = pkceChallenge))
            val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                if (context.findActivity() == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        }.onFailure { err ->
            Log.e(TAG, "Failed to open browser Google OAuth", err)
        }.getOrDefault(false)
    }

    private sealed interface CredentialAttempt {
        data class Success(val account: GoogleAuthAccount) : CredentialAttempt
        data object Cancelled : CredentialAttempt
        data class Failed(val message: String) : CredentialAttempt
    }

    private fun sha256Hex(raw: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun tryCredentialRequest(
        activity: Activity,
        credentialManager: CredentialManager,
        request: GetCredentialRequest,
        rawNonce: String
    ): CredentialAttempt {
        return try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val tokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                CredentialAttempt.Success(
                    GoogleAuthAccount(
                        idToken = tokenCred.idToken,
                        displayName = tokenCred.displayName.orEmpty(),
                        email = tokenCred.id,
                        nonce = rawNonce
                    )
                )
            } else {
                CredentialAttempt.Failed("نوع بيانات الاعتماد غير مدعوم")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}")
            CredentialAttempt.Cancelled
        } catch (e: Exception) {
            CredentialAttempt.Failed(e.localizedMessage ?: "فشل تسجيل الدخول بواسطة غوغل")
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    onComplete()
                }
            }
        }
    }
}
