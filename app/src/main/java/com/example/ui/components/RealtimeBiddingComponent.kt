package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BiddingSessionSummary
import com.example.data.model.CraftsmanBidDraft
import com.example.data.model.FairPriceAssessment
import com.example.data.model.JobPriceProposal
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.theme.WepeSoftCard
import com.example.ui.viewmodel.BiddingUiState
import com.example.ui.viewmodel.RealtimeBiddingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeBiddingBottomSheet(
    viewModel: RealtimeBiddingViewModel,
    request: ServiceRequest,
    roleOverride: UserRole? = null,
    onDismiss: () -> Unit,
    onCompletedOrAccepted: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        RealtimeBiddingComponent(
            viewModel = viewModel,
            request = request,
            roleOverride = roleOverride,
            onCompletedOrAccepted = onCompletedOrAccepted,
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun RealtimeBiddingComponent(
    viewModel: RealtimeBiddingViewModel,
    request: ServiceRequest,
    modifier: Modifier = Modifier,
    roleOverride: UserRole? = null,
    onCompletedOrAccepted: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    val s = hirafiStrings()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(request.id) {
        viewModel.bindRequest(request)
    }

    DisposableEffect(request.id) {
        onDispose {
            viewModel.unbindRequest()
        }
    }

    LaunchedEffect(uiState) {
        val active = uiState as? BiddingUiState.Active ?: return@LaunchedEffect
        if (active.request.status != RequestStatus.SEARCHING) {
            onCompletedOrAccepted()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "live_bidding_pulse")
    val liveDotColor by pulseTransition.animateColor(
        initialValue = WepeEmerald,
        targetValue = Color(0xFF00D962),
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_bidding_component"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = WepeEmeraldSoft,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Gavel,
                            contentDescription = null,
                            tint = WepeEmeraldDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(liveDotColor)
                        )
                        Text(
                            text = when (s.language) {
                                AppLanguage.ARABIC -> "غرفة المزايدة المباشرة"
                                AppLanguage.FRENCH -> "Salle d'enchères en direct"
                                AppLanguage.ENGLISH -> "Live Bidding Room"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                    }
                    val startLabel = when (s.language) {
                        AppLanguage.ARABIC -> "السعر الابتدائي"
                        AppLanguage.FRENCH -> "Prix initial"
                        AppLanguage.ENGLISH -> "Starting price"
                    }
                    Text(
                        text = "${request.categoryName} • $startLabel ${request.proposedPrice} ${s.currencyDh}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val syncing = (uiState as? BiddingUiState.Active)?.isSyncingRealtime == true
                IconButton(
                    onClick = { viewModel.refreshOffersNow() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_bidding_button")
                ) {
                    if (syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = WepeEmerald, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = s.refresh, tint = ObsidianBlack)
                    }
                }
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_bidding_sheet_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = s.close, tint = ObsidianBlack)
                    }
                }
            }
        }

        // Request Brief Card
        Surface(
            color = WepeSoftCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WepeHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${request.clientName} • ${request.addressLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = WepeEmerald,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = "${(uiState as? BiddingUiState.Active)?.request?.proposedPrice ?: request.proposedPrice} ${s.currencyDh}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when (val state = uiState) {
            is BiddingUiState.Idle, is BiddingUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    CircularProgressIndicator(color = WepeEmerald)
                }
            }

            is BiddingUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is BiddingUiState.Active -> {
                val effectiveRole = roleOverride ?: state.session.role
                if (effectiveRole == UserRole.CRAFTSMAN) {
                    CraftsmanBidSubmissionView(
                        request = state.request,
                        priceProposal = state.priceProposal,
                        draft = state.bidDraft,
                        myOffer = state.mySubmittedOffer,
                        nowMillis = state.nowMillis,
                        onAdjustPrice = viewModel::adjustDraftPrice,
                        onSetPrice = viewModel::setDraftPrice,
                        onSelectEta = viewModel::setDraftEtaMinutes,
                        onNoteChange = viewModel::updateDraftNote,
                        onSubmitBid = viewModel::submitCraftsmanBid,
                        onAcceptAtClientPrice = viewModel::acceptAtClientPrice,
                        onAcceptCounterOffer = viewModel::acceptCounterOfferAsCraftsman
                    )
                } else {
                    ClientRealtimeOffersView(
                        request = state.request,
                        priceProposal = state.priceProposal,
                        summary = state.summary,
                        offers = state.offers,
                        sortOption = state.sortOption,
                        nowMillis = state.nowMillis,
                        onSelectSort = viewModel::setOfferSort,
                        onAcceptOffer = viewModel::acceptOffer,
                        onDeclineOffer = viewModel::declineOffer,
                        onCounterOffer = viewModel::counterOffer,
                        onAdjustClientPrice = viewModel::adjustClientProposedPrice,
                        onSetClientPrice = viewModel::setClientProposedPrice
                    )
                }
            }
        }
    }
}

@Composable
private fun CraftsmanBidSubmissionView(
    request: ServiceRequest,
    priceProposal: JobPriceProposal,
    draft: CraftsmanBidDraft,
    myOffer: ServiceOffer?,
    nowMillis: Long,
    onAdjustPrice: (Int) -> Unit,
    onSetPrice: (Int) -> Unit,
    onSelectEta: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmitBid: () -> Unit,
    onAcceptAtClientPrice: () -> Unit,
    onAcceptCounterOffer: (ServiceOffer) -> Unit
) {
    val s = hirafiStrings()
    var showCustomCraftsmanPriceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (myOffer != null) {
            val remainingSec = ((myOffer.expiresAtMillis - nowMillis) / 1000L).coerceAtLeast(0L)
            val isCountered = myOffer.status == OfferStatus.COUNTERED
            Surface(
                color = if (isCountered) StarAmber.copy(alpha = 0.14f) else WepeEmeraldSoft,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isCountered) 1.5.dp else 1.2.dp,
                        color = if (isCountered) StarAmber else WepeEmerald.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag(if (isCountered) "craftsman_counter_offer_card" else "craftsman_submitted_offer_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> if (isCountered) "عرض مضاد من الزبون: ${myOffer.offeredPrice} ${s.currencyDh}" else "عرضك المرسل حالياً: ${myOffer.offeredPrice} ${s.currencyDh}"
                                    AppLanguage.FRENCH -> if (isCountered) "Contre-offre client : ${myOffer.offeredPrice} ${s.currencyDh}" else "Votre offre envoyée : ${myOffer.offeredPrice} ${s.currencyDh}"
                                    AppLanguage.ENGLISH -> if (isCountered) "Client counter-offer: ${myOffer.offeredPrice} ${s.currencyDh}" else "Your active offer: ${myOffer.offeredPrice} ${s.currencyDh}"
                                },
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${myOffer.status.localizedLabel(s)} • ${myOffer.etaMinutes} ${s.unitMin} • ${remainingSec}s",
                                fontSize = 11.sp,
                                color = WepeEmeraldDark
                            )
                        }
                        Surface(
                            color = WepeEmerald,
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = if (isCountered) s.offerCountered else when (s.language) {
                                    AppLanguage.ARABIC -> "عرضك الخاص"
                                    AppLanguage.FRENCH -> "Votre offre"
                                    AppLanguage.ENGLISH -> "Your Offer"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isCountered) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onAcceptCounterOffer(myOffer) },
                                enabled = !draft.isSubmitting,
                                shape = RoundedCornerShape(99.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WepeEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("accept_client_counter_offer_button")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${s.confirm} (${myOffer.offeredPrice} ${s.currencyDh})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    onSetPrice(priceProposal.clampedPrice(myOffer.offeredPrice + 10))
                                },
                                shape = RoundedCornerShape(99.dp),
                                border = BorderStroke(1.dp, WepeHairline),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("counter_back_button")
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = when (s.language) {
                                        AppLanguage.ARABIC -> "عرض آخر"
                                        AppLanguage.FRENCH -> "Autre offre"
                                        AppLanguage.ENGLISH -> "Counter"
                                    },
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Price Stepper Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WepeSoftCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WepeHairline, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (s.language) {
                            AppLanguage.ARABIC -> "حدّد سعر عرضك المضاد"
                            AppLanguage.FRENCH -> "Fixez votre offre"
                            AppLanguage.ENGLISH -> "Set your bid price"
                        },
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    Text(
                        text = "${s.roleClient}: ${request.proposedPrice} ${s.currencyDh}",
                        fontSize = 12.sp,
                        color = WepeEmeraldDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onAdjustPrice(-10) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WepeHairline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = ObsidianBlack),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bidding_minus_10")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "-10", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("10", fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.6.dp, WepeEmerald),
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showCustomCraftsmanPriceDialog = true }
                            .testTag("craftsman_price_display_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            Text(
                                text = "${draft.offeredPrice} ${s.currencyDh}",
                                color = ObsidianBlack,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = s.save,
                                tint = WepeEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onAdjustPrice(10) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WepeHairline),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = ObsidianBlack),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bidding_plus_10")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "+10", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("10", fontWeight = FontWeight.Bold)
                    }
                }

                val bidProposal = priceProposal.copy(proposedPriceDh = draft.offeredPrice)
                FairPriceIndicatorPill(priceProposal = bidProposal)

                // Quick Price Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        request.proposedPrice to "${s.roleClient} (${request.proposedPrice})",
                        (request.proposedPrice + 15) to "+15 ${s.currencyDh}",
                        (request.proposedPrice + 30) to "+30 ${s.currencyDh}",
                        (request.proposedPrice + 50) to "+50 ${s.currencyDh}"
                    )
                    items(presets) { (price, label) ->
                        FilterChip(
                            selected = draft.offeredPrice == price,
                            onClick = { onSetPrice(price) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WepeEmeraldSoft,
                                selectedLabelColor = WepeEmeraldDark
                            )
                        )
                    }
                }
            }
        }

        // ETA Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = WepeEmerald,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = when (s.language) {
                    AppLanguage.ARABIC -> "وقت الوصول:"
                    AppLanguage.FRENCH -> "Arrivée :"
                    AppLanguage.ENGLISH -> "ETA:"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ObsidianBlack
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(5, 10, 15, 20, 30)) { mins ->
                    FilterChip(
                        selected = draft.etaMinutes == mins,
                        onClick = { onSelectEta(mins) },
                        label = { Text("$mins ${s.unitMin}", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WepeEmerald,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("eta_chip_$mins")
                    )
                }
            }
        }

        OutlinedTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            label = {
                Text(
                    when (s.language) {
                        AppLanguage.ARABIC -> "ملاحظة أو تفاصيل للزبون (اختياري)"
                        AppLanguage.FRENCH -> "Note pour le client (optionnel)"
                        AppLanguage.ENGLISH -> "Note for client (optional)"
                    }
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WepeEmerald,
                unfocusedBorderColor = WepeHairline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("craftsman_bid_note_input")
        )

        Button(
            onClick = onSubmitBid,
            enabled = !draft.isSubmitting,
            shape = RoundedCornerShape(99.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_craftsman_bid_button")
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (draft.isSubmitting) s.loading else "${s.craftsmanSendOffer} (${draft.offeredPrice} ${s.currencyDh})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        OutlinedButton(
            onClick = onAcceptAtClientPrice,
            enabled = !draft.isSubmitting,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("bidding_accept_client_price_button")
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            val acceptNowLabel = when (s.language) {
                AppLanguage.ARABIC -> "قبول فوري بسعر الزبون"
                AppLanguage.FRENCH -> "Accepter au prix client"
                AppLanguage.ENGLISH -> "Accept at client price"
            }
            Text(
                text = "$acceptNowLabel (${request.proposedPrice} ${s.currencyDh})",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showCustomCraftsmanPriceDialog) {
        CustomPriceInputDialog(
            title = s.craftsmanSendOffer,
            initialPrice = draft.offeredPrice,
            onDismiss = { showCustomCraftsmanPriceDialog = false },
            onConfirm = { newPrice ->
                onSetPrice(newPrice)
                showCustomCraftsmanPriceDialog = false
            }
        )
    }
}

@Composable
private fun ClientRealtimeOffersView(
    request: ServiceRequest,
    priceProposal: JobPriceProposal,
    summary: BiddingSessionSummary,
    offers: List<ServiceOffer>,
    sortOption: OfferSortOption,
    nowMillis: Long,
    onSelectSort: (OfferSortOption) -> Unit,
    onAcceptOffer: (ServiceOffer) -> Unit,
    onDeclineOffer: (String) -> Unit,
    onCounterOffer: (ServiceOffer, Int) -> Unit,
    onAdjustClientPrice: (Int) -> Unit,
    onSetClientPrice: (Int) -> Unit
) {
    val s = hirafiStrings()
    var showCustomClientPriceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (summary.activeOfferCount > 0) {
            BiddingSessionSummaryStrip(summary = summary)
        }

        if (offers.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(OfferSortOption.entries) { opt ->
                    FilterChip(
                        selected = sortOption == opt,
                        onClick = { onSelectSort(opt) },
                        label = { Text(opt.localizedLabel(s), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WepeEmerald,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("bidding_sort_${opt.name.lowercase()}")
                    )
                }
            }
        }

        if (offers.isEmpty()) {
            Surface(
                color = WepeSoftCard,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = WepeEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = when (s.language) {
                            AppLanguage.ARABIC -> "في انتظار عروض الحرفيين المباشرة..."
                            AppLanguage.FRENCH -> "En attente des offres en direct..."
                            AppLanguage.ENGLISH -> "Waiting for live craftsman offers..."
                        },
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when (s.language) {
                            AppLanguage.ARABIC -> "ستظهر العروض فور وصولها ويمكنك قبولها أو إرسال عرض مضاد."
                            AppLanguage.FRENCH -> "Les offres apparaîtront ici dès leur réception."
                            AppLanguage.ENGLISH -> "Offers appear immediately so you can accept or counter."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                }
            }
        } else {
            val bestPriceId = offers.minByOrNull { it.offeredPrice }?.id
            val fastestId = offers.minByOrNull { it.etaMinutes }?.id
            val topRatedId = offers.maxByOrNull { it.craftsmanRating }?.id

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(offers, key = { it.id }) { offer ->
                    val badge = when (sortOption) {
                        OfferSortOption.BEST_PRICE -> if (offer.id == bestPriceId) OfferSortOption.BEST_PRICE.localizedLabel(s) else null
                        OfferSortOption.FASTEST_ETA -> if (offer.id == fastestId) OfferSortOption.FASTEST_ETA.localizedLabel(s) else null
                        OfferSortOption.TOP_RATED -> if (offer.id == topRatedId) OfferSortOption.TOP_RATED.localizedLabel(s) else null
                    }
                    IncomingOfferCard(
                        offer = offer,
                        clientProposedPrice = request.proposedPrice,
                        nowMillis = nowMillis,
                        onAccept = { onAcceptOffer(offer) },
                        onDecline = { onDeclineOffer(offer.id) },
                        onCounterOffer = { price -> onCounterOffer(offer, price) },
                        highlightBadge = badge
                    )
                }
            }
        }

        HorizontalDivider(color = WepeHairline)

        FairPriceIndicatorPill(priceProposal = priceProposal)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onAdjustClientPrice(-10) },
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, WepeHairline),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("bidding_client_minus_10")
            ) {
                Text("-10", fontWeight = FontWeight.Bold, color = ObsidianBlack)
            }
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.6.dp, WepeEmerald),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .clickable { showCustomClientPriceDialog = true }
                    .testTag("bidding_client_price_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "${request.proposedPrice} ${s.currencyDh}",
                        color = ObsidianBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = s.clientYourProposedPrice,
                        tint = WepeEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Button(
                onClick = { onAdjustClientPrice(10) },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("bidding_client_plus_10")
            ) {
                Text("+10 ${s.currencyDh}", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onAdjustClientPrice(20) },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WepeEmeraldDark, contentColor = Color.White),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("bidding_client_plus_20")
            ) {
                Text("+20", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCustomClientPriceDialog) {
        CustomPriceInputDialog(
            title = s.clientYourProposedPrice,
            initialPrice = request.proposedPrice,
            onDismiss = { showCustomClientPriceDialog = false },
            onConfirm = { newPrice ->
                onSetClientPrice(newPrice)
                showCustomClientPriceDialog = false
            }
        )
    }
}

@Composable
fun FairPriceIndicatorPill(
    priceProposal: JobPriceProposal,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val assessment = priceProposal.assessFairness()
    val (bg, fg) = when (assessment) {
        FairPriceAssessment.LOW -> UrgentCoral.copy(alpha = 0.12f) to UrgentCoral
        FairPriceAssessment.FAIR -> VerifiedEmeraldSoft to WepeEmeraldDark
        FairPriceAssessment.GENEROUS -> WepeEmeraldSoft to WepeEmeraldDark
    }

    val assessmentLabel = when (s.language) {
        AppLanguage.ARABIC -> "${assessment.labelAr} • ${assessment.hintAr}"
        AppLanguage.FRENCH -> when (assessment) {
            FairPriceAssessment.LOW -> "Prix bas • Augmentez pour plus d'offres"
            FairPriceAssessment.FAIR -> "Prix équitable • Bon taux d'acceptation"
            FairPriceAssessment.GENEROUS -> "Prix attractif • Réponse rapide"
        }
        AppLanguage.ENGLISH -> when (assessment) {
            FairPriceAssessment.LOW -> "Low offer • Raise price for faster bids"
            FairPriceAssessment.FAIR -> "Fair price • Balanced market range"
            FairPriceAssessment.GENEROUS -> "Great offer • Fast craftsman response"
        }
    }
    val marketPrefix = when (s.language) {
        AppLanguage.ARABIC -> "السوق"
        AppLanguage.FRENCH -> "Marché"
        AppLanguage.ENGLISH -> "Market"
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fair_price_indicator")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = assessmentLabel,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$marketPrefix: ${priceProposal.recommendedMinDh}–${priceProposal.recommendedMaxDh} ${s.currencyDh}",
                color = fg.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BiddingSessionSummaryStrip(
    summary: BiddingSessionSummary,
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("bidding_session_summary_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val offersWord = when (s.language) {
                AppLanguage.ARABIC -> "${summary.activeOfferCount} عروض متاحة"
                AppLanguage.FRENCH -> "${summary.activeOfferCount} offres"
                AppLanguage.ENGLISH -> "${summary.activeOfferCount} offers"
            }
            Text(
                text = offersWord,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            summary.lowestPriceDh?.let { lowest ->
                Text(
                    text = "${s.sortLowestPrice}: $lowest ${s.currencyDh}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = VerifiedEmerald
                )
            }
            summary.fastestEtaMinutes?.let { fastest ->
                Text(
                    text = "${s.sortFastestEta}: $fastest ${s.unitMin}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomPriceInputDialog(
    title: String,
    initialPrice: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val s = hirafiStrings()
    var input by remember { mutableStateOf(initialPrice.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold, color = ObsidianBlack) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.all { ch -> ch.isDigit() }) input = it },
                label = { Text("${s.clientYourProposedPrice} (${s.currencyDh})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WepeEmerald,
                    unfocusedBorderColor = WepeHairline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_price_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = input.toIntOrNull()
                    if (parsed != null) {
                        onConfirm(parsed)
                    } else {
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_custom_price_button")
            ) {
                Text(s.confirm, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel, color = Color(0xFF475467)) }
        }
    )
}

