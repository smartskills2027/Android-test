package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val department: String,
    val type: String, // AttendanceType name
    val timestampMillis: Long,
    val status: String, // AttendanceStatus name
    val confidenceScore: Float,
    val livenessVerified: Boolean,
    val antiSpoofPassed: Boolean,
    val terminalName: String,
    val photoThumbnail: String?,
    val locationName: String,
    val isSynced: Boolean = false,
    val firestoreId: String? = null
) {
    fun toDomain(): AttendanceRecord {
        return AttendanceRecord(
            id = id,
            employeeId = employeeId,
            employeeCode = employeeCode,
            employeeName = employeeName,
            department = department,
            type = runCatching { AttendanceType.valueOf(type) }.getOrDefault(AttendanceType.CHECK_IN),
            timestampMillis = timestampMillis,
            status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.ON_TIME),
            confidenceScore = confidenceScore,
            livenessVerified = livenessVerified,
            antiSpoofPassed = antiSpoofPassed,
            terminalName = terminalName,
            photoThumbnail = photoThumbnail,
            locationName = locationName,
            isSynced = isSynced,
            firestoreId = firestoreId
        )
    }

    companion object {
        fun fromDomain(record: AttendanceRecord): AttendanceRecordEntity {
            return AttendanceRecordEntity(
                id = record.id,
                employeeId = record.employeeId,
                employeeCode = record.employeeCode,
                employeeName = record.employeeName,
                department = record.department,
                type = record.type.name,
                timestampMillis = record.timestampMillis,
                status = record.status.name,
                confidenceScore = record.confidenceScore,
                livenessVerified = record.livenessVerified,
                antiSpoofPassed = record.antiSpoofPassed,
                terminalName = record.terminalName,
                photoThumbnail = record.photoThumbnail,
                locationName = record.locationName,
                isSynced = record.isSynced,
                firestoreId = record.firestoreId
            )
        }
    }
}
