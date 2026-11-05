package com.example.data.model

enum class AttendanceType(val label: String) {
    CHECK_IN("Check-In"),
    CHECK_OUT("Check-Out"),
    BREAK_START("Break-Out"),
    BREAK_END("Break-In")
}

enum class AttendanceStatus(val label: String) {
    ON_TIME("On-Time"),
    LATE("Late Arrival"),
    EARLY_DEPARTURE("Early Departure"),
    OVERTIME("Overtime"),
    GEO_VERIFIED("Geo-Verified")
}

data class AttendanceRecord(
    val id: Long = 0,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val department: String,
    val type: AttendanceType,
    val timestampMillis: Long,
    val status: AttendanceStatus,
    val confidenceScore: Float, // e.g. 0.985f (98.5%)
    val livenessVerified: Boolean = true,
    val antiSpoofPassed: Boolean = true,
    val terminalName: String = "Reception Kiosk #1",
    val photoThumbnail: String? = null,
    val locationName: String = "Corporate HQ - Lobby Gate",
    val isSynced: Boolean = false,
    val firestoreId: String? = null
)
