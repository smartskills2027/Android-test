package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.biometrics.FaceMatchResult
import com.example.data.biometrics.FaceRecognitionEngine
import com.example.data.biometrics.LivenessAnalysisResult
import com.example.data.biometrics.LivenessChallenge
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import com.example.data.repository.AttendanceRepository
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.FirestoreAttendanceSyncService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class KioskScanState(
    val selectedPunchType: AttendanceType = AttendanceType.CHECK_IN,
    val isScanning: Boolean = false,
    val scanStatus: String = "Position face within biometric guide",
    val verifiedEmployee: Employee? = null,
    val lastRecord: AttendanceRecord? = null,
    val confidence: Float = 0f,
    val showSuccessPopup: Boolean = false,
    val selectedTerminal: String = "Reception Kiosk #1",
    val failureMessage: String? = null
)

sealed interface FaceScanUiState {
    object Idle : FaceScanUiState
    data class Processing(val message: String) : FaceScanUiState
    data class Verified(
        val employee: Employee,
        val record: AttendanceRecord,
        val confidence: Float
    ) : FaceScanUiState
    data class Rejected(
        val reason: String,
        val confidence: Float
    ) : FaceScanUiState
}

data class HistoryFilterCriteria(
    val employee: Employee? = null,
    val punchType: AttendanceType? = null,
    val status: AttendanceStatus? = null,
    val dateRange: String = "ALL",
    val searchQuery: String = ""
)

data class DailyAttendanceSummary(
    val totalSuccessfulCheckIns: Int = 0,
    val uniqueEmployeesPresent: Int = 0,
    val onTimeCount: Int = 0,
    val lateCount: Int = 0,
    val totalEmployees: Int = 0,
    val attendanceRatePercentage: Int = 0,
    val recentCheckIns: List<AttendanceRecord> = emptyList()
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database, viewModelScope)

    // Room-to-Firestore syncing service coordinating cloud persistence
    val syncService = FirestoreAttendanceSyncService(application, database, viewModelScope)
    val syncStatus: StateFlow<CloudSyncStatus> = syncService.syncStatus

    // High-level facial scan processing UI state
    private val _faceScanState = MutableStateFlow<FaceScanUiState>(FaceScanUiState.Idle)
    val faceScanState: StateFlow<FaceScanUiState> = _faceScanState.asStateFlow()

    // Kiosk state
    private val _kioskState = MutableStateFlow(KioskScanState())
    val kioskState: StateFlow<KioskScanState> = _kioskState.asStateFlow()

    // Employees and attendance reactive streams
    val employees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getTodayEndMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    val todayRecords: StateFlow<List<AttendanceRecord>> = repository.getRecordsForDay(getTodayStartMillis(), getTodayEndMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presentTodayCount: StateFlow<Int> = repository.getPresentTodayCount(getTodayStartMillis(), getTodayEndMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lateTodayCount: StateFlow<Int> = repository.getLateTodayCount(getTodayStartMillis(), getTodayEndMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successfulCheckInsTodayCount: StateFlow<Int> = repository.getSuccessfulCheckInsCountToday(getTodayStartMillis(), getTodayEndMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successfulCheckInsToday: StateFlow<List<AttendanceRecord>> = repository.getSuccessfulCheckInsToday(getTodayStartMillis(), getTodayEndMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummary: StateFlow<DailyAttendanceSummary> = combine(
        successfulCheckInsTodayCount,
        presentTodayCount,
        lateTodayCount,
        employees,
        successfulCheckInsToday
    ) { checkInsCount, presentCount, lateCount, empList, checkInRecords ->
        val totalEmp = empList.size
        val rate = if (totalEmp > 0) ((presentCount.toFloat() / totalEmp) * 100).toInt() else 0
        val onTime = (checkInsCount - lateCount).coerceAtLeast(0)
        DailyAttendanceSummary(
            totalSuccessfulCheckIns = checkInsCount,
            uniqueEmployeesPresent = presentCount,
            onTimeCount = onTime,
            lateCount = lateCount,
            totalEmployees = totalEmp,
            attendanceRatePercentage = rate,
            recentCheckIns = checkInRecords
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyAttendanceSummary())

    // Logs search & filter state
    val searchQuery = MutableStateFlow("")
    val departmentFilter = MutableStateFlow<String?>(null)
    val statusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val typeFilter = MutableStateFlow<AttendanceType?>(null)

    val filteredRecords: StateFlow<List<AttendanceRecord>> = combine(
        allRecords,
        searchQuery,
        departmentFilter,
        statusFilter,
        typeFilter
    ) { records, query, dept, status, type ->
        records.filter { record ->
            val matchesQuery = query.isBlank() ||
                record.employeeName.contains(query, ignoreCase = true) ||
                record.employeeCode.contains(query, ignoreCase = true)
            val matchesDept = dept == null || record.department == dept
            val matchesStatus = status == null || record.status == status
            val matchesType = type == null || record.type == type

            matchesQuery && matchesDept && matchesStatus && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Employee Attendance History State & Filters
    val historySelectedEmployee = MutableStateFlow<Employee?>(null)
    val historyPunchTypeFilter = MutableStateFlow<AttendanceType?>(null)
    val historyStatusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val historyDateRangeFilter = MutableStateFlow("ALL") // "ALL", "TODAY", "WEEK", "MONTH"
    val historySearchQuery = MutableStateFlow("")

    private val historyFilterCriteria: kotlinx.coroutines.flow.Flow<HistoryFilterCriteria> = combine(
        historySelectedEmployee,
        historyPunchTypeFilter,
        historyStatusFilter,
        historyDateRangeFilter,
        historySearchQuery
    ) { selectedEmp, punchType, status, range, query ->
        HistoryFilterCriteria(selectedEmp, punchType, status, range, query)
    }

    val currentEmployeeHistory: StateFlow<List<AttendanceRecord>> = combine(
        allRecords,
        historyFilterCriteria
    ) { records, criteria ->
        val empCode = criteria.employee?.employeeCode
        val now = System.currentTimeMillis()
        val minTimestamp = when (criteria.dateRange) {
            "TODAY" -> getTodayStartMillis()
            "WEEK" -> now - 7L * 24 * 60 * 60 * 1000
            "MONTH" -> now - 30L * 24 * 60 * 60 * 1000
            else -> 0L
        }

        records.filter { record ->
            val matchesEmployee = if (criteria.employee == null) true else record.employeeCode == empCode
            val matchesType = criteria.punchType == null || record.type == criteria.punchType
            val matchesStatus = criteria.status == null || record.status == criteria.status
            val matchesTime = record.timestampMillis >= minTimestamp
            val matchesQuery = criteria.searchQuery.isBlank() ||
                record.terminalName.contains(criteria.searchQuery, ignoreCase = true) ||
                record.locationName.contains(criteria.searchQuery, ignoreCase = true) ||
                record.status.label.contains(criteria.searchQuery, ignoreCase = true) ||
                record.type.label.contains(criteria.searchQuery, ignoreCase = true)

            matchesEmployee && matchesType && matchesStatus && matchesTime && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectEmployeeForHistory(employee: Employee?) {
        historySelectedEmployee.value = employee
    }

    fun setHistoryPunchTypeFilter(type: AttendanceType?) {
        historyPunchTypeFilter.value = type
    }

    fun setHistoryStatusFilter(status: AttendanceStatus?) {
        historyStatusFilter.value = status
    }

    fun setHistoryDateRangeFilter(range: String) {
        historyDateRangeFilter.value = range
    }

    fun setHistorySearchQuery(query: String) {
        historySearchQuery.value = query
    }

    /**
     * Direct query to Room database for records matching an employee ID.
     */
    fun getRecordsForEmployeeFromRoom(employeeId: Long): kotlinx.coroutines.flow.Flow<List<AttendanceRecord>> {
        return repository.getRecordsForEmployee(employeeId)
    }

    /**
     * Direct query to Room database for records matching an employee code.
     */
    fun getRecordsForEmployeeCodeFromRoom(employeeCode: String): kotlinx.coroutines.flow.Flow<List<AttendanceRecord>> {
        return repository.getRecordsForEmployeeCode(employeeCode)
    }

    init {
        // Automatically sync pending offline records to Cloud Firestore on initialization
        viewModelScope.launch {
            delay(1500)
            coordinateFirestoreUpload()
        }
    }

    /**
     * Handles the business logic for processing scanned face data.
     * Evaluates face embedding vectors against the enrolled roster using cosine similarity,
     * verifies liveness and anti-spoof integrity, and commits attendance records to local Room storage before
     * coordinating cloud synchronization.
     */
    fun processScannedFaceData(
        scannedVector: List<Float>?,
        livenessResult: LivenessAnalysisResult? = null,
        onResult: ((FaceMatchResult) -> Unit)? = null
    ) {
        if (_kioskState.value.isScanning) return

        viewModelScope.launch {
            _faceScanState.value = FaceScanUiState.Processing("Analyzing facial geometry & verifying liveness...")
            _kioskState.value = _kioskState.value.copy(
                isScanning = true,
                scanStatus = "Analyzing facial geometry & liveness...",
                failureMessage = null
            )

            delay(850) // Optical sensor acquisition & landmark processing latency

            // Check liveness / anti-spoof validation
            if (livenessResult != null && livenessResult.isSpoofDetected) {
                triggerBiometricAlertHaptics()
                val spoofMessage = livenessResult.spoofReason ?: "Anti-Spoof Warning: Static photo or video replay detected!"
                _faceScanState.value = FaceScanUiState.Rejected(
                    reason = spoofMessage,
                    confidence = 0f
                )
                _kioskState.value = _kioskState.value.copy(
                    isScanning = false,
                    scanStatus = "Access Denied: Spoof Detected",
                    verifiedEmployee = null,
                    confidence = 0f,
                    showSuccessPopup = false,
                    failureMessage = spoofMessage
                )
                onResult?.invoke(
                    FaceMatchResult(
                        matchedEmployee = null,
                        confidence = 0f,
                        livenessPassed = false,
                        antiSpoofPassed = false,
                        message = spoofMessage
                    )
                )
                return@launch
            }

            if (livenessResult != null && !livenessResult.isLive) {
                triggerBiometricAlertHaptics()
                val liveFailMessage = "Liveness unconfirmed (${(livenessResult.livenessScore * 100).toInt()}%). Please blink naturally."
                _faceScanState.value = FaceScanUiState.Rejected(
                    reason = liveFailMessage,
                    confidence = 0f
                )
                _kioskState.value = _kioskState.value.copy(
                    isScanning = false,
                    scanStatus = "Liveness Verification Incomplete",
                    verifiedEmployee = null,
                    confidence = 0f,
                    showSuccessPopup = false,
                    failureMessage = liveFailMessage
                )
                onResult?.invoke(
                    FaceMatchResult(
                        matchedEmployee = null,
                        confidence = 0f,
                        livenessPassed = false,
                        antiSpoofPassed = true,
                        message = liveFailMessage
                    )
                )
                return@launch
            }

            val vectorToMatch = scannedVector ?: FaceRecognitionEngine.generateUnknownFaceVector()
            val matchResult = FaceRecognitionEngine.findBestMatch(vectorToMatch, employees.value)

            val finalMatchResult = if (livenessResult != null) {
                matchResult.copy(
                    livenessPassed = livenessResult.isLive,
                    antiSpoofPassed = !livenessResult.isSpoofDetected
                )
            } else {
                matchResult
            }

            handleMatchResult(finalMatchResult)
            onResult?.invoke(finalMatchResult)
        }
    }

    /**
     * Backward-compatible alias for processing scanned face vectors.
     */
    fun processFaceVector(inputVector: List<Float>, livenessResult: LivenessAnalysisResult? = null) {
        processScannedFaceData(inputVector, livenessResult)
    }

    /**
     * Coordinates the Firestore upload of attendance records from Room to Cloud Firestore.
     */
    fun coordinateFirestoreUpload(onComplete: ((Boolean, String?) -> Unit)? = null) {
        syncService.syncPendingRecords(onComplete)
    }

    /**
     * Backward-compatible alias for manual cloud synchronization.
     */
    fun triggerCloudSync(onComplete: ((Boolean, String?) -> Unit)? = null) {
        coordinateFirestoreUpload(onComplete)
    }

    fun setPunchType(type: AttendanceType) {
        _kioskState.value = _kioskState.value.copy(selectedPunchType = type)
    }

    fun setTerminal(terminal: String) {
        _kioskState.value = _kioskState.value.copy(selectedTerminal = terminal)
    }

    /**
     * Simulates scanning an enrolled employee with perturbed facial features.
     */
    fun simulateEmployeeScan(employee: Employee) {
        if (_kioskState.value.isScanning) return
        viewModelScope.launch {
            _faceScanState.value = FaceScanUiState.Processing("Scanning ${employee.fullName}...")
            _kioskState.value = _kioskState.value.copy(
                isScanning = true,
                scanStatus = "Biometric scan in progress for ${employee.fullName}...",
                failureMessage = null
            )
            delay(1000)
            val testVector = FaceRecognitionEngine.generatePerturbedScanVector(employee)
            val matchResult = FaceRecognitionEngine.findBestMatch(testVector, employees.value)
            handleMatchResult(matchResult)
        }
    }

    /**
     * Simulates an unknown visitor or anti-spoof rejection.
     */
    fun simulateUnknownVisitorScan() {
        if (_kioskState.value.isScanning) return
        viewModelScope.launch {
            _faceScanState.value = FaceScanUiState.Processing("Extracting facial descriptor...")
            _kioskState.value = _kioskState.value.copy(
                isScanning = true,
                scanStatus = "Extracting facial descriptor...",
                failureMessage = null
            )
            delay(1000)
            val unknownVector = FaceRecognitionEngine.generateUnknownFaceVector()
            val matchResult = FaceRecognitionEngine.findBestMatch(unknownVector, employees.value)
            handleMatchResult(matchResult)
        }
    }

    private suspend fun handleMatchResult(matchResult: FaceMatchResult) {
        val matched = matchResult.matchedEmployee
        if (matched != null && matchResult.livenessPassed && matchResult.antiSpoofPassed) {
            triggerBiometricSuccessAudioAndHaptics()
            val savedRecord = repository.recordAttendance(
                employee = matched,
                type = _kioskState.value.selectedPunchType,
                confidence = matchResult.confidence,
                livenessVerified = matchResult.livenessPassed,
                antiSpoofPassed = matchResult.antiSpoofPassed,
                terminalName = _kioskState.value.selectedTerminal
            )

            _faceScanState.value = FaceScanUiState.Verified(
                employee = matched,
                record = savedRecord,
                confidence = matchResult.confidence
            )

            _kioskState.value = _kioskState.value.copy(
                isScanning = false,
                scanStatus = "Verified: ${matched.fullName}",
                verifiedEmployee = matched,
                lastRecord = savedRecord,
                confidence = matchResult.confidence,
                showSuccessPopup = true,
                failureMessage = null
            )

            // Coordinate immediate background upload to Cloud Firestore
            coordinateFirestoreUpload()
        } else {
            triggerBiometricAlertHaptics()
            val rejectReason = when {
                !matchResult.antiSpoofPassed -> "Access Denied: Anti-spoof attack detected."
                !matchResult.livenessPassed -> "Access Denied: Liveness verification failed."
                else -> "Access Denied: Biometric profile not recognized."
            }
            _faceScanState.value = FaceScanUiState.Rejected(
                reason = rejectReason,
                confidence = matchResult.confidence
            )

            _kioskState.value = _kioskState.value.copy(
                isScanning = false,
                scanStatus = "Verification failed. Please retry.",
                verifiedEmployee = null,
                confidence = matchResult.confidence,
                showSuccessPopup = false,
                failureMessage = rejectReason
            )
        }
    }

    fun dismissSuccessPopup() {
        _faceScanState.value = FaceScanUiState.Idle
        _kioskState.value = _kioskState.value.copy(
            showSuccessPopup = false,
            verifiedEmployee = null,
            scanStatus = "Ready for next employee"
        )
    }

    fun clearFailureMessage() {
        _faceScanState.value = FaceScanUiState.Idle
        _kioskState.value = _kioskState.value.copy(failureMessage = null)
    }

    fun enrollNewEmployee(
        employeeCode: String,
        fullName: String,
        email: String,
        department: String,
        designation: String,
        shiftName: String,
        shiftStartHour: Int,
        shiftStartMinute: Int,
        shiftEndHour: Int,
        shiftEndMinute: Int,
        avatarUrl: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val faceVector = FaceRecognitionEngine.generateDeterministicEmbedding(employeeCode)
            repository.enrollEmployee(
                employeeCode = employeeCode,
                fullName = fullName,
                email = email,
                department = department,
                designation = designation,
                shiftName = shiftName,
                shiftStartHour = shiftStartHour,
                shiftStartMinute = shiftStartMinute,
                shiftEndHour = shiftEndHour,
                shiftEndMinute = shiftEndMinute,
                avatarUrl = avatarUrl,
                faceVector = faceVector
            )
            triggerBiometricSuccessAudioAndHaptics()
            onSuccess()
        }
    }

    fun exportAndShareCsv(context: Context) {
        val csv = repository.exportToCsv(allRecords.value)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_TITLE, "FaceAttend_Realtime_Audit_Logs.csv")
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Attendance Logs")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    private fun triggerBiometricSuccessAudioAndHaptics() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (ignored: Exception) {}

        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(75)
            }
        } catch (ignored: Exception) {}
    }

    private fun triggerBiometricAlertHaptics() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(longArrayOf(0, 100, 80, 100), -1)
            }
        } catch (ignored: Exception) {}
    }
}
