package com.example.data.remote

import android.util.Log
import com.example.data.model.AdminAuthorizationPolicy
import com.example.data.model.HirafiCategories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class SupabaseUserProfileRepository(
    private val sessionManager: SupabaseSessionManager
) {
    private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    fun triggerRefresh() {
        refreshSignal.tryEmit(Unit)
    }

    fun observeMyProfile(): Flow<SupabaseProfileDto?> = flow {
        while (true) {
            val uid = sessionManager.currentUidOrNull
            val bearer = sessionManager.getValidBearerToken()
            val api = SupabaseClientProvider.getApiServiceOrNull()
            if (uid != null && bearer != null && api != null) {
                runCatching {
                    val res = api.getProfilesByFilter(
                        apiKey = SupabaseConfig.anonKey,
                        bearer = bearer,
                        idFilter = "eq.$uid"
                    )
                    if (res.isSuccessful) {
                        emit(res.body()?.firstOrNull())
                    }
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                    Log.w(TAG, "observeMyProfile poll skipped: ${err.message}")
                }
            }
            withTimeoutOrNull(4000L) {
                refreshSignal.first()
            }
        }
    }

    suspend fun fetchExistingProfileIfLocked(): Result<SupabaseProfileDto?> {
        val auth = sessionManager.authState.value
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("تعذر التحقق من جلسة Supabase"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))

        return try {
            val res = api.getProfilesByFilter(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.${auth.uid}"
            )
            if (!res.isSuccessful) {
                return Result.failure(IllegalStateException("تعذر قراءة الملف الشخصي (${res.code()})"))
            }
            val existing = res.body()?.firstOrNull()
            if (existing != null) {
                val effectiveEmail = existing.email.ifBlank { auth.email }.trim().lowercase()
                if (existing.role != "admin" && isEmailAuthorizedAdmin(effectiveEmail, bearer)) {
                    api.patchProfile(
                        apiKey = SupabaseConfig.anonKey,
                        bearer = bearer,
                        idFilter = "eq.${auth.uid}",
                        patch = SupabaseProfileUpdatePatchDto(role = "admin")
                    )
                    return Result.success(existing.copy(role = "admin", roleLocked = true, email = effectiveEmail))
                }
                if (existing.roleLocked || existing.role == "craftsman" || existing.role == "admin") {
                    return Result.success(existing)
                }
            }
            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fetchExistingProfileIfLocked failed", e)
            Result.failure(e)
        }
    }

    suspend fun isEmailAuthorizedAdmin(email: String, bearerOverride: String? = null): Boolean {
        val normalized = email.trim().lowercase()
        if (normalized == AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL.lowercase()) return true
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return false
        val bearer = bearerOverride ?: sessionManager.getValidBearerToken() ?: return false
        return try {
            val res = api.getAuthorizedAdmins(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                emailFilter = "eq.$normalized"
            )
            res.isSuccessful && !res.body().isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun syncOrRestoreProfile(
        fullName: String,
        email: String,
        city: String,
        chosenRole: String,
        chosenSpecialtyId: String
    ): Result<SupabaseProfileDto> {
        val auth = sessionManager.authState.value
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً في Supabase"))
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("تعذر التحقق من جلسة Supabase"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))

        return try {
            val cleanEmail = email.ifBlank { auth.email }
                .trim()
                .lowercase()
                .let { if (it.length < 3) "user@hirafi.app" else it }
                .take(120)
            val isAdminEmail = isEmailAuthorizedAdmin(cleanEmail, bearer)

            val getRes = api.getProfilesByFilter(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.${auth.uid}"
            )
            val existing = if (getRes.isSuccessful) getRes.body()?.firstOrNull() else null
            if (existing != null && (existing.roleLocked || existing.role == "craftsman" || existing.role == "admin")) {
                val existingEmail = existing.email.ifBlank { cleanEmail }.trim().lowercase()
                if (existing.role != "admin" && (isAdminEmail || isEmailAuthorizedAdmin(existingEmail, bearer))) {
                    api.patchProfile(
                        apiKey = SupabaseConfig.anonKey,
                        bearer = bearer,
                        idFilter = "eq.${auth.uid}",
                        patch = SupabaseProfileUpdatePatchDto(role = "admin")
                    )
                    triggerRefresh()
                    return Result.success(existing.copy(role = "admin", email = existingEmail))
                }
                triggerRefresh()
                return Result.success(existing)
            }

            val safeRole = if (isAdminEmail) {
                "admin"
            } else {
                when (chosenRole) {
                    "craftsman" -> "craftsman"
                    else -> "client"
                }
            }

            val cleanName = fullName.trim().let { if (it.length < 2) "مستخدم حرفي" else it }.take(80)
            val cleanCity = city.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(60)
            val safeSpec = chosenSpecialtyId.trim().let { if (it.length < 2) "plumbing" else it }.take(40)
            val category = HirafiCategories.byId(safeSpec)

            val dto = SupabaseProfileDto(
                id = auth.uid,
                fullName = cleanName,
                email = cleanEmail,
                phone = "+212 612-345678",
                city = cleanCity,
                role = safeRole,
                roleLocked = true,
                specialtyId = safeSpec,
                specialty = category.nameAr,
                bio = "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
                experienceYears = 5,
                basePriceDh = category.defaultPriceDh,
                isOnline = safeRole == "craftsman",
                verificationStatus = "not_submitted",
                rejectionReason = "",
                isSuspended = false
            )
            val upsertRes = api.upsertProfile(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                profile = dto
            )
            if (!upsertRes.isSuccessful) {
                val errBody = runCatching { upsertRes.errorBody()?.string() }.getOrNull().orEmpty()
                Log.e(TAG, "Profile upsert failed (${upsertRes.code()}): $errBody")
                return Result.failure(IllegalStateException("تعذر حفظ الحساب في Supabase (${upsertRes.code()})"))
            }
            triggerRefresh()
            Result.success(upsertRes.body()?.firstOrNull() ?: dto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "syncOrRestoreProfile failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        city: String,
        avatarUri: String?,
        bio: String,
        experienceYears: Int,
        basePriceDh: Int
    ): Result<Unit> {
        val uid = sessionManager.currentUidOrNull
            ?: return Result.failure(IllegalStateException("انتهت جلسة الدخول في Supabase"))
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("تعذر التحقق من رمز الجلسة"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        return try {
            val res = api.patchProfile(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$uid",
                patch = SupabaseProfileUpdatePatchDto(
                    fullName = fullName.trim().let { if (it.length < 2) "مستخدم حرفي" else it }.take(80),
                    phone = phone.trim().let { if (it.length < 6) "+212 612-345678" else it }.take(30),
                    city = city.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(60),
                    avatarUri = avatarUri?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                    bio = bio.trim().take(400),
                    experienceYears = experienceYears.coerceIn(0, 60),
                    basePriceDh = basePriceDh.coerceIn(40, 5000)
                )
            )
            if (!res.isSuccessful) {
                Result.failure(IllegalStateException("تعذر تحديث الملف الشخصي (${res.code()})"))
            } else {
                triggerRefresh()
                Result.success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        val uid = sessionManager.currentUidOrNull ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        val clean = token.trim().take(500)
        if (clean.isBlank()) return Result.success(Unit)
        return runCatching {
            api.patchProfile(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$uid",
                patch = SupabaseProfileUpdatePatchDto(fcmToken = clean)
            )
            Unit
        }
    }

    companion object {
        private const val TAG = "SupabaseUserProfile"
    }
}
