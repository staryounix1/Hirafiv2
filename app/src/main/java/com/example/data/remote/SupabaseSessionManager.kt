package com.example.data.remote

import android.util.Log
import com.example.data.local.UserPreferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import retrofit2.Response

data class SupabaseAuthState(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long = 0L
)

class GoogleIdTokenFallbackToBrowserException(message: String) : IllegalStateException(message)

class SupabaseSessionManager(
    private val prefs: UserPreferencesDataStore
) {
    private val refreshMutex = Mutex()
    private val _authState = MutableStateFlow<SupabaseAuthState?>(null)
    val authState: StateFlow<SupabaseAuthState?> = _authState.asStateFlow()

    val currentUidOrNull: String?
        get() = _authState.value?.uid?.takeIf { it.isNotBlank() && it != "local_user" }

    val currentEmailOrNull: String?
        get() = _authState.value?.email?.takeIf { it.isNotBlank() }

    val isAuthenticated: Boolean
        get() = currentUidOrNull != null && !_authState.value?.accessToken.isNullOrBlank()

    suspend fun restoreSavedSession(): SupabaseAuthState? {
        val saved = prefs.userSessionFlow.first()
        val token = saved.supabaseAccessToken?.takeIf { it.isNotBlank() } ?: return null
        val uid = saved.userId.takeIf { it.isNotBlank() && it != "local_user" } ?: return null
        val email = (saved.supabaseEmail ?: saved.email).trim().lowercase()
        val state = SupabaseAuthState(
            uid = uid,
            email = email,
            displayName = saved.fullName,
            accessToken = token,
            refreshToken = saved.supabaseRefreshToken,
            expiresAtMillis = saved.supabaseExpiresAtMillis
        )
        _authState.value = state
        return refreshSessionIfNeeded(force = false)
    }

    suspend fun signInWithGoogleIdToken(idToken: String, nonce: String? = null): Result<SupabaseAuthState> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        return try {
            val response = api.signInWithIdToken(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseIdTokenAuthRequest(
                    provider = "google",
                    idToken = idToken,
                    nonce = nonce?.takeIf { it.isNotBlank() }
                )
            )
            if (!response.isSuccessful) {
                val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
                val msg = parseErrorMessageFromRaw(response.code(), raw)
                if (raw.contains("provider is not enabled", ignoreCase = true) ||
                    raw.contains("unsupported_provider", ignoreCase = true)
                ) {
                    return Result.failure(IllegalStateException(msg))
                }
                if (response.code() in listOf(400, 401, 403, 422)) {
                    return Result.failure(GoogleIdTokenFallbackToBrowserException(msg))
                }
                return Result.failure(IllegalStateException(msg))
            }
            val body = response.body()
                ?: return Result.failure(IllegalStateException("استجابة فارغة من خادم Supabase"))
            persistAuthState(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google ID token exchange failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun completeOAuthCallback(uriString: String): Result<SupabaseAuthState> {
        val params = parseOAuthCallbackParams(uriString)
        val oauthError = params["error_description"] ?: params["error"]
        if (!oauthError.isNullOrBlank()) {
            val lowered = oauthError.lowercase()
            val friendly = when {
                lowered.contains("provider is not enabled") || lowered.contains("unsupported_provider") ->
                    "تسجيل الدخول عبر غوغل غير مفعّل بعد في إعدادات Supabase Authentication"
                lowered.contains("access_denied") ->
                    "تم إلغاء تسجيل الدخول عبر غوغل"
                else -> oauthError
            }
            return Result.failure(IllegalStateException(friendly))
        }

        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))

        val accessToken = params["access_token"]?.takeIf { it.isNotBlank() }
        if (accessToken != null) {
            val refreshToken = params["refresh_token"]?.takeIf { it.isNotBlank() }
            val expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600L

            return try {
                val userRes = api.getAuthUser(
                    apiKey = SupabaseConfig.anonKey,
                    bearer = "Bearer $accessToken"
                )
                if (!userRes.isSuccessful) {
                    return Result.failure(IllegalStateException(parseErrorMessage(userRes)))
                }
                val user = userRes.body()
                    ?: return Result.failure(IllegalStateException("تعذر قراءة بيانات حساب غوغل من Supabase"))
                persistAuthState(
                    SupabaseAuthResponse(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = expiresIn,
                        tokenType = "bearer",
                        user = user
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "OAuth implicit callback completion failed", e)
                Result.failure(wrapAuthException(e))
            }
        }

        val authCode = params["code"]?.takeIf { it.isNotBlank() }
        if (authCode != null) {
            val pkceVerifier = prefs.consumeOAuthPkceVerifier()
                ?: return Result.failure(IllegalStateException("انتهت صلاحية جلسة التحقق مع غوغل، يرجى إعادة المحاولة"))
            return try {
                val tokenRes = api.exchangePkceCode(
                    apiKey = SupabaseConfig.anonKey,
                    body = SupabasePkceTokenRequest(
                        authCode = authCode,
                        codeVerifier = pkceVerifier
                    )
                )
                handleAuthResponse(tokenRes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "OAuth PKCE exchange failed", e)
                Result.failure(wrapAuthException(e))
            }
        }

        return Result.failure(
            IllegalStateException(
                "لم يتم استلام رمز الجلسة من غوغل. تأكد من تفعيل مزود Google وإضافة hirafi://auth-callback في إعدادات Supabase."
            )
        )
    }

    suspend fun signInWithEmail(email: String, password: String): Result<SupabaseAuthState> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        val cleanEmail = email.trim().lowercase()
        return try {
            val response = api.signInWithPassword(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseAuthRequest(email = cleanEmail, password = password)
            )
            handleAuthResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Email sign in failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<SupabaseAuthState> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        val cleanEmail = email.trim().lowercase()
        return try {
            val response = api.signUp(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseAuthRequest(email = cleanEmail, password = password)
            )
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException(parseErrorMessage(response)))
            }
            val body = response.body()
                ?: return Result.failure(IllegalStateException("استجابة فارغة من Supabase"))
            val accessToken = body.accessToken
            val uid = body.user?.id
            if (accessToken.isNullOrBlank() || uid.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "تم إنشاء الحساب! يرجى تأكيد رابط التفعيل المرسل لبريدك ثم تسجيل الدخول."
                    )
                )
            }
            persistAuthState(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Email sign up failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً"))
        val cleanEmail = email.trim().lowercase()
        return try {
            val response = api.recoverPassword(
                apiKey = SupabaseConfig.anonKey,
                body = SupabaseRecoverRequest(email = cleanEmail)
            )
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException(parseErrorMessage(response)))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(wrapAuthException(e))
        }
    }

    suspend fun getValidBearerToken(): String? {
        if (_authState.value == null) {
            restoreSavedSession()
        }
        val active = refreshSessionIfNeeded(force = false) ?: return null
        return "Bearer ${active.accessToken}"
    }

    suspend fun refreshSessionIfNeeded(force: Boolean = false): SupabaseAuthState? {
        return refreshMutex.withLock {
            val current = _authState.value ?: return@withLock null
            val now = System.currentTimeMillis()
            val needsRefresh = force || (current.expiresAtMillis in 1L..now + 60_000L)
            if (!needsRefresh) return@withLock current
            val refreshToken = current.refreshToken?.takeIf { it.isNotBlank() }
            if (refreshToken == null) {
                if (current.expiresAtMillis in 1L..now) {
                    _authState.value = null
                    prefs.clearSupabaseAuth()
                    return@withLock null
                }
                return@withLock current
            }
            val api = SupabaseClientProvider.getApiServiceOrNull() ?: return@withLock current
            val res = runCatching {
                api.refreshSession(
                    apiKey = SupabaseConfig.anonKey,
                    body = SupabaseRefreshTokenRequest(refreshToken)
                )
            }.getOrNull() ?: return@withLock current

            if (res.isSuccessful) {
                val body = res.body()
                if (body != null && !body.accessToken.isNullOrBlank()) {
                    return@withLock persistAuthState(body).getOrNull() ?: current
                }
            } else if (res.code() == 400 || res.code() == 401) {
                Log.w(TAG, "Refresh token rejected (${res.code()}), clearing session")
                _authState.value = null
                prefs.clearSupabaseAuth()
                return@withLock null
            }
            current
        }
    }

    suspend fun signOut() {
        val current = _authState.value
        val api = SupabaseClientProvider.getApiServiceOrNull()
        if (api != null && current != null) {
            runCatching {
                api.signOut(SupabaseConfig.anonKey, "Bearer ${current.accessToken}")
            }
        }
        _authState.value = null
        prefs.clearSupabaseAuth()
    }

    private suspend fun handleAuthResponse(response: Response<SupabaseAuthResponse>): Result<SupabaseAuthState> {
        if (!response.isSuccessful) {
            return Result.failure(IllegalStateException(parseErrorMessage(response)))
        }
        val body = response.body()
            ?: return Result.failure(IllegalStateException("استجابة فارغة من خادم Supabase"))
        return persistAuthState(body)
    }

    private suspend fun persistAuthState(body: SupabaseAuthResponse): Result<SupabaseAuthState> {
        val token = body.accessToken?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("لم يتم استلام رمز الجلسة من Supabase"))
        val uid = body.user?.id?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("تعذر قراءة معرف المستخدم من Supabase"))
        val email = body.user.email.orEmpty().trim().lowercase()
        val metaName = (body.user.userMetadata?.get("full_name") as? String)
            ?: (body.user.userMetadata?.get("name") as? String)
            ?: ""
        val expiresInSec = (body.expiresIn ?: 3600L).coerceAtLeast(300L)
        val expiresAtMillis = System.currentTimeMillis() + (expiresInSec * 1000L)

        prefs.saveSupabaseAuth(
            userId = uid,
            email = email,
            accessToken = token,
            refreshToken = body.refreshToken,
            expiresAtMillis = expiresAtMillis
        )
        val state = SupabaseAuthState(
            uid = uid,
            email = email,
            displayName = metaName,
            accessToken = token,
            refreshToken = body.refreshToken,
            expiresAtMillis = expiresAtMillis
        )
        _authState.value = state
        return Result.success(state)
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        return parseErrorMessageFromRaw(response.code(), raw)
    }

    private fun parseErrorMessageFromRaw(code: Int, raw: String): String {
        val parsed = runCatching {
            val json = JSONObject(raw)
            json.optString("error_description")
                .ifBlank { json.optString("msg") }
                .ifBlank { json.optString("message") }
        }.getOrNull().orEmpty()

        val lowered = parsed.lowercase()
        return when {
            lowered.contains("invalid login credentials") -> "البريد الإلكتروني أو كلمة المرور غير صحيحة"
            lowered.contains("user already registered") -> "هذا البريد الإلكتروني مسجل مسبقاً، اختر تسجيل الدخول"
            lowered.contains("email not confirmed") -> "يرجى تأكيد بريدك الإلكتروني أولاً قبل تسجيل الدخول"
            lowered.contains("rate limit") || lowered.contains("429") -> "تم تجاوز الحد المسموح من محاولات التسجيل أو إرسال البريد مؤقتاً (429). يرجى الانتظار قليلًا أو تسجيل الدخول مباشرة إذا كان حسابك مسجلاً."
            lowered.contains("provider is not enabled") || lowered.contains("unsupported_provider") ->
                "تسجيل الدخول عبر غوغل غير مفعّل بعد في إعدادات Supabase Authentication"
            lowered.contains("unacceptable audience") || lowered.contains("nonce") ->
                "إعداد معرّف غوغل (Client ID) غير متطابق في Supabase، جارٍ الاعتماد على المتصفح."
            lowered.contains("password should be at least") -> "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل"
            parsed.isNotBlank() -> "$parsed ($code)"
            else -> "تعذر إتمام المصادقة ($code)"
        }
    }

    private fun wrapAuthException(e: Exception): Throwable {
        val msg = e.localizedMessage.orEmpty()
        return if (msg.contains("Unable to resolve host") || msg.contains("No address associated") || e is java.net.UnknownHostException) {
            IllegalStateException("تعذر الوصول إلى الخادم. يرجى التأكد من صحة رابط المشروع (SUPABASE_URL) في لوحة Secrets وتوفر اتصال الإنترنت.")
        } else {
            IllegalStateException(msg.ifBlank { "تعذر الاتصال بالخادم" })
        }
    }

    companion object {
        private const val TAG = "SupabaseSession"

        fun parseOAuthCallbackParams(rawUri: String): Map<String, String> {
            val result = linkedMapOf<String, String>()
            val queryPart = rawUri.substringAfter('?', "").substringBefore('#')
            val fragmentPart = rawUri.substringAfter('#', "")
            listOf(queryPart, fragmentPart).forEach { section ->
                if (section.isBlank()) return@forEach
                section.split('&').forEach { pair ->
                    val idx = pair.indexOf('=')
                    if (idx > 0) {
                        val key = runCatching {
                            java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                        }.getOrDefault(pair.substring(0, idx))
                        val value = runCatching {
                            java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                        }.getOrDefault(pair.substring(idx + 1))
                        if (key.isNotBlank()) {
                            result[key] = value
                        }
                    }
                }
            }
            return result
        }
    }
}
