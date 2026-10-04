package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class GpsFixInfo(
    val location: Location,
    val providerLabel: String,
    val isOnline: Boolean
) {
    val latitude: Double get() = location.latitude
    val longitude: Double get() = location.longitude
    val accuracyMeters: Float get() = if (location.hasAccuracy()) location.accuracy else 99f

    val qualityLabel: String
        get() = when {
            accuracyMeters <= 8f -> "Sangat Akurat"
            accuracyMeters <= 20f -> "Akurat"
            accuracyMeters <= 50f -> "Cukup"
            else -> "Estimasi"
        }

    val formattedAccuracy: String
        get() = if (location.hasAccuracy()) "±${accuracyMeters.toInt().coerceAtLeast(1)}m" else "GPS"

    val formattedCoords: String
        get() = "%.6f, %.6f".format(Locale.US, latitude, longitude)
}

object LocationHelper {

    private const val GEOCODE_CACHE_PREFS = "consigntrack_geocode_cache"
    private const val FUSED_PROVIDER_NAME = "fused"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    /**
     * Checks if the device currently has active internet connectivity (Wi-Fi / Cellular Data).
     */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }

    /**
     * Real-time Flow observing Online/Offline state transitions so offline GPS coordinates
     * can automatically convert to street addresses as soon as data connection becomes available.
     */
    fun observeOnlineStatus(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(isOnline(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasNet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasNet)
            }

            override fun onLost(network: Network) {
                trySend(isOnline(context))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            cm.registerNetworkCallback(request, callback)
        }

        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()

    /**
     * Human-readable label for the location provider (Fused GMaps vs Satelit GPS Offline vs Network).
     */
    fun getProviderDisplayLabel(location: Location?, isDeviceOnline: Boolean): String {
        if (location == null) return if (isDeviceOnline) "Online GPS" else "Offline Satelit GPS"
        val p = location.provider?.lowercase(Locale.ROOT).orEmpty()
        return when {
            p.contains("fused") -> if (isDeviceOnline) "Fused GMaps" else "Fused Satelit"
            p.contains("gps") -> if (isDeviceOnline) "GPS Satelit+" else "Satelit Offline"
            p.contains("network") -> "Network/Wi-Fi"
            p.contains("passive") -> "GMaps Cache"
            else -> if (isDeviceOnline) "Multi-GPS" else "GPS Offline"
        }
    }

    fun toGpsFixInfo(context: Context, location: Location): GpsFixInfo {
        val online = isOnline(context)
        return GpsFixInfo(
            location = location,
            providerLabel = getProviderDisplayLabel(location, online),
            isOnline = online
        )
    }

    /**
     * Hitung jarak akurat garis lurus dalam meter antar 2 titik koordinat (WGS84 Ellipsoid formula)
     */
    fun calculateDistanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    fun formatDistance(meters: Float?): String {
        if (meters == null) return ""
        return if (meters < 1000f) {
            "${meters.toInt()} m"
        } else {
            "%.1f km".format(meters / 1000f)
        }
    }

    /**
     * Evaluates whether [candidate] is a more reliable & accurate GPS fix than [currentBest].
     * Combines timestamp freshness, accuracy radius in meters, and satellite/fused provider preference.
     */
    fun isBetterLocation(candidate: Location?, currentBest: Location?): Boolean {
        if (candidate == null) return false
        if (!isValidCoordinate(candidate.latitude, candidate.longitude)) return false
        if (currentBest == null) return true

        val timeDeltaMs = getLocationAgeMs(currentBest) - getLocationAgeMs(candidate)
        val isSignificantlyNewer = timeDeltaMs > 45_000L
        val isSignificantlyOlder = timeDeltaMs < -45_000L

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) {
            // Only keep older fix if candidate is much worse (>50m worse)
            val accDelta = candidate.accuracy - currentBest.accuracy
            if (accDelta > 50f) return false
        }

        val candidateAcc = if (candidate.hasAccuracy()) candidate.accuracy else 100f
        val currentAcc = if (currentBest.hasAccuracy()) currentBest.accuracy else 100f
        val accuracyDelta = candidateAcc - currentAcc // negative means candidate is more accurate

        val isMoreAccurate = accuracyDelta < 0f
        val isNotSignificantlyWorse = accuracyDelta <= 8f
        val isNewer = timeDeltaMs >= 0L

        val candidateIsSatelliteOrFused = candidate.provider?.lowercase(Locale.ROOT)?.let {
            it.contains("gps") || it.contains("fused")
        } == true

        return when {
            isMoreAccurate -> true
            isNewer && isNotSignificantlyWorse -> true
            isNewer && candidateIsSatelliteOrFused && accuracyDelta <= 15f -> true
            else -> false
        }
    }

    private fun getLocationAgeMs(location: Location): Long {
        val elapsedNanos = location.elapsedRealtimeNanos
        if (elapsedNanos > 0L) {
            val nowNanos = SystemClock.elapsedRealtimeNanos()
            if (nowNanos >= elapsedNanos) {
                return (nowNanos - elapsedNanos) / 1_000_000L
            }
        }
        val nowMs = System.currentTimeMillis()
        return (nowMs - location.time).coerceAtLeast(0L)
    }

    fun isValidCoordinate(lat: Double?, lng: Double?): Boolean {
        if (lat == null || lng == null) return false
        if (lat.isNaN() || lng.isNaN()) return false
        if (abs(lat) < 0.00001 && abs(lng) < 0.00001) return false // 0,0 Null Island guard
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    /**
     * Multi-Source High-Accuracy GPS Acquisition:
     *Simultaneously races:
     * 1. Google Play Services FusedLocationProviderClient (PRIORITY_HIGH_ACCURACY - GMaps sensor fusion)
     * 2. Hardware GNSS Satellite GPS_PROVIDER (100% Offline capable)
     * 3. Android OS FUSED_PROVIDER (Hardware + Inertial fusion)
     * 4. NETWORK_PROVIDER & PASSIVE_PROVIDER (Fast initial indoor/online lock)
     *
     * Calls [onIntermediateFix] in real-time whenever a progressively sharper coordinate arrives.
     */
    @SuppressLint("MissingPermission")
    suspend fun getFreshLocation(
        context: Context,
        timeoutMs: Long = 6500L,
        targetAccuracyMeters: Float = 10f,
        onIntermediateFix: ((Location) -> Unit)? = null
    ): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()

        var bestLocation: Location? = null

        fun considerCandidate(loc: Location?, sourceTag: String? = null) {
            if (loc == null || !isValidCoordinate(loc.latitude, loc.longitude)) return
            if (sourceTag != null && loc.provider.isNullOrBlank()) {
                loc.provider = sourceTag
            }
            if (isBetterLocation(loc, bestLocation)) {
                bestLocation = loc
                onIntermediateFix?.invoke(loc)
            }
        }

        // 1. Seed from all available last-known providers (GPS, Fused, Network, Passive)
        val initialProviders = buildList {
            add(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            } else {
                add(FUSED_PROVIDER_NAME)
            }
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }

        for (provider in initialProviders) {
            runCatching {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && getLocationAgeMs(loc) < 180_000L) {
                    considerCandidate(loc, provider)
                }
            }
        }

        // Also seed from Play Services Fused lastLocation
        if (fusedClient != null) {
            runCatching {
                val fusedLast = withTimeoutOrNull(800L) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        fusedClient.lastLocation
                            .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    }
                }
                if (fusedLast != null && getLocationAgeMs(fusedLast) < 180_000L) {
                    if (fusedLast.provider.isNullOrBlank()) fusedLast.provider = FUSED_PROVIDER_NAME
                    considerCandidate(fusedLast, FUSED_PROVIDER_NAME)
                }
            }
        }

        // If we already have a super fresh (<15s) high-accuracy (<= targetAccuracyMeters) fix, return immediately
        val currentBest = bestLocation
        if (currentBest != null &&
            getLocationAgeMs(currentBest) < 15_000L &&
            currentBest.hasAccuracy() &&
            currentBest.accuracy <= targetAccuracyMeters
        ) {
            return currentBest
        }

        // 2. Race all live providers in parallel until targetAccuracyMeters is met or timeoutMs expires
        val acquired = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { continuation ->
                var isCompleted = false
                val cancellationTokenSource = CancellationTokenSource()

                val lmListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        considerCandidate(location)
                        val best = bestLocation
                        if (!isCompleted && best != null &&
                            best.hasAccuracy() &&
                            best.accuracy <= targetAccuracyMeters &&
                            getLocationAgeMs(best) < 10_000L
                        ) {
                            isCompleted = true
                            if (continuation.isActive) continuation.resume(best)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val fusedCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        for (loc in result.locations) {
                            if (loc.provider.isNullOrBlank()) loc.provider = FUSED_PROVIDER_NAME
                            considerCandidate(loc, FUSED_PROVIDER_NAME)
                        }
                        val best = bestLocation
                        if (!isCompleted && best != null &&
                            best.hasAccuracy() &&
                            best.accuracy <= targetAccuracyMeters &&
                            getLocationAgeMs(best) < 10_000L
                        ) {
                            isCompleted = true
                            if (continuation.isActive) continuation.resume(best)
                        }
                    }
                }

                fun cleanupAll() {
                    runCatching { lm.removeUpdates(lmListener) }
                    runCatching { fusedClient?.removeLocationUpdates(fusedCallback) }
                    runCatching { cancellationTokenSource.cancel() }
                }

                var anyProviderActive = false

                // Start Google Play Services Fused High-Accuracy request + stream
                if (fusedClient != null) {
                    runCatching {
                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.token
                        ).addOnSuccessListener { loc ->
                            if (loc != null) {
                                if (loc.provider.isNullOrBlank()) loc.provider = FUSED_PROVIDER_NAME
                                considerCandidate(loc, FUSED_PROVIDER_NAME)
                                val best = bestLocation
                                if (!isCompleted && best != null &&
                                    best.hasAccuracy() &&
                                    best.accuracy <= targetAccuracyMeters
                                ) {
                                    isCompleted = true
                                    cleanupAll()
                                    if (continuation.isActive) continuation.resume(best)
                                }
                            }
                        }

                        val fusedRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 800L)
                            .setMinUpdateIntervalMillis(400L)
                            .setMinUpdateDistanceMeters(0f)
                            .setWaitForAccurateLocation(false)
                            .build()
                        fusedClient.requestLocationUpdates(
                            fusedRequest,
                            fusedCallback,
                            Looper.getMainLooper()
                        )
                        anyProviderActive = true
                    }
                }

                // Start Hardware Satellite GPS_PROVIDER (Works 100% Offline)
                runCatching {
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            500L,
                            0f,
                            lmListener,
                            Looper.getMainLooper()
                        )
                        anyProviderActive = true
                    }
                }

                // Start OS FUSED_PROVIDER if available
                runCatching {
                    val osFused = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        LocationManager.FUSED_PROVIDER
                    } else {
                        FUSED_PROVIDER_NAME
                    }
                    if (lm.allProviders.contains(osFused) && lm.isProviderEnabled(osFused)) {
                        lm.requestLocationUpdates(
                            osFused,
                            500L,
                            0f,
                            lmListener,
                            Looper.getMainLooper()
                        )
                        anyProviderActive = true
                    }
                }

                // Start NETWORK_PROVIDER for fast assisted lock when online
                runCatching {
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            500L,
                            0f,
                            lmListener,
                            Looper.getMainLooper()
                        )
                        anyProviderActive = true
                    }
                }

                if (!anyProviderActive && !isCompleted) {
                    isCompleted = true
                    cleanupAll()
                    if (continuation.isActive) continuation.resume(bestLocation)
                }

                continuation.invokeOnCancellation {
                    cleanupAll()
                }
            }
        }

        return acquired ?: bestLocation
    }

    /**
     * Real-time multi-source Location Flow combining FusedLocationProviderClient (GMaps engine)
     * and Offline Hardware Satellite GPS_PROVIDER.
     */
    @SuppressLint("MissingPermission")
    fun getLocationFlow(context: Context, intervalMs: Long = 2500L): Flow<Location?> = callbackFlow {
        if (!hasLocationPermission(context)) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val fusedClient = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull()

        var currentBest: Location? = null

        fun emitIfBetter(candidate: Location?, providerDefault: String? = null) {
            if (candidate == null || !isValidCoordinate(candidate.latitude, candidate.longitude)) return
            if (providerDefault != null && candidate.provider.isNullOrBlank()) {
                candidate.provider = providerDefault
            }
            if (isBetterLocation(candidate, currentBest)) {
                currentBest = candidate
                trySend(candidate)
            }
        }

        // Emit best last known immediately
        if (lm != null) {
            for (provider in listOf(LocationManager.GPS_PROVIDER, FUSED_PROVIDER_NAME, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
                runCatching {
                    if (lm.allProviders.contains(provider)) {
                        emitIfBetter(lm.getLastKnownLocation(provider), provider)
                    }
                }
            }
        }
        if (fusedClient != null) {
            runCatching {
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    emitIfBetter(loc, FUSED_PROVIDER_NAME)
                }
            }
        }

        val lmListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                emitIfBetter(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    emitIfBetter(loc, FUSED_PROVIDER_NAME)
                }
            }
        }

        // Register FusedLocationProviderClient (High Accuracy)
        if (fusedClient != null) {
            runCatching {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                    .setMinUpdateIntervalMillis(intervalMs / 2)
                    .setMinUpdateDistanceMeters(0f)
                    .setWaitForAccurateLocation(false)
                    .build()
                fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
            }
        }

        // Register Hardware Satellite GPS_PROVIDER + Network + Passive for 100% Offline & Hybrid coverage
        if (lm != null) {
            runCatching {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        intervalMs,
                        0f,
                        lmListener,
                        Looper.getMainLooper()
                    )
                }
            }
            runCatching {
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        intervalMs * 2,
                        2f,
                        lmListener,
                        Looper.getMainLooper()
                    )
                }
            }
            runCatching {
                if (lm.allProviders.contains(LocationManager.PASSIVE_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        intervalMs,
                        0f,
                        lmListener,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        awaitClose {
            runCatching { lm?.removeUpdates(lmListener) }
            runCatching { fusedClient?.removeLocationUpdates(fusedCallback) }
        }
    }

    /**
     * Checks if an address string is blank or a temporary offline placeholder that should be
     * auto-converted to a real street address when online.
     */
    fun isAddressNeedingAutoConversion(address: String?): Boolean {
        if (address.isNullOrBlank()) return true
        val trimmed = address.trim()
        if (trimmed.startsWith("Koordinat:", ignoreCase = true)) return true
        if (trimmed.startsWith("GPS:", ignoreCase = true)) return true
        if (trimmed.startsWith("[Offline", ignoreCase = true)) return true
        // Also check if it's just raw lat,lng numbers
        val rawCoordRegex = Regex("""^[-+]?\d{1,2}\.\d+,\s*[-+]?\d{1,3}\.\d+$""")
        return rawCoordRegex.matches(trimmed)
    }

    private fun cacheKey(lat: Double, lng: Double): String {
        return "%.4f,%.4f".format(Locale.US, lat, lng)
    }

    fun getCachedAddress(context: Context, lat: Double, lng: Double): String? {
        if (!isValidCoordinate(lat, lng)) return null
        val prefs = context.getSharedPreferences(GEOCODE_CACHE_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(cacheKey(lat, lng), null)?.takeIf { it.isNotBlank() }
    }

    private fun saveCachedAddress(context: Context, lat: Double, lng: Double, address: String) {
        if (address.isBlank()) return
        val prefs = context.getSharedPreferences(GEOCODE_CACHE_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(cacheKey(lat, lng), address).apply()
    }

    /**
     * Converts (latitude, longitude) into a concise, accurate street address.
     * - Checks local offline cache first (instant, works offline for previously visited warungs).
     * - When online: uses Android Geocoder (Google Play backend) + OpenStreetMap Nominatim fallback.
     * - Caches every resolved address locally for seamless offline/online operation.
     */
    suspend fun reverseGeocodeAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!isValidCoordinate(latitude, longitude)) return@withContext null

        // 1. Check Offline Local Geocode Cache first
        getCachedAddress(context, latitude, longitude)?.let { cached ->
            return@withContext cached
        }

        if (!isOnline(context)) {
            return@withContext null
        }

        // 2. Try Android Platform Geocoder (Google Play Services Geocoder)
        val geocoderAddress = runCatching {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("id", "ID"))
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(latitude, longitude, 3)
                list?.firstNotNullOfOrNull { formatAndroidAddress(it) }
            } else {
                null
            }
        }.getOrNull()

        if (!geocoderAddress.isNullOrBlank()) {
            saveCachedAddress(context, latitude, longitude, geocoderAddress)
            return@withContext geocoderAddress
        }

        // 3. Fallback: OpenStreetMap Nominatim Reverse Geocoding API (when online)
        val nominatimAddress = runCatching {
            val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1&accept-language=id"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ConsignTrackAndroid/1.0")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bodyStr = resp.body?.string() ?: return@use null
                val json = JSONObject(bodyStr)
                val addrObj = json.optJSONObject("address")
                if (addrObj != null) {
                    val road = addrObj.optString("road")
                        .ifBlank { addrObj.optString("pedestrian") }
                        .ifBlank { addrObj.optString("neighbourhood") }
                        .ifBlank { addrObj.optString("hamlet") }
                    val houseNumber = addrObj.optString("house_number")
                    val suburb = addrObj.optString("village")
                        .ifBlank { addrObj.optString("suburb") }
                        .ifBlank { addrObj.optString("quarter") }
                    val city = addrObj.optString("city")
                        .ifBlank { addrObj.optString("town") }
                        .ifBlank { addrObj.optString("county") }
                        .ifBlank { addrObj.optString("municipality") }

                    val roadWithNo = if (road.isNotBlank() && houseNumber.isNotBlank()) {
                        "$road No. $houseNumber"
                    } else {
                        road
                    }

                    val parts = listOf(roadWithNo, suburb, city)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (parts.isNotEmpty()) {
                        parts.joinToString(", ")
                    } else {
                        json.optString("display_name")
                            .split(",")
                            .take(3)
                            .joinToString(",") { it.trim() }
                            .takeIf { it.isNotBlank() }
                    }
                } else {
                    json.optString("display_name")
                        .split(",")
                        .take(3)
                        .joinToString(", ") { it.trim() }
                        .takeIf { it.isNotBlank() }
                }
            }
        }.getOrNull()

        if (!nominatimAddress.isNullOrBlank()) {
            saveCachedAddress(context, latitude, longitude, nominatimAddress)
            return@withContext nominatimAddress
        }

        null
    }

    private fun formatAndroidAddress(addr: Address): String? {
        val street = addr.thoroughfare ?: addr.featureName?.takeIf { !it.matches(Regex("""^[-+]?\d+.*""")) }
        val number = addr.subThoroughfare
        val streetFull = when {
            !street.isNullOrBlank() && !number.isNullOrBlank() && !street.contains(number) -> "$street No. $number"
            !street.isNullOrBlank() -> street
            else -> null
        }

        val kelurahan = addr.subLocality
        val kecamatanOrKota = addr.locality ?: addr.subAdminArea

        val parts = listOfNotNull(streetFull, kelurahan, kecamatanOrKota)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (parts.isNotEmpty()) {
            return parts.joinToString(", ")
        }

        // Fallback to full address line 0 trimmed to concise form
        val line0 = addr.getAddressLine(0)
        if (!line0.isNullOrBlank()) {
            return line0.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(3)
                .joinToString(", ")
        }
        return null
    }

    /**
     * Parses latitude & longitude from raw text, Google Maps URLs, or geo: URIs.
     * Supports:
     * - "-6.914744, 107.609810"
     * - "https://www.google.com/maps/place/.../@-6.914744,107.609810,17z"
     * - "https://maps.google.com/?q=-6.914744,107.609810"
     * - Short links ("https://maps.app.goo.gl/...") by resolving redirect URL when online.
     */
    suspend fun parseCoordinatesOrMapsUrl(input: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val raw = input.trim()
        if (raw.isBlank()) return@withContext null

        extractLatLngFromString(raw)?.let { return@withContext it }

        // If it's a shortened Google Maps URL (maps.app.goo.gl / goo.gl/maps), resolve redirect URL
        if (raw.contains("goo.gl", ignoreCase = true) || raw.contains("maps.app", ignoreCase = true) || raw.startsWith("http")) {
            val urlMatch = Regex("""https?://\S+""").find(raw)?.value ?: raw
            val resolvedUrl = runCatching {
                val req = Request.Builder()
                    .url(urlMatch)
                    .header("User-Agent", "Mozilla/5.0")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    resp.request.url.toString()
                }
            }.getOrNull()

            if (!resolvedUrl.isNullOrBlank()) {
                extractLatLngFromString(resolvedUrl)?.let { return@withContext it }
            }
        }

        null
    }

    fun extractLatLngFromString(text: String): Pair<Double, Double>? {
        val decoded = runCatching { Uri.decode(text) }.getOrDefault(text)

        // 1. Check !3d<lat>!4d<lng> pattern (exact pin coordinate in Google Maps URLs)
        val pinPattern = Regex("""!3d([-+]?\d{1,2}\.\d+)!4d([-+]?\d{1,3}\.\d+)""")
        pinPattern.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (isValidCoordinate(lat, lng)) return lat!! to lng!!
        }

        // 2. Check @<lat>,<lng> or q=<lat>,<lng> or ll=<lat>,<lng> or query=<lat>,<lng>
        val urlCoordPattern = Regex("""(?:@|q=|ll=|query=|center=|geo:)([-+]?\d{1,2}\.\d+)\s*,\s*([-+]?\d{1,3}\.\d+)""")
        urlCoordPattern.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (isValidCoordinate(lat, lng)) return lat!! to lng!!
        }

        // 3. Check general "<lat>, <lng>" or "<lat> <lng>" anywhere in the string
        val generalPattern = Regex("""([-+]?\d{1,2}\.\d{3,})\s*[,\s]\s*([-+]?\d{1,3}\.\d{3,})""")
        generalPattern.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (isValidCoordinate(lat, lng)) return lat!! to lng!!
        }

        return null
    }

    /**
     * Buka rute navigasi di Google Maps (mendukung mode Online maupun Offline Maps)
     */
    fun openNavigationInGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
        try {
            val encodedLabel = Uri.encode(label)
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}
