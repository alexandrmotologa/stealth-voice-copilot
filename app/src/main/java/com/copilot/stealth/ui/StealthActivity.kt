package com.copilot.stealth.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.copilot.stealth.data.models.AudioInputSource
import com.copilot.stealth.data.models.StealthUiIntent
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.ui.components.ControlsRow
import com.copilot.stealth.ui.components.FreightMathBar
import com.copilot.stealth.ui.components.QuickScenarioSelector
import com.copilot.stealth.ui.components.TopStatusBar
import com.copilot.stealth.ui.components.TurnCard
import com.copilot.stealth.ui.theme.LocalStealthPalette
import com.copilot.stealth.ui.theme.StealthVoiceCopilotTheme

/**
 * Primary covert lockscreen activity displaying continuous interview assistance
 * with true AMOLED black optimization, freight math HUD, and physical volume key overrides.
 */
class StealthActivity : ComponentActivity() {

    private val viewModel: StealthViewModel by viewModels()

    private var lastVolumeUpPressTime = 0L
    private var volumeDownPressStartTime = 0L

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.processIntent(StealthUiIntent.ToggleListening)
        } else {
            viewModel.processIntent(
                StealthUiIntent.OnError("Audio recording permission required for live mic capture.")
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockscreenFlags()

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            StealthVoiceCopilotTheme(themeMode = uiState.themeMode) {
                StealthScreen(
                    uiState = uiState,
                    onToggleListening = { checkAudioPermissionAndToggle() },
                    onToggleRomanian = { viewModel.processIntent(StealthUiIntent.ToggleRomanian) },
                    onClearHistory = { viewModel.processIntent(StealthUiIntent.ClearHistory) },
                    onPanicWipe = { viewModel.processIntent(StealthUiIntent.PanicWipe) },
                    onExportTranscript = { viewModel.processIntent(StealthUiIntent.ExportTranscript) },
                    onToggleOverlay = { viewModel.processIntent(StealthUiIntent.ToggleOverlayMode) },
                    onCycleEngine = { viewModel.processIntent(StealthUiIntent.CycleEngineType) },
                    onCycleTheme = { viewModel.processIntent(StealthUiIntent.CycleThemeMode) },
                    onToggleCalculator = { viewModel.processIntent(StealthUiIntent.ToggleCalculator) },
                    onToggleAudioSource = {
                        val nextSource = when (uiState.audioInputSource) {
                            AudioInputSource.MICROPHONE -> AudioInputSource.SYSTEM_CALL
                            AudioInputSource.SYSTEM_CALL -> AudioInputSource.TEST_BENCH
                            AudioInputSource.TEST_BENCH -> AudioInputSource.MICROPHONE
                        }
                        viewModel.processIntent(StealthUiIntent.SetAudioSource(nextSource))
                    },
                    onRunTestBench = { viewModel.runSyntheticTestBenchBurst() },
                    onScenarioSelected = { prompt -> viewModel.handleManualQuery(prompt) }
                )
            }
        }
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun checkAudioPermissionAndToggle() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.processIntent(StealthUiIntent.ToggleListening)
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastVolumeUpPressTime < 450) {
                    // Double click: Panic wipe RAM, clipboard and temp files
                    viewModel.processIntent(StealthUiIntent.PanicWipe)
                    lastVolumeUpPressTime = 0L
                    return true
                }
                lastVolumeUpPressTime = currentTime
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event?.repeatCount == 0) {
                    volumeDownPressStartTime = System.currentTimeMillis()
                } else if (event != null && event.repeatCount > 1) {
                    // Long press detected: toggle Romanian translations
                    if (System.currentTimeMillis() - volumeDownPressStartTime > 500) {
                        viewModel.processIntent(StealthUiIntent.ToggleRomanian)
                        volumeDownPressStartTime = Long.MAX_VALUE // prevent continuous toggle
                        return true
                    }
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun StealthScreen(
    uiState: StealthUiState,
    onToggleListening: () -> Unit,
    onToggleRomanian: () -> Unit,
    onClearHistory: () -> Unit,
    onPanicWipe: () -> Unit,
    onExportTranscript: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCycleEngine: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleCalculator: () -> Unit,
    onToggleAudioSource: () -> Unit,
    onRunTestBench: () -> Unit,
    onScenarioSelected: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val palette = LocalStealthPalette.current

    LaunchedEffect(uiState.currentTurns.size) {
        if (uiState.currentTurns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.currentTurns.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Minimal AMOLED Top Status Bar
        TopStatusBar(
            uiState = uiState,
            onToggleRomanian = onToggleRomanian,
            onCycleEngine = onCycleEngine,
            onToggleOverlay = onToggleOverlay,
            onCycleTheme = onCycleTheme,
            onToggleCalculator = onToggleCalculator
        )

        // Freight Math Calculator Bar (Collapsible)
        if (uiState.isCalculatorVisible) {
            FreightMathBar(
                uiState = uiState,
                palette = palette,
                onQuoteSelected = onScenarioSelected,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Quick freight scenario tester chips
        QuickScenarioSelector(
            onScenarioSelected = onScenarioSelected
        )

        // Error banner if any
        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = uiState.errorMessage,
                    color = com.copilot.stealth.ui.theme.StatusError,
                    fontSize = 12.sp
                )
            }
        }

        // Conversation History or Empty Stealth State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.currentTurns.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isListening) "LISTENING FOR INTERVIEWER..." else "READY - TAP MIC OR BENCH",
                        color = palette.textMetadata,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.currentTurns, key = { it.id }) { turn ->
                        TurnCard(
                            turn = turn,
                            isRomanianEnabled = uiState.isRomanianEnabled
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Bottom Controls
        ControlsRow(
            uiState = uiState,
            onToggleListening = onToggleListening,
            onClearHistory = onClearHistory,
            onToggleAudioSource = onToggleAudioSource,
            onRunTestBench = onRunTestBench,
            onExportTranscript = onExportTranscript
        )
    }
}
