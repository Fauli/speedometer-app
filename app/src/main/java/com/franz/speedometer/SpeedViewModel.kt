package com.franz.speedometer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.franz.speedometer.data.SettingsRepository
import com.franz.speedometer.data.SettingsState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class GpsStatus { WAITING, OK, WEAK }

/** UI state in SI units; the UI converts to the user's chosen unit. */
data class SpeedUiState(
    val hasPermission: Boolean = false,
    val running: Boolean = true,
    val speedMps: Float = 0f,
    val maxMps: Float = 0f,
    val distanceM: Double = 0.0,
    val movingMs: Long = 0L,
    val totalMs: Long = 0L,
    val altitudeM: Double? = null,
    val bearingDeg: Float? = null,
    val lifetimeM: Double = 0.0,
    val accuracyM: Float = Float.NaN,
    val status: GpsStatus = GpsStatus.WAITING,
    val history: List<Float> = emptyList(), // recent speeds, m/s
    val overLimit: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedViewModel(app: Application) : AndroidViewModel(app) {

    private val source = SpeedSource(app)
    private val repo = SettingsRepository(app)

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _state = MutableStateFlow(SpeedUiState())
    val state: StateFlow<SpeedUiState> = _state.asStateFlow()

    // Mutable accumulators (single-threaded on the collection coroutine).
    private var maxMps = 0f
    private var distanceM = 0.0
    private var movingMs = 0L
    private var totalMs = 0L
    private var lifetimeM = 0.0
    private var persistedLifetimeM = 0.0
    private var lastNanos = 0L
    private val history = ArrayDeque<Float>()
    private var started = false

    @Volatile private var s: SettingsState = SettingsState()

    init {
        viewModelScope.launch {
            repo.settings.collect { s = it; _settings.value = it }
        }
        viewModelScope.launch {
            lifetimeM = repo.lifetimeMeters.first()
            persistedLifetimeM = lifetimeM
            _state.value = _state.value.copy(lifetimeM = lifetimeM)
        }
    }

    fun onPermissionGranted() {
        if (started) return
        started = true
        _state.value = _state.value.copy(hasPermission = true)
        viewModelScope.launch {
            _settings.map { it.updateRateHz }.distinctUntilChanged()
                .flatMapLatest { hz -> source.readings(if (hz >= 2) 500L else 1000L) }
                .collect { process(it) }
        }
    }

    private fun process(r: SpeedReading) {
        val cfg = s
        val running = _state.value.running

        // dt since last fix, ignoring gaps.
        val dtMs = if (lastNanos == 0L) 0L else (r.elapsedRealtimeNanos - lastNanos) / 1_000_000L
        lastNanos = r.elapsedRealtimeNanos
        val dt = if (dtMs in 1..MAX_GAP_MS) dtMs else 0L

        val rawMps = if (r.hasSpeed) r.speedMetersPerSecond else 0f
        val reliable = !cfg.suppressLowAccuracy ||
            (r.speedAccuracyMps != null && r.speedAccuracyMps <= SUPPRESS_SPEED_ACC_MPS)

        val cutoffMps = cfg.stationaryCutoffKmh / 3.6f
        val shownMps = if (!reliable || rawMps < cutoffMps) 0f else rawMps
        val moving = shownMps > 0f

        if (running && dt > 0L) {
            totalMs += dt
            if (moving) {
                val inc = shownMps.toDouble() * dt / 1000.0
                distanceM += inc
                movingMs += dt
                lifetimeM += inc
                maybePersistLifetime()
            }
        }
        if (reliable) maxMps = maxOf(maxMps, shownMps)

        history.addLast(shownMps)
        while (history.size > HISTORY_SIZE) history.removeFirst()

        val status = when {
            !r.hasSpeed -> GpsStatus.WAITING
            !reliable || r.horizontalAccuracyM.isNaN() || r.horizontalAccuracyM > WEAK_ACCURACY_M ->
                GpsStatus.WEAK
            else -> GpsStatus.OK
        }
        val overLimit = cfg.alertEnabled && shownMps * cfg.unit.speedFactor > cfg.alertThreshold

        _state.value = _state.value.copy(
            speedMps = shownMps,
            maxMps = maxMps,
            distanceM = distanceM,
            movingMs = movingMs,
            totalMs = totalMs,
            altitudeM = r.altitudeM,
            bearingDeg = r.bearingDeg,
            lifetimeM = lifetimeM,
            accuracyM = r.horizontalAccuracyM,
            status = status,
            history = history.toList(),
            overLimit = overLimit,
        )
    }

    fun toggleRunning() {
        _state.value = _state.value.copy(running = !_state.value.running)
        if (!_state.value.running) forcePersistLifetime()
    }

    fun resetTrip() {
        maxMps = 0f; distanceM = 0.0; movingMs = 0L; totalMs = 0L; lastNanos = 0L
        history.clear()
        forcePersistLifetime()
        _state.value = _state.value.copy(
            maxMps = 0f, distanceM = 0.0, movingMs = 0L, totalMs = 0L,
            history = emptyList(),
        )
    }

    fun updateSettings(transform: (SettingsState) -> SettingsState) {
        val next = transform(s)
        viewModelScope.launch { repo.update(next) }
    }

    fun resetLifetime() {
        lifetimeM = 0.0; persistedLifetimeM = 0.0
        viewModelScope.launch { repo.setLifetimeMeters(0.0) }
        _state.value = _state.value.copy(lifetimeM = 0.0)
    }

    private fun maybePersistLifetime() {
        if (lifetimeM - persistedLifetimeM >= PERSIST_EVERY_M) forcePersistLifetime()
    }

    private fun forcePersistLifetime() {
        persistedLifetimeM = lifetimeM
        val v = lifetimeM
        viewModelScope.launch { repo.setLifetimeMeters(v) }
    }

    override fun onCleared() {
        forcePersistLifetime()
        super.onCleared()
    }

    companion object {
        private const val SUPPRESS_SPEED_ACC_MPS = 2f
        private const val WEAK_ACCURACY_M = 25f
        private const val MAX_GAP_MS = 5000L
        private const val HISTORY_SIZE = 150
        private const val PERSIST_EVERY_M = 100.0
    }
}
