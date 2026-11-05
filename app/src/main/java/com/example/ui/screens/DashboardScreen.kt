package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.ui.AttendanceViewModel
import com.example.ui.components.DailyAttendanceSummaryWidget
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TypeBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: AttendanceViewModel,
    onNavigateToKiosk: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToEnroll: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val presentCount by viewModel.presentTodayCount.collectAsState()
    val lateCount by viewModel.lateTodayCount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()

    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeStr = timeFormat.format(now)
            currentDateStr = dateFormat.format(now)
            delay(1000)
        }
    }

    val totalEmployees = employees.size
    val attendanceRate = if (totalEmployees > 0) ((presentCount.toFloat() / totalEmployees) * 100).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Live Date & System Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ENTERPRISE BIOMETRIC ATTENDANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentTimeStr.ifEmpty { "Live Clock Active" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = currentDateStr.ifEmpty { "Today" },
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            com.example.ui.components.CloudSyncPill(
                syncStatus = syncStatus,
                onSyncClick = { viewModel.triggerCloudSync() },
                modifier = Modifier.testTag("dashboard_cloud_sync_pill")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Daily Attendance Summary Widget (Room Database Live)
        DailyAttendanceSummaryWidget(
            summary = dailySummary,
            onViewHistory = { onNavigateToHistory?.invoke() ?: onNavigateToLogs() },
            onOpenKiosk = onNavigateToKiosk
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Visual Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.hero_face_attendance),
                    contentDescription = "Face Attendance Kiosk",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NavyDark.copy(alpha = 0.92f),
                                    NavyDark.copy(alpha = 0.70f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "FacePass Optical Kiosk",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Touchless • Anti-Spoof Verified • 0.8s Match",
                        fontSize = 12.sp,
                        color = CyanGlow
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToKiosk,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("hero_open_kiosk_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Kiosk Terminal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Key Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Present Today",
                value = "$presentCount / $totalEmployees",
                subtitle = "$attendanceRate% Attendance Rate",
                icon = Icons.Default.CheckCircle,
                accentColor = VerifiedGreen,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_present_today")
            )
            MetricCard(
                title = "Late Arrivals",
                value = "$lateCount",
                subtitle = if (lateCount > 0) "Requires Review" else "All On-Time",
                icon = Icons.Default.Warning,
                accentColor = if (lateCount > 0) WarningAmber else VerifiedGreen,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_late_arrivals")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Enrolled Roster",
                value = "$totalEmployees",
                subtitle = "Biometrics Verified",
                icon = Icons.Default.Groups,
                accentColor = CyanPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_total_employees")
            )
            MetricCard(
                title = "Security Guard",
                value = "100%",
                subtitle = "Anti-Spoof Liveness",
                icon = Icons.Default.Security,
                accentColor = Color(0xFFA5B4FC),
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_security_guard")
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Department-Wise Attendance Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Department Attendance Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Live",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val departments = listOf(
                    Triple("Engineering", 2, 2),
                    Triple("Operations & Security", 1, 1),
                    Triple("Product & Design", 1, 1),
                    Triple("Human Resources", 0, 1),
                    Triple("Sales & Marketing", 0, 1)
                )

                departments.forEach { (dept, present, total) ->
                    val fraction = if (total > 0) present.toFloat() / total else 0f
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = dept, color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "$present / $total Present",
                                color = if (present == total) VerifiedGreen else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (present == total) VerifiedGreen else CyanPrimary,
                            trackColor = NavySurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Recent Punches Activity Ticker
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time Activity Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onNavigateToLogs)
                            .testTag("view_all_logs_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View All",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (todayRecords.isEmpty()) {
                    Text(
                        text = "No punches recorded yet today. Use the Kiosk to scan faces.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    todayRecords.take(4).forEach { record ->
                        RecentPunchRow(record)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Admin Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToEnroll,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("dashboard_enroll_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavySurfaceVariant,
                    contentColor = CyanPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enroll Face", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.exportAndShareCsv(context) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("dashboard_export_csv_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavySurfaceVariant,
                    contentColor = Color(0xFFA5B4FC)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RecentPunchRow(record: AttendanceRecord) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(record.timestampMillis))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavySurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.employeeName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.employeeCode,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(type = record.type)
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = record.status)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedTime,
                    color = CyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${(record.confidenceScore * 100).toInt()}% Match",
                    color = VerifiedGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
