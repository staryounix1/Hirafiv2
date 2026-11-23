package com.example.data.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserPreferencesDataStore
import com.example.data.remote.MapRoutingService
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseProfileLocationPatchDto
import com.example.data.remote.SupabaseRequestPatchDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground Android Location Service (`foregroundServiceType="location"`) that streams
 * continuous Play Services [FusedLocationProviderClient] updates during an active craftsman job,
 * keeping live map coordinates synced even while navigating in another app.
 */
class CraftsmanLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationService: RealTimeLocationService
    private lateinit var prefs: UserPreferencesDataStore
    private var collectionJob: Job? = null
    private var lastSupabasePushMs: Long = 0L
    @Volatile private var activeRequestId: String? = null
    @Volatile private var activeRequestLat: Double? = null
    @Volatile private var activeRequestLng: Double? = null

    override fun onCreate() {
        super.onCreate()
        locationService = RealTimeLocationService(applicationContext)
        prefs = UserPreferencesDataStore(applicationContext)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                stopTrackingAndService()
                return START_NOT_STICKY
            }
            ACTION_START_TRACKING, null -> {
                if (!locationService.hasLocationPermission()) {
                    stopTrackingAndService()
                    return START_NOT_STICKY
                }
                val reqId = intent?.getStringExtra(EXTRA_REQUEST_ID)
                if (!reqId.isNullOrBlank()) {
                    activeRequestId = reqId
                }
                if (intent?.hasExtra(EXTRA_REQUEST_LAT) == true && intent.hasExtra(EXTRA_REQUEST_LNG)) {
                    activeRequestLat = intent.getDoubleExtra(EXTRA_REQUEST_LAT, 0.0)
                    activeRequestLng = intent.getDoubleExtra(EXTRA_REQUEST_LNG, 0.0)
                }
                val title = intent?.getStringExtra(EXTRA_REQUEST_TITLE).orEmpty()
                val notification = buildNotification(title)
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
                runCatching {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
                }.onFailure {
                    stopTrackingAndService()
                    return START_NOT_STICKY
                }
                LiveLocationBus.setForegroundServiceActive(true)
                startCollectingLocation()
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun startCollectingLocation() {
        if (collectionJob?.isActive == true) return
        collectionJob = serviceScope.launch {
            locationService.observeRealTimeLocation(
                intervalMillis = 3500L,
                minDistanceMeters = 5f
            ).collect { snapshot ->
                LiveLocationBus.publishFix(snapshot)
                maybePushTelemetryToSupabase(snapshot)
            }
        }
    }

    private suspend fun maybePushTelemetryToSupabase(snapshot: RealTimeLocationSnapshot) {
        if (snapshot.lat == 0.0 && snapshot.lng == 0.0) return
        val targetLat = activeRequestLat
        val targetLng = activeRequestLng
        if (targetLat != null && targetLng != null &&
            MapRoutingService.haversineKm(snapshot.lat, snapshot.lng, targetLat, targetLng) > 150.0
        ) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastSupabasePushMs < 5500L) return
        if (!SupabaseConfig.isConfigured) return
        val session = prefs.userSessionFlow.first()
        val token = session.supabaseAccessToken ?: return
        if (session.userId.length != 36) return
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return

        lastSupabasePushMs = now
        val bearer = "Bearer $token"
        runCatching {
            api.patchProfileLocation(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.${session.userId}",
                patch = SupabaseProfileLocationPatchDto(
                    lat = snapshot.lat,
                    lng = snapshot.lng
                )
            )
        }
        val reqId = activeRequestId
        if (!reqId.isNullOrBlank()) {
            runCatching {
                api.patchServiceRequest(
                    apiKey = SupabaseConfig.anonKey,
                    bearer = bearer,
                    idFilter = "eq.$reqId",
                    patch = SupabaseRequestPatchDto(
                        craftsmanLat = snapshot.lat,
                        craftsmanLng = snapshot.lng
                    )
                )
            }
        }
    }

    private fun stopTrackingAndService() {
        collectionJob?.cancel()
        collectionJob = null
        LiveLocationBus.setForegroundServiceActive(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        collectionJob?.cancel()
        LiveLocationBus.setForegroundServiceActive(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "التتبع المباشر للطلبات",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "يعرض حالة تتبع موقع الحرفي أثناء تنفيذ الطلب الجاري"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(requestTitle: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentText = if (requestTitle.isNotBlank()) {
            "مشاركة موقعك المباشر لطلب: $requestTitle"
        } else {
            "التتبع المباشر لموقع الحرفي نشط"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("حرفي — التتبع المباشر")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START_TRACKING = "com.example.action.START_LOCATION_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.action.STOP_LOCATION_TRACKING"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_REQUEST_TITLE = "extra_request_title"
        const val EXTRA_REQUEST_LAT = "extra_request_lat"
        const val EXTRA_REQUEST_LNG = "extra_request_lng"
        private const val CHANNEL_ID = "hirafi_live_location"
        private const val NOTIFICATION_ID = 4102
    }
}
