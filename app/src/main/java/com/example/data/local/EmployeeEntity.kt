package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Employee

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeCode: String,
    val fullName: String,
    val email: String,
    val department: String,
    val designation: String,
    val shiftName: String,
    val shiftStartHour: Int,
    val shiftStartMinute: Int,
    val shiftEndHour: Int,
    val shiftEndMinute: Int,
    val avatarUrl: String?,
    val faceFeatureVectorJson: String, // serialized List<Float>
    val enrolledAtMillis: Long,
    val isActive: Boolean,
    val officeLocation: String
) {
    fun toDomain(vectorParser: (String) -> List<Float>): Employee {
        return Employee(
            id = id,
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
            faceFeatureVector = vectorParser(faceFeatureVectorJson),
            enrolledAtMillis = enrolledAtMillis,
            isActive = isActive,
            officeLocation = officeLocation
        )
    }

    companion object {
        fun fromDomain(employee: Employee, vectorSerializer: (List<Float>) -> String): EmployeeEntity {
            return EmployeeEntity(
                id = employee.id,
                employeeCode = employee.employeeCode,
                fullName = employee.fullName,
                email = employee.email,
                department = employee.department,
                designation = employee.designation,
                shiftName = employee.shiftName,
                shiftStartHour = employee.shiftStartHour,
                shiftStartMinute = employee.shiftStartMinute,
                shiftEndHour = employee.shiftEndHour,
                shiftEndMinute = employee.shiftEndMinute,
                avatarUrl = employee.avatarUrl,
                faceFeatureVectorJson = vectorSerializer(employee.faceFeatureVector),
                enrolledAtMillis = employee.enrolledAtMillis,
                isActive = employee.isActive,
                officeLocation = employee.officeLocation
            )
        }
    }
}
