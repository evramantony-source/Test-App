package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MonoReadoutStyle
import com.example.ui.viewmodel.CheckItem
import com.example.ui.viewmodel.OrbShape
import com.example.ui.viewmodel.TestBenchUiState
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GestureLabScreen(
    uiState: TestBenchUiState,
    onOrbShapeChange: (OrbShape) -> Unit,
    onGestureTested: () -> Unit,
    onToggleChecklist: (String) -> Unit,
    onResetChecklist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    fun triggerHaptic() {
        if (uiState.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            item(key = "gesture_pad") {
                SpringGesturePadCard(
                    selectedShape = uiState.selectedOrbShape,
                    onShapeChange = {
                        triggerHaptic()
                        onOrbShapeChange(it)
                    },
                    onGestureInteracted = onGestureTested,
                    onHaptic = ::triggerHaptic,
                )
            }

            item(key = "checklist") {
                VerificationChecklistCard(
                    checklist = uiState.checklist,
                    onToggle = {
                        triggerHaptic()
                        onToggleChecklist(it)
                    },
                    onReset = {
                        triggerHaptic()
                        onResetChecklist()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpringGesturePadCard(
    selectedShape: OrbShape,
    onShapeChange: (OrbShape) -> Unit,
    onGestureInteracted: () -> Unit,
    onHaptic: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val orbColor by animateColorAsState(
        targetValue = if (isDragging) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "orb_color",
    )
    val haloColor = orbColor.copy(alpha = 0.20f)
    val centerRingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.gesture_pad_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.gesture_pad_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                OrbShape.entries.forEachIndexed { index, shape ->
                    SegmentedButton(
                        selected = selectedShape == shape,
                        onClick = { onShapeChange(shape) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = OrbShape.entries.size,
                        ),
                        modifier = Modifier.testTag("orb_shape_${shape.name.lowercase()}"),
                    ) {
                        Text(text = shape.label)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .onSizeChanged { canvasSize = it }
                    .testTag("gesture_arena_canvas"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(canvasSize) {
                            detectTapGestures { tapOffset ->
                                val maxDx = (canvasSize.width / 2f - 44f).coerceAtLeast(1f)
                                val maxDy = (canvasSize.height / 2f - 44f).coerceAtLeast(1f)
                                val targetX = (tapOffset.x - canvasSize.width / 2f).coerceIn(-maxDx, maxDx)
                                val targetY = (tapOffset.y - canvasSize.height / 2f).coerceIn(-maxDy, maxDy)
                                onHaptic()
                                onGestureInteracted()
                                scope.launch {
                                    launch {
                                        offsetX.animateTo(
                                            targetX,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                        )
                                    }
                                    launch {
                                        offsetY.animateTo(
                                            targetY,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                        )
                                    }
                                }
                            }
                        }
                        .pointerInput(canvasSize) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    onHaptic()
                                    onGestureInteracted()
                                },
                                onDragEnd = {
                                    isDragging = false
                                    scope.launch {
                                        launch {
                                            offsetX.animateTo(
                                                0f,
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                            )
                                        }
                                        launch {
                                            offsetY.animateTo(
                                                0f,
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                            )
                                        }
                                    }
                                },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val maxDx = (canvasSize.width / 2f - 44f).coerceAtLeast(1f)
                                    val maxDy = (canvasSize.height / 2f - 44f).coerceAtLeast(1f)
                                    scope.launch {
                                        offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(-maxDx, maxDx))
                                        offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-maxDy, maxDy))
                                    }
                                },
                            )
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 32.dp.toPx()
                        var x = step
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                            x += step
                        }
                        var y = step
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            y += step
                        }

                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            color = centerRingColor,
                            radius = 28.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx()),
                        )

                        val orbCenter = Offset(center.x + offsetX.value, center.y + offsetY.value)
                        drawLine(
                            color = orbColor.copy(alpha = 0.45f),
                            start = center,
                            end = orbCenter,
                            strokeWidth = 2.dp.toPx(),
                        )

                        val orbRadius = 24.dp.toPx()
                        when (selectedShape) {
                            OrbShape.CIRCLE -> {
                                drawCircle(color = haloColor, radius = orbRadius * 1.5f, center = orbCenter)
                                drawCircle(color = orbColor, radius = orbRadius, center = orbCenter)
                            }
                            OrbShape.SQUIRCLE -> {
                                val haloSide = orbRadius * 2.6f
                                drawRoundRect(
                                    color = haloColor,
                                    topLeft = Offset(orbCenter.x - haloSide / 2f, orbCenter.y - haloSide / 2f),
                                    size = Size(haloSide, haloSide),
                                    cornerRadius = CornerRadius(18.dp.toPx()),
                                )
                                val side = orbRadius * 1.9f
                                drawRoundRect(
                                    color = orbColor,
                                    topLeft = Offset(orbCenter.x - side / 2f, orbCenter.y - side / 2f),
                                    size = Size(side, side),
                                    cornerRadius = CornerRadius(12.dp.toPx()),
                                )
                            }
                            OrbShape.DIAMOND -> {
                                val outer = orbRadius * 1.65f
                                val outerPath = Path().apply {
                                    moveTo(orbCenter.x, orbCenter.y - outer)
                                    lineTo(orbCenter.x + outer, orbCenter.y)
                                    lineTo(orbCenter.x, orbCenter.y + outer)
                                    lineTo(orbCenter.x - outer, orbCenter.y)
                                    close()
                                }
                                drawPath(outerPath, color = haloColor)

                                val inner = orbRadius * 1.2f
                                val innerPath = Path().apply {
                                    moveTo(orbCenter.x, orbCenter.y - inner)
                                    lineTo(orbCenter.x + inner, orbCenter.y)
                                    lineTo(orbCenter.x, orbCenter.y + inner)
                                    lineTo(orbCenter.x - inner, orbCenter.y)
                                    close()
                                }
                                drawPath(innerPath, color = orbColor)
                            }
                        }
                    }
                }
            }

            val distance = hypot(offsetX.value, offsetY.value).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "dx=${offsetX.value.roundToInt()}  dy=${offsetY.value.roundToInt()}  dist=${distance}px",
                    style = MonoReadoutStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FilledTonalButton(
                    onClick = {
                        onHaptic()
                        scope.launch {
                            launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                            launch {
                                offsetY.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("center_orb_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.center_orb))
                }
            }
        }
    }
}

@Composable
private fun VerificationChecklistCard(
    checklist: List<CheckItem>,
    onToggle: (String) -> Unit,
    onReset: () -> Unit,
) {
    val completed = checklist.count { it.checked }
    val progress = if (checklist.isEmpty()) 0f else completed.toFloat() / checklist.size

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.checklist_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "$completed of ${checklist.size} verified",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TextButton(
                    onClick = onReset,
                    enabled = completed > 0,
                    modifier = Modifier.testTag("reset_checklist_button"),
                ) {
                    Text(text = stringResource(R.string.reset_checklist))
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                checklist.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (item.checked) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggle(item.id) }
                            .minimumInteractiveComponentSize()
                            .testTag("checklist_item_${item.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = if (item.checked) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = if (item.checked) "Completed" else "Pending",
                                tint = if (item.checked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                )
                                AnimatedVisibility(visible = !item.checked) {
                                    Text(
                                        text = item.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
