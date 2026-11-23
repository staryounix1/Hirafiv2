package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.CraftsmanPin
import com.example.data.model.LiveRouteState
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.ui.i18n.HirafiStrings
import com.example.ui.i18n.hirafiStrings
import com.example.ui.i18n.localizedLabel
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.InfoCobalt
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WepeEmerald
import com.example.ui.theme.WepeEmeraldDark
import com.example.ui.theme.WepeEmeraldSoft
import com.example.ui.theme.WepeHairline
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.ComposeMapInitOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln

private enum class HirafiMapStyle(val styleUri: String) {
    STANDARD(Style.STANDARD),
    STREETS(Style.MAPBOX_STREETS),
    DARK(Style.DARK),
    SATELLITE(Style.SATELLITE_STREETS);

    fun localizedLabel(s: HirafiStrings): String = when (this) {
        STANDARD -> s.mapStyleStandard
        STREETS -> s.mapStyleStreets
        DARK -> s.mapStyleDark
        SATELLITE -> s.mapStyleSatellite
    }

    fun next(): HirafiMapStyle {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

private fun resolvedMapboxAccessToken(): String {
    val raw = BuildConfig.MAPBOX_ACCESS_TOKEN.trim()
    return if (raw.isNotBlank() && !raw.contains("YOUR_MAPBOX_ACCESS_TOKEN")) raw else ""
}

@Composable
fun HirafiRealMap(
    centerLat: Double,
    centerLng: Double,
    cameraTarget: Pair<Double, Double>?,
    onCameraTargetConsumed: () -> Unit,
    selectedCategoryId: String,
    craftsmen: List<CraftsmanPin>,
    clientRequestsOnMap: List<ServiceRequest> = emptyList(),
    activeRequest: ServiceRequest?,
    liveRoute: LiveRouteState,
    userGpsLocation: Pair<Double, Double>?,
    isCraftsmanMode: Boolean = false,
    onMapCenterChanged: (Double, Double) -> Unit,
    onCraftsmanPinClick: (CraftsmanPin) -> Unit = {},
    onClientRequestPinClick: (ServiceRequest) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = hirafiStrings()
    val mapboxToken = remember { resolvedMapboxAccessToken() }
    if (mapboxToken.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("hirafi_real_map")
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = "خريطة Mapbox",
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "خريطة Mapbox غير مفعلة بعد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "يرجى التأكد من إضافة MAPBOX_ACCESS_TOKEN في لوحة الأسرار.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    remember(mapboxToken) {
        MapboxOptions.accessToken = mapboxToken
        true
    }

    val scope = rememberCoroutineScope()
    var selectedStyle by remember {
        mutableStateOf(HirafiMapStyle.STANDARD)
    }

    val isSearching = activeRequest?.status == RequestStatus.SEARCHING
    val hasAssigned = activeRequest != null &&
        activeRequest.status != RequestStatus.SEARCHING &&
        activeRequest.status != RequestStatus.CANCELLED &&
        activeRequest.status != RequestStatus.COMPLETED

    val showCenterDragPin = !isCraftsmanMode && activeRequest == null

    var ignoreScrollUntilMs by remember {
        mutableLongStateOf(System.currentTimeMillis() + 800L)
    }
    var lastReportedCenter by remember { mutableStateOf(centerLat to centerLng) }

    val currentShowCenterDragPin by rememberUpdatedState(showCenterDragPin)
    val currentOnMapCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val currentOnCraftsmanPinClick by rememberUpdatedState(onCraftsmanPinClick)
    val currentOnClientRequestPinClick by rememberUpdatedState(onClientRequestPinClick)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(centerLng, centerLat))
            zoom(14.8)
            pitch(20.0)
            bearing(0.0)
        }
    }

    // Smoothly fly camera when requested (GPS button, city change, search result)
    LaunchedEffect(cameraTarget) {
        if (cameraTarget != null) {
            ignoreScrollUntilMs = System.currentTimeMillis() + 950L
            lastReportedCenter = cameraTarget.first to cameraTarget.second
            mapViewportState.flyTo(
                cameraOptions = cameraOptions {
                    center(Point.fromLngLat(cameraTarget.second, cameraTarget.first))
                    zoom(15.8)
                },
                animationOptions = mapAnimationOptions { duration(700L) }
            )
            onCameraTargetConsumed()
        }
    }

    fun fitActiveRouteCamera() {
        val req = activeRequest ?: return
        val craftLat = liveRoute.craftsmanLat ?: return
        val craftLng = liveRoute.craftsmanLng ?: return
        val lats = buildList {
            add(req.lat)
            add(craftLat)
            liveRoute.routePoints.forEach { add(it.first) }
        }
        val lngs = buildList {
            add(req.lng)
            add(craftLng)
            liveRoute.routePoints.forEach { add(it.second) }
        }
        if (lats.isNotEmpty() && lngs.isNotEmpty()) {
            val minLat = lats.min()
            val maxLat = lats.max()
            val minLng = lngs.min()
            val maxLng = lngs.max()
            val midLat = (minLat + maxLat) / 2.0
            val midLng = (minLng + maxLng) / 2.0
            val span = maxOf((maxLat - minLat) * 1.65, (maxLng - minLng) * 1.65, 0.004)
            val targetZoom = (ln(360.0 / span) / ln(2.0)).coerceIn(11.0, 16.0)
            ignoreScrollUntilMs = System.currentTimeMillis() + 950L
            scope.launch {
                mapViewportState.flyTo(
                    cameraOptions = cameraOptions {
                        center(Point.fromLngLat(midLng, midLat))
                        zoom(targetZoom)
                        pitch(28.0)
                    },
                    animationOptions = mapAnimationOptions { duration(750L) }
                )
            }
        }
    }

    // Fit bounding area when live route starts, changes status, or receives first craftsman coordinate
    LaunchedEffect(activeRequest?.id, activeRequest?.status, liveRoute.craftsmanLat != null) {
        if (hasAssigned && liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null) {
            fitActiveRouteCamera()
        }
    }

    // Debounced camera center tracking for location-picker mode
    LaunchedEffect(mapViewportState) {
        snapshotFlow { mapViewportState.cameraState?.center }
            .collectLatest { camCenter ->
                if (camCenter != null &&
                    currentShowCenterDragPin &&
                    System.currentTimeMillis() > ignoreScrollUntilMs
                ) {
                    delay(220L)
                    val lat = camCenter.latitude()
                    val lng = camCenter.longitude()
                    val movedEnough = abs(lat - lastReportedCenter.first) > 0.00008 ||
                        abs(lng - lastReportedCenter.second) > 0.00008
                    if (movedEnough) {
                        lastReportedCenter = lat to lng
                        currentOnMapCenterChanged(lat, lng)
                    }
                }
            }
    }

    val density = LocalDensity.current.density
    val composeMapInitOptions = remember(density) {
        ComposeMapInitOptions(
            pixelRatio = density,
            textureView = true
        )
    }

    Box(modifier = modifier.fillMaxSize().testTag("hirafi_real_map")) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            composeMapInitOptions = composeMapInitOptions,
            mapViewportState = mapViewportState,
            style = {
                MapStyle(style = selectedStyle.styleUri)
            },
            onMapClickListener = { point ->
                if (currentShowCenterDragPin) {
                    ignoreScrollUntilMs = System.currentTimeMillis() + 650L
                    val lat = point.latitude()
                    val lng = point.longitude()
                    lastReportedCenter = lat to lng
                    scope.launch {
                        mapViewportState.flyTo(
                            cameraOptions = cameraOptions { center(point) },
                            animationOptions = mapAnimationOptions { duration(380L) }
                        )
                    }
                    currentOnMapCenterChanged(lat, lng)
                    true
                } else {
                    false
                }
            }
        ) {
            // Live route polyline (clean Wepe emerald inner line with deep emerald casing)
            if (hasAssigned && liveRoute.routePoints.size >= 2) {
                val routePoints = remember(liveRoute.routePoints) {
                    liveRoute.routePoints.map { Point.fromLngLat(it.second, it.first) }
                }
                PolylineAnnotation(points = routePoints) {
                    lineColor = Color(0xFF0B6E3A)
                    lineWidth = 8.5
                    lineJoin = LineJoin.ROUND
                }
                PolylineAnnotation(points = routePoints) {
                    lineColor = WepeEmerald
                    lineWidth = 5.5
                    lineJoin = LineJoin.ROUND
                }
            }

            // User GPS dot
            if (userGpsLocation != null) {
                key("user_gps_dot") {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(userGpsLocation.second, userGpsLocation.first))
                            annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                            allowOverlap(true)
                            visible(true)
                        }
                    ) {
                        GpsDotMarker()
                    }
                }
            }

            if (isCraftsmanMode) {
                clientRequestsOnMap.forEach { req ->
                    key(req.id) {
                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(req.lng, req.lat))
                                annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                                allowOverlap(true)
                                visible(true)
                            }
                        ) {
                            MapBadgePill(
                                primaryText = "${req.proposedPrice} ${s.currencyDh}",
                                secondaryText = req.categoryName,
                                highlighted = true,
                                onClick = { currentOnClientRequestPinClick(req) }
                            )
                        }
                    }
                }

                if (hasAssigned && liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null) {
                    key("craftsman_self_${activeRequest?.id}") {
                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(liveRoute.craftsmanLng, liveRoute.craftsmanLat))
                                annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                                allowOverlap(true)
                                visible(true)
                            }
                        ) {
                            LiveMovingCraftsmanMarker(
                                etaMinutes = liveRoute.etaMinutes,
                                unitMin = s.unitMin,
                                label = s.roleCraftsman
                            )
                        }
                    }
                }
            } else {
                if (activeRequest != null) {
                    key("client_req_${activeRequest.id}") {
                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(activeRequest.lng, activeRequest.lat))
                                annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                                allowOverlap(true)
                                visible(true)
                            }
                        ) {
                            ClientPinBadge(
                                label = if (isSearching) s.statusSearching else s.roleClient
                            )
                        }
                    }
                }

                if (hasAssigned && liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null) {
                    key("assigned_craftsman_${activeRequest?.id}") {
                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(liveRoute.craftsmanLng, liveRoute.craftsmanLat))
                                annotationAnchor { anchor(ViewAnnotationAnchor.CENTER) }
                                allowOverlap(true)
                                visible(true)
                            }
                        ) {
                            LiveMovingCraftsmanMarker(
                                etaMinutes = liveRoute.etaMinutes,
                                unitMin = s.unitMin,
                                label = activeRequest?.assignedCraftsmanName?.split(" ")?.firstOrNull() ?: s.roleCraftsman
                            )
                        }
                    }
                } else if (!hasAssigned) {
                    craftsmen.forEach { pin ->
                        key(pin.id) {
                            val highlighted = pin.specialtyId == selectedCategoryId
                            ViewAnnotation(
                                options = viewAnnotationOptions {
                                    geometry(Point.fromLngLat(pin.lng, pin.lat))
                                    annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                                    allowOverlap(true)
                                    visible(true)
                                }
                            ) {
                                MapBadgePill(
                                    primaryText = "${pin.basePriceDh} ${s.currencyDh}",
                                    secondaryText = "★${pin.rating}",
                                    highlighted = highlighted,
                                    onClick = { currentOnCraftsmanPinClick(pin) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pulsing Radar Overlay during active search
        if (isSearching) {
            SearchRadarOverlay()
        }

        // Live Active Job Telemetry Pill on Map (Crisp White + Emerald)
        if (hasAssigned) {
            Surface(
                color = Color.White.copy(alpha = 0.96f),
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.2.dp, WepeEmerald.copy(alpha = 0.55f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 74.dp, end = 12.dp)
                    .testTag("live_telemetry_map_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    LivePulseDot(color = WepeEmerald, dotSize = 7.dp)
                    val telemetryText = if (liveRoute.etaMinutes > 0) {
                        "${s.mapLiveEtaPrefix} ${liveRoute.etaMinutes} ${s.unitMin} • ${liveRoute.remainingDistanceKm} ${s.unitKm}"
                    } else {
                        activeRequest?.status?.localizedLabel(s).orEmpty()
                    }
                    Text(
                        text = telemetryText,
                        color = ObsidianBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Clean Emerald Center Crosshair Pin when choosing service location
        if (showCenterDragPin) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-26).dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, WepeEmerald.copy(alpha = 0.45f)),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "موقع الخدمة",
                        color = WepeEmeraldDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(WepeEmerald)
                        .border(3.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "موقع الخدمة",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(WepeEmerald.copy(alpha = 0.35f))
                )
            }
        }

        // Map Zoom (+ / -) and Mapbox Style Switcher Controls
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, bottom = 90.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 14.8
                            ignoreScrollUntilMs = System.currentTimeMillis() + 450L
                            scope.launch {
                                mapViewportState.flyTo(
                                    cameraOptions = cameraOptions {
                                        zoom((currentZoom + 1.0).coerceAtMost(20.0))
                                    },
                                    animationOptions = mapAnimationOptions { duration(250L) }
                                )
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "تكبير الخريطة")
                    }
                    HorizontalDivider(modifier = Modifier.width(32.dp))
                    IconButton(
                        onClick = {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 14.8
                            ignoreScrollUntilMs = System.currentTimeMillis() + 450L
                            scope.launch {
                                mapViewportState.flyTo(
                                    cameraOptions = cameraOptions {
                                        zoom((currentZoom - 1.0).coerceAtLeast(3.0))
                                    },
                                    animationOptions = mapAnimationOptions { duration(250L) }
                                )
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "تصغير الخريطة")
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, WepeHairline),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .clickable { selectedStyle = selectedStyle.next() }
                    .testTag("map_layer_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = s.mapStyleStandard,
                        tint = WepeEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = selectedStyle.localizedLabel(s),
                        color = ObsidianBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasAssigned && liveRoute.craftsmanLat != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WepeEmerald,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clickable { fitActiveRouteCamera() }
                        .testTag("map_recenter_route_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = s.mapRecenterRoute,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = s.mapRecenterRoute,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsDotMarker() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(InfoCobalt.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(InfoCobalt)
                .border(2.5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun LiveMovingCraftsmanMarker(
    etaMinutes: Int,
    unitMin: String,
    label: String
) {
    val etaLabel = if (etaMinutes > 0) "$etaMinutes $unitMin" else "LIVE"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = WepeEmerald,
            shape = CircleShape,
            shadowElevation = 6.dp,
            border = BorderStroke(2.dp, Color.White)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = etaLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, WepeEmerald, CircleShape)
        ) {
            Text(
                text = label.take(1).uppercase(),
                color = ObsidianBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ClientPinBadge(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(99.dp),
            border = BorderStroke(1.2.dp, WepeEmerald),
            shadowElevation = 5.dp
        ) {
            Text(
                text = label,
                color = WepeEmeraldDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(WepeEmerald.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(ObsidianBlack)
                    .border(2.5.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun MapBadgePill(
    primaryText: String,
    secondaryText: String,
    highlighted: Boolean,
    onClick: (() -> Unit)?
) {
    val bgColor = if (highlighted) Color.White else Color.White.copy(alpha = 0.94f)
    val borderColor = if (highlighted) WepeEmerald else WepeHairline
    val titleColor = ObsidianBlack
    val subColor = if (highlighted) WepeEmeraldDark else Color(0xFF667085)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(if (highlighted) 1.8.dp else 1.dp, borderColor),
            shadowElevation = if (highlighted) 8.dp else 4.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = primaryText,
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = secondaryText,
                    color = subColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (highlighted) WepeEmerald else ObsidianBlack.copy(alpha = 0.55f))
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun SearchRadarOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxR = size.minDimension * 0.28f
        val center = Offset(size.width / 2f, size.height * 0.44f)
        drawCircle(
            color = WepeEmerald.copy(alpha = radarAlpha),
            radius = maxR * radarScale,
            center = center
        )
        drawCircle(
            color = WepeEmerald.copy(alpha = 0.35f),
            radius = maxR * 0.45f,
            center = center,
            style = Stroke(width = 4f)
        )
    }
}
