package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.ui.AttendanceViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    viewModel: AttendanceViewModel,
    initialEmployeeId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val employees by viewModel.employees.collectAsState()
    val selectedEmployee by viewModel.historySelectedEmployee.collectAsState()
    val historyRecords by viewModel.currentEmployeeHistory.collectAsState()
    val activeDateRange by viewModel.historyDateRangeFilter.collectAsState()
    val activeStatusFilter by viewModel.historyStatusFilter.collectAsState()
    val activePunchType by viewModel.historyPunchTypeFilter.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()

    var selectedRecordForAudit by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Synchronize initial employee selection if passed
    LaunchedEffect(initialEmployeeId, employees) {
        if (initialEmployeeId != null && employees.isNotEmpty()) {
            val targetEmp = employees.find { it.id == initialEmployeeId }
            if (targetEmp != null && selectedEmployee?.id != targetEmp.id) {
                viewModel.selectEmployeeForHistory(targetEmp)
            }
        } else if (selectedEmployee == null && employees.isNotEmpty()) {
            viewModel.selectEmployeeForHistory(employees.first())
        }
    }

    val activeEmployee = selectedEmployee ?: employees.firstOrNull()

    // Aggregate statistics for the current active employee
    val totalRecordsCount = historyRecords.size
    val onTimeCount = historyRecords.count { it.status == AttendanceStatus.ON_TIME }
    val lateCount = historyRecords.count { it.status == AttendanceStatus.LATE }
    val syncedCount = historyRecords.count { it.isSynced }
    val onTimePercentage = if (totalRecordsCount > 0) (onTimeCount * 100) / totalRecordsCount else 100

    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .testTag("attendance_history_screen")
    ) {
        // Top Navigation Header
        Surface(
            color = NavySurface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("history_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Room Database Local Cache",
                            fontSize = 11.sp,
                            color = CyanGlow
                        )
                    }
                }

                Surface(
                    color = NavySurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = VerifiedGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$syncedCount/$totalRecordsCount Synced",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Employee Switcher Pill Row
            item {
                Column {
                    Text(
                        text = "SELECT EMPLOYEE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("history_employee_selector"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        employees.forEach { emp ->
                            val isSelected = activeEmployee?.id == emp.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectEmployeeForHistory(emp) }
                                    .testTag("history_emp_pill_${emp.employeeCode}"),
                                color = if (isSelected) CyanPrimary else NavySurfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanGlow else NavyBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) NavyDark else NavySurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (emp.avatarUrl != null) {
                                            AsyncImage(
                                                model = emp.avatarUrl,
                                                contentDescription = emp.fullName,
                                                modifier = Modifier.size(20.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Face,
                                                contentDescription = null,
                                                tint = if (isSelected) CyanPrimary else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = emp.fullName,
                                        color = if (isSelected) NavyDark else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active Employee Profile Header Card
            if (activeEmployee != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(NavySurfaceVariant)
                                        .border(2.dp, CyanPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeEmployee.avatarUrl != null) {
                                        AsyncImage(
                                            model = activeEmployee.avatarUrl,
                                            contentDescription = activeEmployee.fullName,
                                            modifier = Modifier.size(52.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Face,
                                            contentDescription = null,
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = activeEmployee.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Surface(
                                            color = VerifiedGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, VerifiedGreen)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VerifiedGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${activeEmployee.employeeCode} • ${activeEmployee.designation}",
                                        fontSize = 12.sp,
                                        color = CyanGlow
                                    )
                                    Text(
                                        text = "${activeEmployee.department} • Shift: ${String.format(Locale.US, "%02d:%02d - %02d:%02d", activeEmployee.shiftStartHour, activeEmployee.shiftStartMinute, activeEmployee.shiftEndHour, activeEmployee.shiftEndMinute)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Stats Grid: Total Records, On-Time %, Late, Synced
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = NavySurfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Total Logs", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = "$totalRecordsCount",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = NavySurfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "On-Time Rate", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = "$onTimePercentage%",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (onTimePercentage >= 80) VerifiedGreen else WarningAmber
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = NavySurfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Late Days", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = "$lateCount",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (lateCount > 0) WarningAmber else Color.White
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = NavySurfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Cloud Sync", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text(
                                            text = "$syncedCount",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search and Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setHistorySearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        placeholder = { Text("Search by terminal, status, or date...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setHistorySearchQuery("") }) {
                                    Text("Clear", color = CyanGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Date Range Filters: All, Today, Last 7 Days, Last 30 Days
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All Time",
                            "TODAY" to "Today",
                            "WEEK" to "Last 7 Days",
                            "MONTH" to "This Month"
                        ).forEach { (key, label) ->
                            val isSelected = activeDateRange == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setHistoryDateRangeFilter(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = NavyDark,
                                    containerColor = NavySurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = NavyBorder,
                                    selectedBorderColor = CyanPrimary
                                ),
                                modifier = Modifier.testTag("history_date_chip_$key")
                            )
                        }
                    }

                    // Punch Type and Status Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activePunchType == null,
                            onClick = { viewModel.setHistoryPunchTypeFilter(null) },
                            label = { Text("All Punches", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = NavyDark,
                                containerColor = NavySurfaceVariant,
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = activePunchType == null,
                                borderColor = NavyBorder,
                                selectedBorderColor = CyanPrimary
                            ),
                            modifier = Modifier.testTag("history_filter_all_types")
                        )

                        AttendanceType.values().forEach { type ->
                            val isSelected = activePunchType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setHistoryPunchTypeFilter(if (isSelected) null else type)
                                },
                                label = { Text(type.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = NavyDark,
                                    containerColor = NavySurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = NavyBorder,
                                    selectedBorderColor = CyanPrimary
                                ),
                                modifier = Modifier.testTag("history_filter_type_${type.name}")
                            )
                        }

                        // Status quick filter
                        listOf(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.OVERTIME).forEach { status ->
                            val isSelected = activeStatusFilter == status
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setHistoryStatusFilter(if (isSelected) null else status)
                                },
                                label = { Text(status.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (status == AttendanceStatus.ON_TIME) VerifiedGreen else WarningAmber,
                                    selectedLabelColor = NavyDark,
                                    containerColor = NavySurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = NavyBorder,
                                    selectedBorderColor = if (status == AttendanceStatus.ON_TIME) VerifiedGreen else WarningAmber
                                ),
                                modifier = Modifier.testTag("history_filter_status_${status.name}")
                            )
                        }
                    }
                }
            }

            // Results Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAST ATTENDANCE RECORDS (${historyRecords.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "Queried from Room DB",
                        fontSize = 11.sp,
                        color = CyanGlow
                    )
                }
            }

            // List of Past Attendance Records
            if (historyRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Attendance Records Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No matching punches stored in Room for the selected filters. Use the camera or kiosk to record a new biometric entry.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(historyRecords, key = { it.id }) { record ->
                    AttendanceRecordCard(
                        record = record,
                        dateFormat = dateFormat,
                        timeFormat = timeFormat,
                        onClick = { selectedRecordForAudit = record }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Biometric Record Audit Dialog
    if (selectedRecordForAudit != null) {
        val record = selectedRecordForAudit!!
        Dialog(onDismissRequest = { selectedRecordForAudit = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("history_audit_dialog"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BIOMETRIC AUDIT CERTIFICATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Record #${record.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = if (record.isSynced) VerifiedGreen.copy(alpha = 0.15f) else CyanPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (record.isSynced) VerifiedGreen else CyanPrimary
                            )
                        ) {
                            Text(
                                text = if (record.isSynced) "CLOUD SYNCED" else "ROOM LOCAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (record.isSynced) VerifiedGreen else CyanPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Employee info banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NavySurfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NavySurface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (record.photoThumbnail != null) {
                                    AsyncImage(
                                        model = record.photoThumbnail,
                                        contentDescription = record.employeeName,
                                        modifier = Modifier.size(44.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = CyanPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = record.employeeName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${record.employeeCode} • ${record.department}",
                                    fontSize = 11.sp,
                                    color = CyanGlow
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audit Details
                    val recordDate = Date(record.timestampMillis)
                    AuditDetailRow("Date", dateFormat.format(recordDate))
                    AuditDetailRow("Time", timeFormat.format(recordDate))
                    AuditDetailRow("Punch Action", record.type.label)
                    AuditDetailRow("Punctuality Status", record.status.label)
                    AuditDetailRow("Biometric Match", String.format(Locale.US, "%.1f%% Confidence", record.confidenceScore * 100))
                    AuditDetailRow("Liveness Test", if (record.livenessVerified) "PASSED (Live Blink/Motion)" else "UNCONFIRMED")
                    AuditDetailRow("Anti-Spoofing", if (record.antiSpoofPassed) "PASSED (3D Depth Verified)" else "FAILED")
                    AuditDetailRow("Terminal Station", record.terminalName)
                    AuditDetailRow("Location Gate", record.locationName)
                    AuditDetailRow("Storage Origin", "Room Database (`attendance_records`)")
                    if (record.firestoreId != null) {
                        AuditDetailRow("Firestore Doc ID", record.firestoreId)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    androidx.compose.material3.Button(
                        onClick = { selectedRecordForAudit = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss Certificate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRecordCard(
    record: AttendanceRecord,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val recordDate = Date(record.timestampMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("history_record_card_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Date & Time, Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(recordDate),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeBadge(type = record.type)
                    StatusBadge(status = record.status)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Exact Timestamp, Terminal, Match Confidence
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavySurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = timeFormat.format(recordDate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.terminalName,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Confidence %
                    Surface(
                        color = VerifiedGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%% Match", record.confidenceScore * 100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VerifiedGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Biometric Security Indicators + Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (record.livenessVerified) VerifiedGreen else WarningAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (record.livenessVerified) "Liveness Passed" else "Unverified",
                            fontSize = 10.sp,
                            color = if (record.livenessVerified) VerifiedGreen else WarningAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (record.antiSpoofPassed) VerifiedGreen else AlertRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (record.antiSpoofPassed) "Anti-Spoof Passed" else "Spoof Alert",
                            fontSize = 10.sp,
                            color = if (record.antiSpoofPassed) VerifiedGreen else AlertRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (record.isSynced) Icons.Default.CloudDone else Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (record.isSynced) VerifiedGreen else CyanPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (record.isSynced) "Cloud Synced" else "Room Cached",
                        fontSize = 10.sp,
                        color = if (record.isSynced) VerifiedGreen else CyanPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
