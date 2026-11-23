package com.example.data.remote

import android.util.Log
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppRuntimeConfig
import com.example.data.model.AuthorizedAdminRecord
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.VerificationSubmission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

class SupabaseAdminRepository(
    private val sessionManager: SupabaseSessionManager,
    private val storageService: SupabaseStorageService? = null
) {
    private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 12)

    fun triggerRefresh() {
        refreshSignal.tryEmit(Unit)
    }

    fun observeAppRuntimeConfig(): Flow<AppRuntimeConfig> = pollingFlow(5000L) { api, bearer ->
        val res = api.getAppConfig(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer
        )
        if (res.isSuccessful) {
            val row = res.body()?.firstOrNull()
            if (row == null) AppRuntimeConfig(productionMode = false)
            else AppRuntimeConfig(
                productionMode = row.productionMode,
                updatedByEmail = row.updatedByEmail,
                updatedAtMillis = SupabaseTimestampParser.parseIsoToMillis(row.updatedAt)
            )
        } else null
    }

    suspend fun setProductionMode(enabled: Boolean): Result<Unit> {
        val email = sessionManager.currentEmailOrNull ?: "admin"
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        return try {
            val res = api.upsertAppConfig(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                config = SupabaseAppConfigDto(
                    id = "runtime",
                    productionMode = enabled,
                    updatedByEmail = email.take(120)
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر تحديث وضع الإنتاج (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitVerificationRequest(
        craftsmanName: String,
        specialtyId: String,
        city: String,
        nationalIdNumber: String,
        frontPhotoUri: String,
        backPhotoUri: String,
        selfiePhotoUri: String
    ): Result<Unit> {
        val uid = sessionManager.currentUidOrNull
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("انتهت صلاحية الجلسة"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))

        return try {
            val res = api.upsertVerificationSubmission(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabaseVerificationDto(
                    craftsmanId = uid,
                    craftsmanName = craftsmanName.trim().ifBlank { "حرفي" }.take(80),
                    specialtyId = specialtyId.trim().ifBlank { "plumbing" }.take(40),
                    city = city.trim().ifBlank { "الدار البيضاء" }.take(60),
                    nationalIdNumber = nationalIdNumber.trim().take(20),
                    frontPhotoUri = frontPhotoUri.trim().take(500),
                    backPhotoUri = backPhotoUri.trim().take(500),
                    selfiePhotoUri = selfiePhotoUri.trim().take(500),
                    status = IdentityVerificationStatus.PENDING_REVIEW.dbValue,
                    rejectionReason = ""
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(IllegalStateException("تعذر إرسال ملف التوثيق (${res.code()})"))
            }
            api.patchProfileAsAdmin(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$uid",
                patch = SupabaseAdminProfilePatchDto(
                    verificationStatus = IdentityVerificationStatus.PENDING_REVIEW.dbValue,
                    rejectionReason = ""
                )
            )
            triggerRefresh()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeVerificationSubmissions(): Flow<List<VerificationSubmission>> = pollingFlow(4000L) { api, bearer ->
        val res = api.getVerificationSubmissions(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer
        )
        if (res.isSuccessful) {
            res.body().orEmpty().map { dto ->
                val front = storageService?.resolvePrivateKycUrl(dto.frontPhotoUri) ?: dto.frontPhotoUri
                val back = storageService?.resolvePrivateKycUrl(dto.backPhotoUri) ?: dto.backPhotoUri
                val selfie = storageService?.resolvePrivateKycUrl(dto.selfiePhotoUri) ?: dto.selfiePhotoUri
                VerificationSubmission(
                    craftsmanId = dto.craftsmanId,
                    craftsmanName = dto.craftsmanName,
                    specialtyId = dto.specialtyId,
                    city = dto.city,
                    nationalIdNumber = dto.nationalIdNumber,
                    frontPhotoUri = front,
                    backPhotoUri = back,
                    selfiePhotoUri = selfie,
                    status = IdentityVerificationStatus.fromDb(dto.status),
                    rejectionReason = dto.rejectionReason,
                    reviewedByEmail = dto.reviewedByEmail,
                    submittedAtMillis = SupabaseTimestampParser.parseIsoToMillis(dto.submittedAt),
                    updatedAtMillis = SupabaseTimestampParser.parseIsoToMillis(dto.updatedAt)
                )
            }
        } else null
    }

    suspend fun reviewVerificationSubmission(
        craftsmanId: String,
        approved: Boolean,
        rejectionReason: String = ""
    ): Result<Unit> {
        val adminEmail = sessionManager.currentEmailOrNull ?: "admin"
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))

        val status = if (approved) IdentityVerificationStatus.VERIFIED else IdentityVerificationStatus.REJECTED
        val reason = if (approved) "" else rejectionReason.trim()
            .ifBlank { "الصور أو رقم البطاقة غير واضحة، يرجى إعادة الرفع" }
            .take(300)

        return try {
            val res = api.patchVerificationSubmission(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                craftsmanIdFilter = "eq.$craftsmanId",
                patch = SupabaseVerificationReviewPatchDto(
                    status = status.dbValue,
                    rejectionReason = reason,
                    reviewedByEmail = adminEmail.take(120)
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(IllegalStateException("تعذر حفظ نتيجة المراجعة (${res.code()})"))
            }
            api.patchProfileAsAdmin(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$craftsmanId",
                patch = SupabaseAdminProfilePatchDto(
                    isVerified = approved,
                    verificationStatus = status.dbValue,
                    rejectionReason = reason
                )
            )
            triggerRefresh()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<AdminUserRecord>> = pollingFlow(5000L) { api, bearer ->
        val res = api.getProfilesByFilter(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer
        )
        if (res.isSuccessful) {
            res.body().orEmpty().map { p ->
                AdminUserRecord(
                    userId = p.id,
                    fullName = p.fullName.ifBlank { "مستخدم" },
                    email = p.email,
                    city = p.city.ifBlank { "الدار البيضاء" },
                    role = UserRole.fromDb(p.role),
                    specialtyId = p.specialtyId.ifBlank { "plumbing" },
                    verificationStatus = IdentityVerificationStatus.fromDb(p.verificationStatus),
                    isSuspended = p.isSuspended,
                    fcmTokenPresent = p.fcmToken.isNotBlank(),
                    createdAtMillis = SupabaseTimestampParser.parseIsoToMillis(p.createdAt)
                )
            }
        } else null
    }

    fun observeAllServiceRequests(): Flow<List<ServiceRequest>> = pollingFlow(4000L) { api, bearer ->
        val res = api.getServiceRequests(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer
        )
        if (res.isSuccessful) {
            res.body().orEmpty().map { dto ->
                val createdMs = SupabaseTimestampParser.parseIsoToMillis(dto.createdAt)
                val updatedMs = SupabaseTimestampParser.parseIsoToMillis(dto.updatedAt ?: dto.createdAt)
                ServiceRequest(
                    id = dto.id,
                    clientId = dto.clientId.orEmpty(),
                    clientName = dto.clientName.ifBlank { "زبون" },
                    categoryId = dto.categoryId.ifBlank { "plumbing" },
                    categoryName = dto.categoryName.ifBlank { "سباكة" },
                    title = dto.title.ifBlank { "طلب خدمة" },
                    description = dto.description,
                    addressLabel = dto.addressLabel.ifBlank { "الدار البيضاء" },
                    lat = dto.lat,
                    lng = dto.lng,
                    mapX = 0.5f,
                    mapY = 0.5f,
                    proposedPrice = dto.proposedPrice,
                    acceptedPrice = dto.acceptedPrice,
                    paymentMethod = PaymentMethod.entries.firstOrNull { it.dbValue == dto.paymentMethod } ?: PaymentMethod.CASH,
                    isUrgent = dto.isUrgent,
                    status = RequestStatus.fromDb(dto.status),
                    assignedCraftsmanId = dto.assignedCraftsmanId,
                    assignedCraftsmanName = dto.assignedCraftsmanName,
                    assignedCraftsmanPhone = dto.assignedCraftsmanPhone,
                    assignedCraftsmanRating = dto.assignedCraftsmanRating,
                    photoUri = dto.photoUri,
                    createdAtMillis = createdMs,
                    updatedAtMillis = updatedMs
                )
            }
        } else null
    }

    suspend fun toggleUserSuspended(
        userId: String,
        isCraftsman: Boolean,
        suspended: Boolean
    ): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        return try {
            val res = api.patchProfileAsAdmin(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$userId",
                patch = SupabaseAdminProfilePatchDto(
                    isSuspended = suspended,
                    isOnline = if (isCraftsman && suspended) false else null
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر تحديث حالة الحساب (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRequestAsAdmin(requestId: String): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        return try {
            val res = api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$requestId",
                patch = SupabaseRequestPatchDto(status = RequestStatus.CANCELLED.dbValue)
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر إلغاء الطلب (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAuthorizedAdmins(): Flow<List<AuthorizedAdminRecord>> = pollingFlow(6000L) { api, bearer ->
        val res = api.getAuthorizedAdmins(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer
        )
        if (res.isSuccessful) {
            res.body().orEmpty().map { dto ->
                AuthorizedAdminRecord(
                    email = dto.email,
                    addedBy = dto.addedBy,
                    createdAtMillis = SupabaseTimestampParser.parseIsoToMillis(dto.createdAt)
                )
            }
        } else null
    }

    suspend fun addAuthorizedAdmin(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        if (clean.length < 4 || !clean.contains("@")) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صحيح"))
        }
        val addedBy = sessionManager.currentEmailOrNull ?: "admin"
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        return try {
            val res = api.upsertAuthorizedAdmin(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabaseAuthorizedAdminDto(email = clean, addedBy = addedBy)
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر إضافة البريد الإداري (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAuthorizedAdmin(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        return try {
            val res = api.deleteAuthorizedAdmin(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                emailFilter = "eq.$clean"
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر حذف البريد الإداري (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun <T> pollingFlow(
        intervalMs: Long,
        fetcher: suspend (api: SupabaseApiService, bearer: String) -> T?
    ): Flow<T> = flow {
        while (true) {
            val bearer = sessionManager.getValidBearerToken()
            val api = SupabaseClientProvider.getApiServiceOrNull()
            if (bearer != null && api != null) {
                runCatching { fetcher(api, bearer) }
                    .onSuccess { if (it != null) emit(it) }
                    .onFailure { err ->
                        if (err is CancellationException) throw err
                        Log.w(TAG, "Admin polling error: ${err.message}")
                    }
            }
            withTimeoutOrNull(intervalMs) {
                refreshSignal.first()
            }
        }
    }

    companion object {
        private const val TAG = "SupabaseAdminRepo"
    }
}
