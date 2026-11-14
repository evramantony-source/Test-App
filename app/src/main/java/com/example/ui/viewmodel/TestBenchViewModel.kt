package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OrbShape(val label: String) {
    CIRCLE("Orb"),
    SQUIRCLE("Tile"),
    DIAMOND("Prism"),
}

data class EventLogEntry(
    val id: Long,
    val timestamp: String,
    val message: String,
)

data class CheckItem(
    val id: String,
    val title: String,
    val detail: String,
    val checked: Boolean = false,
)

data class TestBenchUiState(
    val tapCount: Int = 0,
    val gaugeProgress: Float = 0.64f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val selectedOrbShape: OrbShape = OrbShape.CIRCLE,
    val eventLogs: List<EventLogEntry> = emptyList(),
    val checklist: List<CheckItem> = defaultChecklist(),
)

private fun defaultChecklist(): List<CheckItem> = listOf(
    CheckItem(
        id = "tap_state",
        title = "Verify reactive state counter",
        detail = "Increment or decrement the counter on the Playground tab",
    ),
    CheckItem(
        id = "theme_switch",
        title = "Test Light & Dark theme transitions",
        detail = "Switch Theme Override modes and inspect surface contrast",
    ),
    CheckItem(
        id = "overlay_ui",
        title = "Inspect Snackbar, Sheet & Dialog",
        detail = "Trigger Material 3 overlay components from Controls Lab",
    ),
    CheckItem(
        id = "gesture_physics",
        title = "Drag & release the spring orb",
        detail = "Test continuous pointer tracking inside the Gesture Lab pad",
    ),
    CheckItem(
        id = "device_telemetry",
        title = "Copy real hardware telemetry",
        detail = "Review live Build & Display metrics on the Device tab",
    ),
)

class TestBenchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TestBenchUiState())
    val uiState: StateFlow<TestBenchUiState> = _uiState.asStateFlow()

    private var nextEventId = 1L
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        recordEvent("Session initialized (TestBench ready)")
    }

    fun incrementCounter() {
        _uiState.update { state ->
            val updated = state.tapCount + 1
            state.copy(
                tapCount = updated,
                checklist = markChecked(state.checklist, "tap_state"),
                eventLogs = prependLog(state.eventLogs, "Counter incremented -> $updated"),
            )
        }
    }

    fun decrementCounter() {
        _uiState.update { state ->
            val updated = (state.tapCount - 1).coerceAtLeast(0)
            state.copy(
                tapCount = updated,
                checklist = markChecked(state.checklist, "tap_state"),
                eventLogs = prependLog(state.eventLogs, "Counter decremented -> $updated"),
            )
        }
    }

    fun resetCounter() {
        _uiState.update { state ->
            state.copy(
                tapCount = 0,
                eventLogs = prependLog(state.eventLogs, "Counter reset -> 0"),
            )
        }
    }

    fun setGaugeProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(gaugeProgress = clamped) }
    }

    fun logGaugeSettled() {
        val pct = (_uiState.value.gaugeProgress * 100).toInt()
        recordEvent("Calibration slider settled -> $pct%")
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { state ->
            state.copy(
                themeMode = mode,
                checklist = markChecked(state.checklist, "theme_switch"),
                eventLogs = prependLog(state.eventLogs, "Theme mode -> ${mode.label}"),
            )
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                dynamicColorEnabled = enabled,
                eventLogs = prependLog(
                    state.eventLogs,
                    "Dynamic color -> ${if (enabled) "ON" else "OFF"}",
                ),
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                hapticsEnabled = enabled,
                eventLogs = prependLog(
                    state.eventLogs,
                    "Haptic feedback -> ${if (enabled) "ON" else "OFF"}",
                ),
            )
        }
    }

    fun setOrbShape(shape: OrbShape) {
        _uiState.update { state ->
            state.copy(
                selectedOrbShape = shape,
                eventLogs = prependLog(state.eventLogs, "Gesture orb -> ${shape.label}"),
            )
        }
    }

    fun markOverlayTested(overlayName: String) {
        _uiState.update { state ->
            state.copy(
                checklist = markChecked(state.checklist, "overlay_ui"),
                eventLogs = prependLog(state.eventLogs, "Overlay triggered -> $overlayName"),
            )
        }
    }

    fun markGestureTested() {
        _uiState.update { state ->
            val alreadyChecked = state.checklist.firstOrNull { it.id == "gesture_physics" }?.checked == true
            state.copy(
                checklist = markChecked(state.checklist, "gesture_physics"),
                eventLogs = if (alreadyChecked) {
                    state.eventLogs
                } else {
                    prependLog(state.eventLogs, "Gesture physics verified")
                },
            )
        }
    }

    fun markTelemetryCopied() {
        _uiState.update { state ->
            state.copy(
                checklist = markChecked(state.checklist, "device_telemetry"),
                eventLogs = prependLog(state.eventLogs, "Telemetry report copied"),
            )
        }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            state.copy(
                checklist = state.checklist.map { item ->
                    if (item.id == id) item.copy(checked = !item.checked) else item
                },
            )
        }
    }

    fun resetChecklist() {
        _uiState.update { state ->
            state.copy(
                checklist = defaultChecklist(),
                eventLogs = prependLog(state.eventLogs, "Checklist reset"),
            )
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(eventLogs = emptyList()) }
    }

    fun recordEvent(message: String) {
        _uiState.update { state ->
            state.copy(eventLogs = prependLog(state.eventLogs, message))
        }
    }

    private fun markChecked(items: List<CheckItem>, id: String): List<CheckItem> =
        items.map { if (it.id == id) it.copy(checked = true) else it }

    private fun prependLog(existing: List<EventLogEntry>, message: String): List<EventLogEntry> {
        val entry = EventLogEntry(
            id = nextEventId++,
            timestamp = timeFormat.format(Date()),
            message = message,
        )
        return (listOf(entry) + existing).take(12)
    }
}
