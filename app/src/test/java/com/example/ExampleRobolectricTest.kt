package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.biometrics.FaceRecognitionEngine
import com.example.data.biometrics.LivenessDetector
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceRecordEntity
import com.example.data.model.Employee
import com.example.data.sync.FirestoreAttendanceSyncService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var inMemoryDb: AppDatabase

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    inMemoryDb.close()
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("FaceAttend", appName)
  }

  @Test
  fun `verify face recognition engine calculates high similarity for perturbed vector`() {
    val seed = "EMP-TEST-01"
    val baseVector = FaceRecognitionEngine.generateDeterministicEmbedding(seed)
    assertEquals(128, baseVector.size)

    val emp = Employee(
      employeeCode = seed,
      fullName = "Test Engineer",
      email = "test@company.io",
      department = "Engineering",
      designation = "Tester",
      faceFeatureVector = baseVector
    )

    val perturbedVector = FaceRecognitionEngine.generatePerturbedScanVector(emp)
    val matchResult = FaceRecognitionEngine.findBestMatch(perturbedVector, listOf(emp))

    assertNotNull(matchResult.matchedEmployee)
    assertEquals(seed, matchResult.matchedEmployee?.employeeCode)
    assertTrue("Confidence should be above 0.90", matchResult.confidence >= 0.90f)
    assertTrue("Liveness should be verified", matchResult.livenessPassed)
  }

  @Test
  fun `verify liveness detector simulation results`() {
    val detector = LivenessDetector()
    val liveResult = detector.simulateLivePersonPass()
    assertTrue("Live simulation must pass isLive", liveResult.isLive)
    assertTrue("Score should be high", liveResult.livenessScore >= 0.85f)
    assertTrue("Blink should be recorded", liveResult.blinkDetected)
    assertFalse("Spoof should be false", liveResult.isSpoofDetected)

    val spoofResult = detector.simulateStaticPhotoSpoof()
    assertFalse("Static photo must fail isLive", spoofResult.isLive)
    assertTrue("Spoof must be detected", spoofResult.isSpoofDetected)
    assertNotNull(spoofResult.spoofReason)
  }

  @Test
  fun `verify liveness detector identifies static frames as spoof`() {
    val detector = LivenessDetector()
    val width = 64
    val height = 64
    val buffer = java.nio.ByteBuffer.allocateDirect(width * height)

    // Fill constant flat pattern (static photo)
    for (i in 0 until (width * height)) {
      buffer.put(128.toByte())
    }

    var lastResult: com.example.data.biometrics.LivenessAnalysisResult? = null
    // Feed 25 consecutive identical frames with zero motion variance
    for (frame in 0 until 25) {
      buffer.rewind()
      lastResult = detector.analyzeFrame(buffer, width, height, width)
    }

    assertNotNull(lastResult)
    assertTrue("Zero optical motion over 20+ frames should trigger anti-spoof detection", lastResult!!.isSpoofDetected)
    assertFalse("Static photo should not be live", lastResult.isLive)
  }

  @Test
  fun `verify offline caching in Room keeps unsynced attendance records safe`() = runBlocking {
    val attendanceDao = inMemoryDb.attendanceDao()

    val testRecord = AttendanceRecordEntity(
      id = 0,
      employeeId = 101,
      employeeCode = "EMP-101",
      employeeName = "Sarah Connor",
      department = "Security",
      type = "CHECK_IN",
      timestampMillis = System.currentTimeMillis(),
      status = "ON_TIME",
      confidenceScore = 0.98f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Kiosk Offline Terminal #1",
      photoThumbnail = null,
      locationName = "Lobby East Gate",
      isSynced = false,
      firestoreId = null
    )

    val insertedId = attendanceDao.insertRecord(testRecord)
    assertTrue("Inserted record ID should be greater than 0", insertedId > 0)

    val unsyncedList = attendanceDao.getUnsyncedRecords()
    assertEquals(1, unsyncedList.size)
    val cachedRecord = unsyncedList.first()
    assertEquals("EMP-101", cachedRecord.employeeCode)
    assertFalse("Cached record must not be marked synced", cachedRecord.isSynced)
    assertEquals(null, cachedRecord.firestoreId)

    val unsyncedCount = attendanceDao.getUnsyncedCount().first()
    assertEquals(1, unsyncedCount)
  }

  @Test
  fun `verify sync service offline caching behavior and connectivity restoration`() = runBlocking {
    val attendanceDao = inMemoryDb.attendanceDao()
    val testRecord = AttendanceRecordEntity(
      id = 0,
      employeeId = 202,
      employeeCode = "EMP-202",
      employeeName = "John Doe",
      department = "Engineering",
      type = "CHECK_IN",
      timestampMillis = System.currentTimeMillis(),
      status = "ON_TIME",
      confidenceScore = 0.96f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Main Entrance",
      photoThumbnail = null,
      locationName = "Floor 2",
      isSynced = false,
      firestoreId = null
    )
    val localId = attendanceDao.insertRecord(testRecord)

    val syncService = FirestoreAttendanceSyncService(context, inMemoryDb)

    // Set offline mode
    syncService.setNetworkOnlineForTesting(false)
    assertFalse("Service should report offline", syncService.isCurrentlyOnline())

    // Attempt sync while offline
    var syncSuccess: Boolean? = null
    var syncMsg: String? = null
    syncService.syncPendingRecords { success, msg ->
      syncSuccess = success
      syncMsg = msg
    }

    // Verify offline handling: gracefully rejects sync without crashing, records stay cached in Room
    kotlinx.coroutines.delay(200)
    assertFalse("Sync while offline should not claim success", syncSuccess == true)
    val cachedRecords = attendanceDao.getUnsyncedRecords()
    assertEquals(1, cachedRecords.size)
    assertFalse("Record must still be unsynced in Room", cachedRecords[0].isSynced)

    // Simulate connectivity restoration
    syncService.setNetworkOnlineForTesting(true)
    assertTrue("Service should report online after restoration", syncService.isCurrentlyOnline())

    // Simulate successful Firestore upload acknowledgment
    val generatedFirestoreDocId = "firestore_doc_test_98765"
    attendanceDao.markAsSynced(localId, generatedFirestoreDocId)

    // Verify record is marked synced in Room
    val remainingUnsynced = attendanceDao.getUnsyncedRecords()
    assertEquals(0, remainingUnsynced.size)

    val allRecords = attendanceDao.getAllRecords().first()
    assertEquals(1, allRecords.size)
    assertTrue("Record must now be marked as synced", allRecords[0].isSynced)
    assertEquals(generatedFirestoreDocId, allRecords[0].firestoreId)
  }

  @Test
  fun `verify Room queries past attendance records for employee including date, time, and status`() = runBlocking {
    val attendanceDao = inMemoryDb.attendanceDao()
    val empId = 555L
    val empCode = "EMP-555"

    val cal = java.util.Calendar.getInstance()
    cal.set(2026, java.util.Calendar.OCTOBER, 26, 9, 15, 0)
    val checkInTime = cal.timeInMillis

    cal.set(2026, java.util.Calendar.OCTOBER, 26, 18, 5, 0)
    val checkOutTime = cal.timeInMillis

    val checkInRecord = AttendanceRecordEntity(
      id = 0,
      employeeId = empId,
      employeeCode = empCode,
      employeeName = "Jane Foster",
      department = "Astrophysics",
      type = "CHECK_IN",
      timestampMillis = checkInTime,
      status = "ON_TIME",
      confidenceScore = 0.991f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Lab Gate Kiosk",
      photoThumbnail = null,
      locationName = "Research Facility North",
      isSynced = true,
      firestoreId = "cloud_doc_101"
    )

    val checkOutRecord = AttendanceRecordEntity(
      id = 0,
      employeeId = empId,
      employeeCode = empCode,
      employeeName = "Jane Foster",
      department = "Astrophysics",
      type = "CHECK_OUT",
      timestampMillis = checkOutTime,
      status = "ON_TIME",
      confidenceScore = 0.985f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Lab Gate Kiosk",
      photoThumbnail = null,
      locationName = "Research Facility North",
      isSynced = false,
      firestoreId = null
    )

    // Insert different employee record to verify query filters by employee
    val otherEmpRecord = AttendanceRecordEntity(
      id = 0,
      employeeId = 777L,
      employeeCode = "EMP-777",
      employeeName = "Thor Odinson",
      department = "Security",
      type = "CHECK_IN",
      timestampMillis = checkInTime,
      status = "LATE",
      confidenceScore = 0.95f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Main Entrance",
      photoThumbnail = null,
      locationName = "HQ",
      isSynced = false,
      firestoreId = null
    )

    attendanceDao.insertRecord(checkInRecord)
    attendanceDao.insertRecord(checkOutRecord)
    attendanceDao.insertRecord(otherEmpRecord)

    // Query Room for past records for employee 555
    val employeeHistory = attendanceDao.getRecordsForEmployee(empId).first()
    assertEquals("Should retrieve exactly 2 past records for employee 555", 2, employeeHistory.size)

    // Check timestamps and sorting (DESC)
    val firstRecord = employeeHistory[0]
    val secondRecord = employeeHistory[1]
    assertTrue("Most recent record should be first", firstRecord.timestampMillis > secondRecord.timestampMillis)
    assertEquals("CHECK_OUT", firstRecord.type)
    assertEquals("ON_TIME", firstRecord.status)
    assertEquals(checkOutTime, firstRecord.timestampMillis)

    assertEquals("CHECK_IN", secondRecord.type)
    assertEquals("ON_TIME", secondRecord.status)
    assertEquals(checkInTime, secondRecord.timestampMillis)

    // Query by employee code
    val codeHistory = attendanceDao.getRecordsForEmployeeCode(empCode).first()
    assertEquals(2, codeHistory.size)
    assertEquals(empCode, codeHistory[0].employeeCode)
  }

  @Test
  fun `verify Room queries daily successful check-ins count and records for attendance summary widget`() = runBlocking {
    val attendanceDao = inMemoryDb.attendanceDao()

    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis

    cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
    cal.set(java.util.Calendar.MINUTE, 59)
    cal.set(java.util.Calendar.SECOND, 59)
    cal.set(java.util.Calendar.MILLISECOND, 999)
    val endOfDay = cal.timeInMillis

    // Today's Check-In 1 (On Time)
    val checkIn1 = AttendanceRecordEntity(
      id = 0,
      employeeId = 1,
      employeeCode = "EMP-001",
      employeeName = "Alice Morgan",
      department = "Engineering",
      type = "CHECK_IN",
      timestampMillis = startOfDay + 9 * 3600 * 1000,
      status = "ON_TIME",
      confidenceScore = 0.98f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Kiosk 1",
      photoThumbnail = null,
      locationName = "Lobby"
    )

    // Today's Check-In 2 (Late)
    val checkIn2 = AttendanceRecordEntity(
      id = 0,
      employeeId = 2,
      employeeCode = "EMP-002",
      employeeName = "Bob Vance",
      department = "Operations",
      type = "CHECK_IN",
      timestampMillis = startOfDay + 9 * 3600 * 1000 + 35 * 60 * 1000,
      status = "LATE",
      confidenceScore = 0.96f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Kiosk 2",
      photoThumbnail = null,
      locationName = "Lobby"
    )

    // Today's Check-Out (Should NOT count towards successful check-ins)
    val checkOut1 = AttendanceRecordEntity(
      id = 0,
      employeeId = 1,
      employeeCode = "EMP-001",
      employeeName = "Alice Morgan",
      department = "Engineering",
      type = "CHECK_OUT",
      timestampMillis = startOfDay + 17 * 3600 * 1000,
      status = "ON_TIME",
      confidenceScore = 0.97f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Kiosk 1",
      photoThumbnail = null,
      locationName = "Lobby"
    )

    // Yesterday's Check-In (Should NOT count in today's daily summary)
    val yesterdayCheckIn = AttendanceRecordEntity(
      id = 0,
      employeeId = 3,
      employeeCode = "EMP-003",
      employeeName = "Charlie Root",
      department = "Engineering",
      type = "CHECK_IN",
      timestampMillis = startOfDay - 12 * 3600 * 1000,
      status = "ON_TIME",
      confidenceScore = 0.99f,
      livenessVerified = true,
      antiSpoofPassed = true,
      terminalName = "Kiosk 1",
      photoThumbnail = null,
      locationName = "Lobby"
    )

    attendanceDao.insertRecord(checkIn1)
    attendanceDao.insertRecord(checkIn2)
    attendanceDao.insertRecord(checkOut1)
    attendanceDao.insertRecord(yesterdayCheckIn)

    // Query count of successful check-ins for current day
    val checkInCount = attendanceDao.getSuccessfulCheckInsCountToday(startOfDay, endOfDay).first()
    assertEquals("Should accurately report exactly 2 successful check-ins today", 2, checkInCount)

    // Query list of today's check-ins
    val todayCheckIns = attendanceDao.getSuccessfulCheckInsToday(startOfDay, endOfDay).first()
    assertEquals("Should retrieve 2 check-in records for today", 2, todayCheckIns.size)
    assertTrue("All retrieved records must be CHECK_IN type", todayCheckIns.all { it.type == "CHECK_IN" })
    assertTrue("Records should be within today's range", todayCheckIns.all { it.timestampMillis in startOfDay..endOfDay })

    // Verify unique headcount present
    val presentCount = attendanceDao.getPresentEmployeeCountToday(startOfDay, endOfDay).first()
    assertEquals(2, presentCount)

    // Verify late count
    val lateCount = attendanceDao.getLateCountToday(startOfDay, endOfDay).first()
    assertEquals(1, lateCount)
  }
}
