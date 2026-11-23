package com.example.data.remote

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.data.model.SearchPlaceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class DrivingRouteResult(
    val points: List<Pair<Double, Double>>,
    val distanceKm: Double,
    val durationMinutes: Int
)

object MapRoutingService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return ((r * c) * 10.0).roundToInt() / 10.0
    }

    suspend fun fetchDrivingRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): DrivingRouteResult = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "HirafiApp/1.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("OSRM HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) error("No OSRM route")
                val route = routes.getJSONObject(0)
                val distanceMeters = route.getDouble("distance")
                val durationSeconds = route.getDouble("duration")
                val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                val points = buildList {
                    for (i in 0 until coords.length()) {
                        val pt = coords.getJSONArray(i)
                        val lng = pt.getDouble(0)
                        val lat = pt.getDouble(1)
                        add(lat to lng)
                    }
                }
                if (points.size < 2) error("Too few points")
                DrivingRouteResult(
                    points = points,
                    distanceKm = ((distanceMeters / 100.0).roundToInt() / 10.0).coerceAtLeast(0.1),
                    durationMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                )
            }
        }.getOrElse {
            // Smooth interpolated fallback if offline
            val steps = 18
            val fallbackPoints = (0..steps).map { step ->
                val t = step.toDouble() / steps.toDouble()
                val lat = startLat + (endLat - startLat) * t
                val lng = startLng + (endLng - startLng) * t
                lat to lng
            }
            val dist = haversineKm(startLat, startLng, endLat, endLng).coerceAtLeast(0.3)
            val mins = (dist * 3.5).roundToInt().coerceIn(2, 45)
            DrivingRouteResult(fallbackPoints, dist, mins)
        }
    }

    suspend fun reverseGeocode(
        context: Context,
        lat: Double,
        lng: Double,
        languageCode: String = "ar"
    ): String {
        val cleanLang = languageCode.ifBlank { "ar" }
        // 1. Try Android Geocoder first
        val androidResult = withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.forLanguageTag(cleanLang))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(
                            lat,
                            lng,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                    if (!cont.isActive) return
                                    val formatted = addresses.firstOrNull()?.let { addr ->
                                        listOfNotNull(
                                            addr.thoroughfare,
                                            addr.subLocality,
                                            addr.locality
                                        ).distinct().joinToString("، ")
                                    }
                                    cont.resume(formatted?.takeIf { it.isNotBlank() })
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            }
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        listOfNotNull(
                            addr.thoroughfare,
                            addr.subLocality,
                            addr.locality
                        ).distinct().joinToString("، ")
                    }?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()
        }
        if (!androidResult.isNullOrBlank()) return androidResult

        // 2. Fallback to OpenStreetMap Nominatim localized reverse geocoding
        val nominatimResult = withContext(Dispatchers.IO) {
            runCatching {
                val url = String.format(
                    Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f&accept-language=%s",
                    lat,
                    lng,
                    cleanLang
                )
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "HirafiApp/1.0 (contact@hirafi.app)")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val addr = json.optJSONObject("address")
                    val parts = listOfNotNull(
                        addr?.optString("road")?.takeIf { it.isNotBlank() },
                        addr?.optString("suburb")?.takeIf { it.isNotBlank() }
                            ?: addr?.optString("neighbourhood")?.takeIf { it.isNotBlank() },
                        addr?.optString("city")?.takeIf { it.isNotBlank() }
                            ?: addr?.optString("town")?.takeIf { it.isNotBlank() }
                    ).distinct()
                    if (parts.isNotEmpty()) parts.joinToString("، ")
                    else json.optString("display_name").split(",").take(2).joinToString("، ").trim()
                }
            }.getOrNull()
        }
        if (!nominatimResult.isNullOrBlank()) return nominatimResult

        return String.format(Locale.US, "%.4f, %.4f", lat, lng)
    }

    suspend fun searchPlaces(
        context: Context,
        query: String,
        languageCode: String = "ar"
    ): List<SearchPlaceResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val cleanLang = languageCode.ifBlank { "ar" }

        val nominatim = withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=5&accept-language=$cleanLang"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "HirafiApp/1.0 (contact@hirafi.app)")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList()
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    buildList {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            add(
                                SearchPlaceResult(
                                    displayName = item.optString("display_name").split(",").take(3).joinToString("، "),
                                    lat = item.getDouble("lat"),
                                    lng = item.getDouble("lon")
                                )
                            )
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
        if (nominatim.isNotEmpty()) return nominatim

        return withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.forLanguageTag(cleanLang))
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocationName(trimmed, 5).orEmpty()
                list.map { addr ->
                    SearchPlaceResult(
                        displayName = listOfNotNull(addr.featureName, addr.subLocality, addr.locality).distinct().joinToString("، "),
                        lat = addr.latitude,
                        lng = addr.longitude
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
