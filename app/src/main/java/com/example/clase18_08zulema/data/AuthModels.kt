package com.example.clase18_08zulema.data

enum class Role(val label: String) {
    INSTRUCTOR("Instructor"),
    STUDENT("Estudiante")
}

data class UserProfile(
    val id: Long,
    val fullName: String,
    val email: String,
    val role: Role,
    val document: String?
) {
    val isInstructor get() = role == Role.INSTRUCTOR
    val firstName get() = fullName.trim().substringBefore(' ').ifBlank { fullName }
}

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val role: Role,
    val document: String?,
    val instructorCode: String?
)

data class AuthResponse(val token: String, val user: UserProfile)
