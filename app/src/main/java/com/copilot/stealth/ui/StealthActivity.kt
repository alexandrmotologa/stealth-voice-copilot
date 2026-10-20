package com.copilot.stealth.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.copilot.stealth.data.models.AudioInputSource
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.StealthUiIntent
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.engine.mock.MockInterviewManager
import com.copilot.stealth.ui.components.CarrierProfileSheet
import com.copilot.stealth.ui.components.ControlsRow
import com.copilot.stealth.ui.components.EngineSelectionDialog
import com.copilot.stealth.ui.components.FreightMathBar
import com.copilot.stealth.ui.components.MockInterviewSheet
import com.copilot.stealth.ui.components.ToolsBottomSheet
import com.copilot.stealth.ui.components.TopStatusBar
import com.copilot.stealth.ui.components.TurnCard
import com.copilot.stealth.ui.theme.LocalStealthPalette
import com.copilot.stealth.ui.theme.StealthVoiceCopilotTheme

/**
 * Primary covert lockscreen activity displaying continuous interview assistance
 * with true AMOLED black optimization, edge-to-edge insets, carrier customization,
 * mock simulator, and physical hardware volume key shortcuts.
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        configureLockscreenFlags()

        val testQuery = intent?.getStringExtra("test_query")
        if (!testQuery.isNullOrBlank()) {
            viewModel.handleManualQuery(testQuery)
        }

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
                    onCycleSttLanguage = { viewModel.processIntent(StealthUiIntent.CycleSttLanguage) },
                    onToggleWhisper = { viewModel.processIntent(StealthUiIntent.ToggleWhisperMode) },
                    onToggleCalculator = { viewModel.processIntent(StealthUiIntent.ToggleCalculator) },
                    onToggleCarrierProfile = { viewModel.processIntent(StealthUiIntent.ToggleCarrierProfileEditor) },
                    onToggleMockSimulator = { viewModel.processIntent(StealthUiIntent.ToggleMockSimulator) },
                    onSaveCarrierProfile = { profile -> viewModel.processIntent(StealthUiIntent.SaveCarrierProfile(profile)) },
                    onToggleAudioSource = {
                        val nextSource = when (uiState.audioInputSource) {
                            AudioInputSource.MICROPHONE -> AudioInputSource.SYSTEM_CALL
                            AudioInputSource.SYSTEM_CALL -> AudioInputSource.TEST_BENCH
                            AudioInputSource.TEST_BENCH -> AudioInputSource.MICROPHONE
                        }
                        viewModel.processIntent(StealthUiIntent.SetAudioSource(nextSource))
                    },
                    onRunTestBench = { viewModel.runSyntheticTestBenchBurst() },
                    onScenarioSelected = { prompt -> viewModel.handleManualQuery(prompt) },
                    onOpenEngineSelection = { viewModel.processIntent(StealthUiIntent.ToggleEngineSelection) },
                    onSelectEngine = { type -> viewModel.processIntent(StealthUiIntent.SelectEngineType(type)) },
                    onSelectLocalModel = { name -> viewModel.processIntent(StealthUiIntent.SelectLocalModel(name)) },
                    onTriggerDownload = { id -> viewModel.processIntent(StealthUiIntent.TriggerModelDownload(id)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            if (!viewModel.uiState.value.isListening) {
                viewModel.processIntent(StealthUiIntent.ToggleListening)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val testQuery = intent.getStringExtra("test_query")
        if (!testQuery.isNullOrBlank()) {
            viewModel.handleManualQuery(testQuery)
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
                    // Double click: Emergency panic wipe
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
                    // Long press detected: toggle Romanian translation
                    if (System.currentTimeMillis() - volumeDownPressStartTime > 500) {
                        viewModel.processIntent(StealthUiIntent.ToggleRomanian)
                        volumeDownPressStartTime = Long.MAX_VALUE
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
    onCycleSttLanguage: () -> Unit,
    onToggleWhisper: () -> Unit,
    onToggleCalculator: () -> Unit,
    onToggleCarrierProfile: () -> Unit,
    onToggleMockSimulator: () -> Unit,
    onSaveCarrierProfile: (CarrierProfile) -> Unit,
    onToggleAudioSource: () -> Unit,
    onRunTestBench: () -> Unit,
    onScenarioSelected: (String) -> Unit,
    onOpenEngineSelection: () -> Unit,
    onSelectEngine: (EngineType) -> Unit,
    onSelectLocalModel: (String) -> Unit,
    onTriggerDownload: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val palette = LocalStealthPalette.current
    val mockManager = remember { MockInterviewManager() }
    var isToolsSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentTurns.size, uiState.livePartialTranscript, uiState.isProcessing) {
        val totalCount = uiState.currentTurns.size +
                (if (uiState.livePartialTranscript.isNullOrBlank()) 0 else 1) +
                (if (uiState.isProcessing && uiState.livePartialTranscript.isNullOrBlank()) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimal AMOLED Top Status Bar
            TopStatusBar(
                uiState = uiState,
                onOpenTools = { isToolsSheetOpen = true },
                onOpenEngineSelection = onOpenEngineSelection
            )

            // Collapsible Freight Math Bar (if opened in settings)
            if (uiState.isCalculatorVisible) {
                FreightMathBar(
                    uiState = uiState,
                    palette = palette,
                    onQuoteSelected = onScenarioSelected,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Error banner if any
            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = com.copilot.stealth.ui.theme.StatusError,
                        fontSize = 12.sp
                    )
                }
            }

            // Teleprompter Feed (TV News Anchor High-Legibility Display)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                if (uiState.currentTurns.isEmpty() && uiState.livePartialTranscript.isNullOrBlank() && !uiState.isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isListening) palette.statusActive else palette.textMetadata)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.isListening) "● TELEPROMPTER ÎN ASCULTARE PASIVĂ" else "TELEPROMPTER ÎN AȘTEPTARE",
                                color = if (uiState.isListening) palette.statusActive else palette.textMetadata,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (uiState.isListening)
                                    "Microfonul ascultă pasiv interlocutorul.\nRăspunsul optim va apărea instant aici pe ecran."
                                else
                                    "Atinge microfonul pentru a porni ascultarea pasivă.",
                                color = palette.textMetadata,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
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
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Live partial transcript while interviewer is actively speaking
                        if (!uiState.livePartialTranscript.isNullOrBlank()) {
                            item {
                                LiveTranscriptCard(
                                    transcriptEn = uiState.livePartialTranscript,
                                    transcriptRo = uiState.livePartialTranslation,
                                    isRomanianEnabled = uiState.isRomanianEnabled
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Live prompter generating card while AI is inferring
                        if (uiState.isProcessing && uiState.livePartialTranscript.isNullOrBlank()) {
                            item {
                                ProcessingPrompterCard()
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Controls
            ControlsRow(
                uiState = uiState,
                onToggleListening = onToggleListening,
                onClearHistory = onClearHistory,
                onExportTranscript = onExportTranscript,
                onOpenTools = { isToolsSheetOpen = true }
            )
        }

        // Tools & Configuration Bottom Sheet (Clean 2026 Drawer)
        if (isToolsSheetOpen) {
            ToolsBottomSheet(
                uiState = uiState,
                onDismiss = { isToolsSheetOpen = false },
                onToggleRomanian = onToggleRomanian,
                onCycleEngine = onCycleEngine,
                onCycleSttLanguage = onCycleSttLanguage,
                onToggleWhisper = onToggleWhisper,
                onToggleOverlay = onToggleOverlay,
                onCycleTheme = onCycleTheme,
                onToggleCalculator = onToggleCalculator,
                onOpenCarrierProfile = onToggleCarrierProfile,
                onOpenMockSimulator = onToggleMockSimulator,
                onOpenEngineSelection = onOpenEngineSelection,
                onScenarioSelected = { prompt ->
                    isToolsSheetOpen = false
                    onScenarioSelected(prompt)
                }
            )
        }

        // AI Engine Selection & Setup Dialog
        if (uiState.isEngineSelectionOpen) {
            EngineSelectionDialog(
                uiState = uiState,
                onSelectEngine = onSelectEngine,
                onSelectLocalModel = onSelectLocalModel,
                onTriggerDownload = onTriggerDownload,
                onDismiss = onOpenEngineSelection
            )
        }

        // Overlay Sheets
        if (uiState.isCarrierProfileOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background.copy(alpha = 0.75f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                CarrierProfileSheet(
                    currentProfile = uiState.carrierProfile,
                    palette = palette,
                    onSaveProfile = onSaveCarrierProfile,
                    onDismiss = onToggleCarrierProfile
                )
            }
        }

        if (uiState.isMockSimulatorOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background.copy(alpha = 0.75f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                MockInterviewSheet(
                    mockManager = mockManager,
                    palette = palette,
                    onDismiss = onToggleMockSimulator
                )
            }
        }
    }
}

@Composable
fun LiveTranscriptCard(
    transcriptEn: String,
    transcriptRo: String?,
    isRomanianEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1813))
            .border(1.dp, palette.statusActive.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.statusActive)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "● ASCULTARE PASIVĂ (INTERVIEWER VORBEȘTE)",
                color = palette.statusActive,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = transcriptEn,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp
        )
        if (isRomanianEnabled && !transcriptRo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = transcriptRo,
                color = palette.textRomanian,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ProcessingPrompterCard(
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101914))
            .border(1.dp, palette.statusActive.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.statusActive)
            )
            Column {
                Text(
                    text = "TELEPROMPTER: SE GENEREAZĂ RĂSPUNSUL...",
                    color = palette.statusActive,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "⚡ Groq Cloud LPU analizează întrebarea în sub-500ms",
                    color = palette.textMetadata,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

