package com.franz.speedometer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Speed unit with all conversion factors from raw m/s and m. */
enum class SpeedUnit(
    val speedLabel: String,
    val speedFactor: Float,   // m/s -> unit
    val distFactor: Float,    // m   -> distance unit
    val distLabel: String,
    val colorLow: Float,      // below this (in this unit) = green
    val colorMid: Float,      // below this = amber; at/above = red
) {
    KMH("km/h", 3.6f, 0.001f, "km", 50f, 100f),
    MPH("mph", 2.2369363f, 0.000621371f, "mi", 30f, 60f),
    KNOTS("kn", 1.9438445f, 0.000539957f, "NM", 10f, 25f),
}

enum class ThemeMode { AUTO, LIGHT, NIGHT }

/** Everything configurable from the Settings screen (§6.5 of SPEC.md). */
data class SettingsState(
    // Display
    val unit: SpeedUnit = SpeedUnit.KMH,
    val decimals: Int = 0,
    val theme: ThemeMode = ThemeMode.AUTO,
    val colorBySpeed: Boolean = true,
    val hudMirror: Boolean = false,
    val keepScreenOn: Boolean = true,
    val showGraph: Boolean = true,
    val showAvg: Boolean = true,
    val showMax: Boolean = true,
    val showDistance: Boolean = true,
    val showAltitude: Boolean = false,
    val showHeading: Boolean = false,
    val showTime: Boolean = true,
    // Alerts
    val alertEnabled: Boolean = false,
    val alertThreshold: Float = 50f, // in the selected display unit
    val alertFlash: Boolean = true,
    val alertBeep: Boolean = false,
    val alertVibrate: Boolean = true,
    // Measurement
    val updateRateHz: Int = 1,
    val suppressLowAccuracy: Boolean = true,
    val stationaryCutoffKmh: Float = 2f,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists [SettingsState] plus the lifetime odometer in DataStore (local only). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val unit = stringPreferencesKey("unit")
        val decimals = intPreferencesKey("decimals")
        val theme = stringPreferencesKey("theme")
        val colorBySpeed = booleanPreferencesKey("colorBySpeed")
        val hudMirror = booleanPreferencesKey("hudMirror")
        val keepScreenOn = booleanPreferencesKey("keepScreenOn")
        val showGraph = booleanPreferencesKey("showGraph")
        val showAvg = booleanPreferencesKey("showAvg")
        val showMax = booleanPreferencesKey("showMax")
        val showDistance = booleanPreferencesKey("showDistance")
        val showAltitude = booleanPreferencesKey("showAltitude")
        val showHeading = booleanPreferencesKey("showHeading")
        val showTime = booleanPreferencesKey("showTime")
        val alertEnabled = booleanPreferencesKey("alertEnabled")
        val alertThreshold = floatPreferencesKey("alertThreshold")
        val alertFlash = booleanPreferencesKey("alertFlash")
        val alertBeep = booleanPreferencesKey("alertBeep")
        val alertVibrate = booleanPreferencesKey("alertVibrate")
        val updateRateHz = intPreferencesKey("updateRateHz")
        val suppressLowAccuracy = booleanPreferencesKey("suppressLowAccuracy")
        val stationaryCutoffKmh = floatPreferencesKey("stationaryCutoffKmh")
        val lifetimeMeters = doublePreferencesKey("lifetimeMeters")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { p ->
        val d = SettingsState()
        SettingsState(
            unit = p[Keys.unit]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() } ?: d.unit,
            decimals = p[Keys.decimals] ?: d.decimals,
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            colorBySpeed = p[Keys.colorBySpeed] ?: d.colorBySpeed,
            hudMirror = p[Keys.hudMirror] ?: d.hudMirror,
            keepScreenOn = p[Keys.keepScreenOn] ?: d.keepScreenOn,
            showGraph = p[Keys.showGraph] ?: d.showGraph,
            showAvg = p[Keys.showAvg] ?: d.showAvg,
            showMax = p[Keys.showMax] ?: d.showMax,
            showDistance = p[Keys.showDistance] ?: d.showDistance,
            showAltitude = p[Keys.showAltitude] ?: d.showAltitude,
            showHeading = p[Keys.showHeading] ?: d.showHeading,
            showTime = p[Keys.showTime] ?: d.showTime,
            alertEnabled = p[Keys.alertEnabled] ?: d.alertEnabled,
            alertThreshold = p[Keys.alertThreshold] ?: d.alertThreshold,
            alertFlash = p[Keys.alertFlash] ?: d.alertFlash,
            alertBeep = p[Keys.alertBeep] ?: d.alertBeep,
            alertVibrate = p[Keys.alertVibrate] ?: d.alertVibrate,
            updateRateHz = p[Keys.updateRateHz] ?: d.updateRateHz,
            suppressLowAccuracy = p[Keys.suppressLowAccuracy] ?: d.suppressLowAccuracy,
            stationaryCutoffKmh = p[Keys.stationaryCutoffKmh] ?: d.stationaryCutoffKmh,
        )
    }

    val lifetimeMeters: Flow<Double> = context.dataStore.data.map { it[Keys.lifetimeMeters] ?: 0.0 }

    suspend fun update(s: SettingsState) {
        context.dataStore.edit { p ->
            p[Keys.unit] = s.unit.name
            p[Keys.decimals] = s.decimals
            p[Keys.theme] = s.theme.name
            p[Keys.colorBySpeed] = s.colorBySpeed
            p[Keys.hudMirror] = s.hudMirror
            p[Keys.keepScreenOn] = s.keepScreenOn
            p[Keys.showGraph] = s.showGraph
            p[Keys.showAvg] = s.showAvg
            p[Keys.showMax] = s.showMax
            p[Keys.showDistance] = s.showDistance
            p[Keys.showAltitude] = s.showAltitude
            p[Keys.showHeading] = s.showHeading
            p[Keys.showTime] = s.showTime
            p[Keys.alertEnabled] = s.alertEnabled
            p[Keys.alertThreshold] = s.alertThreshold
            p[Keys.alertFlash] = s.alertFlash
            p[Keys.alertBeep] = s.alertBeep
            p[Keys.alertVibrate] = s.alertVibrate
            p[Keys.updateRateHz] = s.updateRateHz
            p[Keys.suppressLowAccuracy] = s.suppressLowAccuracy
            p[Keys.stationaryCutoffKmh] = s.stationaryCutoffKmh
        }
    }

    suspend fun setLifetimeMeters(meters: Double) {
        context.dataStore.edit { it[Keys.lifetimeMeters] = meters }
    }
}
