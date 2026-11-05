package com.example.ui.screens

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.ui.AttendanceViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.TypeBadge
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.VerifiedGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SelfServiceScreen(
    viewModel: AttendanceViewModel,
    onNavigateToKiosk: () -> Unit,
    onNavigateToHistory: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()

    var selectedEmployeeIndex by remember { mutableIntStateOf(1) } // default to Alex Rivera
    val currentEmployee = employees.getOrNull(selectedEmployeeIndex) ?: employees.firstOrNull()

    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }
    var disputeSubmitted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EMPLOYEE SELF-SERVICE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "My Shift & Punch History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Surface(
                color = NavySurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Text(
                    text = "OCTOBER 2026",
                    fontSize = 11.sp,
                    color = CyanGlow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (currentEmployee != null) {
            // Employee Profile Selector Card
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NavySurfaceVariant)
                                .border(2.dp, CyanPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentEmployee.avatarUrl != null) {
                                AsyncImage(
                                    model = currentEmployee.avatarUrl,
                                    contentDescription = currentEmployee.fullName,
                                    modifier = Modifier.size(56.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Face, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(32.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentEmployee.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${currentEmployee.employeeCode} • ${currentEmployee.designation}",
                                fontSize = 12.sp,
                                color = CyanGlow
                            )
                            Text(
                                text = currentEmployee.department,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch Employee Profile Pill Selector
                    Text(
                        text = "Switch active profile preview:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        employees.forEachIndexed { index, emp ->
                            val isSelected = index == selectedEmployeeIndex
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedEmployeeIndex = index },
                                color = if (isSelected) CyanPrimary else NavySurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanPrimary else NavyBorder
                                )
                            ) {
                                Text(
                                    text = emp.fullName.split(" ").firstOrNull() ?: emp.fullName,
                                    color = if (isSelected) NavyDark else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shift Card & Today's Attendance State
            val myRecords = allRecords.filter { it.employeeCode == currentEmployee.employeeCode }
            val latestPunch = myRecords.firstOrNull()

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
                            text = "Today's Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (latestPunch != null) {
                            StatusBadge(status = latestPunch.status)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Assigned Shift", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "09:00 - 18:00",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(text = "General Shift", fontSize = 11.sp, color = CyanGlow)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Latest Punch", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(2.dp))
                                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                Text(
                                    text = if (latestPunch != null) timeFormat.format(Date(latestPunch.timestampMillis)) else "--:--",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (latestPunch != null) VerifiedGreen else Color(0xFF64748B)
                                )
                                Text(
                                    text = latestPunch?.type?.label ?: "Not Clocked In",
                                    fontSize = 11.sp,
                                    color = if (latestPunch != null) VerifiedGreen else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Working Hours & Overtime Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Working Hours (MTD)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "152.5 hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "98.2% Compliance", fontSize = 11.sp, color = VerifiedGreen)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Overtime Earned", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "+4.5 hrs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                                Text(text = "Approved", fontSize = 11.sp, color = Color(0xFFA5B4FC))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Quick Clock In + Dispute Punch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.simulateEmployeeScan(currentEmployee)
                        onNavigateToKiosk()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("self_service_punch_now"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clock In / Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { showDisputeDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("self_service_dispute_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant, contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Dispute / Regularize", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Punches List for Employee
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "My Recent Activity Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (myRecords.isEmpty()) {
                        Text(
                            text = "No logs recorded yet for this profile.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        myRecords.take(5).forEach { rec ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = NavySurfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TypeBadge(type = rec.type)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(status = rec.status)
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = dateFormat.format(Date(rec.timestampMillis)),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = "${(rec.confidenceScore * 100).toInt()}% Match",
                                        color = VerifiedGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (onNavigateToHistory != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onNavigateToHistory(currentEmployee.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("self_service_view_full_history_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View Full Attendance History (Room DB)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dispute / Regularize Modal Dialog
        if (showDisputeDialog) {
            Dialog(onDismissRequest = {
                showDisputeDialog = false
                disputeSubmitted = false
            }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Attendance Dispute / Regularization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Submit a request to HR to adjust a missed punch or correct late status.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (disputeSubmitted) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = VerifiedGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VerifiedGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Dispute #REQ-8821 submitted to HR for approval.",
                                        color = VerifiedGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    showDisputeDialog = false
                                    disputeSubmitted = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark)
                            ) {
                                Text("Close")
                            }
                        } else {
                            OutlinedTextField(
                                value = disputeReason,
                                onValueChange = { disputeReason = it },
                                modifier = Modifier.fillMaxWidth().height(110.dp),
                                placeholder = { Text("Describe reason (e.g. Client offsite meeting, network delay at kiosk)...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = NavyBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDisputeDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = Color(0xFF94A3B8))
                                }
                                Button(
                                    onClick = { disputeSubmitted = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark)
                                ) {
                                    Text("Submit", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
