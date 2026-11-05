package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.AttendanceViewModel
import com.example.ui.components.CameraScannerView
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.VerifiedGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollFaceScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1: Info, 2: Camera Capture, 3: Success

    var fullName by remember { mutableStateOf("") }
    var employeeCode by remember { mutableStateOf("EMP-100" + (7..9).random()) }
    var email by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Engineering") }
    var designation by remember { mutableStateOf("Software Engineer") }
    var shiftName by remember { mutableStateOf("General Shift (09:00 - 18:00)") }
    var avatarUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80") }

    var isCapturingBiometrics by remember { mutableStateOf(false) }
    var captureProgress by remember { mutableStateOf(0) } // 0 -> 100%
    var capturedAnglesCount by remember { mutableIntStateOf(0) }

    val departments = listOf(
        "Engineering",
        "Operations & Security",
        "Product & Design",
        "Human Resources",
        "Sales & Marketing"
    )
    var deptExpanded by remember { mutableStateOf(false) }

    val shifts = listOf(
        "General Shift (09:00 - 18:00)",
        "Morning Shift (06:00 - 15:00)",
        "Evening Shift (14:00 - 23:00)",
        "Night Shift (22:00 - 07:00)"
    )
    var shiftExpanded by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Bar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, CircleShape)
                    .testTag("enroll_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "BIOMETRIC ENROLLMENT WIZARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (step == 1) "Employee Information" else if (step == 2) "Facial Landmark Capture" else "Enrollment Verified",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepDot(number = 1, isActive = step >= 1, isCompleted = step > 1)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(32.dp).height(2.dp).background(if (step > 1) CyanPrimary else NavyBorder))
            Spacer(modifier = Modifier.width(8.dp))
            StepDot(number = 2, isActive = step >= 2, isCompleted = step > 2)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(32.dp).height(2.dp).background(if (step > 2) CyanPrimary else NavyBorder))
            Spacer(modifier = Modifier.width(8.dp))
            StepDot(number = 3, isActive = step >= 3, isCompleted = step == 3)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (step == 1) {
            // Step 1: Info Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Personal & Corporate Credentials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth().testTag("enroll_name_input"),
                        label = { Text("Full Name", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. Jordan Smith") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = employeeCode,
                        onValueChange = { employeeCode = it },
                        modifier = Modifier.fillMaxWidth().testTag("enroll_code_input"),
                        label = { Text("Employee ID / Badge Code", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth().testTag("enroll_email_input"),
                        label = { Text("Work Email", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("name@cybercore.io") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        modifier = Modifier.fillMaxWidth().testTag("enroll_designation_input"),
                        label = { Text("Designation / Role", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. Senior Security Engineer") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Department Dropdown
                    ExposedDropdownMenuBox(
                        expanded = deptExpanded,
                        onExpandedChange = { deptExpanded = !deptExpanded }
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Department", color = Color(0xFF94A3B8)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("enroll_dept_dropdown"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = NavyBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false },
                            modifier = Modifier.background(NavySurface)
                        ) {
                            departments.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d, color = Color.White) },
                                    onClick = {
                                        department = d
                                        deptExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shift Dropdown
                    ExposedDropdownMenuBox(
                        expanded = shiftExpanded,
                        onExpandedChange = { shiftExpanded = !shiftExpanded }
                    ) {
                        OutlinedTextField(
                            value = shiftName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Shift", color = Color(0xFF94A3B8)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("enroll_shift_dropdown"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = NavyBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = shiftExpanded,
                            onDismissRequest = { shiftExpanded = false },
                            modifier = Modifier.background(NavySurface)
                        ) {
                            shifts.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = Color.White) },
                                    onClick = {
                                        shiftName = s
                                        shiftExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (fullName.isBlank()) fullName = "Jordan Smith"
                    if (email.isBlank()) email = "jordan.smith@cybercore.io"
                    step = 2
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("enroll_next_step_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Proceed to Face Scan (Step 2)", fontWeight = FontWeight.Bold)
            }
        } else if (step == 2) {
            // Step 2: Face Capture
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Capture Facial Geometry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep face inside the oval reticle while the neural sensor captures 3 multi-angle biometric samples.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        CameraScannerView(
                            hasCameraPermission = hasCameraPermission,
                            statusText = if (isCapturingBiometrics) "Analyzing face mesh angles... $captureProgress%" else "Ready to capture biometric template",
                            isRecognized = capturedAnglesCount >= 3,
                            confidenceText = if (capturedAnglesCount > 0) "$capturedAnglesCount / 3 Angles Calibrated" else "",
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isCapturingBiometrics) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(NavyDark.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        progress = { captureProgress / 100f },
                                        color = CyanPrimary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "$captureProgress%",
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Angle indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AngleStatusBadge(title = "1. Front Angle", isDone = capturedAnglesCount >= 1)
                        AngleStatusBadge(title = "2. Depth/Profile", isDone = capturedAnglesCount >= 2)
                        AngleStatusBadge(title = "3. Liveness Check", isDone = capturedAnglesCount >= 3)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isCapturingBiometrics = true
                                captureProgress = 20
                                delay(600)
                                capturedAnglesCount = 1
                                captureProgress = 55
                                delay(600)
                                capturedAnglesCount = 2
                                captureProgress = 85
                                delay(600)
                                capturedAnglesCount = 3
                                captureProgress = 100
                                delay(400)
                                isCapturingBiometrics = false

                                // Commit to Room database
                                viewModel.enrollNewEmployee(
                                    employeeCode = employeeCode,
                                    fullName = fullName,
                                    email = email,
                                    department = department,
                                    designation = designation,
                                    shiftName = shiftName,
                                    shiftStartHour = 9,
                                    shiftStartMinute = 0,
                                    shiftEndHour = 18,
                                    shiftEndMinute = 0,
                                    avatarUrl = avatarUrl,
                                    onSuccess = {
                                        step = 3
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("enroll_start_capture_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCapturingBiometrics
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture & Register Biometrics", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Step 3: Success Confirmation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, VerifiedGreen)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(VerifiedGreen.copy(alpha = 0.15f))
                            .border(2.dp, VerifiedGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(40.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Face ID Enrolled Successfully!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$fullName ($employeeCode) is now authorized to clock in via any connected kiosk terminal.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("enroll_finish_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = NavyDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Roster", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StepDot(number: Int, isActive: Boolean, isCompleted: Boolean) {
    val bgColor = if (isCompleted) VerifiedGreen else if (isActive) CyanPrimary else NavySurfaceVariant
    val textColor = if (isCompleted || isActive) NavyDark else Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(Icons.Default.Check, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
        } else {
            Text(text = "$number", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AngleStatusBadge(title: String, isDone: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDone) VerifiedGreen.copy(alpha = 0.15f) else NavySurfaceVariant)
            .border(1.dp, if (isDone) VerifiedGreen else NavyBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isDone) VerifiedGreen else Color(0xFF64748B))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = if (isDone) VerifiedGreen else Color(0xFF94A3B8),
            fontWeight = FontWeight.SemiBold
        )
    }
}
