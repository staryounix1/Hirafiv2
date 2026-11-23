package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BiddingSessionSummary
import com.example.data.model.CounterOfferSubmission
import com.example.data.model.CraftsmanBidDraft as DomainCraftsmanBidDraft
import com.example.data.model.JobPriceProposal
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.repository.HirafiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

typealias CraftsmanBidDraft = DomainCraftsmanBidDraft

sealed interface BiddingUiState {
    data object Idle : BiddingUiState
    data class Loading(val requestId: String) : BiddingUiState
    data class Active(
        val request: ServiceRequest,
        val priceProposal: JobPriceProposal,
        val summary: BiddingSessionSummary,
        val offers: List<ServiceOffer>,
        val mySubmittedOffer: ServiceOffer?,
        val session: UserSession,
        val bidDraft: CraftsmanBidDraft,
        val sortOption: OfferSortOption,
        val isSyncingRealtime: Boolean,
        val nowMillis: Long
    ) : BiddingUiState
    data class Error(val message: String) : BiddingUiState
}

private data class BiddingAuxState(
    val session: UserSession,
    val bidDraft: CraftsmanBidDraft,
    val sortOption: OfferSortOption,
    val isSyncingRealtime: Boolean,
    val nowMillis: Long
)

class RealtimeBiddingViewModel(
    private val repository: HirafiRepository
) : ViewModel() {

    private val _boundRequestId = MutableStateFlow<String?>(null)
    val boundRequestId: StateFlow<String?> = _boundRequestId.asStateFlow()

    private val _bidDraft = MutableStateFlow(CraftsmanBidDraft())
    val bidDraft: StateFlow<CraftsmanBidDraft> = _bidDraft.asStateFlow()

    private val _offerSort = MutableStateFlow(OfferSortOption.BEST_PRICE)
    val offerSort: StateFlow<OfferSortOption> = _offerSort.asStateFlow()

    private val _isSyncingRealtime = MutableStateFlow(false)
    val isSyncingRealtime: StateFlow<Boolean> = _isSyncingRealtime.asStateFlow()

    private val _statusEvent = MutableStateFlow<String?>(null)
    val statusEvent: StateFlow<String?> = _statusEvent.asStateFlow()

    private var realtimePollingJob: Job? = null
    private var cloudOffersJob: Job? = null

    private val tickerMillis: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(500L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val boundRequest: StateFlow<ServiceRequest?> = _boundRequestId
        .flatMapLatest { reqId ->
            if (reqId == null) flowOf(null) else repository.observeRequestById(reqId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val boundOffers: StateFlow<List<ServiceOffer>> = _boundRequestId
        .flatMapLatest { reqId ->
            if (reqId == null) flowOf(emptyList()) else repository.observeOffersForRequest(reqId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val auxState = combine(
        repository.userSession,
        _bidDraft,
        _offerSort,
        _isSyncingRealtime,
        tickerMillis
    ) { session, draft, sort, syncing, now ->
        BiddingAuxState(
            session = session,
            bidDraft = draft,
            sortOption = sort,
            isSyncingRealtime = syncing,
            nowMillis = now
        )
    }

    val uiState: StateFlow<BiddingUiState> = combine(
        _boundRequestId,
        boundRequest,
        boundOffers,
        auxState
    ) { reqId, request, offers, aux ->
        when {
            reqId == null -> BiddingUiState.Idle
            request == null -> BiddingUiState.Loading(reqId)
            else -> {
                val activeOffers = if (aux.session.role == UserRole.CRAFTSMAN) {
                    // Craftsman cannot view competing craftsmen's bids/offers
                    emptyList()
                } else {
                    sortAndFilterActiveOffers(
                        offers = offers,
                        sortOption = aux.sortOption,
                        nowMillis = aux.nowMillis
                    )
                }
                val myOffer = offers.firstOrNull {
                    it.craftsmanId == aux.session.userId &&
                        (it.status == OfferStatus.PENDING ||
                            it.status == OfferStatus.COUNTERED ||
                            it.status == OfferStatus.ACCEPTED)
                }
                val priceProposal = JobPriceProposal.fromRequest(request)
                val summary = BiddingSessionSummary.fromOffers(
                    role = aux.session.role,
                    offers = activeOffers,
                    clientProposedPriceDh = request.proposedPrice
                )
                BiddingUiState.Active(
                    request = request,
                    priceProposal = priceProposal,
                    summary = summary,
                    offers = activeOffers,
                    mySubmittedOffer = myOffer,
                    session = aux.session,
                    bidDraft = aux.bidDraft,
                    sortOption = aux.sortOption,
                    isSyncingRealtime = aux.isSyncingRealtime,
                    nowMillis = aux.nowMillis
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BiddingUiState.Idle)

    fun bindRequest(request: ServiceRequest) {
        if (_boundRequestId.value != request.id) {
            _boundRequestId.value = request.id
            prepareDraftForRequest(request)
            startRealtimeCloudOffers(request.id)
            startRealtimeOfferPolling(request.id)
        } else if (_bidDraft.value.requestId != request.id) {
            prepareDraftForRequest(request)
        }
    }

    fun unbindRequest() {
        _boundRequestId.value = null
        cloudOffersJob?.cancel()
        cloudOffersJob = null
        realtimePollingJob?.cancel()
        realtimePollingJob = null
    }

    fun prepareDraftForRequest(request: ServiceRequest) {
        _bidDraft.value = CraftsmanBidDraft(
            requestId = request.id,
            offeredPrice = (request.proposedPrice + 10).coerceAtLeast(50),
            etaMinutes = 10,
            note = "",
            isSubmitting = false
        )
    }

    fun adjustDraftPrice(delta: Int) {
        _bidDraft.update { current ->
            current.copy(offeredPrice = (current.offeredPrice + delta).coerceIn(40, 5000))
        }
    }

    fun setDraftPrice(price: Int) {
        _bidDraft.update { current ->
            current.copy(offeredPrice = price.coerceIn(40, 5000))
        }
    }

    fun setDraftEtaMinutes(etaMinutes: Int) {
        _bidDraft.update { current ->
            current.copy(etaMinutes = etaMinutes.coerceIn(3, 120))
        }
    }

    fun updateDraftNote(note: String) {
        _bidDraft.update { current ->
            current.copy(note = note)
        }
    }

    fun setOfferSort(sortOption: OfferSortOption) {
        _offerSort.value = sortOption
    }

    fun submitCraftsmanBid() {
        val activeState = uiState.value as? BiddingUiState.Active ?: return
        val draft = activeState.bidDraft
        if (draft.isSubmitting) return

        viewModelScope.launch {
            _bidDraft.update { it.copy(isSubmitting = true) }
            runCatching {
                repository.submitCraftsmanBid(
                    requestId = activeState.request.id,
                    offeredPrice = draft.offeredPrice,
                    etaMinutes = draft.etaMinutes,
                    note = draft.note
                )
            }.onSuccess {
                _bidDraft.update { it.copy(isSubmitting = false, note = "") }
                _statusEvent.value = "تم إرسال عرضك المباشر (${draft.offeredPrice} د.م) للزبون"
            }.onFailure { err ->
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = err.message ?: "تعذر إرسال العرض، حاول مرة أخرى"
            }
        }
    }

    fun submitQuickCraftsmanBid(offeredPrice: Int, etaMinutes: Int = 10, note: String = "") {
        val activeState = uiState.value as? BiddingUiState.Active ?: return
        val clamped = offeredPrice.coerceIn(40, 5000)
        viewModelScope.launch {
            _bidDraft.update { it.copy(isSubmitting = true, offeredPrice = clamped, etaMinutes = etaMinutes) }
            runCatching {
                repository.submitCraftsmanBid(
                    requestId = activeState.request.id,
                    offeredPrice = clamped,
                    etaMinutes = etaMinutes,
                    note = note.ifBlank { "عرض سريع من الحرفي" }
                )
            }.onSuccess {
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = "تم إرسال عرض سعر $clamped د.م للزبون"
            }.onFailure { err ->
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = err.message ?: "تعذر إرسال العرض"
            }
        }
    }

    fun acceptAtClientPrice() {
        val activeState = uiState.value as? BiddingUiState.Active ?: return
        viewModelScope.launch {
            _bidDraft.update { it.copy(isSubmitting = true) }
            runCatching {
                repository.craftsmanDirectAcceptRequest(activeState.request.id)
            }.onSuccess {
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = "تم قبول الطلب بسعر ${activeState.request.proposedPrice} د.م!"
            }.onFailure { err ->
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = err.message ?: "تعذر قبول الطلب"
            }
        }
    }

    fun acceptOffer(offer: ServiceOffer) {
        viewModelScope.launch {
            runCatching {
                repository.acceptOffer(offer)
            }.onSuccess {
                _statusEvent.value = "تم قبول عرض ${offer.craftsmanName} بسعر ${offer.offeredPrice} د.م"
            }.onFailure { err ->
                _statusEvent.value = err.message ?: "تعذر قبول العرض"
            }
        }
    }

    fun declineOffer(offerId: String) {
        viewModelScope.launch {
            runCatching {
                repository.declineOffer(offerId)
            }.onFailure { err ->
                _statusEvent.value = err.message ?: "تعذر رفض العرض"
            }
        }
    }

    fun counterOffer(offer: ServiceOffer, counterPrice: Int) {
        val safePrice = counterPrice.coerceIn(40, 5000)
        viewModelScope.launch {
            runCatching {
                repository.counterOffer(offer, safePrice)
            }.onSuccess {
                _statusEvent.value = "تم إرسال عرض مضاد ($safePrice د.م) إلى ${offer.craftsmanName}"
            }.onFailure { err ->
                _statusEvent.value = err.message ?: "تعذر إرسال العرض المضاد"
            }
        }
    }

    fun acceptCounterOfferAsCraftsman(offer: ServiceOffer) {
        viewModelScope.launch {
            _bidDraft.update { it.copy(isSubmitting = true) }
            runCatching {
                repository.acceptOffer(offer)
            }.onSuccess {
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = "تمت الموافقة على السعر المضاد (${offer.offeredPrice} د.م)!"
            }.onFailure { err ->
                _bidDraft.update { it.copy(isSubmitting = false) }
                _statusEvent.value = err.message ?: "تعذر قبول العرض المضاد"
            }
        }
    }

    fun submitCounterOffer(submission: CounterOfferSubmission) {
        val activeState = uiState.value as? BiddingUiState.Active ?: return
        val safePrice = activeState.priceProposal.clampedPrice(submission.proposedPriceDh)
        if (submission.fromRole == UserRole.CRAFTSMAN) {
            submitQuickCraftsmanBid(
                offeredPrice = safePrice,
                etaMinutes = submission.etaMinutes,
                note = submission.note
            )
        } else {
            val targetOffer = activeState.offers.firstOrNull { it.id == submission.targetOfferId } ?: return
            counterOffer(targetOffer, safePrice)
        }
    }

    fun adjustClientProposedPrice(delta: Int) {
        val activeState = uiState.value as? BiddingUiState.Active ?: return
        val newPrice = (activeState.request.proposedPrice + delta).coerceIn(50, 5000)
        setClientProposedPrice(newPrice)
    }

    fun setClientProposedPrice(price: Int) {
        val activeState = uiState.value as? BiddingUiState.Active ?: return
        val newPrice = activeState.priceProposal.clampedPrice(price).coerceAtLeast(50)
        viewModelScope.launch {
            runCatching {
                repository.raiseRequestPrice(activeState.request.id, newPrice)
            }.onSuccess {
                _statusEvent.value = "تم تحديث السعر المقترح إلى $newPrice د.م"
            }.onFailure { err ->
                _statusEvent.value = err.message ?: "تعذر تحديث السعر المقترح"
            }
        }
    }

    fun refreshOffersNow() {
        val reqId = _boundRequestId.value ?: return
        viewModelScope.launch {
            _isSyncingRealtime.value = true
            repository.syncOffersForRequestFromSupabase(reqId)
            _isSyncingRealtime.value = false
        }
    }

    fun consumeStatusEvent() {
        _statusEvent.value = null
    }

    private fun startRealtimeCloudOffers(requestId: String) {
        cloudOffersJob?.cancel()
        if (!repository.isSupabaseAuthenticated) return
        cloudOffersJob = viewModelScope.launch {
            val session = repository.userSession.first()
            repository.observeCloudOffersForRequest(requestId, session.role)
                .collect { remoteOffers ->
                    repository.cacheRemoteOffers(remoteOffers)
                }
        }
    }

    private fun startRealtimeOfferPolling(requestId: String) {
        realtimePollingJob?.cancel()
        realtimePollingJob = viewModelScope.launch {
            while (isActive && _boundRequestId.value == requestId) {
                val currentReq = boundRequest.value
                if (currentReq == null || currentReq.status == RequestStatus.SEARCHING) {
                    if (!repository.isSupabaseAuthenticated) {
                        _isSyncingRealtime.value = true
                        repository.syncOffersForRequestFromSupabase(requestId)
                        _isSyncingRealtime.value = false
                    }
                }
                delay(3500L)
            }
        }
    }

    override fun onCleared() {
        cloudOffersJob?.cancel()
        realtimePollingJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun sortAndFilterActiveOffers(
            offers: List<ServiceOffer>,
            sortOption: OfferSortOption,
            nowMillis: Long
        ): List<ServiceOffer> {
            val active = offers.filter { offer ->
                val activeStatus = offer.status == OfferStatus.PENDING || offer.status == OfferStatus.COUNTERED
                val notExpired = offer.expiresAtMillis > nowMillis || offer.status == OfferStatus.COUNTERED
                activeStatus && notExpired
            }
            return when (sortOption) {
                OfferSortOption.BEST_PRICE -> active.sortedWith(
                    compareBy<ServiceOffer> { it.offeredPrice }.thenBy { it.etaMinutes }
                )
                OfferSortOption.FASTEST_ETA -> active.sortedWith(
                    compareBy<ServiceOffer> { it.etaMinutes }.thenBy { it.offeredPrice }
                )
                OfferSortOption.TOP_RATED -> active.sortedWith(
                    compareByDescending<ServiceOffer> { it.craftsmanRating }.thenBy { it.offeredPrice }
                )
            }
        }

        fun factory(repository: HirafiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RealtimeBiddingViewModel(repository) as T
                }
            }
    }
}
