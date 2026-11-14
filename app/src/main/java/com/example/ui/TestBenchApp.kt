package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.DeviceInfoScreen
import com.example.ui.screens.GestureLabScreen
import com.example.ui.screens.PlaygroundScreen
import com.example.ui.viewmodel.TestBenchUiState
import com.example.ui.viewmodel.TestBenchViewModel
import kotlinx.coroutines.launch

const val ROUTE_PLAYGROUND = "playground"
const val ROUTE_GESTURE_LAB = "gesture_lab"
const val ROUTE_DEVICE_INFO = "device_info"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val navDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_PLAYGROUND,
        labelRes = R.string.nav_playground,
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science,
        testTag = "nav_playground",
    ),
    NavDestinationItem(
        route = ROUTE_GESTURE_LAB,
        labelRes = R.string.nav_gesture_lab,
        selectedIcon = Icons.Filled.TouchApp,
        unselectedIcon = Icons.Outlined.TouchApp,
        testTag = "nav_gesture_lab",
    ),
    NavDestinationItem(
        route = ROUTE_DEVICE_INFO,
        labelRes = R.string.nav_device_info,
        selectedIcon = Icons.Filled.Devices,
        unselectedIcon = Icons.Outlined.Devices,
        testTag = "nav_device_info",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestBenchApp(
    viewModel: TestBenchViewModel,
    uiState: TestBenchUiState,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val copiedMessage = stringResource(R.string.report_copied)

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = uiState.themeMode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        navDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = { Text(text = stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                    )
                                },
                                label = { Text(text = stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_PLAYGROUND,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_PLAYGROUND) {
                        PlaygroundScreen(
                            uiState = uiState,
                            onIncrement = viewModel::incrementCounter,
                            onDecrement = viewModel::decrementCounter,
                            onResetCounter = viewModel::resetCounter,
                            onGaugeChange = viewModel::setGaugeProgress,
                            onGaugeSettled = viewModel::logGaugeSettled,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColor,
                            onHapticsChange = viewModel::setHapticsEnabled,
                            onTriggerSnackbar = {
                                viewModel.markOverlayTested("Snackbar")
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "TestBench snackbar signal delivered (${uiState.tapCount} taps)",
                                        actionLabel = "Boost +1",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.incrementCounter()
                                    }
                                }
                            },
                            onOpenSheet = {
                                viewModel.markOverlayTested("BottomSheet")
                                showBottomSheet = true
                            },
                            onOpenDialog = {
                                viewModel.markOverlayTested("Dialog")
                                showDialog = true
                            },
                            onClearLogs = viewModel::clearLogs,
                        )
                    }

                    composable(ROUTE_GESTURE_LAB) {
                        GestureLabScreen(
                            uiState = uiState,
                            onOrbShapeChange = viewModel::setOrbShape,
                            onGestureTested = viewModel::markGestureTested,
                            onToggleChecklist = viewModel::toggleChecklistItem,
                            onResetChecklist = viewModel::resetChecklist,
                        )
                    }

                    composable(ROUTE_DEVICE_INFO) {
                        DeviceInfoScreen(
                            hapticsEnabled = uiState.hapticsEnabled,
                            onCopySuccess = {
                                viewModel.markTelemetryCopied()
                                scope.launch {
                                    snackbarHostState.showSnackbar(copiedMessage)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "TestBench Quick Summary",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    text = "Counter is at ${uiState.tapCount}, calibration signal is ${(uiState.gaugeProgress * 100).toInt()}%, and ${uiState.checklist.count { it.checked }}/${uiState.checklist.size} checks are verified.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showBottomSheet = false }) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.incrementCounter()
                            showBottomSheet = false
                        },
                        modifier = Modifier.testTag("sheet_increment_button"),
                    ) {
                        Text("Tap +1 & Close")
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                )
            },
            title = { Text("Material 3 Dialog Check") },
            text = {
                Text("This interactive dialog verifies modal focus, scrim layering, and button actions inside TestBench.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordEvent("Dialog confirmed")
                        showDialog = false
                    },
                    modifier = Modifier.testTag("dialog_confirm_button"),
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Dismiss")
                }
            },
        )
    }
}
