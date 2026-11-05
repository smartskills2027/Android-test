package com.example.data.repository

import com.example.data.biometrics.FaceRecognitionEngine
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.EmployeeDao
import com.example.data.local.EmployeeEntity
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.data.model.Employee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val employeeDao = database.employeeDao()
    private val attendanceDao = database.attendanceDao()

    init {
        scope.launch {
            seedInitialDataIfEmpty()
        }
    }

    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees().map { list ->
        list.map { it.toDomain { json -> parseVector(json) } }
    }

    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords().map { list ->
        list.map { it.toDomain() }
    }

    fun getRecordsForDay(startOfDay: Long, endOfDay: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsForDay(startOfDay, endOfDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getRecordsForEmployee(employeeId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsForEmployee(employeeId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getRecordsForEmployeeCode(employeeCode: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsForEmployeeCode(employeeCode).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getRecordsForEmployeeBetween(employeeId: Long, startMillis: Long, endMillis: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsForEmployeeBetween(employeeId, startMillis, endMillis).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getPresentTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return attendanceDao.getPresentEmployeeCountToday(startOfDay, endOfDay)
    }

    fun getLateTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return attendanceDao.getLateCountToday(startOfDay, endOfDay)
    }

    fun getSuccessfulCheckInsCountToday(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return attendanceDao.getSuccessfulCheckInsCountToday(startOfDay, endOfDay)
    }

    fun getSuccessfulCheckInsToday(startOfDay: Long, endOfDay: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getSuccessfulCheckInsToday(startOfDay, endOfDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun enrollEmployee(
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
        faceVector: List<Float>
    ): Long {
        val vectorToSave = if (faceVector.isNotEmpty()) faceVector else FaceRecognitionEngine.generateDeterministicEmbedding(employeeCode)
        val entity = EmployeeEntity(
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
            faceFeatureVectorJson = serializeVector(vectorToSave),
            enrolledAtMillis = System.currentTimeMillis(),
            isActive = true,
            officeLocation = "HQ - Main Campus"
        )
        return employeeDao.insertEmployee(entity)
    }

    suspend fun recordAttendance(
        employee: Employee,
        type: AttendanceType,
        confidence: Float,
        livenessVerified: Boolean = true,
        antiSpoofPassed: Boolean = true,
        terminalName: String = "Reception Kiosk #1",
        locationName: String = "Corporate HQ - Lobby Gate"
    ): AttendanceRecord {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val status = when (type) {
            AttendanceType.CHECK_IN -> {
                // If arriving >15 minutes after shift start -> LATE
                val shiftStartTotalMinutes = employee.shiftStartHour * 60 + employee.shiftStartMinute
                val currentTotalMinutes = currentHour * 60 + currentMinute
                if (currentTotalMinutes > shiftStartTotalMinutes + 15) {
                    AttendanceStatus.LATE
                } else {
                    AttendanceStatus.ON_TIME
                }
            }
            AttendanceType.CHECK_OUT -> {
                val shiftEndTotalMinutes = employee.shiftEndHour * 60 + employee.shiftEndMinute
                val currentTotalMinutes = currentHour * 60 + currentMinute
                if (currentTotalMinutes > shiftEndTotalMinutes + 60) {
                    AttendanceStatus.OVERTIME
                } else if (currentTotalMinutes < shiftEndTotalMinutes - 15) {
                    AttendanceStatus.EARLY_DEPARTURE
                } else {
                    AttendanceStatus.ON_TIME
                }
            }
            AttendanceType.BREAK_START, AttendanceType.BREAK_END -> AttendanceStatus.GEO_VERIFIED
        }

        val record = AttendanceRecord(
            id = 0,
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            employeeName = employee.fullName,
            department = employee.department,
            type = type,
            timestampMillis = now,
            status = status,
            confidenceScore = confidence,
            livenessVerified = livenessVerified,
            antiSpoofPassed = antiSpoofPassed,
            terminalName = terminalName,
            photoThumbnail = employee.avatarUrl,
            locationName = locationName
        )

        val id = attendanceDao.insertRecord(AttendanceRecordEntity.fromDomain(record))
        return record.copy(id = id)
    }

    fun exportToCsv(records: List<AttendanceRecord>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Log ID,Timestamp,Employee ID,Employee Name,Department,Punch Type,Status,Confidence %,Liveness,Anti-Spoof,Terminal,Location\n")
        records.forEach { r ->
            val dateStr = dateFormat.format(Date(r.timestampMillis))
            val confPct = String.format(Locale.US, "%.1f", r.confidenceScore * 100)
            sb.append("${r.id},\"$dateStr\",\"${r.employeeCode}\",\"${r.employeeName}\",\"${r.department}\",${r.type.label},${r.status.label},$confPct%,${r.livenessVerified},${r.antiSpoofPassed},\"${r.terminalName}\",\"${r.locationName}\"\n")
        }
        return sb.toString()
    }

    private suspend fun seedInitialDataIfEmpty() {
        val existing = employeeDao.getEmployeeByCode("EMP-1001")
        if (existing != null) return

        val seededEmployees = listOf(
            Employee(
                employeeCode = "EMP-1001",
                fullName = "Sarah Connor",
                email = "sarah.connor@cybercore.io",
                department = "Operations & Security",
                designation = "Lead Security Architect",
                shiftName = "Morning Security Shift (08:00 - 17:00)",
                shiftStartHour = 8,
                shiftStartMinute = 0,
                shiftEndHour = 17,
                shiftEndMinute = 0,
                avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80",
                faceFeatureVector = FaceRecognitionEngine.generateDeterministicEmbedding("EMP-1001")
            ),
            Employee(
                employeeCode = "EMP-1002",
                fullName = "Alex Rivera",
                email = "alex.rivera@cybercore.io",
                department = "Engineering",
                designation = "Senior Android Specialist",
                shiftName = "General Shift (09:00 - 18:00)",
                shiftStartHour = 9,
                shiftStartMinute = 0,
                shiftEndHour = 18,
                shiftEndMinute = 0,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                faceFeatureVector = FaceRecognitionEngine.generateDeterministicEmbedding("EMP-1002")
            ),
            Employee(
                employeeCode = "EMP-1003",
                fullName = "Priya Patel",
                email = "priya.patel@cybercore.io",
                department = "Product & Design",
                designation = "Principal UX Researcher",
                shiftName = "General Shift (09:00 - 18:00)",
                shiftStartHour = 9,
                shiftStartMinute = 0,
                shiftEndHour = 18,
                shiftEndMinute = 0,
                avatarUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150&auto=format&fit=crop&q=80",
                faceFeatureVector = FaceRecognitionEngine.generateDeterministicEmbedding("EMP-1003")
            ),
            Employee(
                employeeCode = "EMP-1004",
                fullName = "Marcus Chen",
                email = "marcus.chen@cybercore.io",
                department = "Engineering",
                designation = "Cloud Infrastructure Lead",
                shiftName = "General Shift (09:00 - 18:00)",
                shiftStartHour = 9,
                shiftStartMinute = 0,
                shiftEndHour = 18,
                shiftEndMinute = 0,
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                faceFeatureVector = FaceRecognitionEngine.generateDeterministicEmbedding("EMP-1004")
            ),
            Employee(
                employeeCode = "EMP-1005",
                fullName = "Elena Rostova",
                email = "elena.rostova@cybercore.io",
                department = "Human Resources",
                designation = "People Operations Manager",
                shiftName = "General Shift (09:30 - 18:30)",
                shiftStartHour = 9,
                shiftStartMinute = 30,
                shiftEndHour = 18,
                shiftEndMinute = 30,
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
                faceFeatureVector = FaceRecognitionEngine.generateDeterministicEmbedding("EMP-1005")
            ),
            Employee(
                employeeCode = "EMP-1006",
                fullName = "David Kim",
                email = "david.kim@cybercore.io",
                department = "Sales & Marketing",
                designation = "Enterprise Account Director",
                shiftName = "Flexible Shift (10:00 - 19:00)",
                shiftStartHour = 10,
                shiftStartMinute = 0,
                shiftEndHour = 19,
                shiftEndMinute = 0,
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                faceFeatureVector = FaceRecognitionEngine.generateDeterministicEmbedding("EMP-1006")
            )
        )

        val insertedEntities = seededEmployees.map { emp ->
            EmployeeEntity.fromDomain(emp) { serializeVector(it) }
        }
        employeeDao.insertEmployees(insertedEntities)

        // Prepopulate today's initial attendance logs
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 52)
        val t1 = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 4)
        val t2 = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 28) // Late
        val t3 = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 45)
        val t4 = cal.timeInMillis

        val initialLogs = listOf(
            AttendanceRecordEntity(
                employeeId = 1,
                employeeCode = "EMP-1001",
                employeeName = "Sarah Connor",
                department = "Operations & Security",
                type = AttendanceType.CHECK_IN.name,
                timestampMillis = t1,
                status = AttendanceStatus.ON_TIME.name,
                confidenceScore = 0.992f,
                livenessVerified = true,
                antiSpoofPassed = true,
                terminalName = "Security Gate A",
                photoThumbnail = seededEmployees[0].avatarUrl,
                locationName = "Corporate HQ - Lobby Gate"
            ),
            AttendanceRecordEntity(
                employeeId = 2,
                employeeCode = "EMP-1002",
                employeeName = "Alex Rivera",
                department = "Engineering",
                type = AttendanceType.CHECK_IN.name,
                timestampMillis = t2,
                status = AttendanceStatus.ON_TIME.name,
                confidenceScore = 0.984f,
                livenessVerified = true,
                antiSpoofPassed = true,
                terminalName = "Reception Kiosk #1",
                photoThumbnail = seededEmployees[1].avatarUrl,
                locationName = "Corporate HQ - 4th Floor"
            ),
            AttendanceRecordEntity(
                employeeId = 3,
                employeeCode = "EMP-1003",
                employeeName = "Priya Patel",
                department = "Product & Design",
                type = AttendanceType.CHECK_IN.name,
                timestampMillis = t3,
                status = AttendanceStatus.LATE.name,
                confidenceScore = 0.978f,
                livenessVerified = true,
                antiSpoofPassed = true,
                terminalName = "Reception Kiosk #1",
                photoThumbnail = seededEmployees[2].avatarUrl,
                locationName = "Corporate HQ - Lobby Gate"
            ),
            AttendanceRecordEntity(
                employeeId = 4,
                employeeCode = "EMP-1004",
                employeeName = "Marcus Chen",
                department = "Engineering",
                type = AttendanceType.CHECK_IN.name,
                timestampMillis = t4,
                status = AttendanceStatus.ON_TIME.name,
                confidenceScore = 0.989f,
                livenessVerified = true,
                antiSpoofPassed = true,
                terminalName = "Front Gate Kiosk #2",
                photoThumbnail = seededEmployees[3].avatarUrl,
                locationName = "Corporate HQ - Lobby Gate"
            )
        )
        attendanceDao.insertRecords(initialLogs)
    }

    private fun serializeVector(vector: List<Float>): String = vector.joinToString(",")
    private fun parseVector(json: String): List<Float> {
        if (json.isBlank()) return emptyList()
        return json.split(",").mapNotNull { it.trim().toFloatOrNull() }
    }
}
