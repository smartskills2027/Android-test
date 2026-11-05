package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestampMillis DESC")
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE timestampMillis >= :startOfDayMillis AND timestampMillis <= :endOfDayMillis ORDER BY timestampMillis DESC")
    fun getRecordsForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY timestampMillis DESC")
    fun getRecordsForEmployee(employeeId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeCode = :employeeCode ORDER BY timestampMillis DESC")
    fun getRecordsForEmployeeCode(employeeCode: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND timestampMillis >= :startMillis AND timestampMillis <= :endMillis ORDER BY timestampMillis DESC")
    fun getRecordsForEmployeeBetween(employeeId: Long, startMillis: Long, endMillis: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND timestampMillis >= :startOfDayMillis AND timestampMillis <= :endOfDayMillis ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestRecordForEmployeeToday(employeeId: Long, startOfDayMillis: Long, endOfDayMillis: Long): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0 ORDER BY timestampMillis ASC")
    fun getUnsyncedRecordsFlow(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0 ORDER BY timestampMillis ASC")
    suspend fun getUnsyncedRecords(): List<AttendanceRecordEntity>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("UPDATE attendance_records SET isSynced = 1, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markAsSynced(id: Long, firestoreId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Query("SELECT COUNT(DISTINCT employeeId) FROM attendance_records WHERE type = 'CHECK_IN' AND timestampMillis >= :startOfDayMillis AND timestampMillis <= :endOfDayMillis")
    fun getPresentEmployeeCountToday(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE status = 'LATE' AND timestampMillis >= :startOfDayMillis AND timestampMillis <= :endOfDayMillis")
    fun getLateCountToday(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE type = 'CHECK_IN' AND timestampMillis >= :startOfDayMillis AND timestampMillis <= :endOfDayMillis")
    fun getSuccessfulCheckInsCountToday(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    @Query("SELECT * FROM attendance_records WHERE type = 'CHECK_IN' AND timestampMillis >= :startOfDayMillis AND timestampMillis <= :endOfDayMillis ORDER BY timestampMillis DESC")
    fun getSuccessfulCheckInsToday(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<AttendanceRecordEntity>>
}
