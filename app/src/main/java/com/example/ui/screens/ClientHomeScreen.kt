package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CraftsmanPin
import com.example.data.model.HirafiCategories
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.UserRole
import com.example.ui.components.ActiveJobSheet
import com.example.ui.components.CraftsmanDetailDialog
import com.example.ui.components.HirafiRealMap
import com.example.ui.components.IncomingOfferCard
import com.example.ui.components.LiveDispatchSonarOverlay
import com.example.ui.components.LivePulseDot
import com.example.ui.components.NearbyCraftsmenSearchSheet
import com.example.ui.components.PriceProposalBar
import com.example.ui.components.RatingDialog
import com.example.ui.components.RealtimeBiddingBottomSheet
import com.example.ui.components.SheetDragHandleBar
import com.example.ui.components.SmartTriageCarousel
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.theme.WepeSoftCard
import com.example.ui.viewmodel.HirafiViewModel
import com.example.ui.viewmodel.RealtimeBiddingViewModel

@Composable
fun ClientHomeScreen(
    viewModel: HirafiViewModel,
    biddingViewModel: RealtimeBiddingViewModel,
    onOpenDrawer: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenSafety: () -> Unit
) {
    val context = LocalContext.current
    val s = hirafiStrings()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val isSubmittingRequest by viewModel.isSubmittingRequest.collectAsStateWithLifecycle()
    val filteredCraftsmen by viewModel.filteredNearbyCraftsmen.collectAsStateWithLifecycle()
    val craftsmanSearchQuery by viewModel.craftsmanSearchQuery.collectAsStateWithLifecycle()
    val searchRadiusKm by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
    val craftsmanSort by viewModel.craftsmanSort.collectAsStateWithLifecycle()
    val onlySelectedCategoryOnMap by viewModel.onlySelectedCategoryOnMap.collectAsStateWithLifecycle()
    val onlyCloudCraftsmen by viewModel.onlyCloudCraftsmen.collectAsStateWithLifecycle()
    val cloudCraftsmenCount by viewModel.cloudCraftsmenCount.collectAsStateWithLifecycle()
    val offerSort by viewModel.offerSort.collectAsStateWithLifecycle()
    val userGps by viewModel.userGpsLocation.collectAsStateWithLifecycle()
    val cameraTarget by viewModel.cameraTarget.collectAsStateWithLifecycle()
    val liveRoute by viewModel.liveRoute.collectAsStateWithLifecycle()
    val activeRequest by viewModel.activeClientRequest.collectAsStateWithLifecycle()
    val sortedOffers by viewModel.sortedActiveOffers.collectAsStateWithLifecycle()
    val nowMillis by viewModel.tickerMillis.collectAsStateWithLifecycle()
    val pendingReview by viewModel.pendingReviewRequest.collectAsStateWithLifecycle()
    val placeResults by viewModel.placeSearchResults.collectAsStateWithLifecycle()
    val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsStateWithLifecycle()
    val previewedPortfolio by viewModel.previewedCraftsmanPortfolio.collectAsStateWithLifecycle()
    val runtimeConfig by viewModel.appRuntimeConfig.collectAsStateWithLifecycle()
    val currentSession by viewModel.userSession.collectAsStateWithLifecycle()
    val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
    val unreadChatCounts by viewModel.unreadChatCounts.collectAsStateWithLifecycle()
    val lastPastOrder = remember(allRequests, currentSession.userId) {
        allRequests.firstOrNull { req ->
            req.clientId == currentSession.userId &&
                (req.status == RequestStatus.COMPLETED || req.status == RequestStatus.CANCELLED)
        }
    }

    var showLocationPicker by remember { mutableStateOf(false) }
    var showCraftsmenSheet by remember { mutableStateOf(false) }
    var showRealtimeBiddingSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPinPreview by remember { mutableStateOf<CraftsmanPin?>(null) }
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var isPassengerSheetExpanded by remember { mutableStateOf(true) }
    var isSearchingSheetExpanded by remember { mutableStateOf(true) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.fetchCurrentGpsLocation(context)
        } else {
            viewModel.showMessage("يرجى السماح بصلاحية الموقع أو تحريك الخريطة لتحديد مكانك")
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted && userGps == null) {
            viewModel.fetchCurrentGpsLocation(context, isAutoStartup = true)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.setDraftPhotoUri(uri?.toString())
    }

    val selectedCategory = HirafiCategories.byId(draft.selectedCategoryId)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxSheetBodyHeight = (maxHeight * 0.45f).coerceAtLeast(220.dp)
        // 1. Live inDrive-speed real Mapbox map
        HirafiRealMap(
            centerLat = activeRequest?.lat ?: draft.lat,
            centerLng = activeRequest?.lng ?: draft.lng,
            cameraTarget = cameraTarget,
            onCameraTargetConsumed = { viewModel.consumeCameraTarget() },
            selectedCategoryId = draft.selectedCategoryId,
            craftsmen = filteredCraftsmen,
            activeRequest = activeRequest,
            liveRoute = liveRoute,
            userGpsLocation = userGps,
            isCraftsmanMode = false,
            onMapCenterChanged = { lat, lng -> viewModel.onMapPinMoved(context, lat, lng) },
            onCraftsmanPinClick = { pin -> selectedPinPreview = pin },
            modifier = Modifier.fillMaxSize()
        )

        if (activeRequest?.status == RequestStatus.SEARCHING) {
            val req = activeRequest!!
            LiveDispatchSonarOverlay(
                createdAtMillis = req.createdAtMillis,
                nowMillis = nowMillis,
                categoryName = req.categoryName,
                radiusKm = searchRadiusKm,
                offersCount = sortedOffers.size
            )
        }

        // 2. Crisp White & Emerald Top Floating HUD (Screens 1 & 2 look)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, WepeHairline),
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_drawer_button")
                    ) {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = "القائمة",
                            tint = ObsidianBlack
                        )
                    }
                }

                // Floating Location / Route Summary Pill (Screen 2 style)
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, WepeHairline),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .clickable { showLocationPicker = true }
                        .testTag("location_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        LivePulseDot(color = WepeEmerald, dotSize = 8.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "موقع الخدمة"
                                    AppLanguage.FRENCH -> "Lieu d'intervention"
                                    AppLanguage.ENGLISH -> "Service location"
                                },
                                fontSize = 10.sp,
                                color = Color(0xFF667085)
                            )
                            val displayLocation = when {
                                activeRequest != null -> "${activeRequest!!.addressLabel}  →  ${activeRequest!!.categoryName}"
                                draft.isResolvingAddress -> s.loading
                                else -> draft.addressLabel
                            }
                            Text(
                                text = displayLocation,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                        }
                    }
                }
            }

            // GPS Locate Pill & Nearby Pros Count Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(99.dp),
                        border = BorderStroke(1.dp, WepeHairline),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            LivePulseDot(color = WepeEmerald, dotSize = 6.dp)
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "${filteredCraftsmen.size} حرفي متاح"
                                    AppLanguage.FRENCH -> "${filteredCraftsmen.size} artisans"
                                    AppLanguage.ENGLISH -> "${filteredCraftsmen.size} pros nearby"
                                },
                                color = ObsidianBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (runtimeConfig.productionMode) {
                        Surface(
                            color = WepeEmerald,
                            shape = RoundedCornerShape(99.dp),
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "مباشر"
                                    AppLanguage.FRENCH -> "En direct"
                                    AppLanguage.ENGLISH -> "Live"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (currentSession.isSuspended) {
                        Surface(
                            color = UrgentCoral,
                            shape = RoundedCornerShape(99.dp),
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = s.accountSuspendedMsg,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, WepeHairline),
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.fetchCurrentGpsLocation(context)
                            } else {
                                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gps_locate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "تحديد موقعي عبر GPS",
                            tint = WepeEmerald
                        )
                    }
                }
            }
        }

        // 3. Bottom Interactive White & Emerald Sheets
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when {
                activeRequest == null -> {
                    // STATE A: Crisp White & Emerald Client Service Sheet (Screens 1 & 2)
                    Card(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp)
                        ) {
                            SheetDragHandleBar(
                                isExpanded = isPassengerSheetExpanded,
                                onToggle = { isPassengerSheetExpanded = !isPassengerSheetExpanded },
                                onExpandChange = { isPassengerSheetExpanded = it }
                            )

                            if (!isPassengerSheetExpanded) {
                                // Compact Peek Dock when collapsed
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = WepeSoftCard,
                                        shape = RoundedCornerShape(99.dp),
                                        border = BorderStroke(1.dp, WepeHairline),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(99.dp))
                                            .clickable { isPassengerSheetExpanded = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = selectedCategory.icon,
                                                contentDescription = null,
                                                tint = WepeEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = selectedCategory.localizedName(s),
                                                color = ObsidianBlack,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.submitServiceRequest() },
                                        enabled = !isSubmittingRequest,
                                        shape = RoundedCornerShape(99.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WepeEmerald,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("collapsed_find_craftsman_button")
                                    ) {
                                        if (isSubmittingRequest) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = s.loading,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "${s.search} • ${draft.proposedPrice} ${s.currencyDh}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isPassengerSheetExpanded,
                                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = s.clientWhereNeedCraftsman,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ObsidianBlack
                                        )
                                        Surface(
                                            color = if (isDetailsExpanded) WepeEmeraldSoft else WepeSoftCard,
                                            shape = RoundedCornerShape(99.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .clickable { isDetailsExpanded = !isDetailsExpanded }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = when (s.language) {
                                                        AppLanguage.ARABIC -> if (isDetailsExpanded) "إخفاء التفاصيل" else "صورة وتفاصيل"
                                                        AppLanguage.FRENCH -> if (isDetailsExpanded) "Masquer" else "Photo & détails"
                                                        AppLanguage.ENGLISH -> if (isDetailsExpanded) "Hide details" else "Photo & details"
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDetailsExpanded) WepeEmeraldDark else Color(0xFF475467)
                                                )
                                                Icon(
                                                    imageVector = if (isDetailsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                    contentDescription = null,
                                                    tint = if (isDetailsExpanded) WepeEmeraldDark else Color(0xFF475467),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Scrollable Body: Stacked Route & Issue Box, Option Cards, Triage, Price Proposal
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = maxSheetBodyHeight)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Screen 1 Stacked Location + Issue Input Card
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(18.dp),
                                            border = BorderStroke(1.dp, WepeHairline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { showLocationPicker = true }
                                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(WepeEmeraldSoft)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(WepeEmerald)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        text = if (draft.isResolvingAddress) s.loading else draft.addressLabel,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ObsidianBlack,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = null,
                                                        tint = WepeEmerald,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Subtle vertical route connector line
                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                                                        .width(2.dp)
                                                        .height(10.dp)
                                                        .background(WepeEmerald.copy(alpha = 0.45f))
                                                )

                                                OutlinedTextField(
                                                    value = draft.title,
                                                    onValueChange = { viewModel.updateDraftTitle(it) },
                                                    placeholder = {
                                                        Text(
                                                            s.clientIssueTitleHint,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF98A2B3)
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Search,
                                                            contentDescription = null,
                                                            tint = WepeEmerald,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = WepeEmerald,
                                                        unfocusedBorderColor = WepeEmerald.copy(alpha = 0.65f),
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("request_title_input")
                                                )
                                            }
                                        }

                                        if (lastPastOrder != null) {
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, WepeEmerald.copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { viewModel.prefillDraftFromRequest(lastPastOrder) }
                                                    .testTag("reorder_last_service_chip")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier
                                                                .size(34.dp)
                                                                .clip(CircleShape)
                                                                .background(WepeEmeraldSoft)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Replay,
                                                                contentDescription = null,
                                                                tint = WepeEmeraldDark,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        Column {
                                                            Text(
                                                                text = "إعادة طلب: ${lastPastOrder.title}",
                                                                color = ObsidianBlack,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${lastPastOrder.categoryName} • ${lastPastOrder.acceptedPrice ?: lastPastOrder.proposedPrice} ${s.currencyDh}",
                                                                color = WepeEmeraldDark,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        color = WepeEmeraldSoft,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "تكرار",
                                                            color = WepeEmeraldDark,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Screen 2 Selectable Craft Option Cards (white cards, green border when active, Fastest badge)
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            HirafiCategories.all.forEachIndexed { idx, cat ->
                                                val selected = cat.id == draft.selectedCategoryId
                                                val etaLabel = when (idx) {
                                                    0 -> "8 ${s.unitMin}"
                                                    1 -> "10 ${s.unitMin}"
                                                    2 -> "15 ${s.unitMin}"
                                                    else -> "12 ${s.unitMin}"
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (selected) WepeEmeraldSoft.copy(alpha = 0.45f) else Color.White,
                                                    border = BorderStroke(
                                                        width = if (selected) 1.8.dp else 1.dp,
                                                        color = if (selected) WepeEmerald else WepeHairline
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable { viewModel.selectCategory(cat.id) }
                                                        .testTag("category_${cat.id}")
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(if (selected) WepeEmerald else WepeSoftCard)
                                                            ) {
                                                                Icon(
                                                                    imageVector = cat.icon,
                                                                    contentDescription = cat.localizedName(s),
                                                                    tint = if (selected) Color.White else cat.accentColor,
                                                                    modifier = Modifier.size(22.dp)
                                                                )
                                                            }
                                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = cat.localizedName(s),
                                                                        color = ObsidianBlack,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 14.sp
                                                                    )
                                                                    if (idx == 0 || selected) {
                                                                        Surface(
                                                                            color = WepeEmerald,
                                                                            shape = RoundedCornerShape(6.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = when (s.language) {
                                                                                    AppLanguage.ARABIC -> if (idx == 0) "الأسرع" else "مختار"
                                                                                    AppLanguage.FRENCH -> if (idx == 0) "Plus rapide" else "Choisi"
                                                                                    AppLanguage.ENGLISH -> if (idx == 0) "Fastest" else "Selected"
                                                                                },
                                                                                color = Color.White,
                                                                                fontSize = 9.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                Text(
                                                                    text = "⏱ $etaLabel • ${cat.minRecommendedDh}–${cat.maxRecommendedDh} ${s.currencyDh}",
                                                                    fontSize = 11.sp,
                                                                    color = Color(0xFF667085)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "${cat.defaultPriceDh} ${s.currencyDh}",
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (selected) WepeEmeraldDark else ObsidianBlack
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        SmartTriageCarousel(
                                            selectedCategoryId = draft.selectedCategoryId,
                                            currentTitle = draft.title,
                                            onSelectScenario = { scenario ->
                                                viewModel.updateDraftTitle(scenario.titleAr)
                                                viewModel.updateDraftDescription(scenario.descriptionAr)
                                                viewModel.setDraftPrice(scenario.recommendedPriceDh)
                                                viewModel.toggleUrgent(scenario.isUrgentSuggested)
                                            }
                                        )

                                        AnimatedVisibility(visible = isDetailsExpanded) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = draft.description,
                                                    onValueChange = { viewModel.updateDraftDescription(it) },
                                                    placeholder = {
                                                        Text(
                                                            when (s.language) {
                                                                AppLanguage.ARABIC -> "تفاصيل إضافية للحرفي (اختياري)..."
                                                                AppLanguage.FRENCH -> "Détails supplémentaires (optionnel)..."
                                                                AppLanguage.ENGLISH -> "Extra details for the pro (optional)..."
                                                            }
                                                        )
                                                    },
                                                    maxLines = 2,
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = WepeEmerald,
                                                        unfocusedBorderColor = WepeHairline
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            photoPickerLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.dp, WepeHairline),
                                                        modifier = Modifier.height(48.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = WepeEmerald, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            when (s.language) {
                                                                AppLanguage.ARABIC -> if (draft.photoUri == null) "إرفاق صورة" else "تغيير الصورة"
                                                                AppLanguage.FRENCH -> if (draft.photoUri == null) "Ajouter photo" else "Changer"
                                                                AppLanguage.ENGLISH -> if (draft.photoUri == null) "Add Photo" else "Change"
                                                            },
                                                            color = ObsidianBlack,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Rounded.Bolt,
                                                            contentDescription = null,
                                                            tint = UrgentCoral,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(s.filterUrgent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ObsidianBlack)
                                                        Spacer(Modifier.width(6.dp))
                                                        Switch(
                                                            checked = draft.isUrgent,
                                                            onCheckedChange = { viewModel.toggleUrgent(it) },
                                                            colors = SwitchDefaults.colors(
                                                                checkedThumbColor = Color.White,
                                                                checkedTrackColor = WepeEmerald
                                                            )
                                                        )
                                                    }
                                                }

                                                if (draft.photoUri != null) {
                                                    AsyncImage(
                                                        model = draft.photoUri,
                                                        contentDescription = "Photo",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(100.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                    )
                                                }
                                            }
                                        }

                                        PriceProposalBar(
                                            priceDh = draft.proposedPrice,
                                            recommendedMin = selectedCategory.minRecommendedDh,
                                            recommendedMax = selectedCategory.maxRecommendedDh,
                                            onAdjust = { viewModel.adjustDraftPrice(it) },
                                            onSetExact = { viewModel.setDraftPrice(it) }
                                        )
                                    }

                                    // Screen 2 Sticky Payment + Primary Pill CTA + Circular Nearby Browse Button
                                    HorizontalDivider(color = WepeHairline)

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Icon(
                                                imageVector = Icons.Rounded.Payments,
                                                contentDescription = null,
                                                tint = WepeEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        items(PaymentMethod.entries) { method ->
                                            val isSelected = draft.paymentMethod == method
                                            Surface(
                                                color = if (isSelected) WepeEmeraldSoft else WepeSoftCard,
                                                shape = RoundedCornerShape(99.dp),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) WepeEmerald else WepeHairline
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(99.dp))
                                                    .clickable { viewModel.selectPaymentMethod(method) }
                                            ) {
                                                Text(
                                                    text = method.localizedLabel(s),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) WepeEmeraldDark else Color(0xFF475467),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { viewModel.submitServiceRequest() },
                                            enabled = !isSubmittingRequest,
                                            shape = RoundedCornerShape(99.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = WepeEmerald,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .testTag("find_craftsman_button")
                                        ) {
                                            if (isSubmittingRequest) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.5.dp
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = s.loading,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            } else {
                                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "${s.clientPublishRequest} • ${draft.proposedPrice} ${s.currencyDh}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        val radiusText = if (searchRadiusKm >= 20) s.all else "$searchRadiusKm ${s.unitKm}"
                                        Surface(
                                            shape = CircleShape,
                                            color = WepeEmeraldSoft,
                                            border = BorderStroke(1.5.dp, WepeEmerald),
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .clickable { showCraftsmenSheet = true }
                                                .testTag("browse_nearby_craftsmen_button")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PersonSearch,
                                                    contentDescription = "تصفح ${filteredCraftsmen.size} حرفي ($radiusText)",
                                                    tint = WepeEmeraldDark,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                activeRequest?.status == RequestStatus.SEARCHING -> {
                    val req = activeRequest!!
                    val bestPriceId = sortedOffers.minByOrNull { it.offeredPrice }?.id
                    val fastestId = sortedOffers.minByOrNull { it.etaMinutes }?.id
                    val topRatedId = sortedOffers.maxByOrNull { it.craftsmanRating }?.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (sortedOffers.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 310.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(sortedOffers, key = { it.id }) { offer ->
                                    val badge = when (offerSort) {
                                        OfferSortOption.BEST_PRICE -> if (offer.id == bestPriceId) OfferSortOption.BEST_PRICE.localizedLabel(s) else null
                                        OfferSortOption.FASTEST_ETA -> if (offer.id == fastestId) OfferSortOption.FASTEST_ETA.localizedLabel(s) else null
                                        OfferSortOption.TOP_RATED -> if (offer.id == topRatedId) OfferSortOption.TOP_RATED.localizedLabel(s) else null
                                    }
                                    IncomingOfferCard(
                                        offer = offer,
                                        clientProposedPrice = req.proposedPrice,
                                        nowMillis = nowMillis,
                                        onAccept = { viewModel.acceptOffer(offer) },
                                        onDecline = { viewModel.declineOffer(offer.id) },
                                        onCounterOffer = { price -> viewModel.counterOffer(offer, price) },
                                        highlightBadge = badge
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SheetDragHandleBar(
                                    isExpanded = isSearchingSheetExpanded,
                                    onToggle = { isSearchingSheetExpanded = !isSearchingSheetExpanded },
                                    onExpandChange = { isSearchingSheetExpanded = it }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (sortedOffers.isEmpty())
                                                s.statusSearching
                                            else
                                                "${s.clientOffersIncoming} (${sortedOffers.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ObsidianBlack
                                        )
                                        val radiusLabel = if (searchRadiusKm >= 20) s.all else "$searchRadiusKm ${s.unitKm}"
                                        Text(
                                            text = "${HirafiCategories.byId(req.categoryId).localizedName(s)} • ${req.proposedPrice} ${s.currencyDh} • $radiusLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF667085)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.cancelActiveRequest() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("cancel_searching_button")
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = s.cancel, tint = UrgentCoral)
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isSearchingSheetExpanded,
                                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    Column {
                                        Spacer(Modifier.height(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = true,
                                                    onClick = { showRealtimeBiddingSheet = true },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Rounded.Gavel,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = when (s.language) {
                                                                AppLanguage.ARABIC -> "غرفة المزايدة"
                                                                AppLanguage.FRENCH -> "Enchères"
                                                                AppLanguage.ENGLISH -> "Bidding Room"
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = WepeEmerald,
                                                        selectedLabelColor = Color.White,
                                                        selectedLeadingIconColor = Color.White
                                                    ),
                                                    modifier = Modifier.testTag("open_realtime_bidding_sheet")
                                                )
                                            }
                                            if (sortedOffers.size > 1) {
                                                items(OfferSortOption.entries) { sortOpt ->
                                                    FilterChip(
                                                        selected = offerSort == sortOpt,
                                                        onClick = { viewModel.setOfferSort(sortOpt) },
                                                        label = { Text(sortOpt.localizedLabel(s), fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = WepeEmerald,
                                                            selectedLabelColor = Color.White
                                                        ),
                                                        modifier = Modifier.testTag("offer_sort_${sortOpt.name.lowercase()}")
                                                    )
                                                }
                                            }
                                            if (searchRadiusKm < 20) {
                                                val nextRadius = if (searchRadiusKm < 10) 10 else 20
                                                val nextLabel = "+$nextRadius ${s.unitKm}"
                                                item {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = { viewModel.expandActiveSearchRadius(nextRadius) },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Rounded.NearMe,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(15.dp),
                                                                tint = WepeEmerald
                                                            )
                                                        },
                                                        label = {
                                                            Text(
                                                                nextLabel,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = WepeEmeraldDark
                                                            )
                                                        },
                                                        modifier = Modifier.testTag("expand_search_radius_button")
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.raiseActiveRequestPrice(-10) },
                                                shape = RoundedCornerShape(99.dp),
                                                border = BorderStroke(1.dp, WepeHairline),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                            ) {
                                                Text("-10 ${s.currencyDh}", fontWeight = FontWeight.Bold, color = ObsidianBlack)
                                            }
                                            Button(
                                                onClick = { viewModel.raiseActiveRequestPrice(10) },
                                                shape = RoundedCornerShape(99.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .height(48.dp)
                                                    .testTag("raise_fare_plus_10")
                                            ) {
                                                Text("+10 ${s.currencyDh}", fontWeight = FontWeight.ExtraBold)
                                            }
                                            Button(
                                                onClick = { viewModel.raiseActiveRequestPrice(20) },
                                                shape = RoundedCornerShape(99.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = WepeEmeraldDark, contentColor = Color.White),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                            ) {
                                                Text("+20 ${s.currencyDh}", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                activeRequest != null -> {
                    val req = activeRequest!!
                    ActiveJobSheet(
                        request = req,
                        liveRoute = liveRoute,
                        unreadChatCount = unreadChatCounts[req.id] ?: 0,
                        onCallClick = { phone ->
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(dialIntent)
                        },
                        onChatClick = { onOpenChat(req.id) },
                        onSafetyClick = onOpenSafety,
                        onAdvanceStatus = { nextStatus -> viewModel.advanceJobStatus(req, nextStatus) },
                        onCancel = { viewModel.cancelActiveRequest(req.id) }
                    )
                }
            }
        }
    }

    if (showLocationPicker) {
        AlertDialog(
            onDismissRequest = {
                showLocationPicker = false
                viewModel.clearPlaceSearchResults()
            },
            containerColor = Color.White,
            title = { Text("ابحث عن عنوان أو اختر حياً", fontWeight = FontWeight.Bold, color = ObsidianBlack) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن شارع أو حي...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WepeEmerald,
                                unfocusedBorderColor = WepeHairline
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.searchPlaces(context, searchQuery) }) {
                            Icon(Icons.Rounded.Search, contentDescription = "بحث", tint = WepeEmerald)
                        }
                    }

                    if (isSearchingPlaces) {
                        CircularProgressIndicator(
                            color = WepeEmerald,
                            modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp)
                        )
                    }

                    if (placeResults.isNotEmpty()) {
                        Text("نتائج البحث:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WepeEmeraldDark)
                        placeResults.forEach { res ->
                            Surface(
                                color = WepeEmeraldSoft,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, WepeEmerald.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectNeighborhood(res.displayName, res.lat, res.lng)
                                        viewModel.clearPlaceSearchResults()
                                        showLocationPicker = false
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = WepeEmeraldDark)
                                    Spacer(Modifier.width(8.dp))
                                    Text(res.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ObsidianBlack)
                                }
                            }
                        }
                        HorizontalDivider(color = WepeHairline)
                    }

                    Text("أحياء مشهورة:", fontSize = 12.sp, color = Color(0xFF667085))
                    MoroccanLocations.neighborhoods.forEach { n ->
                        Surface(
                            color = WepeSoftCard,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectNeighborhood(n.nameAr, n.lat, n.lng)
                                    showLocationPicker = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(11.dp)
                            ) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = WepeEmerald)
                                Spacer(Modifier.width(8.dp))
                                Text(n.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ObsidianBlack)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationPicker = false
                        viewModel.clearPlaceSearchResults()
                    }
                ) { Text("إغلاق", color = WepeEmeraldDark, fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showCraftsmenSheet) {
        NearbyCraftsmenSearchSheet(
            craftsmen = filteredCraftsmen,
            selectedCategoryId = draft.selectedCategoryId,
            searchQuery = craftsmanSearchQuery,
            searchRadiusKm = searchRadiusKm,
            selectedSort = craftsmanSort,
            onlySelectedCategory = onlySelectedCategoryOnMap,
            onlyCloudCraftsmen = onlyCloudCraftsmen,
            cloudCraftsmenCount = cloudCraftsmenCount,
            onSearchQueryChange = { viewModel.updateCraftsmanSearchQuery(it) },
            onSelectRadiusKm = { viewModel.setSearchRadiusKm(it) },
            onSelectSort = { viewModel.setCraftsmanSort(it) },
            onToggleOnlyCategory = { viewModel.toggleOnlyCategoryOnMap(it) },
            onToggleOnlyCloudCraftsmen = { viewModel.toggleOnlyCloudCraftsmen(it) },
            onPreviewCraftsman = { pin ->
                showCraftsmenSheet = false
                viewModel.focusCraftsmanOnMap(pin)
                selectedPinPreview = pin
            },
            onDirectRequestCraftsman = { pin ->
                showCraftsmenSheet = false
                viewModel.requestSpecificCraftsman(pin)
            },
            onDismiss = { showCraftsmenSheet = false }
        )
    }

    selectedPinPreview?.let { pin ->
        LaunchedEffect(pin.id) {
            viewModel.loadCraftsmanPortfolio(pin.id)
        }
        CraftsmanDetailDialog(
            pin = pin,
            portfolioItems = previewedPortfolio,
            onDirectRequest = { target ->
                selectedPinPreview = null
                viewModel.requestSpecificCraftsman(target)
            },
            onSelectSpecialtyOnly = { target ->
                viewModel.selectCategory(target.specialtyId)
                viewModel.setDraftPrice(target.basePriceDh)
                selectedPinPreview = null
            },
            onDismiss = { selectedPinPreview = null }
        )
    }

    val biddingRequest = activeRequest
    if (showRealtimeBiddingSheet && biddingRequest != null && biddingRequest.status == RequestStatus.SEARCHING) {
        RealtimeBiddingBottomSheet(
            viewModel = biddingViewModel,
            request = biddingRequest,
            roleOverride = UserRole.CLIENT,
            onDismiss = { showRealtimeBiddingSheet = false },
            onCompletedOrAccepted = { showRealtimeBiddingSheet = false }
        )
    }

    pendingReview?.let { completedReq ->
        RatingDialog(
            request = completedReq,
            onDismiss = { viewModel.dismissPendingReview() },
            onSubmit = { stars, tags, comment ->
                viewModel.submitReview(completedReq, stars, tags, comment)
            }
        )
    }
}
