package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

enum class LocationSource(val labelAr: String) {
    FUSED_GPS("GPS Play Services"),
    LAST_KNOWN("آخر موقع معروف"),
    NATIVE_FALLBACK("مستشعر الجهاز")
}

data class RealTimeLocationSnapshot(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val bearing: Float = 0f,
    val timestampMillis: Long = System.currentTimeMillis(),
    val source: LocationSource = LocationSource.FUSED_GPS
)

data class LiveTrackingState(
    val isTracking: Boolean = false,
    val isForegroundServiceActive: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val trackedRequestId: String? = null,
    val latestFix: RealTimeLocationSnapshot? = null,
    val errorMessage: String? = null
)

/**
 * In-process reactive telemetry bus bridging the background/foreground [CraftsmanLocationService]
 * with [com.example.ui.viewmodel.HirafiViewModel] and active map overlays.
 */
object LiveLocationBus {
    private val _latestCraftsmanFix = MutableStateFlow<RealTimeLocationSnapshot?>(null)
    val latestCraftsmanFix: StateFlow<RealTimeLocationSnapshot?> = _latestCraftsmanFix.asStateFlow()

    private val _isForegroundServiceActive = MutableStateFlow(false)
    val isForegroundServiceActive: StateFlow<Boolean> = _isForegroundServiceActive.asStateFlow()

    fun publishFix(fix: RealTimeLocationSnapshot) {
        if (RealTimeLocationService.isCoordinateValid(fix.lat, fix.lng)) {
            _latestCraftsmanFix.value = fix
        }
    }

    fun setForegroundServiceActive(active: Boolean) {
        _isForegroundServiceActive.value = active
    }

    fun clear() {
        _latestCraftsmanFix.value = null
        _isForegroundServiceActive.value = false
    }
}

/**
 * Real-time location provider built on Google Play Services [FusedLocationProviderClient],
 * exposing cold reactive [Flow] updates and one-shot high-accuracy fixes for craftsman tracking.
 */
class RealTimeLocationService(context: Context) {

    private val appContext: Context = context.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationProviderEnabled(): Boolean {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    suspend fun isHighAccuracySettingsReady(intervalMillis: Long = 3500L): Boolean =
        suspendCancellableCoroutine { cont ->
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()
            val settingsReq = LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .setAlwaysShow(false)
                .build()
            LocationServices.getSettingsClient(appContext)
                .checkLocationSettings(settingsReq)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(true)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(false)
                }
        }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentFix(): RealTimeLocationSnapshot? {
        if (!hasLocationPermission()) return null

        val primary = suspendCancellableCoroutine<Location?> { cont ->
            val cancellation = CancellationTokenSource()
            cont.invokeOnCancellation { cancellation.cancel() }
            runCatching {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
        if (primary != null && isCoordinateValid(primary.latitude, primary.longitude)) {
            return primary.toSnapshot(LocationSource.FUSED_GPS)
        }

        val lastFused = suspendCancellableCoroutine<Location?> { cont ->
            runCatching {
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
        if (lastFused != null && isCoordinateValid(lastFused.latitude, lastFused.longitude)) {
            return lastFused.toSnapshot(LocationSource.LAST_KNOWN)
        }

        return getNativeLastKnownLocation()?.toSnapshot(LocationSource.NATIVE_FALLBACK)
    }

    @SuppressLint("MissingPermission")
    fun observeRealTimeLocation(
        intervalMillis: Long = 3500L,
        minDistanceMeters: Float = 5f,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<RealTimeLocationSnapshot> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val effectivePriority = if (hasFineLocationPermission()) priority else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val locationRequest = LocationRequest.Builder(effectivePriority, intervalMillis)
            .setMinUpdateIntervalMillis((intervalMillis / 2L).coerceAtLeast(1500L))
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (isCoordinateValid(loc.latitude, loc.longitude)) {
                    trySend(loc.toSnapshot(LocationSource.FUSED_GPS))
                }
            }
        }

        runCatching {
            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }

        // Native LocationManager backup for environments/emulators where GMS updates are sparse
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val nativeListener = LocationListener { loc ->
            if (isCoordinateValid(loc.latitude, loc.longitude)) {
                trySend(loc.toSnapshot(LocationSource.NATIVE_FALLBACK))
            }
        }
        if (lm != null) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                runCatching {
                    if (lm.isProviderEnabled(provider)) {
                        lm.requestLocationUpdates(
                            provider,
                            intervalMillis.coerceAtLeast(4000L),
                            minDistanceMeters.coerceAtLeast(5f),
                            nativeListener,
                            Looper.getMainLooper()
                        )
                    }
                }
            }
        }

        // Seed stream with last known location immediately if available
        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && isCoordinateValid(loc.latitude, loc.longitude)) {
                    trySend(loc.toSnapshot(LocationSource.LAST_KNOWN))
                }
            }
        }

        awaitClose {
            runCatching { fusedClient.removeLocationUpdates(callback) }
            if (lm != null) {
                runCatching { lm.removeUpdates(nativeListener) }
            }
        }
    }

    fun startForegroundJobTracking(
        requestId: String,
        requestTitle: String,
        requestLat: Double? = null,
        requestLng: Double? = null
    ) {
        if (!hasLocationPermission()) return
        runCatching {
            val intent = Intent(appContext, CraftsmanLocationService::class.java).apply {
                action = CraftsmanLocationService.ACTION_START_TRACKING
                putExtra(CraftsmanLocationService.EXTRA_REQUEST_ID, requestId)
                putExtra(CraftsmanLocationService.EXTRA_REQUEST_TITLE, requestTitle)
                if (requestLat != null && requestLng != null) {
                    putExtra(CraftsmanLocationService.EXTRA_REQUEST_LAT, requestLat)
                    putExtra(CraftsmanLocationService.EXTRA_REQUEST_LNG, requestLng)
                }
            }
            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    fun stopForegroundJobTracking() {
        runCatching {
            val intent = Intent(appContext, CraftsmanLocationService::class.java).apply {
                action = CraftsmanLocationService.ACTION_STOP_TRACKING
            }
            appContext.startService(intent)
        }
        LiveLocationBus.setForegroundServiceActive(false)
    }

    @SuppressLint("MissingPermission")
    private fun getNativeLastKnownLocation(): Location? {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private fun Location.toSnapshot(source: LocationSource) = RealTimeLocationSnapshot(
        lat = latitude,
        lng = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else 0f,
        speedMps = if (hasSpeed()) speed else 0f,
        bearing = if (hasBearing()) bearing else 0f,
        timestampMillis = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        source = source
    )

    companion object {
        fun isCoordinateValid(lat: Double, lng: Double): Boolean {
            if (lat.isNaN() || lng.isNaN()) return false
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return false
            return !(lat == 0.0 && lng == 0.0)
        }

        fun computeEtaMinutes(distanceKm: Double, speedMps: Float): Int {
            if (distanceKm <= 0.08) return 0
            // Use real moving speed when speed >= 2 m/s (~7.2 km/h), clamped to realistic urban range (15..60 km/h)
            val speedKmh = if (speedMps >= 2.0f) {
                (speedMps * 3.6).coerceIn(15.0, 60.0)
            } else {
                24.0
            }
            val minutes = ((distanceKm / speedKmh) * 60.0).roundToInt()
            return minutes.coerceIn(1, 90)
        }
    }
}
