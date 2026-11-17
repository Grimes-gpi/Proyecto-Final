package com.example.clase18_08zulema.data

import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    /** [code] es el HTTP status (0 si no hubo conexión); permite reaccionar distinto a 403, 409, etc. */
    data class Failure(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

private val gson = Gson()

/** Ejecuta la llamada y entrega el resultado en el hilo principal (Retrofit usa el main thread en Android). */
internal fun <T> Call<T>.deliver(done: (ApiResult<T>) -> Unit) = enqueue(object : Callback<T> {
    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
            response.body()?.let { done(ApiResult.Success(it)) }
                ?: done(ApiResult.Failure("El servidor respondió sin datos.", response.code()))
        } else {
            done(ApiResult.Failure(errorMessage(response), response.code()))
        }
    }

    override fun onFailure(call: Call<T>, error: Throwable) {
        done(ApiResult.Failure(connectionMessage(error)))
    }
})

/** Variante para respuestas sin cuerpo (204 No Content). */
internal fun Call<Void>.deliverEmpty(done: (ApiResult<Unit>) -> Unit) = enqueue(object : Callback<Void> {
    override fun onResponse(call: Call<Void>, response: Response<Void>) {
        if (response.isSuccessful) done(ApiResult.Success(Unit))
        else done(ApiResult.Failure(errorMessage(response), response.code()))
    }

    override fun onFailure(call: Call<Void>, error: Throwable) {
        done(ApiResult.Failure(connectionMessage(error)))
    }
})

private fun errorMessage(response: Response<*>): String {
    val serverMessage = runCatching {
        gson.fromJson(response.errorBody()?.string(), ApiError::class.java)?.message
    }.getOrNull()?.takeIf { it.isNotBlank() }
    return serverMessage ?: when (response.code()) {
        401 -> "Tu sesión expiró. Inicia sesión de nuevo."
        403 -> "Tu rol no tiene permiso para esta acción."
        404 -> "No encontramos ese taller."
        409 -> "No se pudo completar porque el taller está lleno o la persona ya está inscrita."
        else -> "No se pudo completar la solicitud (${response.code()})."
    }
}

private fun connectionMessage(error: Throwable): String {
    val cause = error.localizedMessage?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
    return "No logramos conectar con la API en ${WorkshopsApi.baseUrl}.\n$cause"
}

object AuthRepository {
    private val api get() = WorkshopsApi.instance

    fun login(email: String, password: String, done: (ApiResult<UserProfile>) -> Unit) =
        api.login(LoginRequest(email.trim(), password)).deliver { it.persist(done) }

    fun register(request: RegisterRequest, done: (ApiResult<UserProfile>) -> Unit) =
        api.register(request).deliver { it.persist(done) }

    fun logout() = SessionManager.clear()

    private fun ApiResult<AuthResponse>.persist(done: (ApiResult<UserProfile>) -> Unit) = when (this) {
        is ApiResult.Success -> { SessionManager.save(value); done(ApiResult.Success(value.user)) }
        is ApiResult.Failure -> done(this)
    }
}

object WorkshopsRepository {
    private val api get() = WorkshopsApi.instance

    fun workshops(name: String? = null, category: String? = null, done: (ApiResult<List<Workshop>>) -> Unit) =
        api.listWorkshops(name?.takeIf { it.isNotBlank() }, category?.takeIf { it.isNotBlank() }).deliver(done)

    fun workshop(id: Long, done: (ApiResult<Workshop>) -> Unit) = api.getWorkshop(id).deliver(done)
    fun registrations(id: Long, done: (ApiResult<List<Registration>>) -> Unit) = api.listRegistrations(id).deliver(done)
    fun myRegistrations(done: (ApiResult<List<Registration>>) -> Unit) = api.myRegistrations().deliver(done)

    fun saveWorkshop(id: Long?, request: WorkshopRequest, done: (ApiResult<Workshop>) -> Unit) {
        if (id == null) {
            api.createWorkshop(request).deliver(done)
            return
        }
        // HU-05: tras el PUT se vuelve a consultar el taller para confirmar que quedó persistido.
        api.updateWorkshop(id, request).deliver { updateResult ->
            when (updateResult) {
                is ApiResult.Failure -> done(updateResult)
                is ApiResult.Success -> api.getWorkshop(id).deliver { persistedResult ->
                    when (persistedResult) {
                        is ApiResult.Failure -> done(ApiResult.Failure(
                            "El servidor aceptó la edición, pero no se pudo confirmar el registro guardado. Vuelve a intentarlo."
                        ))
                        is ApiResult.Success -> {
                            val persisted = persistedResult.value
                            if (matchesRequest(persisted, request)) done(ApiResult.Success(persisted))
                            else done(ApiResult.Failure(
                                "El taller no quedó actualizado en la base de datos. Revisa la conexión del backend y vuelve a intentarlo."
                            ))
                        }
                    }
                }
            }
        }
    }

    private fun matchesRequest(workshop: Workshop, request: WorkshopRequest): Boolean {
        val savedDateTime = runCatching { LocalDateTime.parse(workshop.dateTime).withNano(0) }.getOrNull()
        val requestedDateTime = runCatching { LocalDateTime.parse(request.dateTime).withNano(0) }.getOrNull()
        return workshop.name == request.name.trim() &&
            workshop.description == request.description.trim() &&
            workshop.instructor == request.instructor.trim() &&
            workshop.category == request.category.trim() &&
            workshop.imageUrl == request.imageUrl?.trim()?.takeIf { it.isNotEmpty() } &&
            workshop.maxCapacity == request.maxCapacity &&
            savedDateTime != null && savedDateTime == requestedDateTime
    }

    fun cancelWorkshop(id: Long, done: (ApiResult<Unit>) -> Unit) = api.cancelWorkshop(id).deliverEmpty(done)

    /** Instructor: inscribe a otra persona. */
    fun register(request: RegistrationRequest, done: (ApiResult<Registration>) -> Unit) =
        api.createRegistration(request).deliver(done)

    /** Estudiante: se inscribe con los datos de su cuenta. */
    fun enrollMyself(workshopId: Long, done: (ApiResult<Registration>) -> Unit) =
        api.createRegistration(RegistrationRequest(workshopId)).deliver(done)

    fun cancelRegistration(workshopId: Long, document: String, done: (ApiResult<Registration>) -> Unit) =
        api.cancelRegistration(workshopId, document.trim()).deliver(done)

    fun markAttendance(registrationId: Long, done: (ApiResult<Registration>) -> Unit) =
        api.markAttendance(registrationId).deliver(done)
}
