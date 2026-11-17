package com.example.clase18_08zulema.data

data class Workshop(
    val id: Long,
    val name: String,
    val description: String,
    val instructor: String,
    val category: String?,
    val imageUrl: String?,
    val maxCapacity: Int,
    val dateTime: String,
    val status: String,
    val registeredPeople: Long,
    val availableSlots: Long,
    /** Instructor dueño del taller; nulo en talleres creados antes de la autenticación. */
    val ownerId: Long? = null
) {
    val isActive get() = status == "ACTIVE"
    val fillRatio get() = if (maxCapacity <= 0) 0f else (registeredPeople.toFloat() / maxCapacity).coerceIn(0f, 1f)
}

data class WorkshopRequest(
    val name: String,
    val description: String,
    val instructor: String,
    val category: String,
    val imageUrl: String?,
    val maxCapacity: Int,
    val dateTime: String
)

data class Registration(
    val id: Long,
    val workshopId: Long,
    val workshopName: String,
    val beneficiaryName: String,
    val beneficiaryDocument: String,
    val registrationDate: String,
    val status: String,
    val attended: Boolean = false
) {
    val isActive get() = status == "ACTIVE"
}

/** Un estudiante solo envía [workshopId]; el backend usa los datos de su cuenta. */
data class RegistrationRequest(
    val workshopId: Long,
    val beneficiaryName: String? = null,
    val beneficiaryDocument: String? = null
)

data class ApiError(val message: String? = null)
