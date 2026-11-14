package com.example.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.MonoReadoutStyle
import kotlin.math.roundToInt

private data class TelemetryRow(
    val label: String,
    val value: String,
)

@Composable
fun DeviceInfoScreen(
    hapticsEnabled: Boolean,
    onCopySuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    val metrics = context.resources.displayMetrics
    val memInfo = remember(context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
    }

    val hardwareRows = remember {
        listOf(
            TelemetryRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}"),
            TelemetryRow("Android OS", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
            TelemetryRow("ABIs", Build.SUPPORTED_ABIS.joinToString(", ")),
            TelemetryRow("Hardware", "${Build.HARDWARE} / ${Build.BOARD}"),
        )
    }

    val displayRows = remember(configuration, density.density, density.fontScale, metrics.widthPixels, metrics.heightPixels) {
        listOf(
            TelemetryRow("Viewport (dp)", "${configuration.screenWidthDp} x ${configuration.screenHeightDp} dp"),
            TelemetryRow("Resolution (px)", "${metrics.widthPixels} x ${metrics.heightPixels} px"),
            TelemetryRow("Density", "${(density.density * 160).roundToInt()} dpi (${density.density}x)"),
            TelemetryRow("Font Scale", "${density.fontScale}x"),
        )
    }

    val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
    val availRamMb = (memInfo.availMem / (1024 * 1024)).toInt()
    val buildRows = remember(totalRamMb, availRamMb) {
        listOf(
            TelemetryRow("RAM (Avail / Total)", "$availRamMb MB / $totalRamMb MB"),
            TelemetryRow("Application ID", BuildConfig.APPLICATION_ID),
            TelemetryRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
            TelemetryRow("Build Type", BuildConfig.BUILD_TYPE.uppercase()),
        )
    }

    val formattedReport = remember(hardwareRows, displayRows, buildRows) {
        buildString {
            appendLine("=== TestBench Telemetry ===")
            (hardwareRows + displayRows + buildRows).forEach {
                appendLine("${it.label}: ${it.value}")
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hardware") {
                TelemetrySectionCard(
                    title = stringResource(R.string.device_section_hardware),
                    icon = Icons.Outlined.PhoneAndroid,
                    rows = hardwareRows,
                )
            }

            item(key = "display") {
                TelemetrySectionCard(
                    title = stringResource(R.string.device_section_display),
                    icon = Icons.Outlined.AspectRatio,
                    rows = displayRows,
                )
            }

            item(key = "memory") {
                TelemetrySectionCard(
                    title = stringResource(R.string.device_section_memory),
                    icon = Icons.Outlined.Memory,
                    rows = buildRows,
                )
            }

            item(key = "copy_action") {
                Button(
                    onClick = {
                        if (hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        clipboard.setText(AnnotatedString(formattedReport))
                        onCopySuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("copy_telemetry_button"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.copy_report))
                }
            }
        }
    }
}

@Composable
private fun TelemetrySectionCard(
    title: String,
    icon: ImageVector,
    rows: List<TelemetryRow>,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.42f),
                    )
                    Text(
                        text = row.value,
                        style = MonoReadoutStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.58f),
                    )
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }
    }
}
