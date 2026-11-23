package com.example.data.repository

import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.HirafiDao
import com.example.data.local.PortfolioItemEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.ServiceOfferEntity
import com.example.data.local.ServiceRequestEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppRuntimeConfig
import com.example.data.model.AuthorizedAdminRecord
import com.example.data.model.ChatMessage
import com.example.data.model.CraftsmanPin
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PortfolioItem
import com.example.data.model.RequestStatus
import com.example.data.model.ReviewRecord
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.model.VerificationSubmission
import com.example.data.remote.MapRoutingService
import com.example.data.remote.SupabaseAdminRepository
import com.example.data.remote.SupabaseAuthState
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseMarketplaceRepository
import com.example.data.remote.SupabaseProfileDto
import com.example.data.remote.SupabaseProfileLocationPatchDto
import com.example.data.remote.SupabaseSessionManager
import com.example.data.remote.SupabaseStorageService
import com.example.data.remote.SupabaseTimestampParser
import com.example.data.remote.SupabaseUserProfileRepository
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

class HirafiRepository(
    private val dao: HirafiDao,
    private val prefs: UserPreferencesDataStore,
    private val supabaseSessionManager: SupabaseSessionManager? = null,
    private val supabaseUserRepo: SupabaseUserProfileRepository? = null,
    private val supabaseMarketplaceRepo: SupabaseMarketplaceRepository? = null,
    private val supabaseAdminRepo: SupabaseAdminRepository? = null,
    private val supabaseStorageService: SupabaseStorageService? = null
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fallbackAuthStateFlow = MutableStateFlow<SupabaseAuthState?>(null).asStateFlow()
    private val fallbackSyncErrorFlow = MutableStateFlow<String?>(null).asStateFlow()

    val userSession: Flow<UserSession> = prefs.userSessionFlow
    val appLanguage: Flow<AppLanguage> = prefs.appLanguageFlow

    suspend fun setAppLanguage(language: AppLanguage) {
        prefs.setAppLanguage(language)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val mgr = supabaseSessionManager
            ?: return Result.failure(IllegalStateException("خدمة استعادة كلمة المرور غير متاحة حالياً"))
        return mgr.sendPasswordResetEmail(email)
    }

    val supabaseAuthStateFlow: StateFlow<SupabaseAuthState?>
        get() = supabaseSessionManager?.authState ?: fallbackAuthStateFlow

    val cloudRequestsSyncError: StateFlow<String?>
        get() = supabaseMarketplaceRepo?.lastRequestsSyncError ?: fallbackSyncErrorFlow

    val isSupabaseAuthenticated: Boolean
        get() = supabaseMarketplaceRepo?.isAuthenticated == true

    fun refreshMarketplaceNow() {
        supabaseMarketplaceRepo?.triggerRefresh()
    }

    fun clearRequestsSyncError() {
        supabaseMarketplaceRepo?.clearRequestsSyncError()
    }

    fun getCraftsmenAround(centerLat: Double = 33.5822, centerLng: Double = -7.6324): List<CraftsmanPin> =
        computeCraftsmenAround(centerLat, centerLng)

    val nearbyCraftsmen: List<CraftsmanPin>
        get() = getCraftsmenAround()

    fun observeRequests(): Flow<List<ServiceRequest>> =
        dao.observeAllRequests().map { list -> list.map { it.toDomain() } }

    fun observeRequestById(requestId: String): Flow<ServiceRequest?> =
        dao.observeAllRequests().map { list -> list.firstOrNull { it.id == requestId }?.toDomain() }

    fun observeOffersForRequest(requestId: String): Flow<List<ServiceOffer>> =
        dao.observeOffersForRequest(requestId).map { list -> list.map { it.toDomain() } }

    fun observeOffersByCraftsman(craftsmanId: String): Flow<List<ServiceOffer>> =
        dao.observeOffersByCraftsman(craftsmanId).map { list -> list.map { it.toDomain() } }

    fun observeMessagesForRequest(requestId: String): Flow<List<ChatMessage>> =
        dao.observeMessagesForRequest(requestId).map { list -> list.map { it.toDomain() } }

    fun observeReviews(): Flow<List<ReviewRecord>> =
        dao.observeReviews().map { list -> list.map { it.toDomain() } }

    fun observePortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItem>> =
        dao.observePortfolioForCraftsman(craftsmanId).map { list -> list.map { it.toDomain() } }

    fun observeCloudClientRequests(): Flow<List<ServiceRequest>> =
        supabaseMarketplaceRepo?.observeClientRequests() ?: emptyFlow()

    fun observeCloudCraftsmanAssignedRequests(): Flow<List<ServiceRequest>> =
        supabaseMarketplaceRepo?.observeCraftsmanAssignedRequests() ?: emptyFlow()

    fun observeCloudSearchingRequests(specialtyId: String): Flow<List<ServiceRequest>> =
        supabaseMarketplaceRepo?.observeSearchingRequests(specialtyId) ?: emptyFlow()

    fun observeCloudOffersForRequest(requestId: String, role: UserRole): Flow<List<ServiceOffer>> =
        supabaseMarketplaceRepo?.observeOffersForRequest(requestId, role) ?: emptyFlow()

    fun observeCloudOffersForCraftsman(): Flow<List<ServiceOffer>> =
        supabaseMarketplaceRepo?.observeOffersForCraftsman() ?: emptyFlow()

    fun observeCloudMessagesForRequest(requestId: String): Flow<List<ChatMessage>> =
        supabaseMarketplaceRepo?.observeMessagesForRequest(requestId) ?: emptyFlow()

    fun observeCloudOnlineCraftsmen(centerLat: Double, centerLng: Double): Flow<List<CraftsmanPin>> =
        supabaseMarketplaceRepo?.observeOnlineCraftsmen(centerLat, centerLng) ?: emptyFlow()

    fun observeCloudRequestLiveCoords(requestId: String): Flow<Pair<Double, Double>?> =
        supabaseMarketplaceRepo?.observeRequestLiveCoords(requestId) ?: emptyFlow()

    fun observeCloudPortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItem>> =
        supabaseMarketplaceRepo?.observePortfolioForCraftsman(craftsmanId) ?: emptyFlow()

    suspend fun fetchPortfolioOnce(craftsmanId: String): List<PortfolioItem> =
        supabaseMarketplaceRepo?.fetchPortfolioOnce(craftsmanId)?.getOrNull().orEmpty()

    suspend fun cacheRemoteRequests(requests: List<ServiceRequest>) {
        if (requests.isEmpty()) return
        dao.upsertRequests(requests.map { it.toEntity() })
    }

    suspend fun cacheSearchingRequestsForCategory(categoryId: String, requests: List<ServiceRequest>) {
        dao.replaceSearchingRequestsForCategory(categoryId, requests.map { it.toEntity() })
    }

    suspend fun cacheClientRequests(clientId: String, requests: List<ServiceRequest>) {
        dao.replaceRequestsForClient(clientId, requests.map { it.toEntity() })
    }

    suspend fun cacheRemoteOffers(offers: List<ServiceOffer>) {
        if (offers.isEmpty()) return
        dao.upsertOffers(offers.map { it.toEntity() })
    }

    suspend fun cacheRemoteMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        dao.insertMessages(messages.map { it.toEntity() })
    }

    suspend fun cacheRemotePortfolio(items: List<PortfolioItem>) {
        if (items.isEmpty()) return
        dao.upsertPortfolioItems(items.map { it.toEntity() })
    }

    fun observeMyCloudProfile(): Flow<SupabaseProfileDto?> =
        supabaseUserRepo?.observeMyProfile() ?: emptyFlow()

    fun observeAppRuntimeConfig(): Flow<AppRuntimeConfig> =
        supabaseAdminRepo?.observeAppRuntimeConfig() ?: emptyFlow()

    fun observeAdminVerificationSubmissions(): Flow<List<VerificationSubmission>> =
        supabaseAdminRepo?.observeVerificationSubmissions() ?: emptyFlow()

    fun observeAdminAllUsers(): Flow<List<AdminUserRecord>> =
        supabaseAdminRepo?.observeAllUsers() ?: emptyFlow()

    fun observeAdminAllRequests(): Flow<List<ServiceRequest>> =
        supabaseAdminRepo?.observeAllServiceRequests() ?: emptyFlow()

    fun observeAuthorizedAdmins(): Flow<List<AuthorizedAdminRecord>> =
        supabaseAdminRepo?.observeAuthorizedAdmins() ?: emptyFlow()

    suspend fun clearSeededRequests() {
        dao.clearSeededRequests()
    }

    suspend fun applyRemoteVerificationUpdate(
        status: IdentityVerificationStatus,
        rejectionReason: String,
        isSuspended: Boolean
    ) {
        prefs.updateIdentityVerificationStatus(status, rejectionReason)
        prefs.setSuspended(isSuspended)
    }

    suspend fun seedInitialMarketplaceRequestsIfEmpty() {
        if (isSupabaseAuthenticated) return
        val existing = dao.observeAllRequests().first()
        if (existing.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val samples = listOf(
            ServiceRequestEntity(
                id = "req_seed_1",
                clientId = "client_sara",
                clientName = "سارة المنصوري",
                categoryId = "plumbing",
                categoryName = "سباكة",
                title = "تسرب مياه تحت حوض المطبخ",
                description = "يوجد تسرب مستمر في أنبوب الصرف تحت الحوض وأحتاج لإصلاح عاجل مع تبديل الجوان.",
                addressLabel = "المعاريف، شارع الزرقطوني",
                lat = 33.5822,
                lng = -7.6324,
                mapX = 0.48f,
                mapY = 0.44f,
                proposedPrice = 130,
                acceptedPrice = null,
                paymentMethod = PaymentMethod.CASH.dbValue,
                isUrgent = true,
                status = RequestStatus.SEARCHING.dbValue,
                assignedCraftsmanId = null,
                assignedCraftsmanName = null,
                assignedCraftsmanPhone = null,
                assignedCraftsmanRating = null,
                photoUri = null,
                createdAtMillis = now - 180_000L,
                updatedAtMillis = now - 180_000L
            ),
            ServiceRequestEntity(
                id = "req_seed_2",
                clientId = "client_mehdi",
                clientName = "المهدي بركة",
                categoryId = "electricity",
                categoryName = "كهرباء",
                title = "انقطاع القاطع الكهربائي الرئيسي",
                description = "الديجونكتور يفصل عند تشغيل السخان الكهربائي، أحتاج فحص اللوحة وتغيير القاطع إذا لزم.",
                addressLabel = "سيدي معروف، التكنوبارك",
                lat = 33.5398,
                lng = -7.6481,
                mapX = 0.42f,
                mapY = 0.72f,
                proposedPrice = 160,
                acceptedPrice = null,
                paymentMethod = PaymentMethod.CASH.dbValue,
                isUrgent = true,
                status = RequestStatus.SEARCHING.dbValue,
                assignedCraftsmanId = null,
                assignedCraftsmanName = null,
                assignedCraftsmanPhone = null,
                assignedCraftsmanRating = null,
                photoUri = null,
                createdAtMillis = now - 420_000L,
                updatedAtMillis = now - 420_000L
            ),
            ServiceRequestEntity(
                id = "req_seed_3",
                clientId = "client_nadia",
                clientName = "نادية الشرايبي",
                categoryId = "ac",
                categoryName = "تبريد وتكييف",
                title = "تنظيف وتعبئة غاز المكيف",
                description = "مكيف سبليت 12000 BTU لا يبرد بشكل جيد منذ أسبوع.",
                addressLabel = "أنفا، قرب موروكو مول",
                lat = 33.5898,
                lng = -7.6852,
                mapX = 0.24f,
                mapY = 0.36f,
                proposedPrice = 220,
                acceptedPrice = null,
                paymentMethod = PaymentMethod.TRANSFER.dbValue,
                isUrgent = false,
                status = RequestStatus.SEARCHING.dbValue,
                assignedCraftsmanId = null,
                assignedCraftsmanName = null,
                assignedCraftsmanPhone = null,
                assignedCraftsmanRating = null,
                photoUri = null,
                createdAtMillis = now - 720_000L,
                updatedAtMillis = now - 720_000L
            )
        )
        dao.upsertRequests(samples)
    }

    suspend fun setRole(role: UserRole): Boolean = prefs.setRole(role)

    suspend fun restoreSupabaseSession(): Result<UserSession?> {
        val manager = supabaseSessionManager ?: return Result.success(null)
        val restored = manager.restoreSavedSession() ?: return Result.success(null)
        val result = restoreExistingSupabaseProfileIfLocked(
            uid = restored.uid,
            fallbackEmail = restored.email
        )
        val restoredSession = result.getOrNull()
        if (result.isSuccess && restoredSession == null) {
            val savedLocal = prefs.userSessionFlow.first()
            if (savedLocal.isRoleLocked && savedLocal.userId == restored.uid && supabaseUserRepo != null) {
                return applySyncedSupabaseProfile(
                    authState = restored,
                    displayName = savedLocal.fullName,
                    fallbackEmail = restored.email.ifBlank { savedLocal.email },
                    city = savedLocal.city,
                    preferredRole = savedLocal.role,
                    preferredSpecialtyId = savedLocal.craftsmanSpecialtyId,
                    authProvider = savedLocal.authProvider
                )
            }
        }
        return result
    }

    suspend fun completeGoogleSupabaseLogin(
        idToken: String,
        nonce: String? = null,
        fallbackDisplayName: String,
        preferredDisplayName: String = "",
        fallbackEmail: String,
        city: String,
        preferredRole: UserRole,
        preferredSpecialtyId: String?,
        autoCreateIfMissing: Boolean = true
    ): Result<UserSession?> {
        val manager = supabaseSessionManager
            ?: return Result.failure(IllegalStateException("تعذر تهيئة جلسة Supabase"))
        val authState = if (idToken.isNotBlank()) {
            manager.signInWithGoogleIdToken(idToken, nonce).getOrElse { return Result.failure(it) }
        } else {
            manager.authState.value
                ?: return Result.failure(IllegalStateException("يرجى اختيار حساب غوغل أولاً"))
        }

        val resolvedEmail = authState.email.ifBlank { fallbackEmail }.trim().lowercase()
        val resolvedName = preferredDisplayName.trim().takeIf { it.length >= 2 }
            ?: authState.displayName.trim().takeIf { it.length >= 2 }
            ?: fallbackDisplayName.trim().takeIf { it.length >= 2 }
            ?: resolvedEmail.substringBefore("@").trim().takeIf { it.length >= 2 }
            ?: "مستخدم غوغل"

        val restored = restoreExistingSupabaseProfileIfLocked(
            uid = authState.uid,
            fallbackEmail = resolvedEmail,
            authProvider = "google"
        ).getOrElse { return Result.failure(it) }
        if (restored != null) {
            return Result.success(restored)
        }

        val isAdminEmail = resolvedEmail.isNotBlank() &&
            (supabaseUserRepo?.isEmailAuthorizedAdmin(resolvedEmail) == true)
        if (!autoCreateIfMissing && !isAdminEmail) {
            return Result.success(null)
        }

        return applySyncedSupabaseProfile(
            authState = authState,
            displayName = resolvedName,
            fallbackEmail = fallbackEmail,
            city = city,
            preferredRole = if (isAdminEmail) UserRole.ADMIN else preferredRole,
            preferredSpecialtyId = preferredSpecialtyId,
            authProvider = "google"
        ).map { it }
    }

    suspend fun completeGoogleOAuthCallback(uriString: String): Result<UserSession?> {
        val manager = supabaseSessionManager
            ?: return Result.failure(IllegalStateException("تعذر تهيئة جلسة Supabase"))
        val authState = manager.completeOAuthCallback(uriString).getOrElse {
            return Result.failure(it)
        }
        val restored = restoreExistingSupabaseProfileIfLocked(
            uid = authState.uid,
            fallbackEmail = authState.email,
            authProvider = "google"
        ).getOrElse { return Result.failure(it) }
        if (restored != null) {
            return Result.success(restored)
        }
        val resolvedEmail = authState.email.trim().lowercase()
        val isAdminEmail = resolvedEmail.isNotBlank() &&
            (supabaseUserRepo?.isEmailAuthorizedAdmin(resolvedEmail) == true)
        if (isAdminEmail) {
            val adminName = authState.displayName.trim().takeIf { it.length >= 2 }
                ?: resolvedEmail.substringBefore("@").trim().takeIf { it.length >= 2 }
                ?: "أدمين المنصة"
            val adminSession = applySyncedSupabaseProfile(
                authState = authState,
                displayName = adminName,
                fallbackEmail = resolvedEmail,
                city = "الدار البيضاء",
                preferredRole = UserRole.ADMIN,
                preferredSpecialtyId = null,
                authProvider = "google"
            ).getOrElse { return Result.failure(it) }
            return Result.success(adminSession)
        }
        return Result.success(null)
    }

    suspend fun signUpWithSupabaseEmailAccount(
        email: String,
        password: String,
        fullName: String,
        city: String,
        preferredRole: UserRole,
        preferredSpecialtyId: String?
    ): Result<UserSession> {
        val manager = supabaseSessionManager
            ?: return Result.failure(IllegalStateException("تعذر تهيئة جلسة Supabase"))
        val authState = manager.signUpWithEmail(email, password).getOrElse {
            return Result.failure(it)
        }
        val resolvedName = fullName.trim().takeIf { it.length >= 2 }
            ?: email.trim().substringBefore("@").takeIf { it.length >= 2 }
            ?: "مستخدم حرفي"
        return applySyncedSupabaseProfile(
            authState = authState,
            displayName = resolvedName,
            fallbackEmail = email,
            city = city,
            preferredRole = preferredRole,
            preferredSpecialtyId = preferredSpecialtyId,
            authProvider = "supabase"
        )
    }

    suspend fun signInWithSupabaseEmailAccount(
        email: String,
        password: String
    ): Result<UserSession> {
        val manager = supabaseSessionManager
            ?: return Result.failure(IllegalStateException("تعذر تهيئة جلسة Supabase"))
        val authState = manager.signInWithEmail(email, password).getOrElse {
            return Result.failure(it)
        }
        val restored = restoreExistingSupabaseProfileIfLocked(
            uid = authState.uid,
            fallbackEmail = authState.email.ifBlank { email },
            authProvider = "supabase"
        ).getOrElse { return Result.failure(it) }

        if (restored != null) {
            return Result.success(restored)
        }

        // First-time sign in for an auth user created in Supabase Dashboard (e.g. Admin)
        val fallbackName = authState.displayName.trim().takeIf { it.length >= 2 }
            ?: authState.email.substringBefore("@").trim().takeIf { it.length >= 2 }
            ?: "مستخدم حرفي"
        return applySyncedSupabaseProfile(
            authState = authState,
            displayName = fallbackName,
            fallbackEmail = authState.email.ifBlank { email },
            city = "الدار البيضاء",
            preferredRole = UserRole.CLIENT,
            preferredSpecialtyId = null,
            authProvider = "supabase"
        )
    }

    suspend fun restoreExistingSupabaseProfileIfLocked(
        uid: String,
        fallbackEmail: String,
        authProvider: String = "supabase"
    ): Result<UserSession?> {
        val userRepo = supabaseUserRepo ?: return Result.success(null)
        val existing = userRepo.fetchExistingProfileIfLocked().getOrElse { err ->
            return Result.failure(err)
        } ?: return Result.success(null)

        val currentSaved = prefs.userSessionFlow.first()
        val restoredProvider = if (currentSaved.authProvider == "google") "google" else authProvider
        val restoredRole = UserRole.fromDb(existing.role)
        val restoredSpec = if (restoredRole == UserRole.CRAFTSMAN) {
            existing.specialtyId.takeIf { it.isNotBlank() } ?: "plumbing"
        } else null

        prefs.registerFirstTimeAccount(
            fullName = existing.fullName.ifBlank { "مستخدم حرفي" },
            email = existing.email.ifBlank { fallbackEmail },
            phone = existing.phone.ifBlank { "+212 612-345678" },
            city = existing.city.ifBlank { "الدار البيضاء" },
            role = restoredRole,
            specialtyId = restoredSpec,
            authProvider = restoredProvider,
            userIdOverride = uid,
            avatarUri = existing.avatarUri,
            bio = existing.bio,
            experienceYears = existing.experienceYears,
            basePriceDh = existing.basePriceDh,
            verificationStatus = IdentityVerificationStatus.fromDb(existing.verificationStatus),
            rejectionReason = existing.rejectionReason,
            isSuspended = existing.isSuspended,
            forceOverwrite = true
        )
        val updatedSession = prefs.userSessionFlow.first()
        if (restoredRole == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(updatedSession.city)
            supabaseMarketplaceRepo?.upsertCraftsmanDirectory(updatedSession, coord.lat, coord.lng)
        }
        return Result.success(updatedSession)
    }

    private suspend fun applySyncedSupabaseProfile(
        authState: SupabaseAuthState,
        displayName: String,
        fallbackEmail: String,
        city: String,
        preferredRole: UserRole,
        preferredSpecialtyId: String?,
        authProvider: String
    ): Result<UserSession> {
        val userRepo = supabaseUserRepo
            ?: return Result.failure(IllegalStateException("تعذر الاتصال بخدمة Supabase"))
        val resolvedEmail = authState.email.ifBlank { fallbackEmail }.trim().lowercase()
        val resolvedSpec = preferredSpecialtyId ?: "plumbing"

        val synced = userRepo.syncOrRestoreProfile(
            fullName = displayName,
            email = resolvedEmail,
            city = city,
            chosenRole = preferredRole.dbValue,
            chosenSpecialtyId = resolvedSpec
        ).getOrElse { err ->
            return Result.failure(err)
        }

        val finalRole = UserRole.fromDb(synced.role)
        val finalSpecialty = if (finalRole == UserRole.CRAFTSMAN) {
            synced.specialtyId.takeIf { it.isNotBlank() } ?: resolvedSpec
        } else null

        prefs.registerFirstTimeAccount(
            fullName = synced.fullName.ifBlank { displayName },
            email = synced.email.ifBlank { resolvedEmail },
            phone = synced.phone.ifBlank { "+212 612-345678" },
            city = synced.city.ifBlank { city },
            role = finalRole,
            specialtyId = finalSpecialty,
            authProvider = authProvider,
            userIdOverride = authState.uid,
            avatarUri = synced.avatarUri,
            bio = synced.bio,
            experienceYears = synced.experienceYears,
            basePriceDh = synced.basePriceDh,
            verificationStatus = IdentityVerificationStatus.fromDb(synced.verificationStatus),
            rejectionReason = synced.rejectionReason,
            isSuspended = synced.isSuspended,
            forceOverwrite = true
        )

        val updatedSession = prefs.userSessionFlow.first()
        if (finalRole == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(updatedSession.city)
            val dirResult = supabaseMarketplaceRepo?.upsertCraftsmanDirectory(updatedSession, coord.lat, coord.lng)
            if (dirResult != null && dirResult.isFailure) {
                return Result.failure(
                    dirResult.exceptionOrNull()
                        ?: IllegalStateException("تم حفظ حسابك ولكن تعذر نشر ملفك في دليل الحرفيين")
                )
            }
        }
        return Result.success(updatedSession)
    }

    suspend fun syncCraftsmanDirectoryPresence(
        latOverride: Double? = null,
        lngOverride: Double? = null
    ): Result<Unit> {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN || !isSupabaseAuthenticated) {
            return Result.success(Unit)
        }
        val cityCoord = MoroccanLocations.coordForCity(session.city)
        val lat = latOverride ?: cityCoord.lat
        val lng = lngOverride ?: cityCoord.lng
        return supabaseMarketplaceRepo?.upsertCraftsmanDirectory(session, lat, lng)
            ?: Result.success(Unit)
    }

    suspend fun setCraftsmanOnline(isOnline: Boolean): Result<Unit> {
        prefs.setCraftsmanOnline(isOnline)
        val session = prefs.userSessionFlow.first()
        if (session.role == UserRole.CRAFTSMAN && requiresCloudSyncFor(session)) {
            val coord = MoroccanLocations.coordForCity(session.city)
            val res = supabaseMarketplaceRepo?.upsertCraftsmanDirectory(session, coord.lat, coord.lng)
                ?: Result.success(Unit)
            if (isOnline) {
                refreshMarketplaceNow()
            }
            return res
        }
        return Result.success(Unit)
    }

    suspend fun setCity(city: String) {
        prefs.setCity(city)
        val session = prefs.userSessionFlow.first()
        supabaseUserRepo?.updateProfile(
            fullName = session.fullName,
            phone = session.phone,
            city = city,
            avatarUri = session.avatarUri,
            bio = session.bio,
            experienceYears = session.experienceYears,
            basePriceDh = session.basePriceDh
        )
        if (session.role == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(city)
            supabaseMarketplaceRepo?.upsertCraftsmanDirectory(session.copy(city = city), coord.lat, coord.lng)
        }
    }

    suspend fun setSpecialty(specialtyId: String): Boolean {
        val updated = prefs.setSpecialty(specialtyId)
        if (updated) {
            val session = prefs.userSessionFlow.first()
            if (session.role == UserRole.CRAFTSMAN && requiresCloudSyncFor(session)) {
                val coord = MoroccanLocations.coordForCity(session.city)
                supabaseMarketplaceRepo?.upsertCraftsmanDirectory(
                    session = session.copy(craftsmanSpecialtyId = specialtyId, isCraftsmanSpecialtyLocked = true),
                    lat = coord.lat,
                    lng = coord.lng
                )
                refreshMarketplaceNow()
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
        val cloudAvatarUri = if (!avatarUri.isNullOrBlank() && supabaseStorageService != null && isSupabaseAuthenticated) {
            supabaseStorageService.uploadAvatar(avatarUri).getOrNull() ?: avatarUri
        } else avatarUri

        prefs.updateProfile(
            fullName = fullName,
            phone = phone,
            city = city,
            avatarUri = cloudAvatarUri,
            bio = bio,
            experienceYears = experienceYears,
            basePriceDh = basePriceDh
        )
        val session = prefs.userSessionFlow.first()
        supabaseUserRepo?.updateProfile(
            fullName = session.fullName,
            phone = session.phone,
            city = session.city,
            avatarUri = session.avatarUri,
            bio = session.bio,
            experienceYears = session.experienceYears,
            basePriceDh = session.basePriceDh
        )
        if (session.role == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(session.city)
            supabaseMarketplaceRepo?.upsertCraftsmanDirectory(session, coord.lat, coord.lng)
        }
    }

    suspend fun addPortfolioItem(
        title: String,
        description: String,
        photoUri: String?
    ): Result<PortfolioItem> {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN) {
            return Result.failure(IllegalStateException("معرض الأعمال متاح للحرفيين فقط"))
        }
        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("يرجى إدخال عنوان للعمل المنجز"))
        }
        val category = HirafiCategories.byId(session.craftsmanSpecialtyId)
        val itemId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val uploadedPhotoUri = if (!photoUri.isNullOrBlank() && supabaseStorageService != null && isSupabaseAuthenticated) {
            supabaseStorageService.uploadPortfolioPhoto(itemId, photoUri).getOrNull() ?: photoUri
        } else photoUri

        val item = PortfolioItem(
            id = itemId,
            craftsmanId = session.userId,
            title = cleanTitle.take(80),
            description = description.trim().take(350),
            categoryName = category.nameAr,
            photoUri = uploadedPhotoUri?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            createdAtMillis = now
        )
        dao.upsertPortfolioItem(item.toEntity())
        val cloudResult = supabaseMarketplaceRepo?.addPortfolioItem(item)
        if (cloudResult != null && cloudResult.isFailure) {
            return Result.failure(
                cloudResult.exceptionOrNull() ?: IllegalStateException("تعذر حفظ العمل في Supabase")
            )
        }
        return Result.success(item)
    }

    suspend fun deletePortfolioItem(itemId: String) {
        dao.deletePortfolioItem(itemId)
        supabaseMarketplaceRepo?.deletePortfolioItem(itemId)
    }

    suspend fun submitCraftsmanIdentityVerification(
        nationalIdNumber: String,
        frontPhotoUri: String,
        backPhotoUri: String,
        selfiePhotoUri: String
    ): Result<Boolean> {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN) {
            return Result.failure(IllegalStateException("التحقق من الهوية مخصص للحرفيين فقط"))
        }

        var cloudImagesUploaded = true
        val finalFront = if (supabaseStorageService != null && isSupabaseAuthenticated) {
            supabaseStorageService.uploadVerificationPhoto("front", frontPhotoUri).getOrElse {
                cloudImagesUploaded = false
                frontPhotoUri
            }
        } else frontPhotoUri

        val finalBack = if (supabaseStorageService != null && isSupabaseAuthenticated) {
            supabaseStorageService.uploadVerificationPhoto("back", backPhotoUri).getOrElse {
                cloudImagesUploaded = false
                backPhotoUri
            }
        } else backPhotoUri

        val finalSelfie = if (supabaseStorageService != null && isSupabaseAuthenticated) {
            supabaseStorageService.uploadVerificationPhoto("selfie", selfiePhotoUri).getOrElse {
                cloudImagesUploaded = false
                selfiePhotoUri
            }
        } else selfiePhotoUri

        val previewFront = if (supabaseStorageService != null && finalFront.startsWith("kyc://")) {
            supabaseStorageService.resolvePrivateKycUrl(finalFront)
        } else finalFront
        val previewBack = if (supabaseStorageService != null && finalBack.startsWith("kyc://")) {
            supabaseStorageService.resolvePrivateKycUrl(finalBack)
        } else finalBack
        val previewSelfie = if (supabaseStorageService != null && finalSelfie.startsWith("kyc://")) {
            supabaseStorageService.resolvePrivateKycUrl(finalSelfie)
        } else finalSelfie

        prefs.saveIdentityVerificationSubmission(
            nationalIdNumber = nationalIdNumber,
            frontPhotoUri = previewFront,
            backPhotoUri = previewBack,
            selfiePhotoUri = previewSelfie,
            status = IdentityVerificationStatus.PENDING_REVIEW
        )
        prefs.updateIdentityVerificationStatus(IdentityVerificationStatus.PENDING_REVIEW, "")

        val cloudResult = supabaseAdminRepo?.submitVerificationRequest(
            craftsmanName = session.fullName,
            specialtyId = session.craftsmanSpecialtyId,
            city = session.city,
            nationalIdNumber = nationalIdNumber,
            frontPhotoUri = finalFront,
            backPhotoUri = finalBack,
            selfiePhotoUri = finalSelfie
        )
        if (cloudResult != null && cloudResult.isFailure) {
            return Result.failure(
                cloudResult.exceptionOrNull() ?: IllegalStateException("تعذر إرسال طلب التوثيق إلى Supabase")
            )
        }
        return Result.success(cloudImagesUploaded)
    }

    suspend fun resetCraftsmanIdentityVerification() {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN) return
        prefs.updateIdentityVerificationStatus(IdentityVerificationStatus.NOT_SUBMITTED, "")
    }

    suspend fun setProductionMode(enabled: Boolean): Result<Unit> =
        supabaseAdminRepo?.setProductionMode(enabled)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun reviewCraftsmanVerification(
        craftsmanId: String,
        approved: Boolean,
        rejectionReason: String = ""
    ): Result<Unit> =
        supabaseAdminRepo?.reviewVerificationSubmission(craftsmanId, approved, rejectionReason)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun toggleUserSuspended(
        userId: String,
        isCraftsman: Boolean,
        suspended: Boolean
    ): Result<Unit> =
        supabaseAdminRepo?.toggleUserSuspended(userId, isCraftsman, suspended)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun cancelRequestAsAdmin(requestId: String): Result<Unit> =
        supabaseAdminRepo?.cancelRequestAsAdmin(requestId)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun addAuthorizedAdmin(email: String): Result<Unit> =
        supabaseAdminRepo?.addAuthorizedAdmin(email)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun removeAuthorizedAdmin(email: String): Result<Unit> =
        supabaseAdminRepo?.removeAuthorizedAdmin(email)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun createServiceRequest(
        categoryId: String,
        title: String,
        description: String,
        addressLabel: String,
        lat: Double,
        lng: Double,
        mapX: Float,
        mapY: Float,
        proposedPrice: Int,
        paymentMethod: PaymentMethod,
        isUrgent: Boolean,
        photoUri: String?,
        preferredCraftsmanId: String? = null,
        radiusKm: Int = 5,
        productionMode: Boolean = false
    ): Result<ServiceRequest> {
        val session = prefs.userSessionFlow.first()
        val requiresCloudSync = supabaseMarketplaceRepo != null &&
            (isSupabaseAuthenticated || session.authProvider == "supabase" || session.authProvider == "google")

        if (requiresCloudSync && supabaseSessionManager?.currentUidOrNull == null) {
            supabaseSessionManager?.restoreSavedSession()
        }
        val activeUid = supabaseSessionManager?.currentUidOrNull
        if (requiresCloudSync && activeUid == null) {
            return Result.failure(IllegalStateException("انتهت جلسة الاتصال بـ Supabase، يرجى تسجيل الدخول من جديد"))
        }

        val category = HirafiCategories.byId(categoryId)
        val now = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()
        val uploadedPhotoUri = if (!photoUri.isNullOrBlank() && supabaseStorageService != null && isSupabaseAuthenticated) {
            supabaseStorageService.uploadRequestPhoto(requestId, photoUri).getOrNull() ?: photoUri
        } else photoUri
        val resolvedTitle = title.trim().let { if (it.length < 2) "طلب ${category.nameAr}" else it }
        val effectiveClientId = activeUid ?: session.userId
        val entity = ServiceRequestEntity(
            id = requestId,
            clientId = effectiveClientId,
            clientName = session.fullName,
            categoryId = category.id,
            categoryName = category.nameAr,
            title = resolvedTitle,
            description = description.trim(),
            addressLabel = addressLabel.trim().ifBlank { session.city },
            lat = lat,
            lng = lng,
            mapX = mapX,
            mapY = mapY,
            proposedPrice = proposedPrice,
            acceptedPrice = null,
            paymentMethod = paymentMethod.dbValue,
            isUrgent = isUrgent,
            status = RequestStatus.SEARCHING.dbValue,
            assignedCraftsmanId = null,
            assignedCraftsmanName = null,
            assignedCraftsmanPhone = null,
            assignedCraftsmanRating = null,
            photoUri = uploadedPhotoUri,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val domain = entity.toDomain()
        dao.upsertRequest(entity)

        if (requiresCloudSync) {
            var cloudResult = supabaseMarketplaceRepo!!.createServiceRequest(domain)
            if (cloudResult.isFailure && supabaseUserRepo != null) {
                // Auto-heal missing public.profiles row (FK 23503 / 409) before retrying once
                val healResult = supabaseUserRepo.syncOrRestoreProfile(
                    fullName = session.fullName,
                    email = session.email,
                    city = session.city,
                    chosenRole = session.role.dbValue,
                    chosenSpecialtyId = session.craftsmanSpecialtyId
                )
                if (healResult.isSuccess) {
                    cloudResult = supabaseMarketplaceRepo.createServiceRequest(domain)
                }
            }
            if (cloudResult.isFailure) {
                dao.deleteRequestById(requestId)
                val err = cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر إرسال الطلب إلى Supabase")
                return Result.failure(err)
            }
        }

        val isSeededSampleTarget = preferredCraftsmanId != null && preferredCraftsmanId.startsWith("c_")
        if (!productionMode && (!isSupabaseAuthenticated || isSeededSampleTarget)) {
            simulateLiveIncomingOffers(
                requestId = requestId,
                categoryId = categoryId,
                lat = lat,
                lng = lng,
                proposedPrice = proposedPrice,
                preferredCraftsmanId = preferredCraftsmanId,
                radiusKm = radiusKm
            )
        }
        return Result.success(domain)
    }

    private fun requiresCloudSyncFor(session: UserSession): Boolean =
        supabaseMarketplaceRepo != null &&
            (isSupabaseAuthenticated || session.authProvider == "supabase" || session.authProvider == "google")

    private suspend fun ensureActiveCloudUid(session: UserSession): String {
        if (supabaseSessionManager?.currentUidOrNull == null) {
            supabaseSessionManager?.restoreSavedSession()
        }
        return supabaseSessionManager?.currentUidOrNull
            ?: throw IllegalStateException("انتهت جلسة الاتصال بـ Supabase، يرجى تسجيل الدخول من جديد")
    }

    private suspend fun executeCloudMutationWithProfileHeal(
        session: UserSession,
        block: suspend () -> Result<Unit>
    ): Result<Unit> {
        var result = block()
        if (result.isFailure && supabaseUserRepo != null) {
            val healResult = supabaseUserRepo.syncOrRestoreProfile(
                fullName = session.fullName,
                email = session.email,
                city = session.city,
                chosenRole = session.role.dbValue,
                chosenSpecialtyId = session.craftsmanSpecialtyId
            )
            if (healResult.isSuccess) {
                result = block()
            }
        }
        return result
    }

    suspend fun raiseRequestPrice(requestId: String, newPrice: Int) {
        val existing = dao.getRequestById(requestId) ?: return
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        if (requiresCloud) {
            ensureActiveCloudUid(session)
        }
        val updated = existing.copy(
            proposedPrice = newPrice,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertRequest(updated)
        if (requiresCloud) {
            val cloudResult = supabaseMarketplaceRepo!!.updateProposedPrice(requestId, newPrice)
            if (cloudResult.isFailure) {
                dao.upsertRequest(existing)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر تحديث السعر في Supabase")
            }
        }
        if (!isSupabaseAuthenticated && updated.status == RequestStatus.SEARCHING.dbValue) {
            simulateQuickPriceBoostOffer(requestId, existing.categoryId, existing.lat, existing.lng, newPrice)
        }
    }

    suspend fun expandSearchRadius(requestId: String, newRadiusKm: Int) {
        val existing = dao.getRequestById(requestId) ?: return
        if (existing.status != RequestStatus.SEARCHING.dbValue) return
        if (isSupabaseAuthenticated) return
        val existingOffers = dao.observeOffersForRequest(requestId).first()
        val alreadyOfferedIds = existingOffers.map { it.craftsmanId }.toSet()
        val candidates = getCraftsmenAround(existing.lat, existing.lng)
            .filter { it.id !in alreadyOfferedIds && it.distanceKm <= newRadiusKm.toDouble() }
            .sortedWith(compareByDescending<CraftsmanPin> { it.specialtyId == existing.categoryId }.thenBy { it.distanceKm })
            .take(2)

        repoScope.launch {
            candidates.forEachIndexed { idx, craftsman ->
                delay(if (idx == 0) 800L else 1900L)
                val currentReq = dao.getRequestById(requestId) ?: return@launch
                if (currentReq.status != RequestStatus.SEARCHING.dbValue) return@launch
                val now = System.currentTimeMillis()
                val offerPrice = if (idx == 0) currentReq.proposedPrice else currentReq.proposedPrice + 15
                dao.upsertOffer(
                    ServiceOfferEntity(
                        id = UUID.randomUUID().toString(),
                        requestId = requestId,
                        craftsmanId = craftsman.id,
                        craftsmanName = craftsman.name,
                        craftsmanPhone = craftsman.phone,
                        craftsmanRating = craftsman.rating,
                        completedJobs = craftsman.completedJobs,
                        specialty = craftsman.specialtyName,
                        offeredPrice = offerPrice,
                        etaMinutes = craftsman.etaMinutes,
                        distanceKm = craftsman.distanceKm,
                        note = "استلمت طلبك بعد توسيع نطاق البحث إلى $newRadiusKm كم",
                        status = OfferStatus.PENDING.dbValue,
                        createdAtMillis = now,
                        expiresAtMillis = now + 50_000L
                    )
                )
            }
        }
    }

    private fun simulateLiveIncomingOffers(
        requestId: String,
        categoryId: String,
        lat: Double,
        lng: Double,
        proposedPrice: Int,
        preferredCraftsmanId: String?,
        radiusKm: Int
    ) {
        repoScope.launch {
            val allAround = getCraftsmenAround(lat, lng)
            val preferred = preferredCraftsmanId?.let { id -> allAround.firstOrNull { it.id == id } }
            val matching = allAround.filter { it.specialtyId == categoryId && it.id != preferredCraftsmanId }
            val withinRadius = matching.filter { it.distanceKm <= radiusKm.toDouble() }
            val others = allAround.filter { it.specialtyId != categoryId && it.id != preferredCraftsmanId }
            val pool = (listOfNotNull(preferred) + withinRadius + matching + others)
                .distinctBy { it.id }
                .take(3)

            val delays = if (preferred != null) listOf(550L, 2100L, 3600L) else listOf(1000L, 2300L, 3900L)
            pool.forEachIndexed { index, craftsman ->
                delay(delays.getOrElse(index) { 2000L })
                val currentReq = dao.getRequestById(requestId) ?: return@launch
                if (currentReq.status != RequestStatus.SEARCHING.dbValue) return@launch

                val offerPrice = when {
                    preferred != null && index == 0 -> proposedPrice
                    index == 0 -> proposedPrice + 20
                    index == 1 -> proposedPrice
                    else -> (proposedPrice - 10).coerceAtLeast(60)
                }
                val note = when {
                    preferred != null && index == 0 -> "تلقيت طلبك المباشر ومستعد للانطلاق فوراً!"
                    index == 0 -> "متوفر الآن مع كامل المعدات الأصلية (${craftsman.experienceYears} سنوات خبرة)"
                    index == 1 -> "سأصل خلال ${craftsman.etaMinutes} دقائق وبنفس سعرك"
                    else -> "عرض سريع على بُعد ${craftsman.distanceKm} كم منك"
                }
                val now = System.currentTimeMillis()
                val offer = ServiceOfferEntity(
                    id = UUID.randomUUID().toString(),
                    requestId = requestId,
                    craftsmanId = craftsman.id,
                    craftsmanName = craftsman.name,
                    craftsmanPhone = craftsman.phone,
                    craftsmanRating = craftsman.rating,
                    completedJobs = craftsman.completedJobs,
                    specialty = craftsman.specialtyName,
                    offeredPrice = offerPrice,
                    etaMinutes = craftsman.etaMinutes,
                    distanceKm = craftsman.distanceKm,
                    note = note,
                    status = OfferStatus.PENDING.dbValue,
                    createdAtMillis = now,
                    expiresAtMillis = now + 55_000L
                )
                dao.upsertOffer(offer)
            }
        }
    }

    private fun simulateQuickPriceBoostOffer(
        requestId: String,
        categoryId: String,
        lat: Double,
        lng: Double,
        newPrice: Int
    ) {
        repoScope.launch {
            delay(950L)
            val currentReq = dao.getRequestById(requestId) ?: return@launch
            if (currentReq.status != RequestStatus.SEARCHING.dbValue) return@launch
            val around = getCraftsmenAround(lat, lng)
            val craftsman = around.firstOrNull { it.specialtyId == categoryId } ?: around.first()
            val now = System.currentTimeMillis()
            val offer = ServiceOfferEntity(
                id = UUID.randomUUID().toString(),
                requestId = requestId,
                craftsmanId = "${craftsman.id}_boost_${now % 1000}",
                craftsmanName = craftsman.name,
                craftsmanPhone = craftsman.phone,
                craftsmanRating = craftsman.rating,
                completedJobs = craftsman.completedJobs,
                specialty = craftsman.specialtyName,
                offeredPrice = newPrice,
                etaMinutes = (craftsman.etaMinutes - 2).coerceAtLeast(3),
                distanceKm = craftsman.distanceKm,
                note = "موافق على السعر الجديد ($newPrice درهم) وجاهز للانطلاق",
                status = OfferStatus.PENDING.dbValue,
                createdAtMillis = now,
                expiresAtMillis = now + 50_000L
            )
            dao.upsertOffer(offer)
        }
    }

    suspend fun submitCraftsmanBid(
        requestId: String,
        offeredPrice: Int,
        etaMinutes: Int,
        note: String
    ) {
        val session = prefs.userSessionFlow.first()
        require(session.isCraftsmanSpecialtyLocked) { "يجب تأكيد مهنتك الوحيدة أولاً" }
        val req = dao.getRequestById(requestId)
            ?: throw IllegalStateException("لم يعد هذا الطلب متاحاً")
        require(req.categoryId == session.craftsmanSpecialtyId) { "لا يمكنك تقديم عرض لطلب خارج مهنتك" }
        val requiresCloud = requiresCloudSyncFor(session)
        val activeUid = if (requiresCloud) ensureActiveCloudUid(session) else session.userId

        val specialty = HirafiCategories.byId(session.craftsmanSpecialtyId)
        val now = System.currentTimeMillis()
        val offer = ServiceOfferEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            craftsmanId = activeUid,
            craftsmanName = session.fullName,
            craftsmanPhone = session.phone,
            craftsmanRating = 4.9,
            completedJobs = 64,
            specialty = specialty.nameAr,
            offeredPrice = offeredPrice,
            etaMinutes = etaMinutes,
            distanceKm = 1.2,
            note = note.ifBlank { "جاهز للوصول خلال $etaMinutes دقائق" },
            status = OfferStatus.PENDING.dbValue,
            createdAtMillis = now,
            expiresAtMillis = now + 60_000L
        )
        dao.upsertOffer(offer)
        if (requiresCloud) {
            val cloudResult = executeCloudMutationWithProfileHeal(session) {
                supabaseMarketplaceRepo!!.submitOffer(offer.toDomain(), req.clientId)
            }
            if (cloudResult.isFailure) {
                dao.deleteOfferById(offer.id)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر إرسال العرض إلى Supabase")
            }
        }
    }

    suspend fun craftsmanDirectAcceptRequest(requestId: String) {
        val req = dao.getRequestById(requestId)
            ?: throw IllegalStateException("لم يعد هذا الطلب متاحاً")
        val session = prefs.userSessionFlow.first()
        require(session.isCraftsmanSpecialtyLocked) { "يجب تأكيد مهنتك الوحيدة أولاً" }
        require(req.categoryId == session.craftsmanSpecialtyId) { "لا يمكنك قبول طلب خارج مهنتك" }
        val requiresCloud = requiresCloudSyncFor(session)
        val activeUid = if (requiresCloud) ensureActiveCloudUid(session) else session.userId

        val now = System.currentTimeMillis()
        val updated = req.copy(
            status = RequestStatus.ACCEPTED.dbValue,
            acceptedPrice = req.proposedPrice,
            assignedCraftsmanId = activeUid,
            assignedCraftsmanName = session.fullName,
            assignedCraftsmanPhone = session.phone,
            assignedCraftsmanRating = 4.9,
            updatedAtMillis = now
        )
        dao.upsertRequest(updated)
        if (requiresCloud) {
            val cloudResult = executeCloudMutationWithProfileHeal(session) {
                supabaseMarketplaceRepo!!.directAcceptRequestAsCraftsman(
                    requestId = requestId,
                    acceptedPrice = req.proposedPrice,
                    session = session.copy(userId = activeUid)
                )
            }
            if (cloudResult.isFailure) {
                dao.upsertRequest(req)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر قبول الطلب في Supabase")
            }
        }
        runCatching {
            insertSystemWelcomeMessage(
                requestId = requestId,
                senderId = activeUid,
                senderName = session.fullName,
                role = UserRole.CRAFTSMAN,
                text = "تم قبول طلبك بسعر ${req.proposedPrice} درهم. أنا متوجه إليك الآن!"
            )
        }
    }

    suspend fun acceptOffer(offer: ServiceOffer) {
        val req = dao.getRequestById(offer.requestId)
            ?: throw IllegalStateException("لم يعد الطلب موجوداً")
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        if (requiresCloud) {
            ensureActiveCloudUid(session)
        }

        val previousOffer = dao.getOfferById(offer.id)
        val now = System.currentTimeMillis()
        val updatedOffer = ServiceOfferEntity(
            id = offer.id,
            requestId = offer.requestId,
            craftsmanId = offer.craftsmanId,
            craftsmanName = offer.craftsmanName,
            craftsmanPhone = offer.craftsmanPhone,
            craftsmanRating = offer.craftsmanRating,
            completedJobs = offer.completedJobs,
            specialty = offer.specialty,
            offeredPrice = offer.offeredPrice,
            etaMinutes = offer.etaMinutes,
            distanceKm = offer.distanceKm,
            note = offer.note,
            status = OfferStatus.ACCEPTED.dbValue,
            createdAtMillis = offer.createdAtMillis,
            expiresAtMillis = offer.expiresAtMillis
        )
        val updatedReq = req.copy(
            status = RequestStatus.ACCEPTED.dbValue,
            acceptedPrice = offer.offeredPrice,
            assignedCraftsmanId = offer.craftsmanId,
            assignedCraftsmanName = offer.craftsmanName,
            assignedCraftsmanPhone = offer.craftsmanPhone,
            assignedCraftsmanRating = offer.craftsmanRating,
            updatedAtMillis = now
        )
        dao.upsertOffer(updatedOffer)
        dao.declineOtherOffers(offer.requestId, offer.id)
        dao.upsertRequest(updatedReq)

        if (requiresCloud) {
            val cloudResult = supabaseMarketplaceRepo!!.acceptOffer(offer)
            if (cloudResult.isFailure) {
                if (previousOffer != null) dao.upsertOffer(previousOffer)
                dao.upsertRequest(req)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر قبول العرض في Supabase")
            }
        }

        runCatching {
            insertSystemWelcomeMessage(
                requestId = req.id,
                senderId = req.clientId,
                senderName = req.clientName,
                role = UserRole.CLIENT,
                text = "تم قبول عرض ${offer.craftsmanName} بسعر ${offer.offeredPrice} درهم، بانتظار الوصول خلال ${offer.etaMinutes} دقائق."
            )
        }
    }

    suspend fun declineOffer(offerId: String) {
        val offer = dao.getOfferById(offerId) ?: return
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        if (requiresCloud) {
            ensureActiveCloudUid(session)
        }
        dao.upsertOffer(offer.copy(status = OfferStatus.DECLINED.dbValue))
        if (requiresCloud) {
            val cloudResult = supabaseMarketplaceRepo!!.updateOfferStatusOrCounter(
                offerId = offerId,
                status = OfferStatus.DECLINED,
                offeredPrice = offer.offeredPrice,
                note = offer.note,
                expiresAtMillis = offer.expiresAtMillis
            )
            if (cloudResult.isFailure) {
                dao.upsertOffer(offer)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر رفض العرض في Supabase")
            }
        }
    }

    suspend fun counterOffer(offer: ServiceOffer, counterPrice: Int) {
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        if (requiresCloud) {
            ensureActiveCloudUid(session)
        }
        val previousOffer = dao.getOfferById(offer.id)
        val newExpires = System.currentTimeMillis() + 45_000L
        val noteText = "عرض مضاد من الزبون: $counterPrice درهم"
        val updated = ServiceOfferEntity(
            id = offer.id,
            requestId = offer.requestId,
            craftsmanId = offer.craftsmanId,
            craftsmanName = offer.craftsmanName,
            craftsmanPhone = offer.craftsmanPhone,
            craftsmanRating = offer.craftsmanRating,
            completedJobs = offer.completedJobs,
            specialty = offer.specialty,
            offeredPrice = counterPrice,
            etaMinutes = offer.etaMinutes,
            distanceKm = offer.distanceKm,
            note = noteText,
            status = OfferStatus.COUNTERED.dbValue,
            createdAtMillis = offer.createdAtMillis,
            expiresAtMillis = newExpires
        )
        dao.upsertOffer(updated)
        if (requiresCloud) {
            val cloudResult = supabaseMarketplaceRepo!!.updateOfferStatusOrCounter(
                offerId = offer.id,
                status = OfferStatus.COUNTERED,
                offeredPrice = counterPrice,
                note = noteText,
                expiresAtMillis = newExpires
            )
            if (cloudResult.isFailure) {
                if (previousOffer != null) dao.upsertOffer(previousOffer)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر إرسال العرض المضاد إلى Supabase")
            }
        }
        if (!isSupabaseAuthenticated || offer.craftsmanId.startsWith("c_")) {
            repoScope.launch {
                delay(1400L)
                val latest = dao.getOfferById(offer.id) ?: return@launch
                if (latest.status == OfferStatus.COUNTERED.dbValue) {
                    val agreed = (offer.offeredPrice + counterPrice) / 2
                    val finalPrice = if (counterPrice >= offer.offeredPrice - 20) counterPrice else agreed
                    dao.upsertOffer(
                        latest.copy(
                            offeredPrice = finalPrice,
                            note = "تمت الموافقة على التفاوض: $finalPrice درهم",
                            status = OfferStatus.PENDING.dbValue,
                            expiresAtMillis = System.currentTimeMillis() + 45_000L
                        )
                    )
                }
            }
        }
    }

    suspend fun advanceJobStatus(requestId: String, nextStatus: RequestStatus) {
        val req = dao.getRequestById(requestId) ?: return
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        if (requiresCloud) {
            ensureActiveCloudUid(session)
        }
        val updated = req.copy(status = nextStatus.dbValue, updatedAtMillis = System.currentTimeMillis())
        dao.upsertRequest(updated)
        if (requiresCloud) {
            val cloudResult = supabaseMarketplaceRepo!!.advanceRequestStatus(requestId, nextStatus)
            if (cloudResult.isFailure) {
                dao.upsertRequest(req)
                throw cloudResult.exceptionOrNull()
                    ?: IllegalStateException("تعذر تحديث حالة الطلب في Supabase")
            }
        }
    }

    suspend fun cancelRequest(requestId: String) {
        val session = prefs.userSessionFlow.first()
        advanceJobStatus(requestId, RequestStatus.CANCELLED)
        val actorLabel = session.fullName.trim().ifBlank {
            if (session.role == UserRole.CRAFTSMAN) "الحرفي" else "الزبون"
        }
        val notice = if (session.role == UserRole.CRAFTSMAN) {
            "تم إلغاء الخدمة من طرف الحرفي ($actorLabel)"
        } else {
            "تم إلغاء الخدمة من طرف الزبون ($actorLabel)"
        }
        runCatching {
            val senderId = if (requiresCloudSyncFor(session)) ensureActiveCloudUid(session) else session.userId
            insertSystemWelcomeMessage(
                requestId = requestId,
                senderId = senderId,
                senderName = actorLabel,
                role = session.role,
                text = notice
            )
        }
        supabaseMarketplaceRepo?.triggerRefresh()
    }

    suspend fun sendChatMessage(requestId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        val activeUid = if (requiresCloud) ensureActiveCloudUid(session) else session.userId
        insertSystemWelcomeMessage(
            requestId = requestId,
            senderId = activeUid,
            senderName = session.fullName,
            role = session.role,
            text = trimmed
        )
    }

    private suspend fun insertSystemWelcomeMessage(
        requestId: String,
        senderId: String,
        senderName: String,
        role: UserRole,
        text: String
    ) {
        val msg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            senderId = senderId,
            senderName = senderName,
            senderRole = role.dbValue,
            text = text,
            createdAtMillis = System.currentTimeMillis()
        )
        dao.insertMessage(msg)
        val session = prefs.userSessionFlow.first()
        if (requiresCloudSyncFor(session)) {
            val cloudRes = supabaseMarketplaceRepo!!.sendChatMessage(msg.toDomain())
            if (cloudRes.isFailure) {
                throw cloudRes.exceptionOrNull()
                    ?: IllegalStateException("تعذر إرسال الرسالة إلى Supabase")
            }
        }
    }

    suspend fun submitReview(
        requestId: String,
        craftsmanId: String,
        rating: Int,
        tags: List<String>,
        comment: String
    ) {
        val session = prefs.userSessionFlow.first()
        val requiresCloud = requiresCloudSyncFor(session)
        if (requiresCloud) {
            ensureActiveCloudUid(session)
        }
        val review = ReviewEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            craftsmanId = craftsmanId,
            rating = rating,
            tagsJoined = tags.joinToString(","),
            comment = comment,
            createdAtMillis = System.currentTimeMillis()
        )
        dao.insertReview(review)
        if (requiresCloud) {
            val cloudRes = supabaseMarketplaceRepo!!.submitReview(review.toDomain())
            if (cloudRes.isFailure) {
                throw cloudRes.exceptionOrNull()
                    ?: IllegalStateException("تعذر حفظ التقييم في Supabase")
            }
        }
    }

    suspend fun publishCraftsmanLiveLocation(lat: Double, lng: Double, activeRequestId: String? = null) {
        val session = prefs.userSessionFlow.first()
        if (session.role == UserRole.CRAFTSMAN && isSupabaseAuthenticated) {
            supabaseMarketplaceRepo?.upsertCraftsmanDirectory(session, lat, lng)
            if (!activeRequestId.isNullOrBlank()) {
                supabaseMarketplaceRepo?.updateCraftsmanLiveLocationOnRequest(activeRequestId, lat, lng)
            }
            return
        }
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val bearer = supabaseSessionManager?.getValidBearerToken() ?: return
        if (session.userId.length != 36) return
        runCatching {
            api.patchProfileLocation(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.${session.userId}",
                patch = SupabaseProfileLocationPatchDto(lat = lat, lng = lng)
            )
        }.onFailure { Log.w(TAG, "Live location publish failed", it) }
    }

    suspend fun fetchAssignedCraftsmanLocation(craftsmanId: String): Pair<Double, Double>? {
        if (craftsmanId.length != 36) return null
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return null
        val bearer = supabaseSessionManager?.getValidBearerToken() ?: return null
        return runCatching {
            val response = api.getCraftsmanDiscoveryPins(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$craftsmanId",
                onlineFilter = null
            )
            if (response.isSuccessful) {
                response.body()?.firstOrNull()?.let { dto ->
                    if (dto.lat != 0.0 || dto.lng != 0.0) dto.lat to dto.lng else null
                }
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun syncOffersForRequestFromSupabase(requestId: String): Result<Int> {
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(0)
        val bearer = supabaseSessionManager?.getValidBearerToken() ?: return Result.success(0)
        val session = prefs.userSessionFlow.first()
        return runCatching {
            val response = api.getOffersForRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                requestIdFilter = "eq.$requestId",
                craftsmanIdFilter = if (session.role == UserRole.CRAFTSMAN) "eq.${session.userId}" else null
            )
            if (!response.isSuccessful) {
                error("تعذر جلب العروض (${response.code()})")
            }
            val now = System.currentTimeMillis()
            val mapped = response.body().orEmpty().map { dto ->
                val createdMs = SupabaseTimestampParser.parseIsoToMillis(dto.createdAt)
                val resolvedExpiry = if (dto.expiresAtMillis > 0L) dto.expiresAtMillis else (createdMs + 60_000L)
                ServiceOfferEntity(
                    id = dto.id,
                    requestId = dto.requestId,
                    craftsmanId = dto.craftsmanId ?: "remote_craftsman",
                    craftsmanName = dto.craftsmanName,
                    craftsmanPhone = dto.craftsmanPhone,
                    craftsmanRating = dto.craftsmanRating,
                    completedJobs = dto.completedJobs,
                    specialty = dto.specialty,
                    offeredPrice = dto.offeredPrice,
                    etaMinutes = dto.etaMinutes,
                    distanceKm = dto.distanceKm,
                    note = dto.note,
                    status = dto.status,
                    createdAtMillis = createdMs.takeIf { it > 0L } ?: now,
                    expiresAtMillis = resolvedExpiry
                )
            }
            if (mapped.isNotEmpty()) {
                dao.upsertOffers(mapped)
            }
            mapped.size
        }
    }

    suspend fun logoutAccount() {
        supabaseSessionManager?.signOut()
        prefs.logoutAccount()
        dao.clearMarketplaceCache()
    }

    private fun ServiceRequest.toEntity() = ServiceRequestEntity(
        id = id,
        clientId = clientId,
        clientName = clientName,
        categoryId = categoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        addressLabel = addressLabel,
        lat = lat,
        lng = lng,
        mapX = mapX,
        mapY = mapY,
        proposedPrice = proposedPrice,
        acceptedPrice = acceptedPrice,
        paymentMethod = paymentMethod.dbValue,
        isUrgent = isUrgent,
        status = status.dbValue,
        assignedCraftsmanId = assignedCraftsmanId,
        assignedCraftsmanName = assignedCraftsmanName,
        assignedCraftsmanPhone = assignedCraftsmanPhone,
        assignedCraftsmanRating = assignedCraftsmanRating,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    private fun ServiceOffer.toEntity() = ServiceOfferEntity(
        id = id,
        requestId = requestId,
        craftsmanId = craftsmanId,
        craftsmanName = craftsmanName,
        craftsmanPhone = craftsmanPhone,
        craftsmanRating = craftsmanRating,
        completedJobs = completedJobs,
        specialty = specialty,
        offeredPrice = offeredPrice,
        etaMinutes = etaMinutes,
        distanceKm = distanceKm,
        note = note,
        status = status.dbValue,
        createdAtMillis = createdAtMillis,
        expiresAtMillis = expiresAtMillis
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id,
        requestId = requestId,
        senderId = senderId,
        senderName = senderName,
        senderRole = senderRole.dbValue,
        text = text,
        createdAtMillis = createdAtMillis
    )

    private fun PortfolioItem.toEntity() = PortfolioItemEntity(
        id = id,
        craftsmanId = craftsmanId,
        title = title,
        description = description,
        categoryName = categoryName,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis
    )

    private fun PortfolioItemEntity.toDomain() = PortfolioItem(
        id = id,
        craftsmanId = craftsmanId,
        title = title,
        description = description,
        categoryName = categoryName,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis
    )

    private fun ServiceRequestEntity.toDomain() = ServiceRequest(
        id = id,
        clientId = clientId,
        clientName = clientName,
        categoryId = categoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        addressLabel = addressLabel,
        lat = lat,
        lng = lng,
        mapX = mapX,
        mapY = mapY,
        proposedPrice = proposedPrice,
        acceptedPrice = acceptedPrice,
        paymentMethod = PaymentMethod.entries.firstOrNull { it.dbValue == paymentMethod } ?: PaymentMethod.CASH,
        isUrgent = isUrgent,
        status = RequestStatus.fromDb(status),
        assignedCraftsmanId = assignedCraftsmanId,
        assignedCraftsmanName = assignedCraftsmanName,
        assignedCraftsmanPhone = assignedCraftsmanPhone,
        assignedCraftsmanRating = assignedCraftsmanRating,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    private fun ServiceOfferEntity.toDomain() = ServiceOffer(
        id = id,
        requestId = requestId,
        craftsmanId = craftsmanId,
        craftsmanName = craftsmanName,
        craftsmanPhone = craftsmanPhone,
        craftsmanRating = craftsmanRating,
        completedJobs = completedJobs,
        specialty = specialty,
        offeredPrice = offeredPrice,
        etaMinutes = etaMinutes,
        distanceKm = ((distanceKm * 10.0).roundToInt() / 10.0),
        note = note,
        status = OfferStatus.fromDb(status),
        createdAtMillis = createdAtMillis,
        expiresAtMillis = expiresAtMillis
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        requestId = requestId,
        senderId = senderId,
        senderName = senderName,
        senderRole = if (senderRole == UserRole.CRAFTSMAN.dbValue) UserRole.CRAFTSMAN else UserRole.CLIENT,
        text = text,
        createdAtMillis = createdAtMillis
    )

    private fun ReviewEntity.toDomain() = ReviewRecord(
        id = id,
        requestId = requestId,
        craftsmanId = craftsmanId,
        rating = rating,
        tags = tagsJoined.split(",").filter { it.isNotBlank() },
        comment = comment,
        createdAtMillis = createdAtMillis
    )

    companion object {
        private const val TAG = "HirafiRepository"

        fun computeCraftsmenAround(
            centerLat: Double = 33.5822,
            centerLng: Double = -7.6324
        ): List<CraftsmanPin> {
            data class Seed(
                val id: String,
                val name: String,
                val specialtyId: String,
                val rating: Double,
                val jobs: Int,
                val dLat: Double,
                val dLng: Double,
                val basePrice: Int,
                val phone: String,
                val years: Int,
                val bio: String
            )

            val seeds = listOf(
                Seed("c_karim", "كريم بنعلي", "plumbing", 4.9, 184, 0.0065, -0.0052, 120, "+212 661-234510", 8, "متخصص في كشف وإصلاح التسربات والترصيص الصحي المنزلي"),
                Seed("c_hamza", "حمزة السليماني", "plumbing", 4.7, 96, -0.0115, 0.0125, 110, "+212 664-112233", 5, "تركيب سخانات المياه، الصنابير، وإصلاح الأعطال المستعجلة"),
                Seed("c_omar", "عمر التازي", "electricity", 4.8, 142, 0.0042, 0.0110, 140, "+212 662-887120", 7, "صيانة اللوحات الكهربائية، القواطع، وتمديد الأسلاك بأمان"),
                Seed("c_amine", "أمين بنجلون", "electricity", 4.92, 203, -0.0140, -0.0130, 150, "+212 668-550190", 9, "تشخيص الأعطال الكهربائية المنزلية وتركيب الإضاءة الذكية"),
                Seed("c_rachid", "رشيد الفاسي", "ac", 4.9, 215, -0.0088, -0.0045, 180, "+212 663-449012", 10, "تعبئة غاز المكيفات، تنظيف الفلاتر، وإصلاح التبريد"),
                Seed("c_badr", "بدر الشرقاوي", "ac", 4.78, 118, 0.0165, 0.0145, 190, "+212 669-221478", 6, "صيانة المكيفات المنفصلة والمركزية بضمان على الخدمة"),
                Seed("c_youssef", "يوسف الإدريسي", "carpentry", 4.85, 129, 0.0028, -0.0115, 150, "+212 665-998877", 8, "إصلاح الأبواب والنوافذ الخشبية وخزائن المطبخ"),
                Seed("c_hicham", "هشام الزروالي", "carpentry", 4.72, 74, 0.0190, -0.0080, 140, "+212 670-334890", 5, "نجارة الخشب والألمنيوم وتركيب الأثاث المنزلي"),
                Seed("c_salim", "سليم أمين", "painting", 4.75, 88, -0.0105, 0.0048, 220, "+212 667-334455", 7, "صباغة الجدران الداخلية والديكور العصري بدون بقع"),
                Seed("c_fouad", "فؤاد المرابط", "painting", 4.88, 162, 0.0210, 0.0095, 240, "+212 671-889044", 11, "معالجة الرطوبة والصباغة المائية والزيتية بسرعة وإتقان"),
                Seed("c_mourad", "مراد الشرقاوي", "locksmith", 4.95, 310, 0.0045, 0.0032, 100, "+212 666-771122", 12, "فتح الأقفال المغلقة بدون إتلاف وتبديل الأسطوانات فوراً"),
                Seed("c_zakaria", "زكرياء الحلو", "locksmith", 4.81, 136, -0.0155, 0.0160, 95, "+212 672-665544", 6, "نسخ المفاتيح المشفرة وإصلاح أقفال الأبواب المصفحة"),
                Seed("c_hassan", "حسن الودغيري", "masonry", 4.84, 151, 0.0095, -0.0135, 210, "+212 673-443322", 14, "ترميم الجدران، تركيب الزليج والرخام وإصلاح التسربات"),
                Seed("c_driss", "إدريس الناصري", "masonry", 4.76, 92, -0.0220, -0.0070, 200, "+212 674-119988", 9, "أشغال البناء الصغيرة، الجبس، وتجديد الحمامات والمطابخ"),
                Seed("c_nadia_pro", "خديجة السفياني", "cleaning", 4.93, 245, -0.0048, 0.0075, 120, "+212 675-778899", 7, "تنظيف عميق للمنازل والمفروشات بمواد آمنة ومعقمة"),
                Seed("c_samira", "سميرة العلمي", "cleaning", 4.82, 114, 0.0145, -0.0165, 110, "+212 676-223344", 5, "تنظيف الشقق بعد الإصلاحات والزجاج والمطابخ")
            )

            return seeds.map { s ->
                val pLat = centerLat + s.dLat
                val pLng = centerLng + s.dLng
                val dist = MapRoutingService.haversineKm(centerLat, centerLng, pLat, pLng).coerceAtLeast(0.3)
                val eta = (dist * 4.5).roundToInt().coerceIn(4, 35)
                CraftsmanPin(
                    id = s.id,
                    name = s.name,
                    specialtyId = s.specialtyId,
                    specialtyName = HirafiCategories.byId(s.specialtyId).nameAr,
                    rating = s.rating,
                    completedJobs = s.jobs,
                    lat = pLat,
                    lng = pLng,
                    basePriceDh = s.basePrice,
                    distanceKm = dist,
                    etaMinutes = eta,
                    phone = s.phone,
                    isVerified = true,
                    experienceYears = s.years,
                    bio = s.bio,
                    isLiveCloud = false
                )
            }.sortedBy { it.distanceKm }
        }
    }
}
