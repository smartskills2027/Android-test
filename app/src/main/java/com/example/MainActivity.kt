package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AttendanceViewModel
import com.example.ui.screens.AttendanceHistoryScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnrollFaceScreen
import com.example.ui.screens.EmployeeRosterScreen
import com.example.ui.screens.KioskTerminalScreen
import com.example.ui.screens.LiveLogsScreen
import com.example.ui.screens.SelfServiceScreen
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface

enum class AppDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    CAMERA("camera", "Camera", Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    KIOSK("kiosk", "Kiosk", Icons.Filled.Fingerprint, Icons.Outlined.Fingerprint),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    LOGS("logs", "Live Logs", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    ROSTER("roster", "Roster", Icons.Filled.Badge, Icons.Outlined.Badge),
    SELF_SERVICE("self_service", "Self-Service", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AttendanceViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var isEnrollingFace by remember { mutableStateOf(false) }
    var viewingHistoryForEmployeeId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = isEnrollingFace || viewingHistoryForEmployeeId != null || currentDestination != AppDestination.DASHBOARD) {
        if (isEnrollingFace) {
            isEnrollingFace = false
        } else if (viewingHistoryForEmployeeId != null) {
            viewingHistoryForEmployeeId = null
        } else {
            currentDestination = AppDestination.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = NavyDark,
        bottomBar = {
            if (!isEnrollingFace && viewingHistoryForEmployeeId == null) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav"),
                    containerColor = NavySurface,
                    tonalElevation = 8.dp
                ) {
                    AppDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyDark,
                                selectedTextColor = CyanPrimary,
                                indicatorColor = CyanPrimary,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("nav_item_${destination.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isEnrollingFace) {
                EnrollFaceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { isEnrollingFace = false }
                )
            } else if (viewingHistoryForEmployeeId != null) {
                AttendanceHistoryScreen(
                    viewModel = viewModel,
                    initialEmployeeId = viewingHistoryForEmployeeId,
                    onNavigateBack = { viewingHistoryForEmployeeId = null }
                )
            } else {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToKiosk = { currentDestination = AppDestination.CAMERA },
                        onNavigateToLogs = { currentDestination = AppDestination.LOGS },
                        onNavigateToEnroll = { isEnrollingFace = true },
                        onNavigateToHistory = { currentDestination = AppDestination.HISTORY }
                    )
                    AppDestination.CAMERA -> CameraScreen(
                        viewModel = viewModel
                    )
                    AppDestination.KIOSK -> KioskTerminalScreen(
                        viewModel = viewModel
                    )
                    AppDestination.HISTORY -> AttendanceHistoryScreen(
                        viewModel = viewModel,
                        initialEmployeeId = null,
                        onNavigateBack = { currentDestination = AppDestination.DASHBOARD }
                    )
                    AppDestination.LOGS -> LiveLogsScreen(
                        viewModel = viewModel
                    )
                    AppDestination.ROSTER -> EmployeeRosterScreen(
                        viewModel = viewModel,
                        onNavigateToEnroll = { isEnrollingFace = true },
                        onNavigateToHistory = { empId -> viewingHistoryForEmployeeId = empId }
                    )
                    AppDestination.SELF_SERVICE -> SelfServiceScreen(
                        viewModel = viewModel,
                        onNavigateToKiosk = { currentDestination = AppDestination.KIOSK },
                        onNavigateToHistory = { empId -> viewingHistoryForEmployeeId = empId }
                    )
                }
            }
        }
    }
}
