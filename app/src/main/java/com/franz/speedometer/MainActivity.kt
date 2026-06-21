package com.franz.speedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.franz.speedometer.data.SettingsState
import com.franz.speedometer.ui.SettingsScreen
import com.franz.speedometer.ui.SpeedScreen
import com.franz.speedometer.ui.theme.SpeedoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App() {
    val vm: SpeedViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SpeedoTheme(mode = settings.theme) {
        // Keep-screen-on follows the setting.
        KeepScreenOn(settings.keepScreenOn)

        // Permission flow.
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> if (granted) vm.onPermissionGranted() }
        LaunchedEffect(Unit) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) vm.onPermissionGranted()
            else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Audible/haptic alert on crossing the limit.
        LaunchedEffect(state.overLimit) {
            if (state.overLimit) {
                if (settings.alertVibrate) vibrate(context)
                if (settings.alertBeep) beep()
            }
        }

        var showSettings by rememberSaveable { mutableStateOf(false) }
        androidx.activity.compose.BackHandler(enabled = showSettings) { showSettings = false }

        Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    onChange = { next: SettingsState -> vm.updateSettings { next } },
                    onResetLifetime = vm::resetLifetime,
                    onBack = { showSettings = false },
                )
            } else {
                SpeedScreen(
                    state = state,
                    settings = settings,
                    onToggleRunning = vm::toggleRunning,
                    onResetTrip = vm::resetTrip,
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? ComponentActivity)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

private fun beep() {
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
}
