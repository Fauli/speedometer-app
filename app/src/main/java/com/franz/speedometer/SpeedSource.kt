package com.franz.speedometer

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** A single GPS reading, in raw units straight from the chipset. */
data class SpeedReading(
    /** Speed in metres per second — Doppler if the chipset supplies it, else derived. */
    val speedMetersPerSecond: Float,
    /** ± uncertainty of the speed (m/s), or null when derived / unavailable. */
    val speedAccuracyMps: Float?,
    /** Horizontal position accuracy (m); proxy for overall GPS quality. */
    val horizontalAccuracyM: Float,
    val hasSpeed: Boolean,
    /** True when speed was derived from position deltas rather than the chipset. */
    val derivedSpeed: Boolean,
    val bearingDeg: Float?,
    val altitudeM: Double?,
    /** Monotonic timestamp of the fix, used for distance/time integration. */
    val elapsedRealtimeNanos: Long,
)

/**
 * Wraps the Fused Location Provider as a cold [Flow] of [SpeedReading].
 *
 * We deliberately read [Location.getSpeed] (Doppler, from the GNSS chipset) rather than
 * diffing positions over time — it is smoother and more accurate.
 */
class SpeedSource(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Caller MUST hold ACCESS_FINE_LOCATION before collecting. */
    @SuppressLint("MissingPermission")
    fun readings(intervalMs: Long = 1000L): Flow<SpeedReading> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .build()

        var prev: Location? = null
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc.toReading(prev))
                prev = loc
            }
        }

        client.requestLocationUpdates(request, callback, null)
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun Location.toReading(prev: Location?): SpeedReading {
        // Prefer the chipset's Doppler speed; otherwise derive from position change.
        var hasSpd = hasSpeed()
        var spd = if (hasSpd) speed else 0f
        var derived = false
        if (!hasSpd && prev != null) {
            val dtSec = (elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1_000_000_000.0
            if (dtSec > 0.0) {
                spd = (distanceTo(prev) / dtSec).toFloat()
                hasSpd = true
                derived = true
            }
        }
        return SpeedReading(
            speedMetersPerSecond = spd,
            speedAccuracyMps = if (!derived && hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
            horizontalAccuracyM = if (hasAccuracy()) accuracy else Float.NaN,
            hasSpeed = hasSpd,
            derivedSpeed = derived,
            bearingDeg = if (hasBearing()) bearing else null,
            altitudeM = if (hasAltitude()) altitude else null,
            elapsedRealtimeNanos = elapsedRealtimeNanos,
        )
    }
}
