package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hirafi_prefs")

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val EMAIL = stringPreferencesKey("user_email")
        val PHONE = stringPreferencesKey("phone")
        val CITY = stringPreferencesKey("city")
        val ROLE = stringPreferencesKey("role")
        val ROLE_LOCKED = booleanPreferencesKey("role_locked")
        val SPECIALTY_ID = stringPreferencesKey("craftsman_specialty_id")
        val SPECIALTY_LOCKED = booleanPreferencesKey("craftsman_specialty_locked")
        val IS_ONLINE = booleanPreferencesKey("craftsman_is_online")
        val AUTH_PROVIDER = stringPreferencesKey("auth_provider")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val AVATAR_URI = stringPreferencesKey("profile_avatar_uri")
        val BIO = stringPreferencesKey("profile_bio")
        val EXPERIENCE_YEARS = intPreferencesKey("craftsman_experience_years")
        val BASE_PRICE_DH = intPreferencesKey("craftsman_base_price_dh")
        val VERIFICATION_STATUS = stringPreferencesKey("craftsman_verification_status")
        val NATIONAL_ID_NUMBER = stringPreferencesKey("craftsman_national_id")
        val ID_FRONT_URI = stringPreferencesKey("craftsman_id_front_uri")
        val ID_BACK_URI = stringPreferencesKey("craftsman_id_back_uri")
        val SELFIE_URI = stringPreferencesKey("craftsman_selfie_uri")
        val VERIFICATION_SUBMITTED_AT = longPreferencesKey("craftsman_verification_submitted_at")
        val REJECTION_REASON = stringPreferencesKey("craftsman_rejection_reason")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val IS_SUSPENDED = booleanPreferencesKey("is_suspended")
        val SUPABASE_EMAIL = stringPreferencesKey("supabase_email")
        val SUPABASE_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
        val SUPABASE_REFRESH_TOKEN = stringPreferencesKey("supabase_refresh_token")
        val SUPABASE_EXPIRES_AT = longPreferencesKey("supabase_expires_at")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val appLanguageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE])
    }

    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        val roleDb = prefs[Keys.ROLE] ?: UserRole.CLIENT.dbValue
        val specId = prefs[Keys.SPECIALTY_ID] ?: "plumbing"
        UserSession(
            userId = prefs[Keys.USER_ID] ?: "local_user",
            fullName = prefs[Keys.FULL_NAME] ?: "يونس العلمي",
            email = prefs[Keys.EMAIL] ?: "",
            phone = prefs[Keys.PHONE] ?: "+212 612-345678",
            city = prefs[Keys.CITY] ?: "الدار البيضاء",
            role = UserRole.fromDb(roleDb),
            isRoleLocked = prefs[Keys.ROLE_LOCKED] ?: false,
            craftsmanSpecialtyId = specId,
            isCraftsmanSpecialtyLocked = prefs[Keys.SPECIALTY_LOCKED] ?: false,
            isCraftsmanOnline = prefs[Keys.IS_ONLINE] ?: true,
            authProvider = prefs[Keys.AUTH_PROVIDER] ?: "email",
            avatarUri = prefs[Keys.AVATAR_URI],
            bio = prefs[Keys.BIO] ?: "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
            experienceYears = prefs[Keys.EXPERIENCE_YEARS] ?: 5,
            basePriceDh = prefs[Keys.BASE_PRICE_DH] ?: HirafiCategories.byId(specId).defaultPriceDh,
            verificationStatus = IdentityVerificationStatus.fromDb(prefs[Keys.VERIFICATION_STATUS]),
            nationalIdNumber = prefs[Keys.NATIONAL_ID_NUMBER] ?: "",
            idFrontPhotoUri = prefs[Keys.ID_FRONT_URI],
            idBackPhotoUri = prefs[Keys.ID_BACK_URI],
            selfiePhotoUri = prefs[Keys.SELFIE_URI],
            verificationSubmittedAtMillis = prefs[Keys.VERIFICATION_SUBMITTED_AT],
            rejectionReason = prefs[Keys.REJECTION_REASON] ?: "",
            fcmToken = prefs[Keys.FCM_TOKEN] ?: "",
            isSuspended = prefs[Keys.IS_SUSPENDED] ?: false,
            supabaseEmail = prefs[Keys.SUPABASE_EMAIL],
            supabaseAccessToken = prefs[Keys.SUPABASE_ACCESS_TOKEN],
            supabaseRefreshToken = prefs[Keys.SUPABASE_REFRESH_TOKEN],
            supabaseExpiresAtMillis = prefs[Keys.SUPABASE_EXPIRES_AT] ?: 0L
        )
    }

    suspend fun setRole(role: UserRole): Boolean {
        var changed = false
        context.dataStore.edit { prefs ->
            val locked = prefs[Keys.ROLE_LOCKED] ?: false
            if (!locked) {
                prefs[Keys.ROLE] = role.dbValue
                changed = true
            }
        }
        return changed
    }

    suspend fun registerFirstTimeAccount(
        fullName: String,
        email: String = "",
        phone: String = "+212 612-345678",
        city: String,
        role: UserRole,
        specialtyId: String?,
        passwordHash: String? = null,
        authProvider: String = "email",
        userIdOverride: String? = null,
        avatarUri: String? = null,
        bio: String? = null,
        experienceYears: Int? = null,
        basePriceDh: Int? = null,
        verificationStatus: IdentityVerificationStatus? = null,
        rejectionReason: String? = null,
        isSuspended: Boolean? = null,
        forceOverwrite: Boolean = false
    ): Boolean {
        var registered = false
        context.dataStore.edit { prefs ->
            val alreadyLocked = prefs[Keys.ROLE_LOCKED] ?: false
            if (!alreadyLocked || forceOverwrite) {
                prefs[Keys.FULL_NAME] = fullName.trim()
                if (email.isNotBlank()) prefs[Keys.EMAIL] = email.trim().lowercase()
                if (phone.isNotBlank()) prefs[Keys.PHONE] = phone.trim()
                prefs[Keys.CITY] = city
                prefs[Keys.ROLE] = role.dbValue
                prefs[Keys.ROLE_LOCKED] = true
                prefs[Keys.AUTH_PROVIDER] = authProvider
                prefs[Keys.IS_ONLINE] = true
                if (!avatarUri.isNullOrBlank()) prefs[Keys.AVATAR_URI] = avatarUri
                if (!bio.isNullOrBlank()) prefs[Keys.BIO] = bio
                if (experienceYears != null) prefs[Keys.EXPERIENCE_YEARS] = experienceYears
                if (basePriceDh != null) prefs[Keys.BASE_PRICE_DH] = basePriceDh
                if (verificationStatus != null) prefs[Keys.VERIFICATION_STATUS] = verificationStatus.dbValue
                if (rejectionReason != null) prefs[Keys.REJECTION_REASON] = rejectionReason
                if (isSuspended != null) prefs[Keys.IS_SUSPENDED] = isSuspended
                if (!passwordHash.isNullOrBlank()) {
                    prefs[Keys.PASSWORD_HASH] = passwordHash
                }
                if (!userIdOverride.isNullOrBlank()) {
                    prefs[Keys.USER_ID] = userIdOverride
                } else {
                    val existingId = prefs[Keys.USER_ID]
                    if (existingId.isNullOrBlank() || existingId == "local_user") {
                        prefs[Keys.USER_ID] = "${role.dbValue}_${java.util.UUID.randomUUID().toString().take(8)}"
                    }
                }
                if (role == UserRole.CRAFTSMAN && !specialtyId.isNullOrBlank()) {
                    prefs[Keys.SPECIALTY_ID] = specialtyId
                    prefs[Keys.SPECIALTY_LOCKED] = true
                    if (basePriceDh == null) {
                        prefs[Keys.BASE_PRICE_DH] = HirafiCategories.byId(specialtyId).defaultPriceDh
                    }
                } else {
                    prefs[Keys.SPECIALTY_LOCKED] = false
                }
                registered = true
            }
        }
        return registered
    }

    suspend fun setCraftsmanOnline(isOnline: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONLINE] = isOnline }
    }

    suspend fun setCity(city: String) {
        context.dataStore.edit { it[Keys.CITY] = city }
    }

    suspend fun setSpecialty(specialtyId: String): Boolean {
        var updated = false
        context.dataStore.edit { prefs ->
            val alreadyLocked = prefs[Keys.SPECIALTY_LOCKED] ?: false
            if (!alreadyLocked) {
                prefs[Keys.SPECIALTY_ID] = specialtyId
                prefs[Keys.SPECIALTY_LOCKED] = true
                updated = true
            }
        }
        return updated
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        city: String,
        avatarUri: String? = null,
        bio: String? = null,
        experienceYears: Int? = null,
        basePriceDh: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FULL_NAME] = fullName.trim()
            prefs[Keys.PHONE] = phone.trim()
            prefs[Keys.CITY] = city
            if (avatarUri != null) {
                if (avatarUri.isBlank()) prefs.remove(Keys.AVATAR_URI)
                else prefs[Keys.AVATAR_URI] = avatarUri
            }
            if (bio != null) prefs[Keys.BIO] = bio.trim()
            if (experienceYears != null) prefs[Keys.EXPERIENCE_YEARS] = experienceYears.coerceIn(0, 60)
            if (basePriceDh != null) prefs[Keys.BASE_PRICE_DH] = basePriceDh.coerceIn(40, 5000)
        }
    }

    suspend fun saveSupabaseAuth(
        userId: String,
        email: String,
        accessToken: String,
        refreshToken: String? = null,
        expiresAtMillis: Long = 0L
    ) {
        context.dataStore.edit {
            it[Keys.USER_ID] = userId
            it[Keys.EMAIL] = email.trim().lowercase()
            it[Keys.SUPABASE_EMAIL] = email.trim().lowercase()
            it[Keys.SUPABASE_ACCESS_TOKEN] = accessToken
            if (!refreshToken.isNullOrBlank()) {
                it[Keys.SUPABASE_REFRESH_TOKEN] = refreshToken
            }
            if (expiresAtMillis > 0L) {
                it[Keys.SUPABASE_EXPIRES_AT] = expiresAtMillis
            }
        }
    }

    suspend fun clearSupabaseAuth() {
        context.dataStore.edit {
            it.remove(Keys.SUPABASE_EMAIL)
            it.remove(Keys.SUPABASE_ACCESS_TOKEN)
            it.remove(Keys.SUPABASE_REFRESH_TOKEN)
            it.remove(Keys.SUPABASE_EXPIRES_AT)
            it[Keys.USER_ID] = "local_user"
        }
    }

    suspend fun saveIdentityVerificationSubmission(
        nationalIdNumber: String,
        frontPhotoUri: String,
        backPhotoUri: String,
        selfiePhotoUri: String,
        status: IdentityVerificationStatus
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NATIONAL_ID_NUMBER] = nationalIdNumber
            prefs[Keys.ID_FRONT_URI] = frontPhotoUri
            prefs[Keys.ID_BACK_URI] = backPhotoUri
            prefs[Keys.SELFIE_URI] = selfiePhotoUri
            prefs[Keys.VERIFICATION_STATUS] = status.dbValue
            prefs[Keys.VERIFICATION_SUBMITTED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateIdentityVerificationStatus(
        status: IdentityVerificationStatus,
        rejectionReason: String = ""
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VERIFICATION_STATUS] = status.dbValue
            prefs[Keys.REJECTION_REASON] = rejectionReason
        }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FCM_TOKEN] = token
        }
    }

    suspend fun setSuspended(suspended: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SUSPENDED] = suspended
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.code
        }
    }

    fun saveOAuthPkceVerifier(verifier: String) {
        context.getSharedPreferences(PKCE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PKCE_VERIFIER, verifier)
            .commit()
    }

    fun consumeOAuthPkceVerifier(): String? {
        val sp = context.getSharedPreferences(PKCE_PREFS_NAME, Context.MODE_PRIVATE)
        val value = sp.getString(KEY_PKCE_VERIFIER, null)
        sp.edit().remove(KEY_PKCE_VERIFIER).commit()
        return value?.takeIf { it.isNotBlank() }
    }

    suspend fun logoutAccount() {
        context.dataStore.edit { prefs ->
            val savedLang = prefs[Keys.APP_LANGUAGE]
            prefs.clear()
            if (!savedLang.isNullOrBlank()) {
                prefs[Keys.APP_LANGUAGE] = savedLang
            }
        }
        context.getSharedPreferences(PKCE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PKCE_VERIFIER)
            .commit()
    }

    companion object {
        const val PKCE_PREFS_NAME = "hirafi_oauth_pkce"
        const val KEY_PKCE_VERIFIER = "pkce_verifier"
    }
}
