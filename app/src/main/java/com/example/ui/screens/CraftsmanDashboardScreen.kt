package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.location.LiveTrackingState
import com.example.data.model.HirafiCategories
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.LiveRouteState
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.remote.MapRoutingService
import com.example.ui.components.CraftsmanHandshakePinVerifier
import com.example.ui.components.CraftsmanRequestFilter
import com.example.ui.components.HirafiProAnalytics
import com.example.ui.components.HirafiRealMap
import com.example.ui.components.JobMilestoneTimeline
import com.example.ui.components.LivePulseDot
import com.example.ui.components.ObsidianHeroBrush
import com.example.ui.components.RealtimeBiddingBottomSheet
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.localizedName
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.UrgentCoralSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.theme.VoltGlowAccent
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.example.ui.viewmodel.HirafiViewModel
import com.example.ui.viewmodel.RealtimeBiddingViewModel

@Composable
fun CraftsmanDashboardScreen(
    viewModel: HirafiViewModel,
    biddingViewModel: RealtimeBiddingViewModel,
    onOpenDrawer: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    val s = hirafiStrings()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
    val openRequests by viewModel.craftsmanOpenRequests.collectAsStateWithLifecycle()
    val userGps by viewModel.userGpsLocation.collectAsStateWithLifecycle()
    val cameraTarget by viewModel.cameraTarget.collectAsStateWithLifecycle()
    val liveRoute by viewModel.liveRoute.collectAsStateWithLifecycle()
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val cloudSyncError by viewModel.cloudRequestsSyncError.collectAsStateWithLifecycle()
    val craftsmanOffersByRequest by viewModel.craftsmanSubmittedOffers.collectAsStateWithLifecycle()
    val unreadChatCounts by viewModel.unreadChatCounts.collectAsStateWithLifecycle()
    val nowMillis by viewModel.tickerMillis.collectAsStateWithLifecycle()

    var isMapMode by remember { mutableStateOf(false) }
    var selectedMapRequest by remember { mutableStateOf<ServiceRequest?>(null) }
    var customBidTarget by remember { mutableStateOf<ServiceRequest?>(null) }
    var selectedRequestFilter by rememberSaveable { mutableStateOf(CraftsmanRequestFilter.ALL) }

    val craftsmanLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchCurrentGpsLocation(context)
            viewModel.refreshLocationTracking(context)
        } else {
            viewModel.showMessage("يرجى السماح بصلاحية الموقع لتحديد مكانك")
        }
    }

    val activeCraftsmanJob = allRequests.firstOrNull {
        it.assignedCraftsmanId == session.userId &&
            it.categoryId == session.craftsmanSpecialtyId &&
            it.status != RequestStatus.COMPLETED &&
            it.status != RequestStatus.CANCELLED
    }

    LaunchedEffect(activeCraftsmanJob?.id, activeCraftsmanJob?.status) {
        if (activeCraftsmanJob != null) {
            viewModel.refreshLocationTracking(context)
        }
    }

    LaunchedEffect(
        session.userId,
        session.craftsmanSpecialtyId,
        session.isCraftsmanOnline,
        session.isCraftsmanSpecialtyLocked
    ) {
        if (session.isCraftsmanOnline && session.isCraftsmanSpecialtyLocked) {
            viewModel.refreshCraftsmanRequests()
        }
    }

    val completedToday = allRequests.filter {
        it.status == RequestStatus.COMPLETED &&
            it.assignedCraftsmanId == session.userId &&
            it.categoryId == session.craftsmanSpecialtyId
    }
    val totalEarningsDh = completedToday.sumOf { it.acceptedPrice ?: it.proposedPrice }
    val cityCoord = MoroccanLocations.coordForCity(session.city)
    val lockedCategory = HirafiCategories.byId(session.craftsmanSpecialtyId)
    val localGpsOrCity = userGps?.takeIf { gps ->
        MapRoutingService.haversineKm(gps.first, gps.second, cityCoord.lat, cityCoord.lng) <= 150.0
    } ?: (cityCoord.lat to cityCoord.lng)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Authentic inDrive Driver Header with Obsidian Gradient Depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianHeroBrush)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("craftsman_drawer_button")
                    ) {
                        Icon(Icons.Rounded.Menu, contentDescription = "القائمة", tint = Color.White)
                    }

                    // Iconic Online/Offline Emerald Pill
                    Surface(
                        color = if (session.isCraftsmanOnline) WepeEmerald else Color(0xFF262A31),
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            if (session.isCraftsmanOnline) {
                                LivePulseDot(color = Color.White, dotSize = 6.dp)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = if (session.isCraftsmanOnline) s.craftsmanOnline else s.craftsmanOffline,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = session.isCraftsmanOnline,
                                onCheckedChange = { viewModel.toggleCraftsmanOnline(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color.White.copy(alpha = 0.35f),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF3E444F)
                                ),
                                modifier = Modifier.testTag("craftsman_online_switch")
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF262A31),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.testTag("craftsman_locked_mode_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = s.roleCraftsman, tint = VoltGlowAccent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.roleCraftsman, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DriverStatCard(
                        title = when (s.language) {
                            AppLanguage.ARABIC -> "أرباحك اليوم"
                            AppLanguage.FRENCH -> "Gains du jour"
                            AppLanguage.ENGLISH -> "Today's Earnings"
                        },
                        value = "$totalEarningsDh ${s.currencyDh}",
                        modifier = Modifier.weight(1f)
                    )
                    DriverStatCard(
                        title = s.craftsmanOpenRequestsTitle,
                        value = "${openRequests.size}",
                        modifier = Modifier.weight(0.9f)
                    )
                    DriverStatCard(
                        title = s.authChooseSpecialty,
                        value = if (session.isCraftsmanSpecialtyLocked) lockedCategory.localizedName(s) else "—",
                        modifier = Modifier.weight(1.25f)
                    )
                }
            }
        }

        if (!session.isCraftsmanSpecialtyLocked) {
            // Mandatory Single-Profession Selection Gate
            SingleProfessionSelectionGate(
                initialSelection = session.craftsmanSpecialtyId,
                onConfirmSingleSpecialty = { selectedId ->
                    viewModel.setCraftsmanSpecialty(selectedId)
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            // Single-Profession Info Strip + inDrive List/Map Toggle (ZERO cross-trade tabs)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(lockedCategory.accentColor.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = lockedCategory.icon,
                                contentDescription = null,
                                tint = lockedCategory.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${lockedCategory.localizedName(s)} 🔒",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = when (s.language) {
                                    AppLanguage.ARABIC -> "تظهر طلبات ${lockedCategory.localizedName(s)} فقط"
                                    AppLanguage.FRENCH -> "Demandes ${lockedCategory.localizedName(s)} uniquement"
                                    AppLanguage.ENGLISH -> "Showing ${lockedCategory.localizedName(s)} requests only"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    IconButton(
                        onClick = {
                            viewModel.refreshCraftsmanRequests()
                            viewModel.showMessage(s.refresh)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("craftsman_header_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = s.refresh,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = WepeEmerald,
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable { isMapMode = !isMapMode }
                        .testTag("craftsman_map_list_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isMapMode) Icons.AutoMirrored.Rounded.FormatListBulleted else Icons.Rounded.Map,
                            contentDescription = if (isMapMode) "List" else "Map",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (s.language) {
                                AppLanguage.ARABIC -> if (isMapMode) "قائمة" else "خريطة"
                                AppLanguage.FRENCH -> if (isMapMode) "Liste" else "Carte"
                                AppLanguage.ENGLISH -> if (isMapMode) "List" else "Map"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isMapMode) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val mapPins = buildList {
                        if (session.isCraftsmanOnline) addAll(openRequests)
                        if (activeCraftsmanJob != null && none { it.id == activeCraftsmanJob.id }) {
                            add(activeCraftsmanJob)
                        }
                    }
                    val mapCenterLat = activeCraftsmanJob?.lat
                        ?: openRequests.firstOrNull()?.lat
                        ?: localGpsOrCity.first
                    val mapCenterLng = activeCraftsmanJob?.lng
                        ?: openRequests.firstOrNull()?.lng
                        ?: localGpsOrCity.second
                    HirafiRealMap(
                        centerLat = mapCenterLat,
                        centerLng = mapCenterLng,
                        cameraTarget = cameraTarget,
                        onCameraTargetConsumed = { viewModel.consumeCameraTarget() },
                        selectedCategoryId = session.craftsmanSpecialtyId,
                        craftsmen = emptyList(),
                        clientRequestsOnMap = mapPins,
                        activeRequest = activeCraftsmanJob,
                        liveRoute = liveRoute,
                        userGpsLocation = userGps,
                        isCraftsmanMode = true,
                        onMapCenterChanged = { _, _ -> },
                        onClientRequestPinClick = { req ->
                            if (req.id != activeCraftsmanJob?.id && req.categoryId == session.craftsmanSpecialtyId) {
                                selectedMapRequest = req
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
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
                                    craftsmanLocationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("craftsman_gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "تحديد موقعي عبر GPS",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val overlayRequest = selectedMapRequest
                    if (overlayRequest != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = ObsidianBlack,
                                    shape = CircleShape,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    IconButton(onClick = { selectedMapRequest = null }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "إغلاق", tint = Color.White)
                                    }
                                }
                                val distKm = MapRoutingService.haversineKm(
                                    localGpsOrCity.first,
                                    localGpsOrCity.second,
                                    overlayRequest.lat,
                                    overlayRequest.lng
                                )
                                InDriveDriverRequestCard(
                                    request = overlayRequest,
                                    distanceKm = distKm,
                                    nowMillis = nowMillis,
                                    existingOffer = craftsmanOffersByRequest[overlayRequest.id],
                                    onAcceptAtPrice = {
                                        viewModel.craftsmanAcceptAtClientPrice(overlayRequest.id)
                                        selectedMapRequest = null
                                    },
                                    onQuickCounter = { delta ->
                                        viewModel.craftsmanSubmitCounterOffer(
                                            requestId = overlayRequest.id,
                                            offeredPrice = overlayRequest.proposedPrice + delta,
                                            etaMinutes = 10,
                                            note = "عرض سريع من الحرفي"
                                        )
                                        selectedMapRequest = null
                                    },
                                    onCustomBid = {
                                        customBidTarget = overlayRequest
                                        selectedMapRequest = null
                                    }
                                )
                            }
                        }
                    } else if (activeCraftsmanJob != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        ) {
                            DriverActiveJobCard(
                                request = activeCraftsmanJob,
                                liveRoute = liveRoute,
                                trackingState = trackingState,
                                unreadChatCount = unreadChatCounts[activeCraftsmanJob.id] ?: 0,
                                onRequestTrackingPermission = {
                                    craftsmanLocationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onNavigate = {
                                    val navUri = Uri.parse("geo:${activeCraftsmanJob.lat},${activeCraftsmanJob.lng}?q=${activeCraftsmanJob.lat},${activeCraftsmanJob.lng}(${Uri.encode(activeCraftsmanJob.clientName)})")
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, navUri))
                                    }.onFailure {
                                        viewModel.showMessage("يرجى تثبيت تطبيق خرائط للملاحة الخارجية")
                                    }
                                },
                                onCall = {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+212612345678"))
                                    runCatching { context.startActivity(dial) }
                                },
                                onChat = { onOpenChat(activeCraftsmanJob.id) },
                                onAdvance = { next -> viewModel.advanceJobStatus(activeCraftsmanJob, next) },
                                onCancel = { viewModel.cancelCraftsmanActiveJob(activeCraftsmanJob) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (activeCraftsmanJob != null) {
                        item {
                            DriverActiveJobCard(
                                request = activeCraftsmanJob,
                                liveRoute = liveRoute,
                                trackingState = trackingState,
                                unreadChatCount = unreadChatCounts[activeCraftsmanJob.id] ?: 0,
                                onRequestTrackingPermission = {
                                    craftsmanLocationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onNavigate = {
                                    val navUri = Uri.parse("geo:${activeCraftsmanJob.lat},${activeCraftsmanJob.lng}?q=${activeCraftsmanJob.lat},${activeCraftsmanJob.lng}(${Uri.encode(activeCraftsmanJob.clientName)})")
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, navUri))
                                    }.onFailure {
                                        viewModel.showMessage("يرجى تثبيت تطبيق خرائط للملاحة الخارجية")
                                    }
                                },
                                onCall = {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+212612345678"))
                                    runCatching { context.startActivity(dial) }
                                },
                                onChat = { onOpenChat(activeCraftsmanJob.id) },
                                onAdvance = { next -> viewModel.advanceJobStatus(activeCraftsmanJob, next) },
                                onCancel = { viewModel.cancelCraftsmanActiveJob(activeCraftsmanJob) }
                            )
                        }
                    }

                    if (cloudSyncError != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = UrgentCoralSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, UrgentCoral.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = UrgentCoral)
                                        Text(
                                            text = "مشكلة في مزامنة الطلبات",
                                            fontWeight = FontWeight.Bold,
                                            color = UrgentCoral,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = cloudSyncError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ObsidianBlack
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.refreshCraftsmanRequests()
                                            viewModel.showMessage("جاري إعادة المحاولة...")
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("craftsman_retry_sync_button")
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (!session.isCraftsmanOnline) {
                        item {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                                    Spacer(Modifier.height(10.dp))
                                    Text("أنت غير متصل حالياً", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "فعّل زر الاتصال بالأعلى لاستقبال طلبات ${lockedCategory.nameAr} وتقديم عروضك.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (openRequests.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(42.dp))
                                    Text("لا توجد طلبات جديدة في تخصص ${lockedCategory.nameAr}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "يتم عرض طلبات مهنة (${lockedCategory.nameAr}) فقط. تأكد من أن الزبون اختار تخصص ${lockedCategory.nameAr} عند إرسال الطلب.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.refreshCraftsmanRequests()
                                            viewModel.showMessage("جاري فحص الطلبات الجديدة في السحابة...")
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("craftsman_refresh_requests_button")
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("تحديث الطلبات الآن", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        val filteredOpenRequests = HirafiProAnalytics.filterAndSortOpenRequests(
                            requests = openRequests,
                            filter = selectedRequestFilter,
                            refLat = localGpsOrCity.first,
                            refLng = localGpsOrCity.second
                        )
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("craftsman_request_filter_bar")
                            ) {
                                items(CraftsmanRequestFilter.entries) { filter ->
                                    FilterChip(
                                        selected = selectedRequestFilter == filter,
                                        onClick = { selectedRequestFilter = filter },
                                        label = {
                                            Text(
                                                text = filter.localizedLabel(s),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WepeEmerald,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("craftsman_filter_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (filteredOpenRequests.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = "لا توجد طلبات مطابقة لهذا التصنيف حالياً.",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(filteredOpenRequests, key = { it.id }) { req ->
                                val distKm = MapRoutingService.haversineKm(
                                    localGpsOrCity.first,
                                    localGpsOrCity.second,
                                    req.lat,
                                    req.lng
                                )
                                InDriveDriverRequestCard(
                                    request = req,
                                    distanceKm = distKm,
                                    nowMillis = nowMillis,
                                    existingOffer = craftsmanOffersByRequest[req.id],
                                    onAcceptAtPrice = { viewModel.craftsmanAcceptAtClientPrice(req.id) },
                                    onQuickCounter = { delta ->
                                        viewModel.craftsmanSubmitCounterOffer(
                                            requestId = req.id,
                                            offeredPrice = req.proposedPrice + delta,
                                            etaMinutes = 10,
                                            note = "عرض سريع من الحرفي"
                                        )
                                    },
                                    onCustomBid = { customBidTarget = req }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    customBidTarget?.let { targetReq ->
        RealtimeBiddingBottomSheet(
            viewModel = biddingViewModel,
            request = targetReq,
            roleOverride = UserRole.CRAFTSMAN,
            onDismiss = { customBidTarget = null },
            onCompletedOrAccepted = { customBidTarget = null }
        )
    }
}

@Composable
private fun SingleProfessionSelectionGate(
    initialSelection: String,
    onConfirmSingleSpecialty: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickedId by remember(initialSelection) { mutableStateOf(initialSelection) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = VoltGlowAccent, modifier = Modifier.size(22.dp))
                    Text(
                        text = "تحديد مهنة واحدة فقط",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Text(
                    text = "اختر مهنتك الأساسية التي تتقنها. بعد التأكيد يتم تثبيت هذه المهنة على حسابك، ولا يمكنك مشاهدة طلبات أو عروض الحرفيين في المهن الأخرى.",
                    color = Color(0xFFB8C0CC),
                    fontSize = 12.sp
                )
                Surface(
                    color = Color(0xFF262A31),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = VoltGlowAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "حماية العروض: كل حرفي يرى طلبات مهنته وعروضه الخاصة فقط",
                            color = VoltGlowAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        HirafiCategories.all.forEach { cat ->
            val selected = cat.id == pickedId
            Surface(
                color = if (selected) WepeEmeraldSoft else Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) WepeEmerald else WepeHairline,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { pickedId = cat.id }
                    .testTag("select_single_profession_${cat.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cat.accentColor.copy(alpha = 0.14f))
                        ) {
                            Icon(cat.icon, contentDescription = null, tint = cat.accentColor, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = cat.nameAr,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "متوسط الأجرة: ${cat.minRecommendedDh} - ${cat.maxRecommendedDh} د.م",
                                fontSize = 11.sp,
                                color = Color(0xFF667085)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) WepeEmerald else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        val chosen = HirafiCategories.byId(pickedId)
        Button(
            onClick = { onConfirmSingleSpecialty(pickedId) },
            shape = RoundedCornerShape(99.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("confirm_single_profession_button")
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "تأكيد مهنتي الوحيدة (${chosen.nameAr})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun DriverStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF20242A),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(title, color = Color(0xFF98A2B3), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(value, color = VoltGlowAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InDriveDriverRequestCard(
    request: ServiceRequest,
    distanceKm: Double,
    nowMillis: Long,
    existingOffer: ServiceOffer? = null,
    onAcceptAtPrice: () -> Unit,
    onQuickCounter: (Int) -> Unit,
    onCustomBid: () -> Unit
) {
    val s = hirafiStrings()
    val category = HirafiCategories.byId(request.categoryId)
    val ageLabel = HirafiProAnalytics.formatRelativeAge(request.createdAtMillis, nowMillis, s.language)
    val marketDeltaPct = HirafiProAnalytics.marketPriceDeltaPercent(
        proposedPrice = request.proposedPrice,
        recommendedMin = category.minRecommendedDh,
        recommendedMax = category.maxRecommendedDh
    )
    val (marketBadgeLabel, marketBadgeBg, marketBadgeFg) = when {
        marketDeltaPct >= 8 -> Triple("+${marketDeltaPct}%", VerifiedEmeraldSoft, WepeEmeraldDark)
        marketDeltaPct <= -10 -> Triple("${marketDeltaPct}%", UrgentCoralSoft, UrgentCoral)
        else -> Triple(
            when (s.language) {
                AppLanguage.ARABIC -> "سعر السوق العادل"
                AppLanguage.FRENCH -> "Prix marché"
                AppLanguage.ENGLISH -> "Fair Price"
            },
            WepeEmeraldSoft,
            WepeEmeraldDark
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = request.clientName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = request.clientName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "• $ageLabel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(color = VerifiedEmeraldSoft, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "$distanceKm ${s.unitKm}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = request.paymentMethod.localizedLabel(s),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${request.proposedPrice} ${s.currencyDh}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = marketBadgeBg, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = marketBadgeLabel,
                                color = marketBadgeFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (request.isUrgent) {
                            Surface(color = UrgentCoralSoft, shape = RoundedCornerShape(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = UrgentCoral, modifier = Modifier.size(12.dp))
                                    Text("عاجل", color = UrgentCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Pickup & Problem Pill (inDrive style)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(VerifiedEmerald)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = request.addressLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(category.icon, contentDescription = null, tint = category.accentColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = request.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (request.description.isNotBlank()) {
                        Text(
                            text = request.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (existingOffer != null && existingOffer.status != OfferStatus.DECLINED) {
                val (badgeBg, badgeTextColor, badgeLabel) = when (existingOffer.status) {
                    OfferStatus.COUNTERED -> Triple(
                        UrgentCoralSoft,
                        UrgentCoral,
                        "عرض مضاد من الزبون: ${existingOffer.offeredPrice} د.م — اضغط للرد"
                    )
                    OfferStatus.ACCEPTED -> Triple(
                        VerifiedEmeraldSoft,
                        WepeEmeraldDark,
                        "وافق الزبون على عرضك (${existingOffer.offeredPrice} د.م)!"
                    )
                    else -> Triple(
                        WepeEmeraldSoft,
                        WepeEmeraldDark,
                        "تم إرسال عرضك (${existingOffer.offeredPrice} د.م) • بانتظار رد الزبون"
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomBid() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = badgeLabel,
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Signature Emerald Accept Button
            Button(
                onClick = onAcceptAtPrice,
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WepeEmerald, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("craftsman_accept_${request.id}")
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("قبول بـ ${request.proposedPrice} د.م", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onQuickCounter(20) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("${request.proposedPrice + 20} د.م", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onQuickCounter(40) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("${request.proposedPrice + 40} د.م", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCustomBid,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(48.dp)
                ) {
                    Text("سعر مخصص", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DriverActiveJobCard(
    request: ServiceRequest,
    liveRoute: LiveRouteState,
    trackingState: LiveTrackingState,
    unreadChatCount: Int = 0,
    onRequestTrackingPermission: () -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    onChat: () -> Unit,
    onAdvance: (RequestStatus) -> Unit,
    onCancel: () -> Unit
) {
    val s = hirafiStrings()
    val nextAction: Pair<String, RequestStatus>? = when (request.status) {
        RequestStatus.ACCEPTED -> s.craftsmanImEnRoute to RequestStatus.EN_ROUTE
        RequestStatus.EN_ROUTE -> s.craftsmanImArrived to RequestStatus.ARRIVED
        RequestStatus.ARRIVED -> s.craftsmanVerifyStart to RequestStatus.IN_PROGRESS
        RequestStatus.IN_PROGRESS -> "${s.craftsmanCompleteJob} (${request.acceptedPrice ?: request.proposedPrice} ${s.currencyDh})" to RequestStatus.COMPLETED
        else -> null
    }
    var isPinVerified by rememberSaveable(request.id) { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    val expectedPin = remember(request.id, request.categoryId) {
        JobHandshakeSecurity.deriveSecurityPin(request)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianHeroBrush)
            .border(1.dp, WepeEmerald.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = WepeEmeraldSoft, shape = RoundedCornerShape(99.dp)) {
                    Text(
                        text = request.status.localizedLabel(s),
                        color = WepeEmeraldDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${request.acceptedPrice ?: request.proposedPrice} ${s.currencyDh}",
                    color = VoltGlowAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }

            JobMilestoneTimeline(currentStatus = request.status, isDarkSurface = true)

            Text(request.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = VoltGlowAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${request.clientName} • ${request.addressLabel}", color = Color(0xFFB8C0CC), fontSize = 13.sp)
            }

            if (liveRoute.etaMinutes > 0 || liveRoute.remainingDistanceKm > 0.0) {
                Surface(
                    color = Color(0xFF20242A),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${s.mapLiveEtaPrefix} ${liveRoute.etaMinutes} ${s.unitMin} (${liveRoute.remainingDistanceKm} ${s.unitKm})",
                        color = VoltGlowAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (request.status == RequestStatus.ARRIVED) {
                CraftsmanHandshakePinVerifier(
                    expectedPin = expectedPin,
                    isVerified = isPinVerified,
                    onVerified = { isPinVerified = true }
                )
            }

            if (trackingState.hasLocationPermission) {
                val accText = trackingState.latestFix?.accuracyMeters?.takeIf { it > 0f }?.let { " • ±${it.toInt()}m" }.orEmpty()
                Surface(
                    color = WepeEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        LivePulseDot(color = VoltGlowAccent, dotSize = 7.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${s.craftsmanLiveGpsActive}$accText",
                            color = VoltGlowAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onRequestTrackingPermission,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VoltGlowAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("enable_craftsman_gps_tracking_button")
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = VoltGlowAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (s.language) {
                            AppLanguage.ARABIC -> "تفعيل التتبع المباشر لموقعك (GPS)"
                            AppLanguage.FRENCH -> "Activer le suivi GPS en direct"
                            AppLanguage.ENGLISH -> "Enable Live GPS Tracking"
                        },
                        color = VoltGlowAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onNavigate,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E444F)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Navigation, contentDescription = s.mapRecenterRoute, tint = VoltGlowAccent)
                }
                OutlinedButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E444F)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = s.roleClient, tint = VoltGlowAccent)
                }
                BadgedBox(
                    badge = {
                        if (unreadChatCount > 0) {
                            Badge(containerColor = UrgentCoral, contentColor = Color.White) {
                                Text(
                                    text = if (unreadChatCount > 9) "9+" else "$unreadChatCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                ) {
                    OutlinedButton(
                        onClick = onChat,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF3E444F)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("driver_open_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = s.clientOpenChat, tint = VoltGlowAccent)
                    }
                }
                if (nextAction != null) {
                    val canAdvance = request.status != RequestStatus.ARRIVED || isPinVerified
                    Button(
                        onClick = { onAdvance(nextAction.second) },
                        enabled = canAdvance,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WepeEmerald,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF2E333B),
                            disabledContentColor = Color(0xFF8A94A3)
                        ),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("driver_advance_job_button")
                    ) {
                        Text(
                            text = if (canAdvance) nextAction.first else s.craftsmanEnterCodeHint,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (request.status == RequestStatus.ACCEPTED ||
                request.status == RequestStatus.EN_ROUTE ||
                request.status == RequestStatus.ARRIVED ||
                request.status == RequestStatus.IN_PROGRESS
            ) {
                OutlinedButton(
                    onClick = { showCancelConfirmDialog = true },
                    border = BorderStroke(1.dp, UrgentCoral.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("craftsman_cancel_job_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = s.clientCancelService,
                        tint = UrgentCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = s.clientCancelService,
                        color = UrgentCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = when (s.language) {
                        AppLanguage.ARABIC -> "تأكيد إلغاء الخدمة"
                        AppLanguage.FRENCH -> "Confirmer l'annulation"
                        AppLanguage.ENGLISH -> "Confirm Cancellation"
                    },
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    when (s.language) {
                        AppLanguage.ARABIC -> "هل أنت متأكد من إلغاء هذه الخدمة؟ سيتم إشعار الزبون بإلغاء الطلب."
                        AppLanguage.FRENCH -> "Voulez-vous vraiment annuler ce service ? Le client sera notifié."
                        AppLanguage.ENGLISH -> "Are you sure you want to cancel this job? The client will be notified."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UrgentCoral,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_craftsman_cancel_job_button")
                ) {
                    Text(s.confirm, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text(s.cancel, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
