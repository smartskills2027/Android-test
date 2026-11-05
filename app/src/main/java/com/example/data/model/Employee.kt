package com.example.data.model

data class Employee(
    val id: Long = 0,
    val employeeCode: String,
    val fullName: String,
    val email: String,
    val department: String,
    val designation: String,
    val shiftName: String = "General Shift (09:00 - 18:00)",
    val shiftStartHour: Int = 9,
    val shiftStartMinute: Int = 0,
    val shiftEndHour: Int = 18,
    val shiftEndMinute: Int = 0,
    val avatarUrl: String? = null,
    val faceFeatureVector: List<Float> = emptyList(),
    val enrolledAtMillis: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val officeLocation: String = "Headquarters, Main Campus"
)
