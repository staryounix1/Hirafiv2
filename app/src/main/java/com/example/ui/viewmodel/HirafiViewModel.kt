package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.location.LiveLocationBus
import com.example.data.location.LiveTrackingState
import com.example.data.location.RealTimeLocationService
import com.example.data.location.RealTimeLocationSnapshot
import com.example.data.model.AccountRolePolicy
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppRuntimeConfig
import com.example.data.model.AuthorizedAdminRecord
import com.example.data.model.ChatMessage
import com.example.data.model.CraftsmanPin
import com.example.data.model.CraftsmanSortOption
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.IdentityVerificationValidator
import com.example.data.model.LiveRouteState
import com.example.data.model.LoginValidator
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PhoneValidator
import com.example.data.model.PortfolioItem
import com.example.data.model.RequestStatus
import com.example.data.model.ReviewRecord
import com.example.data.model.SearchPlaceResult
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.model.VerificationSubmission
import com.example.data.remote.GoogleIdTokenFallbackToBrowserException
import com.example.data.remote.MapRoutingService
import com.example.data.remote.SupabaseAuthState
import com.example.data.remote.SupabaseConfig
import com.example.data.repository.HirafiRepository
import com.example.notifications.HirafiNotificationHelper
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.HirafiI18n
import com.example.ui.i18n.localizedLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DraftRequestState(
    val selectedCategoryId: String = "plumbing",
    val title: String = "",
    val description: String = "",
    val proposedPrice: Int = HirafiCategories.byId("plumbing").defaultPriceDh,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isUrgent: Boolean = true,
    val photoUri: String? = null,
    val addressLabel: String = MoroccanLocations.neighborhoods.first().nameAr,
    val lat: Double = MoroccanLocations.neighborhoods.first().lat,
    val lng: Double = MoroccanLocations.neighborhoods.first().lng,
    val isGpsActive: Boolean = false,
    val isResolvingAddress: Boolean = false
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

private data class CraftsmanFilterParams(
    val query: String,
    val radiusKm: Int,
    val sort: CraftsmanSortOption,
    val onlySelectedCategory: Boolean,
    val selectedCategoryId: String,
    val onlyCloudCraftsmen: Boolean
)

class HirafiViewModel(
    private val repository: HirafiRepository,
    private val locationService: RealTimeLocationService? = null,
    private val appContext: Context? = null
) : ViewModel() {

    val userSession: StateFlow<UserSession> = repository.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSession()
    )

    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppLanguage.ARABIC
    )

    val allRequests: StateFlow<List<ServiceRequest>> = repository.observeRequests().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val allReviews: StateFlow<List<ReviewRecord>> = repository.observeReviews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _draft = MutableStateFlow(DraftRequestState())
    val draft: StateFlow<DraftRequestState> = _draft.asStateFlow()

    private val _nearbyCraftsmen = MutableStateFlow(
        repository.getCraftsmenAround(
            MoroccanLocations.neighborhoods.first().lat,
            MoroccanLocations.neighborhoods.first().lng
        )
    )
    private val _liveCloudCraftsmen = MutableStateFlow<List<CraftsmanPin>>(emptyList())

    private val _appRuntimeConfig = MutableStateFlow(AppRuntimeConfig())
    val appRuntimeConfig: StateFlow<AppRuntimeConfig> = _appRuntimeConfig.asStateFlow()

    private val _adminVerifications = MutableStateFlow<List<VerificationSubmission>>(emptyList())
    val adminVerifications: StateFlow<List<VerificationSubmission>> = _adminVerifications.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUserRecord>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserRecord>> = _adminUsers.asStateFlow()

    private val _adminAllRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val adminAllRequests: StateFlow<List<ServiceRequest>> = _adminAllRequests.asStateFlow()

    private val _authorizedAdmins = MutableStateFlow<List<AuthorizedAdminRecord>>(emptyList())
    val authorizedAdmins: StateFlow<List<AuthorizedAdminRecord>> = _authorizedAdmins.asStateFlow()

    val nearbyCraftsmenFlow: StateFlow<List<CraftsmanPin>> = combine(
        _nearbyCraftsmen,
        _liveCloudCraftsmen,
        _draft,
        _appRuntimeConfig
    ) { localPins, cloudPins, draftState, runtimeConfig ->
        val recomputedCloud = cloudPins.map { pin ->
            val dist = MapRoutingService.haversineKm(draftState.lat, draftState.lng, pin.lat, pin.lng)
                .coerceAtLeast(0.2)
            val eta = (dist * 4.5).roundToInt().coerceIn(4, 35)
            pin.copy(distanceKm = dist, etaMinutes = eta)
        }
        if (runtimeConfig.productionMode) {
            recomputedCloud.distinctBy { it.id }
        } else {
            (recomputedCloud + localPins).distinctBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), _nearbyCraftsmen.value)

    val cloudCraftsmenCount: StateFlow<Int> = _liveCloudCraftsmen
        .combine(_draft) { cloudPins, _ -> cloudPins.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _craftsmanSearchQuery = MutableStateFlow("")
    val craftsmanSearchQuery: StateFlow<String> = _craftsmanSearchQuery.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(5)
    val searchRadiusKm: StateFlow<Int> = _searchRadiusKm.asStateFlow()

    private val _craftsmanSort = MutableStateFlow(CraftsmanSortOption.NEAREST)
    val craftsmanSort: StateFlow<CraftsmanSortOption> = _craftsmanSort.asStateFlow()

    private val _onlySelectedCategoryOnMap = MutableStateFlow(false)
    val onlySelectedCategoryOnMap: StateFlow<Boolean> = _onlySelectedCategoryOnMap.asStateFlow()

    private val _onlyCloudCraftsmen = MutableStateFlow(false)
    val onlyCloudCraftsmen: StateFlow<Boolean> = _onlyCloudCraftsmen.asStateFlow()

    private val _offerSort = MutableStateFlow(OfferSortOption.BEST_PRICE)
    val offerSort: StateFlow<OfferSortOption> = _offerSort.asStateFlow()

    private val _userGpsLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userGpsLocation: StateFlow<Pair<Double, Double>?> = _userGpsLocation.asStateFlow()

    private val _cameraTarget = MutableStateFlow<Pair<Double, Double>?>(null)
    val cameraTarget: StateFlow<Pair<Double, Double>?> = _cameraTarget.asStateFlow()

    private val _liveRoute = MutableStateFlow(LiveRouteState())
    val liveRoute: StateFlow<LiveRouteState> = _liveRoute.asStateFlow()

    private val _placeSearchResults = MutableStateFlow<List<SearchPlaceResult>>(emptyList())
    val placeSearchResults: StateFlow<List<SearchPlaceResult>> = _placeSearchResults.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces: StateFlow<Boolean> = _isSearchingPlaces.asStateFlow()

    private val _activeClientRequestId = MutableStateFlow<String?>(null)
    val activeClientRequestId: StateFlow<String?> = _activeClientRequestId.asStateFlow()

    private val _activeChatRequestId = MutableStateFlow<String?>(null)
    val activeChatRequestId: StateFlow<String?> = _activeChatRequestId.asStateFlow()

    private val _unreadChatCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadChatCounts: StateFlow<Map<String, Int>> = _unreadChatCounts.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _pendingReviewRequest = MutableStateFlow<ServiceRequest?>(null)
    val pendingReviewRequest: StateFlow<ServiceRequest?> = _pendingReviewRequest.asStateFlow()

    private val _trackingState = MutableStateFlow(
        LiveTrackingState(hasLocationPermission = locationService?.hasLocationPermission() == true)
    )
    val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

    private var geocodeJob: Job? = null
    private var trackingJob: Job? = null
    private var gpsStreamJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var activeTrackedRequestKey: String? = null
    private var lastCraftsmenCenter: Pair<Double, Double>? = null

    private val _previewedCraftsmanId = MutableStateFlow<String?>(null)
    private val _isSubmittingRequest = MutableStateFlow(false)
    val isSubmittingRequest: StateFlow<Boolean> = _isSubmittingRequest.asStateFlow()

    val supabaseAuthState: StateFlow<SupabaseAuthState?> = repository.supabaseAuthStateFlow
    val cloudRequestsSyncError: StateFlow<String?> = repository.cloudRequestsSyncError

    @OptIn(ExperimentalCoroutinesApi::class)
    val myPortfolio: StateFlow<List<PortfolioItem>> = userSession
        .flatMapLatest { s ->
            if (s.role == UserRole.CRAFTSMAN && s.userId.isNotBlank()) {
                repository.observePortfolioForCraftsman(s.userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val previewedCraftsmanPortfolio: StateFlow<List<PortfolioItem>> = _previewedCraftsmanId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList()) else repository.observePortfolioForCraftsman(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tickerMillis: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(500L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), System.currentTimeMillis())

    val filteredNearbyCraftsmen: StateFlow<List<CraftsmanPin>> = combine(
        nearbyCraftsmenFlow,
        combine(
            _craftsmanSearchQuery,
            _searchRadiusKm,
            _craftsmanSort,
            _onlySelectedCategoryOnMap,
            _draft
        ) { query, radius, sort, onlyCat, draftState ->
            CraftsmanFilterParams(
                query = query,
                radiusKm = radius,
                sort = sort,
                onlySelectedCategory = onlyCat,
                selectedCategoryId = draftState.selectedCategoryId,
                onlyCloudCraftsmen = false
            )
        },
        _onlyCloudCraftsmen,
        userSession
    ) { pins, baseParams, onlyCloud, session ->
        filterAndSortCraftsmen(
            pins = pins,
            query = baseParams.query,
            radiusKm = baseParams.radiusKm,
            sort = baseParams.sort,
            onlySelectedCategory = baseParams.onlySelectedCategory,
            selectedCategoryId = baseParams.selectedCategoryId,
            onlyCloudCraftsmen = onlyCloud,
            clientCity = session.city
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), _nearbyCraftsmen.value)

    val activeClientRequest: StateFlow<ServiceRequest?> = combine(
        allRequests,
        _activeClientRequestId,
        userSession
    ) { requests, explicitId, session ->
        if (explicitId != null) {
            requests.firstOrNull {
                it.id == explicitId &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED
            }
        } else {
            requests.firstOrNull {
                it.clientId == session.userId &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val craftsmanOpenRequests: StateFlow<List<ServiceRequest>> = combine(
        allRequests,
        userSession
    ) { requests, session ->
        if (!session.isCraftsmanSpecialtyLocked || !session.isCraftsmanOnline) {
            emptyList()
        } else {
            requests.filter { req ->
                req.status == RequestStatus.SEARCHING &&
                    req.categoryId == session.craftsmanSpecialtyId
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val craftsmanSubmittedOffers: StateFlow<Map<String, ServiceOffer>> = userSession
        .flatMapLatest { session ->
            if (session.role == UserRole.CRAFTSMAN && session.userId.isNotBlank()) {
                repository.observeOffersByCraftsman(session.userId)
            } else {
                flowOf(emptyList())
            }
        }
        .map { offers ->
            offers.groupBy { it.requestId }.mapValues { (_, list) -> list.first() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeOffers: StateFlow<List<ServiceOffer>> = activeClientRequest
        .flatMapLatest { req ->
            if (req == null) flowOf(emptyList()) else repository.observeOffersForRequest(req.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sortedActiveOffers: StateFlow<List<ServiceOffer>> = combine(
        activeOffers,
        _offerSort,
        tickerMillis
    ) { offers, sort, now ->
        val visible = offers.filter { offer ->
            val activeStatus = offer.status == OfferStatus.PENDING || offer.status == OfferStatus.COUNTERED
            val notExpired = offer.expiresAtMillis > now || offer.status == OfferStatus.COUNTERED
            activeStatus && notExpired
        }
        when (sort) {
            OfferSortOption.BEST_PRICE -> visible.sortedWith(
                compareBy<ServiceOffer> { it.offeredPrice }.thenBy { it.etaMinutes }
            )
            OfferSortOption.FASTEST_ETA -> visible.sortedWith(
                compareBy<ServiceOffer> { it.etaMinutes }.thenBy { it.offeredPrice }
            )
            OfferSortOption.TOP_RATED -> visible.sortedWith(
                compareByDescending<ServiceOffer> { it.craftsmanRating }.thenBy { it.offeredPrice }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatRequestId
        .flatMapLatest { reqId ->
            if (reqId == null) flowOf(emptyList()) else repository.observeMessagesForRequest(reqId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val isSupabaseConfigured: Boolean
        get() = SupabaseConfig.isConfigured

    init {
        viewModelScope.launch {
            combine(supabaseAuthState, _appRuntimeConfig) { auth, config ->
                (auth != null) to config.productionMode
            }.distinctUntilChanged().collect { (isSignedIn, productionMode) ->
                if (isSignedIn || productionMode) {
                    repository.clearSeededRequests()
                } else {
                    repository.seedInitialMarketplaceRequestsIfEmpty()
                }
            }
        }
        viewModelScope.launch {
            combine(supabaseAuthState, userSession) { auth, session ->
                if (auth != null && session.isRoleLocked && session.userId == auth.uid) {
                    Triple(auth.uid, session.role, session.craftsmanSpecialtyId)
                } else {
                    null
                }
            }.distinctUntilChanged().collect { active ->
                if (active == null) {
                    cloudSyncJob?.cancel()
                    cloudSyncJob = null
                    _liveCloudCraftsmen.value = emptyList()
                } else {
                    startSupabaseSync(active.second, active.third)
                }
            }
        }
        viewModelScope.launch {
            combine(allRequests, userSession, _activeClientRequestId) { requests, session, explicitId ->
                if (session.role == UserRole.CRAFTSMAN) {
                    requests.firstOrNull {
                        it.assignedCraftsmanId == session.userId &&
                            it.status != RequestStatus.COMPLETED &&
                            it.status != RequestStatus.CANCELLED
                    }
                } else {
                    if (explicitId != null) {
                        requests.firstOrNull {
                            it.id == explicitId &&
                                it.status != RequestStatus.COMPLETED &&
                                it.status != RequestStatus.CANCELLED
                        }
                    } else {
                        requests.firstOrNull {
                            it.clientId == session.userId &&
                                it.status != RequestStatus.COMPLETED &&
                                it.status != RequestStatus.CANCELLED
                        }
                    }
                }
            }.collect { req ->
                if (req == null ||
                    req.status == RequestStatus.SEARCHING ||
                    req.status == RequestStatus.CANCELLED ||
                    req.status == RequestStatus.COMPLETED
                ) {
                    stopActiveTrackingState()
                } else {
                    val key = "${req.id}_${req.assignedCraftsmanId}_${req.status.dbValue}"
                    if (activeTrackedRequestKey != key) {
                        activeTrackedRequestKey = key
                        startLiveRouteTracking(req)
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(allRequests, _activeClientRequestId, userSession) { requests, explicitId, session ->
                val id = explicitId ?: return@combine null
                val target = requests.firstOrNull { it.id == id } ?: return@combine null
                Triple(id, target.status, session.role)
            }.distinctUntilChanged().collect { state ->
                val (id, status, role) = state ?: return@collect
                if (status == RequestStatus.CANCELLED || status == RequestStatus.COMPLETED) {
                    if (_activeClientRequestId.value == id) {
                        _activeClientRequestId.value = null
                        if (status == RequestStatus.CANCELLED && role == UserRole.CLIENT) {
                            showMessage("تم إلغاء الخدمة")
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var trackedCraftsmanJobId: String? = null
            combine(allRequests, userSession) { requests, session ->
                requests to session
            }.collect { (requests, session) ->
                if (session.role != UserRole.CRAFTSMAN) {
                    trackedCraftsmanJobId = null
                    return@collect
                }
                val currentActive = requests.firstOrNull {
                    it.assignedCraftsmanId == session.userId &&
                        it.status != RequestStatus.COMPLETED &&
                        it.status != RequestStatus.CANCELLED
                }
                if (currentActive != null) {
                    trackedCraftsmanJobId = currentActive.id
                } else {
                    val prevId = trackedCraftsmanJobId
                    if (prevId != null) {
                        val prevReq = requests.firstOrNull { it.id == prevId }
                        if (prevReq?.status == RequestStatus.CANCELLED) {
                            showMessage("تم إلغاء الخدمة من طرف الزبون")
                        }
                        trackedCraftsmanJobId = null
                    }
                }
            }
        }
    }

    private fun stopActiveTrackingState() {
        activeTrackedRequestKey = null
        trackingJob?.cancel()
        trackingJob = null
        locationService?.stopForegroundJobTracking()
        _liveRoute.value = LiveRouteState()
        _trackingState.update {
            it.copy(
                isTracking = false,
                isForegroundServiceActive = false,
                trackedRequestId = null,
                hasLocationPermission = locationService?.hasLocationPermission() == true
            )
        }
    }

    fun refreshLocationTracking(context: Context) {
        val svc = locationService ?: RealTimeLocationService(context.applicationContext)
        val hasPerm = svc.hasLocationPermission()
        _trackingState.update { it.copy(hasLocationPermission = hasPerm) }
        val currentSession = userSession.value
        val activeJob = if (currentSession.role == UserRole.CRAFTSMAN) {
            allRequests.value.firstOrNull {
                it.assignedCraftsmanId == currentSession.userId &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED &&
                    it.status != RequestStatus.SEARCHING
            }
        } else {
            activeClientRequest.value?.takeIf {
                it.status != RequestStatus.SEARCHING &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED
            }
        }
        if (activeJob != null && hasPerm) {
            activeTrackedRequestKey = "${activeJob.id}_${activeJob.assignedCraftsmanId}_${activeJob.status.dbValue}_gps"
            startLiveRouteTracking(activeJob)
        }
    }

    private fun isValidRegionalCoord(lat: Double, lng: Double, refLat: Double, refLng: Double): Boolean {
        if (lat == 0.0 && lng == 0.0) return false
        return MapRoutingService.haversineKm(lat, lng, refLat, refLng) <= 150.0
    }

    private fun startLiveRouteTracking(req: ServiceRequest) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            val isCraftsman = userSession.value.role == UserRole.CRAFTSMAN
            val hasPerm = locationService?.hasLocationPermission() == true
            val gps = _userGpsLocation.value?.takeIf { isValidRegionalCoord(it.first, it.second, req.lat, req.lng) }
            val cityCoord = MoroccanLocations.coordForCity(userSession.value.city)
            val assignedPin = _liveCloudCraftsmen.value.firstOrNull { it.id == req.assignedCraftsmanId }
                ?: _nearbyCraftsmen.value.firstOrNull { it.id == req.assignedCraftsmanId }
                ?: _nearbyCraftsmen.value.firstOrNull()
            val startLat = if (isCraftsman) {
                gps?.first ?: cityCoord.lat
            } else {
                assignedPin?.lat?.takeIf { isValidRegionalCoord(it, assignedPin.lng, req.lat, req.lng) }
                    ?: (req.lat + 0.008)
            }
            val startLng = if (isCraftsman) {
                gps?.second ?: cityCoord.lng
            } else {
                assignedPin?.lng?.takeIf { isValidRegionalCoord(assignedPin.lat, it, req.lat, req.lng) }
                    ?: (req.lng - 0.007)
            }

            _trackingState.update {
                it.copy(
                    isTracking = true,
                    hasLocationPermission = hasPerm,
                    trackedRequestId = req.id,
                    errorMessage = null
                )
            }

            if (isCraftsman) {
                trackAsCraftsmanWithPlayServices(req, startLat, startLng)
            } else {
                trackCraftsmanForClient(req, startLat, startLng)
            }
        }
    }

    private suspend fun trackAsCraftsmanWithPlayServices(
        req: ServiceRequest,
        fallbackLat: Double,
        fallbackLng: Double
    ) {
        val svc = locationService
        val hasPerm = svc?.hasLocationPermission() == true
        if (!hasPerm) {
            // Compute initial route & push fallback city coord so the client immediately sees the craftsman on map
            val initialRoute = MapRoutingService.fetchDrivingRoute(fallbackLat, fallbackLng, req.lat, req.lng)
            val distKm = MapRoutingService.haversineKm(fallbackLat, fallbackLng, req.lat, req.lng)
            val arrivedOrWorking = req.status == RequestStatus.ARRIVED || req.status == RequestStatus.IN_PROGRESS
            val pts = if (initialRoute.points.size >= 2) initialRoute.points else listOf(fallbackLat to fallbackLng, req.lat to req.lng)
            _liveRoute.value = LiveRouteState(
                routePoints = if (arrivedOrWorking) emptyList() else pts,
                craftsmanLat = fallbackLat,
                craftsmanLng = fallbackLng,
                remainingDistanceKm = if (arrivedOrWorking) 0.0 else maxOf(distKm, initialRoute.distanceKm),
                etaMinutes = if (arrivedOrWorking) 0 else initialRoute.durationMinutes.coerceAtLeast(1)
            )
            repository.publishCraftsmanLiveLocation(fallbackLat, fallbackLng, activeRequestId = req.id)
            _trackingState.update {
                it.copy(
                    isTracking = true,
                    hasLocationPermission = false,
                    isForegroundServiceActive = false,
                    errorMessage = "يرجى تفعيل صلاحية الموقع للتتبع المباشر"
                )
            }
            return
        }

        // Start foreground location service for continuous GPS tracking during active job (even during Google Maps navigation)
        if (req.status == RequestStatus.ACCEPTED || req.status == RequestStatus.EN_ROUTE) {
            svc?.startForegroundJobTracking(req.id, req.title, req.lat, req.lng)
        } else {
            svc?.stopForegroundJobTracking()
        }

        var lastValidLat = fallbackLat
        var lastValidLng = fallbackLng
        var lastRoutedLat = fallbackLat
        var lastRoutedLng = fallbackLng
        var lastRouteFetchMs = 0L
        var lastSupabaseSyncMs = 0L
        var cachedPolyline: List<Pair<Double, Double>> = emptyList()

        suspend fun applyCraftsmanFix(rawFix: RealTimeLocationSnapshot) {
            val useRaw = isValidRegionalCoord(rawFix.lat, rawFix.lng, req.lat, req.lng)
            val effectiveLat = if (useRaw) rawFix.lat else lastValidLat
            val effectiveLng = if (useRaw) rawFix.lng else lastValidLng
            if (useRaw) {
                lastValidLat = effectiveLat
                lastValidLng = effectiveLng
            }
            val effectiveFix = if (useRaw) rawFix else rawFix.copy(lat = effectiveLat, lng = effectiveLng)

            _userGpsLocation.value = effectiveLat to effectiveLng
            _trackingState.update {
                it.copy(
                    isTracking = true,
                    hasLocationPermission = true,
                    isForegroundServiceActive = LiveLocationBus.isForegroundServiceActive.value,
                    latestFix = effectiveFix,
                    errorMessage = null
                )
            }

            val distKm = MapRoutingService.haversineKm(effectiveLat, effectiveLng, req.lat, req.lng)
            val arrivedOrWorking = req.status == RequestStatus.ARRIVED || req.status == RequestStatus.IN_PROGRESS
            val nowMs = System.currentTimeMillis()
            val movedKm = MapRoutingService.haversineKm(lastRoutedLat, lastRoutedLng, effectiveLat, effectiveLng)

            if (!arrivedOrWorking && (cachedPolyline.isEmpty() || movedKm >= 0.04 || nowMs - lastRouteFetchMs >= 12_000L)) {
                lastRoutedLat = effectiveLat
                lastRoutedLng = effectiveLng
                lastRouteFetchMs = nowMs
                val route = MapRoutingService.fetchDrivingRoute(effectiveLat, effectiveLng, req.lat, req.lng)
                cachedPolyline = if (route.points.size >= 2) route.points else listOf(effectiveLat to effectiveLng, req.lat to req.lng)
            }

            val eta = if (arrivedOrWorking) 0 else RealTimeLocationService.computeEtaMinutes(distKm, effectiveFix.speedMps).coerceAtLeast(1)
            _liveRoute.value = LiveRouteState(
                routePoints = if (arrivedOrWorking) emptyList() else cachedPolyline,
                craftsmanLat = effectiveLat,
                craftsmanLng = effectiveLng,
                remainingDistanceKm = if (arrivedOrWorking) 0.0 else distKm,
                etaMinutes = eta
            )

            if (nowMs - lastSupabaseSyncMs >= 4500L) {
                lastSupabaseSyncMs = nowMs
                repository.publishCraftsmanLiveLocation(effectiveLat, effectiveLng, activeRequestId = req.id)
            }
        }

        // Seed immediately with high-accuracy Play Services fix or fallback
        val immediate = svc?.getCurrentFix()
        if (immediate != null) {
            applyCraftsmanFix(immediate)
        } else {
            applyCraftsmanFix(
                RealTimeLocationSnapshot(
                    lat = fallbackLat,
                    lng = fallbackLng,
                    source = com.example.data.location.LocationSource.LAST_KNOWN
                )
            )
        }

        // Continuously collect Play Services real-time updates (from direct callbackFlow & LiveLocationBus)
        val nonNullSvc = svc ?: return
        val liveFlow = merge(
            nonNullSvc.observeRealTimeLocation(intervalMillis = 3500L, minDistanceMeters = 5f),
            LiveLocationBus.latestCraftsmanFix.let { busFlow ->
                kotlinx.coroutines.flow.flow {
                    busFlow.collect { snap -> if (snap != null) emit(snap) }
                }
            }
        )

        liveFlow.collect { fix ->
            applyCraftsmanFix(fix)
        }
    }

    private suspend fun trackCraftsmanForClient(
        req: ServiceRequest,
        initialCraftsmanLat: Double,
        initialCraftsmanLng: Double
    ) {
        val assignedId = req.assignedCraftsmanId
        val remoteSeed = assignedId
            ?.takeIf { it.length == 36 }
            ?.let { repository.fetchAssignedCraftsmanLocation(it) }
            ?.takeIf { isValidRegionalCoord(it.first, it.second, req.lat, req.lng) }
        val seedLat = remoteSeed?.first ?: initialCraftsmanLat
        val seedLng = remoteSeed?.second ?: initialCraftsmanLng

        val route = MapRoutingService.fetchDrivingRoute(
            startLat = seedLat,
            startLng = seedLng,
            endLat = req.lat,
            endLng = req.lng
        )
        val seedPts = if (route.points.size >= 2) route.points else listOf(seedLat to seedLng, req.lat to req.lng)
        val isCloudTracked = !assignedId.isNullOrBlank() &&
            !assignedId.startsWith("c_") &&
            assignedId.length == 36 &&
            SupabaseConfig.isConfigured

        when (req.status) {
            RequestStatus.ACCEPTED, RequestStatus.EN_ROUTE -> {
                if (isCloudTracked) {
                    val seedDist = MapRoutingService.haversineKm(seedLat, seedLng, req.lat, req.lng)
                    var lastRoutedLat = seedLat
                    var lastRoutedLng = seedLng
                    var lastRouteFetchMs = System.currentTimeMillis()
                    var cachedPts = seedPts

                    _liveRoute.value = LiveRouteState(
                        routePoints = cachedPts,
                        craftsmanLat = seedLat,
                        craftsmanLng = seedLng,
                        remainingDistanceKm = (seedDist * 10.0).roundToInt() / 10.0,
                        etaMinutes = if (seedDist <= 0.08) 0 else route.durationMinutes.coerceAtLeast(1)
                    )

                    repository.observeCloudRequestLiveCoords(req.id).collect { observedCoord ->
                        val fallbackCoord = observedCoord ?: repository.fetchAssignedCraftsmanLocation(assignedId)
                        val validCoord = fallbackCoord?.takeIf { isValidRegionalCoord(it.first, it.second, req.lat, req.lng) }
                        if (validCoord != null) {
                            val dist = MapRoutingService.haversineKm(validCoord.first, validCoord.second, req.lat, req.lng)
                            val nowMs = System.currentTimeMillis()
                            val movedKm = MapRoutingService.haversineKm(lastRoutedLat, lastRoutedLng, validCoord.first, validCoord.second)
                            val activeRoute = if (cachedPts.isEmpty() || movedKm >= 0.04 || nowMs - lastRouteFetchMs >= 10_000L) {
                                lastRoutedLat = validCoord.first
                                lastRoutedLng = validCoord.second
                                lastRouteFetchMs = nowMs
                                val fetched = MapRoutingService.fetchDrivingRoute(validCoord.first, validCoord.second, req.lat, req.lng)
                                cachedPts = if (fetched.points.size >= 2) fetched.points else listOf(validCoord.first to validCoord.second, req.lat to req.lng)
                                fetched
                            } else {
                                MapRoutingService.fetchDrivingRoute(validCoord.first, validCoord.second, req.lat, req.lng)
                            }
                            _liveRoute.value = LiveRouteState(
                                routePoints = cachedPts,
                                craftsmanLat = validCoord.first,
                                craftsmanLng = validCoord.second,
                                remainingDistanceKm = if (dist <= 0.08) 0.0 else (dist * 10.0).roundToInt() / 10.0,
                                etaMinutes = if (dist <= 0.08) 0 else activeRoute.durationMinutes.coerceAtLeast(1)
                            )
                        }
                    }
                } else if (req.status == RequestStatus.ACCEPTED) {
                    val dist = MapRoutingService.haversineKm(seedLat, seedLng, req.lat, req.lng)
                    _liveRoute.value = LiveRouteState(
                        routePoints = seedPts,
                        craftsmanLat = seedLat,
                        craftsmanLng = seedLng,
                        remainingDistanceKm = (dist * 10.0).roundToInt() / 10.0,
                        etaMinutes = route.durationMinutes.coerceAtLeast(1)
                    )
                } else {
                    // Smooth route-following live progression for local/verified offline craftsmen
                    val totalSteps = seedPts.size.coerceAtLeast(2)
                    for (idx in 0 until totalSteps) {
                        val liveBusOverride = LiveLocationBus.latestCraftsmanFix.value
                        if (liveBusOverride != null && req.assignedCraftsmanId == userSession.value.userId) {
                            val dist = MapRoutingService.haversineKm(liveBusOverride.lat, liveBusOverride.lng, req.lat, req.lng)
                            _liveRoute.value = LiveRouteState(
                                routePoints = seedPts.subList(idx, seedPts.size),
                                craftsmanLat = liveBusOverride.lat,
                                craftsmanLng = liveBusOverride.lng,
                                remainingDistanceKm = dist,
                                etaMinutes = RealTimeLocationService.computeEtaMinutes(dist, liveBusOverride.speedMps)
                            )
                        } else {
                            val currentPos = seedPts[idx]
                            val fractionRemaining = 1.0 - (idx.toDouble() / (totalSteps - 1).coerceAtLeast(1))
                            val remDist = ((route.distanceKm * fractionRemaining) * 10.0).roundToInt() / 10.0
                            val remEta = (route.durationMinutes * fractionRemaining).roundToInt().coerceAtLeast(1)
                            _liveRoute.value = LiveRouteState(
                                routePoints = seedPts.subList(idx, seedPts.size),
                                craftsmanLat = currentPos.first,
                                craftsmanLng = currentPos.second,
                                remainingDistanceKm = remDist,
                                etaMinutes = remEta
                            )
                        }
                        delay(1100L)
                    }
                    _liveRoute.value = LiveRouteState(
                        routePoints = listOf(req.lat to req.lng),
                        craftsmanLat = req.lat,
                        craftsmanLng = req.lng,
                        remainingDistanceKm = 0.0,
                        etaMinutes = 0
                    )
                }
            }
            RequestStatus.ARRIVED, RequestStatus.IN_PROGRESS -> {
                _liveRoute.value = LiveRouteState(
                    routePoints = emptyList(),
                    craftsmanLat = req.lat,
                    craftsmanLng = req.lng,
                    remainingDistanceKm = 0.0,
                    etaMinutes = 0
                )
            }
            else -> {}
        }
    }

    fun selectCategory(categoryId: String) {
        val category = HirafiCategories.byId(categoryId)
        _draft.update {
            it.copy(
                selectedCategoryId = categoryId,
                proposedPrice = category.defaultPriceDh
            )
        }
    }

    fun updateDraftTitle(title: String) {
        _draft.update { it.copy(title = title) }
    }

    fun updateDraftDescription(description: String) {
        _draft.update { it.copy(description = description) }
    }

    fun adjustDraftPrice(delta: Int) {
        _draft.update {
            it.copy(proposedPrice = (it.proposedPrice + delta).coerceIn(40, 5000))
        }
    }

    fun setDraftPrice(price: Int) {
        _draft.update {
            it.copy(proposedPrice = price.coerceIn(40, 5000))
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _draft.update { it.copy(paymentMethod = method) }
    }

    fun toggleUrgent(isUrgent: Boolean) {
        _draft.update { it.copy(isUrgent = isUrgent) }
    }

    fun setDraftPhotoUri(uri: String?) {
        _draft.update { it.copy(photoUri = uri) }
    }

    fun prefillDraftFromRequest(request: ServiceRequest) {
        _draft.update {
            it.copy(
                selectedCategoryId = request.categoryId,
                title = request.title,
                description = request.description,
                proposedPrice = (request.acceptedPrice ?: request.proposedPrice).coerceIn(40, 5000),
                paymentMethod = request.paymentMethod,
                isUrgent = request.isUrgent,
                addressLabel = request.addressLabel,
                lat = request.lat,
                lng = request.lng,
                isGpsActive = false,
                isResolvingAddress = false
            )
        }
        _cameraTarget.value = request.lat to request.lng
        lastCraftsmenCenter = request.lat to request.lng
        _nearbyCraftsmen.value = repository.getCraftsmenAround(request.lat, request.lng)
        showMessage("تم تجهيز مسودة الطلب السابق بنجاح")
    }

    fun onMapPinMoved(context: Context, lat: Double, lng: Double, moveCamera: Boolean = false) {
        if (moveCamera) {
            _cameraTarget.value = lat to lng
        }
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            delay(450L)
            _draft.update {
                it.copy(
                    lat = lat,
                    lng = lng,
                    isGpsActive = false,
                    isResolvingAddress = true
                )
            }
            val prev = lastCraftsmenCenter
            if (prev == null || MapRoutingService.haversineKm(prev.first, prev.second, lat, lng) >= 0.25) {
                lastCraftsmenCenter = lat to lng
                _nearbyCraftsmen.value = repository.getCraftsmenAround(lat, lng)
            }
            val resolved = MapRoutingService.reverseGeocode(context, lat, lng, appLanguage.value.code)
            _draft.update {
                it.copy(
                    addressLabel = resolved,
                    isResolvingAddress = false
                )
            }
        }
    }

    fun selectNeighborhood(label: String, lat: Double, lng: Double) {
        geocodeJob?.cancel()
        _draft.update {
            it.copy(
                addressLabel = label,
                lat = lat,
                lng = lng,
                isGpsActive = false,
                isResolvingAddress = false
            )
        }
        _cameraTarget.value = lat to lng
        lastCraftsmenCenter = lat to lng
        _nearbyCraftsmen.value = repository.getCraftsmenAround(lat, lng)
    }

    fun searchPlaces(context: Context, query: String) {
        viewModelScope.launch {
            _isSearchingPlaces.value = true
            _placeSearchResults.value = MapRoutingService.searchPlaces(context, query, appLanguage.value.code)
            _isSearchingPlaces.value = false
        }
    }

    fun clearPlaceSearchResults() {
        _placeSearchResults.value = emptyList()
    }

    fun consumeCameraTarget() {
        _cameraTarget.value = null
    }

    fun startLiveGpsTracking(context: Context) {
        val svc = locationService ?: RealTimeLocationService(context.applicationContext)
        _trackingState.update { it.copy(hasLocationPermission = svc.hasLocationPermission()) }
        if (!svc.hasLocationPermission()) return
        if (gpsStreamJob?.isActive == true) return
        gpsStreamJob = viewModelScope.launch {
            svc.observeRealTimeLocation(intervalMillis = 8000L, minDistanceMeters = 15f).collect { fix ->
                _userGpsLocation.value = fix.lat to fix.lng
                _trackingState.update { it.copy(latestFix = fix, hasLocationPermission = true) }
            }
        }
    }

    fun fetchCurrentGpsLocation(context: Context, isAutoStartup: Boolean = false) {
        val appContext = context.applicationContext
        val svc = locationService ?: RealTimeLocationService(appContext)
        _trackingState.update { it.copy(hasLocationPermission = svc.hasLocationPermission()) }
        startLiveGpsTracking(appContext)

        viewModelScope.launch {
            val fix = svc.getCurrentFix()
            if (fix != null) {
                val lat = fix.lat
                val lng = fix.lng
                val cityCoord = MoroccanLocations.coordForCity(userSession.value.city)
                val distFromCityKm = MapRoutingService.haversineKm(lat, lng, cityCoord.lat, cityCoord.lng)
                if (isAutoStartup && distFromCityKm > 150.0) {
                    // Keep Moroccan city coordinates anchored on emulator/out-of-region auto-startup
                    _trackingState.update { it.copy(latestFix = fix, hasLocationPermission = true, errorMessage = null) }
                    return@launch
                }
                _userGpsLocation.value = lat to lng
                _cameraTarget.value = lat to lng
                _trackingState.update { it.copy(latestFix = fix, hasLocationPermission = true, errorMessage = null) }
                lastCraftsmenCenter = lat to lng
                _nearbyCraftsmen.value = repository.getCraftsmenAround(lat, lng)
                _draft.update {
                    it.copy(
                        lat = lat,
                        lng = lng,
                        isGpsActive = true,
                        isResolvingAddress = true
                    )
                }
                geocodeJob?.cancel()
                geocodeJob = viewModelScope.launch {
                    val address = MapRoutingService.reverseGeocode(appContext, lat, lng, appLanguage.value.code)
                    _draft.update {
                        it.copy(
                            addressLabel = address,
                            isResolvingAddress = false
                        )
                    }
                }
                if (!isAutoStartup) {
                    showMessage("تم تحديد موقعك الحقيقي عبر GPS")
                }
            } else if (!isAutoStartup) {
                showMessage("فعّل خدمة الموقع (GPS) على الجهاز أو حرّك الخريطة لتحديد مكانك")
            }
        }
    }

    fun updateCraftsmanSearchQuery(query: String) {
        _craftsmanSearchQuery.value = query
    }

    fun setSearchRadiusKm(radiusKm: Int) {
        _searchRadiusKm.value = radiusKm
    }

    fun setCraftsmanSort(sort: CraftsmanSortOption) {
        _craftsmanSort.value = sort
    }

    fun toggleOnlyCategoryOnMap(only: Boolean) {
        _onlySelectedCategoryOnMap.value = only
    }

    fun toggleOnlyCloudCraftsmen(only: Boolean) {
        _onlyCloudCraftsmen.value = only
    }

    fun setOfferSort(sort: OfferSortOption) {
        _offerSort.value = sort
    }

    fun focusCraftsmanOnMap(pin: CraftsmanPin) {
        _cameraTarget.value = pin.lat to pin.lng
    }

    fun expandActiveSearchRadius(newRadiusKm: Int) {
        val req = activeClientRequest.value ?: return
        _searchRadiusKm.value = newRadiusKm
        viewModelScope.launch {
            repository.expandSearchRadius(req.id, newRadiusKm)
            showMessage("تم توسيع نطاق البحث إلى $newRadiusKm كم ودعوة حرفيين إضافيين")
        }
    }

    fun requestSpecificCraftsman(pin: CraftsmanPin) {
        _draft.update { current ->
            current.copy(
                selectedCategoryId = pin.specialtyId,
                proposedPrice = pin.basePriceDh,
                title = current.title.ifBlank { "طلب ${pin.specialtyName} مباشر - ${pin.name}" }
            )
        }
        submitServiceRequest(preferredCraftsmanId = pin.id)
    }

    fun refreshCraftsmanRequests() {
        repository.refreshMarketplaceNow()
    }

    fun submitServiceRequest(preferredCraftsmanId: String? = null) {
        if (userSession.value.isSuspended) {
            showMessage("تم تعليق هذا الحساب مؤقتاً من طرف الإدارة")
            return
        }
        if (_isSubmittingRequest.value) return
        val current = _draft.value
        val radiusKm = _searchRadiusKm.value
        val isProduction = _appRuntimeConfig.value.productionMode
        viewModelScope.launch {
            _isSubmittingRequest.value = true
            try {
                repository.createServiceRequest(
                    categoryId = current.selectedCategoryId,
                    title = current.title.ifBlank { "طلب ${HirafiCategories.byId(current.selectedCategoryId).nameAr}" },
                    description = current.description,
                    addressLabel = current.addressLabel,
                    lat = current.lat,
                    lng = current.lng,
                    mapX = 0.5f,
                    mapY = 0.5f,
                    proposedPrice = current.proposedPrice,
                    paymentMethod = current.paymentMethod,
                    isUrgent = current.isUrgent,
                    photoUri = current.photoUri,
                    preferredCraftsmanId = preferredCraftsmanId,
                    radiusKm = radiusKm,
                    productionMode = isProduction
                ).onSuccess { req ->
                    _activeClientRequestId.value = req.id
                    _draft.update { it.copy(title = "", description = "", photoUri = null) }
                    repository.refreshMarketplaceNow()
                    val msg = if (preferredCraftsmanId != null) {
                        "تم إرسال طلب مباشر للحرفي المختار وللحرفيين القريبين!"
                    } else {
                        "تم إرسال طلبك! جاري استقبال عروض الحرفيين..."
                    }
                    showMessage(msg)
                }.onFailure { err ->
                    showMessage(err.message ?: "تعذر إرسال الطلب إلى Supabase، تحقق من الاتصال وأعد المحاولة")
                }
            } finally {
                _isSubmittingRequest.value = false
            }
        }
    }

    fun raiseActiveRequestPrice(delta: Int) {
        val req = activeClientRequest.value ?: return
        val newPrice = (req.proposedPrice + delta).coerceAtLeast(50)
        viewModelScope.launch {
            runCatching {
                repository.raiseRequestPrice(req.id, newPrice)
            }.onSuccess {
                showMessage("تم رفع السعر المقترح إلى $newPrice درهم")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر تحديث السعر في Supabase")
            }
        }
    }

    fun acceptOffer(offer: ServiceOffer) {
        viewModelScope.launch {
            runCatching {
                repository.acceptOffer(offer)
            }.onSuccess {
                showMessage("تم قبول عرض ${offer.craftsmanName} بسعر ${offer.offeredPrice} درهم")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر قبول العرض")
            }
        }
    }

    fun declineOffer(offerId: String) {
        viewModelScope.launch {
            runCatching {
                repository.declineOffer(offerId)
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر رفض العرض")
            }
        }
    }

    fun counterOffer(offer: ServiceOffer, counterPrice: Int) {
        viewModelScope.launch {
            runCatching {
                repository.counterOffer(offer, counterPrice.coerceAtLeast(40))
            }.onSuccess {
                showMessage("تم إرسال عرض مضاد ($counterPrice درهم) لـ ${offer.craftsmanName}")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إرسال العرض المضاد")
            }
        }
    }

    fun cancelActiveRequest(requestId: String? = null) {
        val targetId = requestId ?: activeClientRequest.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                repository.cancelRequest(targetId)
            }.onSuccess {
                if (_activeClientRequestId.value == targetId) {
                    _activeClientRequestId.value = null
                }
                stopActiveTrackingState()
                showMessage("تم إلغاء الخدمة وإغلاق الطلب")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إلغاء الخدمة")
            }
        }
    }

    fun cancelCraftsmanActiveJob(request: ServiceRequest) {
        viewModelScope.launch {
            runCatching {
                repository.cancelRequest(request.id)
            }.onSuccess {
                stopActiveTrackingState()
                showMessage("تم إلغاء الخدمة وإشعار الزبون")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إلغاء الخدمة")
            }
        }
    }

    fun advanceJobStatus(request: ServiceRequest, nextStatus: RequestStatus) {
        viewModelScope.launch {
            runCatching {
                repository.advanceJobStatus(request.id, nextStatus)
            }.onSuccess {
                if (nextStatus == RequestStatus.COMPLETED) {
                    if (userSession.value.role == UserRole.CLIENT) {
                        _pendingReviewRequest.value = request.copy(status = RequestStatus.COMPLETED)
                    }
                    if (_activeClientRequestId.value == request.id) {
                        _activeClientRequestId.value = null
                    }
                    showMessage("تم إتمام الخدمة بنجاح!")
                } else {
                    showMessage("تم تحديث الحالة إلى: ${nextStatus.labelAr}")
                }
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر تحديث حالة الطلب")
            }
        }
    }

    fun dismissPendingReview() {
        _pendingReviewRequest.value = null
    }

    fun submitReview(request: ServiceRequest, rating: Int, tags: List<String>, comment: String) {
        viewModelScope.launch {
            runCatching {
                repository.submitReview(
                    requestId = request.id,
                    craftsmanId = request.assignedCraftsmanId ?: "craftsman",
                    rating = rating,
                    tags = tags,
                    comment = comment
                )
            }.onSuccess {
                _pendingReviewRequest.value = null
                showMessage("شكراً لتقييمك! يساعد هذا في تحسين جودة الحرفيين.")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر حفظ التقييم")
            }
        }
    }

    fun craftsmanAcceptAtClientPrice(requestId: String) {
        if (userSession.value.isSuspended) {
            showMessage("تم تعليق حسابك مؤقتاً من طرف الإدارة")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.craftsmanDirectAcceptRequest(requestId)
            }.onSuccess {
                showMessage("تم قبول الطلب! يمكنك الآن تتبع موقع الزبون والانطلاق.")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر قبول الطلب")
            }
        }
    }

    fun craftsmanSubmitCounterOffer(requestId: String, offeredPrice: Int, etaMinutes: Int, note: String) {
        if (userSession.value.isSuspended) {
            showMessage("تم تعليق حسابك مؤقتاً من طرف الإدارة")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.submitCraftsmanBid(requestId, offeredPrice, etaMinutes, note)
            }.onSuccess {
                showMessage("تم إرسال عرضك بسعر $offeredPrice درهم للزبون")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إرسال العرض")
            }
        }
    }

    fun openChatForRequest(requestId: String) {
        _activeChatRequestId.value = requestId
        _unreadChatCounts.update { it - requestId }
    }

    fun closeActiveChat() {
        _activeChatRequestId.value = null
    }

    fun sendChatMessage(requestId: String, text: String) {
        viewModelScope.launch {
            runCatching {
                repository.sendChatMessage(requestId, text)
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إرسال الرسالة")
            }
        }
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            val current = userSession.value
            if (current.isRoleLocked && current.role != role) {
                showMessage("حسابك مسجل بشكل دائم كـ ${current.role.labelAr} ولا يمكن الدخول إلى وضع ${role.labelAr}")
                return@launch
            }
            val changed = repository.setRole(role)
            if (changed) {
                showMessage(if (role == UserRole.CRAFTSMAN) "تم اختيار وضع الحرفي" else "تم اختيار وضع الزبون")
            } else {
                showMessage("حسابك مقفل كـ ${current.role.labelAr} ولا يمكن التبديل")
            }
        }
    }

    fun restoreSupabaseSession() {
        viewModelScope.launch {
            val result = repository.restoreSupabaseSession()
            result.onSuccess { restored ->
                if (restored != null) {
                    val coord = MoroccanLocations.coordForCity(restored.city)
                    selectNeighborhood(restored.city, coord.lat, coord.lng)
                }
            }
        }
    }

    fun handleSupabaseOAuthCallback(uriString: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.completeGoogleOAuthCallback(uriString)
            result.onSuccess { restored ->
                _authUiState.value = AuthUiState(isLoading = false)
                if (restored != null) {
                    val coord = MoroccanLocations.coordForCity(restored.city)
                    selectNeighborhood(restored.city, coord.lat, coord.lng)
                    when (restored.role) {
                        UserRole.ADMIN -> showMessage("مرحباً ${restored.fullName}! تم تسجيل دخولك كأدمين رسمي للمنصة ✓")
                        UserRole.CRAFTSMAN -> showMessage("مرحباً بعودتك ${restored.fullName} (${HirafiCategories.byId(restored.craftsmanSpecialtyId).nameAr})")
                        UserRole.CLIENT -> showMessage("مرحباً بعودتك ${restored.fullName}!")
                    }
                } else {
                    showMessage("تم التحقق من حساب غوغل — اختر نوع الحساب والمدينة للتأكيد")
                }
            }.onFailure { err ->
                val msg = err.message ?: "تعذر إتمام تسجيل الدخول عبر غوغل"
                _authUiState.value = AuthUiState(isLoading = false, message = msg, isError = true)
                showMessage(msg)
            }
        }
    }

    fun completeGoogleSignIn(
        idToken: String,
        nonce: String? = null,
        displayName: String,
        preferredDisplayName: String = "",
        email: String,
        city: String,
        preferredRole: UserRole,
        preferredSpecialtyId: String?,
        autoCreateIfMissing: Boolean = true,
        onFallbackToBrowser: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.completeGoogleSupabaseLogin(
                idToken = idToken,
                nonce = nonce,
                fallbackDisplayName = displayName,
                preferredDisplayName = preferredDisplayName,
                fallbackEmail = email,
                city = city,
                preferredRole = preferredRole,
                preferredSpecialtyId = preferredSpecialtyId,
                autoCreateIfMissing = autoCreateIfMissing
            )
            val err = result.exceptionOrNull()
            if (err is GoogleIdTokenFallbackToBrowserException && idToken.isNotBlank() && SupabaseConfig.isConfigured) {
                _authUiState.value = AuthUiState(isLoading = false)
                showMessage("جاري التحويل لإكمال تسجيل الدخول عبر متصفح غوغل...")
                onFallbackToBrowser()
                return@launch
            }
            if (result.isSuccess && result.getOrNull() == null) {
                _authUiState.value = AuthUiState(isLoading = false)
                showMessage("تم التحقق من حساب غوغل — اختر نوع الحساب والمدينة للتأكيد")
                return@launch
            }
            handleAuthResult(result.map { it!! }, preferredRole)
        }
    }

    fun signUpSupabaseEmail(
        email: String,
        password: String,
        fullName: String,
        city: String,
        preferredRole: UserRole,
        preferredSpecialtyId: String?
    ) {
        if (!LoginValidator.isValidEmail(email)) {
            showMessage("يرجى إدخال بريد إلكتروني صحيح")
            return
        }
        if (!LoginValidator.isValidPassword(password)) {
            showMessage("يجب أن تتكون كلمة المرور من 6 أحرف على الأقل")
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.signUpWithSupabaseEmailAccount(
                email = email,
                password = password,
                fullName = fullName,
                city = city,
                preferredRole = preferredRole,
                preferredSpecialtyId = preferredSpecialtyId
            )
            handleAuthResult(result, preferredRole)
        }
    }

    fun signInSupabaseEmail(email: String, password: String) {
        if (!LoginValidator.isValidEmail(email)) {
            showMessage("يرجى إدخال بريد إلكتروني صحيح")
            return
        }
        if (password.isBlank()) {
            showMessage("يرجى إدخال كلمة المرور")
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.signInWithSupabaseEmailAccount(email, password)
            result.onSuccess { session ->
                _authUiState.value = AuthUiState(isLoading = false)
                val coord = MoroccanLocations.coordForCity(session.city)
                selectNeighborhood(session.city, coord.lat, coord.lng)
                when (session.role) {
                    UserRole.ADMIN -> showMessage("مرحباً ${session.fullName}! تم تسجيل دخولك كأدمين رسمي للمنصة ✓")
                    UserRole.CRAFTSMAN -> showMessage("مرحباً بعودتك ${session.fullName} (${HirafiCategories.byId(session.craftsmanSpecialtyId).nameAr})")
                    UserRole.CLIENT -> showMessage("مرحباً بعودتك ${session.fullName}!")
                }
            }.onFailure { err ->
                _authUiState.value = AuthUiState(isLoading = false, message = err.message, isError = true)
                showMessage(err.message ?: "تعذر تسجيل الدخول في Supabase")
            }
        }
    }

    fun clearAuthMessage() {
        _authUiState.update { it.copy(message = null, isError = false) }
    }

    fun sendPasswordReset(email: String) {
        val clean = email.trim().lowercase()
        val s = HirafiI18n.forLanguage(appLanguage.value)
        if (!LoginValidator.isValidEmail(clean)) {
            showMessage(s.authEmailRequiredError)
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            repository.sendPasswordResetEmail(clean).onSuccess {
                _authUiState.value = AuthUiState(
                    isLoading = false,
                    message = s.authResetEmailSent,
                    isError = false
                )
                showMessage(s.authResetEmailSent)
            }.onFailure { err ->
                val msg = err.message ?: "تعذر إرسال رابط الاسترجاع"
                _authUiState.value = AuthUiState(isLoading = false, message = msg, isError = true)
                showMessage(msg)
            }
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
        }
    }

    private fun handleAuthResult(result: Result<UserSession>, preferredRole: UserRole) {
        result.onSuccess { session ->
            _authUiState.value = AuthUiState(isLoading = false)
            val coord = MoroccanLocations.coordForCity(session.city)
            selectNeighborhood(session.city, coord.lat, coord.lng)
            when {
                session.role == UserRole.ADMIN -> {
                    showMessage("مرحباً ${session.fullName}! تم تسجيل دخولك كأدمين رسمي للمنصة ✓")
                }
                session.role != preferredRole -> {
                    showMessage(
                        "هذا الحساب مسجل مسبقاً كـ ${session.role.labelAr} (مقفل). لإنشاء حساب ${preferredRole.labelAr} جديد، استخدم بريداً مختلفاً."
                    )
                }
                session.role == UserRole.CRAFTSMAN -> {
                    showMessage("أهلاً ${session.fullName}! تم حفظ حسابك في Supabase كحرفي (${HirafiCategories.byId(session.craftsmanSpecialtyId).nameAr})")
                }
                else -> {
                    showMessage("أهلاً ${session.fullName}! تم حفظ حسابك في Supabase كزبون")
                }
            }
        }.onFailure { err ->
            _authUiState.value = AuthUiState(isLoading = false, message = err.message, isError = true)
            showMessage(err.message ?: "تعذر حفظ الحساب في Supabase")
        }
    }

    fun toggleCraftsmanOnline(isOnline: Boolean) {
        viewModelScope.launch {
            repository.setCraftsmanOnline(isOnline)
                .onFailure { err ->
                    showMessage(err.message ?: "تعذر تحديث حالة الاتصال في Supabase")
                }
        }
    }

    fun setCity(city: String) {
        viewModelScope.launch {
            repository.setCity(city)
            val coord = MoroccanLocations.coordForCity(city)
            _userGpsLocation.value?.let { gps ->
                if (MapRoutingService.haversineKm(gps.first, gps.second, coord.lat, coord.lng) > 150.0) {
                    _userGpsLocation.value = null
                }
            }
            selectNeighborhood(city, coord.lat, coord.lng)
        }
    }

    fun setCraftsmanSpecialty(specialtyId: String) {
        viewModelScope.launch {
            val lockedNow = repository.setSpecialty(specialtyId)
            if (lockedNow) {
                showMessage("تم تثبيت مهنتك الوحيدة (${HirafiCategories.byId(specialtyId).nameAr}) بنجاح")
            } else {
                showMessage("تم تحديد مهنتك مسبقاً — يُسمح بمهنة واحدة فقط لكل حرفي")
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, city: String) {
        viewModelScope.launch {
            repository.updateProfile(fullName, phone, city)
            val coord = MoroccanLocations.coordForCity(city)
            selectNeighborhood(city, coord.lat, coord.lng)
            showMessage("تم حفظ بيانات الملف الشخصي")
        }
    }

    fun saveFullProfile(
        fullName: String,
        phone: String,
        city: String,
        avatarUri: String?,
        bio: String,
        experienceYears: Int,
        basePriceDh: Int
    ) {
        val cleanName = fullName.trim()
        if (!LoginValidator.isValidFullName(cleanName)) {
            showMessage("يرجى إدخال الاسم الكامل (حرفان على الأقل)")
            return
        }
        val cleanPhone = PhoneValidator.normalize(phone)
        if (!PhoneValidator.isValidMoroccanPhone(cleanPhone)) {
            showMessage("يرجى إدخال رقم هاتف مغربي صحيح (مثال: 0612345678)")
            return
        }
        viewModelScope.launch {
            repository.updateProfile(
                fullName = cleanName,
                phone = cleanPhone,
                city = city,
                avatarUri = avatarUri,
                bio = bio,
                experienceYears = experienceYears,
                basePriceDh = basePriceDh
            )
            val coord = MoroccanLocations.coordForCity(city)
            selectNeighborhood(city, coord.lat, coord.lng)
            showMessage("تم حفظ الملف الشخصي بنجاح ✓")
        }
    }

    fun addPortfolioWork(title: String, description: String, photoUri: String?) {
        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            showMessage("يرجى إدخال عنوان قصير للعمل المنجز")
            return
        }
        viewModelScope.launch {
            repository.addPortfolioItem(cleanTitle, description, photoUri)
                .onSuccess { showMessage("تمت إضافة العمل إلى معرض أعمالك ✓") }
                .onFailure { showMessage(it.message ?: "تعذر حفظ العمل المنجز") }
        }
    }

    fun deletePortfolioWork(itemId: String) {
        viewModelScope.launch {
            repository.deletePortfolioItem(itemId)
            showMessage("تم حذف العمل من المعرض")
        }
    }

    fun loadCraftsmanPortfolio(craftsmanId: String) {
        _previewedCraftsmanId.value = craftsmanId
        if (repository.isSupabaseAuthenticated && !craftsmanId.startsWith("c_")) {
            viewModelScope.launch {
                val remote = repository.fetchPortfolioOnce(craftsmanId)
                if (remote.isNotEmpty()) {
                    repository.cacheRemotePortfolio(remote)
                }
            }
        }
    }

    fun submitCraftsmanVerification(
        nationalIdNumber: String,
        frontPhotoUri: String?,
        backPhotoUri: String?,
        selfiePhotoUri: String?
    ) {
        val current = userSession.value
        if (!AccountRolePolicy.canAccessIdentityVerification(current)) {
            showMessage("التحقق من الهوية مخصص لحسابات الحرفيين فقط")
            return
        }
        val normalizedCin = IdentityVerificationValidator.normalizeCin(nationalIdNumber)
        if (!IdentityVerificationValidator.isValidCin(normalizedCin)) {
            showMessage("يرجى إدخال رقم البطاقة الوطنية بشكل صحيح (مثال: BK123456)")
            return
        }
        if (frontPhotoUri.isNullOrBlank() || backPhotoUri.isNullOrBlank() || selfiePhotoUri.isNullOrBlank()) {
            showMessage("يرجى إرفاق الوجه الأمامي والخلفي للبطاقة وصورة السيلفي")
            return
        }
        viewModelScope.launch {
            repository.submitCraftsmanIdentityVerification(
                nationalIdNumber = normalizedCin,
                frontPhotoUri = frontPhotoUri,
                backPhotoUri = backPhotoUri,
                selfiePhotoUri = selfiePhotoUri
            ).onSuccess { cloudImagesUploaded ->
                if (cloudImagesUploaded) {
                    showMessage("تم رفع الوثائق إلى Supabase Storage وإرسالها لمراجعة الأدمين ✓")
                } else {
                    showMessage("تم إرسال طلب التوثيق لمراجعة الأدمين (تأكد من إنشاء Bucket hirafi-media في Supabase)")
                }
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إرسال ملف التحقق")
            }
        }
    }

    fun resetCraftsmanVerificationForUpdate() {
        val current = userSession.value
        if (!AccountRolePolicy.canAccessIdentityVerification(current)) return
        viewModelScope.launch {
            repository.resetCraftsmanIdentityVerification()
        }
    }

    fun setProductionMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setProductionMode(enabled)
                .onSuccess {
                    if (enabled) repository.clearSeededRequests()
                    showMessage(
                        if (enabled) "تم تفعيل وضع البيانات الحقيقية بالكامل ✓"
                        else "تم الرجوع لوضع العرض المختلط"
                    )
                }
                .onFailure { showMessage(it.message ?: "تعذر تحديث وضع الإنتاج") }
        }
    }

    fun reviewVerification(craftsmanId: String, approved: Boolean, rejectionReason: String = "") {
        viewModelScope.launch {
            repository.reviewCraftsmanVerification(craftsmanId, approved, rejectionReason)
                .onSuccess {
                    showMessage(if (approved) "تم اعتماد وتوثيق الحرفي بنجاح ✓" else "تم رفض الطلب وإبلاغ الحرفي بالسبب")
                }
                .onFailure { showMessage(it.message ?: "تعذر تنفيذ المراجعة") }
        }
    }

    fun toggleUserSuspended(userId: String, isCraftsman: Boolean, suspended: Boolean) {
        viewModelScope.launch {
            repository.toggleUserSuspended(userId, isCraftsman, suspended)
                .onSuccess {
                    showMessage(if (suspended) "تم تعليق الحساب" else "تم تفعيل الحساب من جديد")
                }
                .onFailure { showMessage(it.message ?: "تعذر تحديث حالة الحساب") }
        }
    }

    fun cancelRequestAsAdmin(requestId: String) {
        viewModelScope.launch {
            repository.cancelRequestAsAdmin(requestId)
                .onSuccess { showMessage("تم إلغاء الطلب من طرف الإدارة") }
                .onFailure { showMessage(it.message ?: "تعذر إلغاء الطلب") }
        }
    }

    fun addAuthorizedAdmin(email: String) {
        viewModelScope.launch {
            repository.addAuthorizedAdmin(email)
                .onSuccess { showMessage("تمت إضافة الأدمين الجديد بنجاح ✓") }
                .onFailure { showMessage(it.message ?: "تعذر إضافة البريد الإداري") }
        }
    }

    fun removeAuthorizedAdmin(email: String) {
        viewModelScope.launch {
            repository.removeAuthorizedAdmin(email)
                .onSuccess { showMessage("تمت إزالة صلاحية الأدمين عن $email") }
                .onFailure { showMessage(it.message ?: "تعذر حذف البريد الإداري") }
        }
    }

    fun refreshAdminData() {
        repository.refreshMarketplaceNow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startSupabaseSync(role: UserRole, specialtyId: String) {
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            // Global Runtime Config (Production Mode toggle)
            launch {
                repository.observeAppRuntimeConfig().collect { cfg ->
                    _appRuntimeConfig.value = cfg
                    if (cfg.productionMode) {
                        repository.clearSeededRequests()
                    }
                }
            }

            // Live Own Profile Sync (Verification decisions + Suspended status)
            launch {
                var lastStatus = userSession.value.verificationStatus
                repository.observeMyCloudProfile().collect { profile ->
                    if (profile != null) {
                        val remoteStatus = IdentityVerificationStatus.fromDb(profile.verificationStatus)
                        if (lastStatus == IdentityVerificationStatus.PENDING_REVIEW &&
                            remoteStatus != lastStatus
                        ) {
                            when (remoteStatus) {
                                IdentityVerificationStatus.VERIFIED -> {
                                    postSystemNotification(
                                        channelId = HirafiNotificationHelper.CHANNEL_ADMIN,
                                        notifId = 3101,
                                        title = "تهانينا! تم توثيق حسابك ✓",
                                        body = "اعتمدت الإدارة بطاقتك الوطنية وحصلت على شارة الحرفي الموثق."
                                    )
                                    showMessage("تم اعتماد وتوثيق هويتك من طرف الإدارة ✓")
                                }
                                IdentityVerificationStatus.REJECTED -> {
                                    val reasonText = profile.rejectionReason.ifBlank { "يرجى إعادة رفع صور أوضح." }
                                    postSystemNotification(
                                        channelId = HirafiNotificationHelper.CHANNEL_ADMIN,
                                        notifId = 3102,
                                        title = "تحديث بخصوص ملف التوثيق",
                                        body = reasonText
                                    )
                                    showMessage("تم رفض ملف التوثيق: $reasonText")
                                }
                                else -> {}
                            }
                        }
                        lastStatus = remoteStatus
                        repository.applyRemoteVerificationUpdate(
                            status = remoteStatus,
                            rejectionReason = profile.rejectionReason,
                            isSuspended = profile.isSuspended
                        )
                    }
                }
            }

            when (role) {
                UserRole.CLIENT -> {
                    launch {
                        val clientUid = supabaseAuthState.value?.uid ?: userSession.value.userId
                        repository.observeCloudClientRequests().collect { list ->
                            repository.cacheClientRequests(clientUid, list)
                        }
                    }
                    launch {
                        val center = lastCraftsmenCenter ?: MoroccanLocations.coordForCity(userSession.value.city)
                            .let { it.lat to it.lng }
                        repository.observeCloudOnlineCraftsmen(center.first, center.second)
                            .collect { cloudPins ->
                                _liveCloudCraftsmen.value = cloudPins
                            }
                    }
                    launch {
                        var baselineReady = false
                        val seenOfferIds = mutableSetOf<String>()
                        activeClientRequest
                            .flatMapLatest { req ->
                                baselineReady = false
                                seenOfferIds.clear()
                                if (req != null && req.status == RequestStatus.SEARCHING) {
                                    repository.observeCloudOffersForRequest(req.id, UserRole.CLIENT)
                                } else {
                                    emptyFlow()
                                }
                            }
                            .collect { offers ->
                                repository.cacheRemoteOffers(offers)
                                if (!baselineReady) {
                                    seenOfferIds.addAll(offers.map { it.id })
                                    baselineReady = true
                                } else {
                                    offers.filter { it.id !in seenOfferIds }.forEach { newOffer ->
                                        seenOfferIds.add(newOffer.id)
                                        postSystemNotification(
                                            channelId = HirafiNotificationHelper.CHANNEL_ORDERS,
                                            notifId = newOffer.id.hashCode(),
                                            title = "عرض جديد من ${newOffer.craftsmanName}",
                                            body = "${newOffer.offeredPrice} درهم • يصل خلال ${newOffer.etaMinutes} د"
                                        )
                                    }
                                }
                            }
                    }
                }

                UserRole.CRAFTSMAN -> {
                    launch {
                        val cityCoord = MoroccanLocations.coordForCity(userSession.value.city)
                        val validGps = _userGpsLocation.value?.takeIf { gps ->
                            MapRoutingService.haversineKm(gps.first, gps.second, cityCoord.lat, cityCoord.lng) <= 150.0
                        }
                        repository.syncCraftsmanDirectoryPresence(validGps?.first, validGps?.second)
                    }
                    launch {
                        repository.observeCloudCraftsmanAssignedRequests().collect { list ->
                            repository.cacheRemoteRequests(list)
                        }
                    }
                    launch {
                        repository.observeCloudOffersForCraftsman().collect { offers ->
                            repository.cacheRemoteOffers(offers)
                        }
                    }
                    launch {
                        var baselineReady = false
                        val seenReqIds = mutableSetOf<String>()
                        repository.observeCloudSearchingRequests(specialtyId).collect { list ->
                            repository.cacheSearchingRequestsForCategory(specialtyId, list)
                            if (!baselineReady) {
                                seenReqIds.addAll(list.map { it.id })
                                baselineReady = true
                            } else {
                                list.filter { it.id !in seenReqIds }.forEach { req ->
                                    seenReqIds.add(req.id)
                                    postSystemNotification(
                                        channelId = HirafiNotificationHelper.CHANNEL_ORDERS,
                                        notifId = req.id.hashCode(),
                                        title = "طلب ${req.categoryName} جديد قريب منك",
                                        body = "${req.title} • ${req.proposedPrice} درهم (${req.addressLabel})"
                                    )
                                }
                            }
                        }
                    }
                    launch {
                        val uid = supabaseAuthState.value?.uid
                        if (!uid.isNullOrBlank()) {
                            repository.observeCloudPortfolioForCraftsman(uid).collect { items ->
                                repository.cacheRemotePortfolio(items)
                            }
                        }
                    }
                }

                UserRole.ADMIN -> {
                    launch {
                        repository.observeAdminVerificationSubmissions().collect { list ->
                            _adminVerifications.value = list
                        }
                    }
                    launch {
                        repository.observeAdminAllUsers().collect { list ->
                            _adminUsers.value = list
                        }
                    }
                    launch {
                        repository.observeAdminAllRequests().collect { list ->
                            _adminAllRequests.value = list
                        }
                    }
                    launch {
                        repository.observeAuthorizedAdmins().collect { list ->
                            _authorizedAdmins.value = list
                        }
                    }
                }
            }

            if (role != UserRole.ADMIN) {
                launch {
                    val myUid = supabaseAuthState.value?.uid ?: userSession.value.userId
                    val activeJobChatFlow = combine(
                        _activeChatRequestId,
                        allRequests,
                        userSession
                    ) { openChatId, requests, sess ->
                        if (!openChatId.isNullOrBlank()) {
                            openChatId
                        } else if (sess.role == UserRole.CRAFTSMAN) {
                            requests.firstOrNull {
                                it.assignedCraftsmanId == sess.userId &&
                                    it.status != RequestStatus.COMPLETED &&
                                    it.status != RequestStatus.CANCELLED &&
                                    it.status != RequestStatus.SEARCHING
                            }?.id
                        } else {
                            requests.firstOrNull {
                                it.clientId == sess.userId &&
                                    it.status != RequestStatus.COMPLETED &&
                                    it.status != RequestStatus.CANCELLED &&
                                    it.status != RequestStatus.SEARCHING
                            }?.id
                        }
                    }.distinctUntilChanged()

                    var msgBaselineReady = false
                    val seenMsgIds = mutableSetOf<String>()
                    activeJobChatFlow
                        .flatMapLatest { reqId ->
                            msgBaselineReady = false
                            seenMsgIds.clear()
                            if (reqId.isNullOrBlank()) emptyFlow()
                            else repository.observeCloudMessagesForRequest(reqId).map { reqId to it }
                        }
                        .collect { (reqId, messages) ->
                            repository.cacheRemoteMessages(messages)
                            if (!msgBaselineReady) {
                                seenMsgIds.addAll(messages.map { it.id })
                                msgBaselineReady = true
                            } else {
                                val newIncoming = messages.filter { it.id !in seenMsgIds && it.senderId != myUid }
                                if (newIncoming.isNotEmpty()) {
                                    seenMsgIds.addAll(newIncoming.map { it.id })
                                    if (_activeChatRequestId.value != reqId) {
                                        _unreadChatCounts.update { map ->
                                            map + (reqId to ((map[reqId] ?: 0) + newIncoming.size))
                                        }
                                    }
                                    val latest = newIncoming.last()
                                    val strings = HirafiI18n.forLanguage(appLanguage.value)
                                    postSystemNotification(
                                        channelId = HirafiNotificationHelper.CHANNEL_MESSAGES,
                                        notifId = latest.id.hashCode(),
                                        title = "${strings.chatHeaderTitle}: ${latest.senderName}",
                                        body = latest.text,
                                        targetRoute = "chat/$reqId"
                                    )
                                }
                            }
                        }
                }

                // Notify client/craftsman on active request status changes
                launch {
                    val lastStatuses = mutableMapOf<String, RequestStatus>()
                    var baselineStatusesReady = false
                    allRequests.collect { list ->
                        val myUid = userSession.value.userId
                        val mine = list.filter { it.clientId == myUid || it.assignedCraftsmanId == myUid }
                        if (!baselineStatusesReady) {
                            mine.forEach { lastStatuses[it.id] = it.status }
                            baselineStatusesReady = true
                        } else {
                            val strings = HirafiI18n.forLanguage(appLanguage.value)
                            mine.forEach { req ->
                                val prev = lastStatuses[req.id]
                                lastStatuses[req.id] = req.status
                                if (prev != null && prev != req.status && req.status != RequestStatus.SEARCHING) {
                                    postSystemNotification(
                                        channelId = HirafiNotificationHelper.CHANNEL_ORDERS,
                                        notifId = ("status_${req.id}").hashCode(),
                                        title = req.title,
                                        body = req.status.localizedLabel(strings)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun postSystemNotification(
        channelId: String,
        notifId: Int,
        title: String,
        body: String,
        targetRoute: String? = null
    ) {
        val ctx = appContext ?: return
        HirafiNotificationHelper.showNotification(
            context = ctx,
            channelId = channelId,
            notificationId = notifId,
            title = title,
            body = body,
            targetRoute = targetRoute
        )
    }

    fun stopCloudListenersBeforeSignOut() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        trackingJob?.cancel()
        gpsStreamJob?.cancel()
        _liveCloudCraftsmen.value = emptyList()
        _adminVerifications.value = emptyList()
        _adminUsers.value = emptyList()
        _adminAllRequests.value = emptyList()
        _authorizedAdmins.value = emptyList()
        activeTrackedRequestKey = null
        locationService?.stopForegroundJobTracking()
    }

    fun logoutAccount() {
        stopCloudListenersBeforeSignOut()
        repository.clearRequestsSyncError()
        _activeClientRequestId.value = null
        _activeChatRequestId.value = null
        _unreadChatCounts.value = emptyMap()
        _pendingReviewRequest.value = null
        _userGpsLocation.value = null
        lastCraftsmenCenter = null
        _craftsmanSearchQuery.value = ""
        _searchRadiusKm.value = 5
        _onlySelectedCategoryOnMap.value = false
        _onlyCloudCraftsmen.value = false
        _draft.value = DraftRequestState()
        _liveRoute.value = LiveRouteState()
        _trackingState.update {
            it.copy(
                isTracking = false,
                isForegroundServiceActive = false,
                trackedRequestId = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            repository.logoutAccount()
            _authUiState.value = AuthUiState()
            showMessage("تم تسجيل الخروج من الحساب بنجاح")
        }
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        cloudSyncJob?.cancel()
        trackingJob?.cancel()
        gpsStreamJob?.cancel()
        locationService?.stopForegroundJobTracking()
        super.onCleared()
    }

    companion object {
        fun filterAndSortCraftsmen(
            pins: List<CraftsmanPin>,
            query: String,
            radiusKm: Int,
            sort: CraftsmanSortOption,
            onlySelectedCategory: Boolean,
            selectedCategoryId: String,
            onlyCloudCraftsmen: Boolean = false,
            clientCity: String? = null
        ): List<CraftsmanPin> {
            val q = query.trim()
            val cleanClientCity = clientCity?.trim()?.takeIf { it.isNotBlank() }
            val isSearchingByText = q.isNotEmpty()
            val filtered = pins.filter { pin ->
                val sameCityCloud = cleanClientCity != null &&
                    pin.isLiveCloud &&
                    pin.city.trim().equals(cleanClientCity, ignoreCase = true)
                val withinRadius = isSearchingByText ||
                    onlyCloudCraftsmen ||
                    sameCityCloud ||
                    radiusKm >= 20 ||
                    pin.distanceKm <= radiusKm.toDouble()
                val matchesCat = !onlySelectedCategory || pin.specialtyId == selectedCategoryId
                val matchesCloud = !onlyCloudCraftsmen || pin.isLiveCloud
                val matchesQuery = q.isEmpty() ||
                    pin.name.contains(q, ignoreCase = true) ||
                    pin.specialtyName.contains(q, ignoreCase = true) ||
                    pin.bio.contains(q, ignoreCase = true) ||
                    pin.city.contains(q, ignoreCase = true)
                withinRadius && matchesCat && matchesCloud && matchesQuery
            }
            return when (sort) {
                CraftsmanSortOption.NEAREST -> filtered.sortedWith(
                    compareBy<CraftsmanPin> { it.distanceKm }
                        .thenByDescending { it.isLiveCloud }
                )
                CraftsmanSortOption.TOP_RATED -> filtered.sortedWith(
                    compareByDescending<CraftsmanPin> { it.rating }
                        .thenByDescending { it.isLiveCloud }
                        .thenBy { it.distanceKm }
                )
                CraftsmanSortOption.LOWEST_PRICE -> filtered.sortedWith(
                    compareBy<CraftsmanPin> { it.basePriceDh }
                        .thenByDescending { it.isLiveCloud }
                        .thenBy { it.distanceKm }
                )
            }
        }

        fun factory(
            repository: HirafiRepository,
            locationService: RealTimeLocationService? = null,
            appContext: Context? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HirafiViewModel(repository, locationService, appContext?.applicationContext) as T
                }
            }
    }
}
