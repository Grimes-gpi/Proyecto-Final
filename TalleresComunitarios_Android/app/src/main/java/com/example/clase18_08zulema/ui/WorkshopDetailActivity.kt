package com.example.clase18_08zulema.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.clase18_08zulema.MainActivity
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.Registration
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkshopDetailActivity : AppCompatActivity() {
    private var workshopId: Long = -1
    private lateinit var page: LinearLayout
    private lateinit var details: LinearLayout
    private lateinit var registrations: LinearLayout
    private lateinit var registrationFilter: com.google.android.material.textfield.TextInputEditText
    private var allRegistrations: List<Registration> = emptyList()
    private var current: Workshop? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workshopId = intent.getLongExtra("workshop_id", -1)
        page = Ui.page(this)
        page.addView(Ui.body(this, "DETALLE DEL TALLER", 12f))
        page.addView(Ui.gap(this, 8))
        details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(details)
        page.addView(Ui.gap(this, 14))
        page.addView(Ui.title(this, "Personas inscritas", 19f))
        page.addView(Ui.gap(this, 6))
        val (filterLayout, filterEdit) = Ui.field(
            this,
            "Buscar por nombre o documento",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        )
        registrationFilter = filterEdit
        page.addView(filterLayout)
        registrationFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                showRegistrations(allRegistrations)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        page.addView(Ui.gap(this, 6))
        registrations = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(registrations)
        page.addView(Ui.gap(this, 14))
        page.addView(Ui.button(this, "Volver a talleres", Ui.muted) { finish() })
        setContentView(ScrollView(this).apply { addView(page) })
    }

    override fun onResume() {
        super.onResume()
        if (workshopId > 0) load()
    }

    private fun load() {
        details.removeAllViews()
        details.addView(Ui.body(this, "Cargando información…"))
        WorkshopsRepository.workshop(workshopId) { result ->
            when (result) {
                is ApiResult.Success -> { current = result.value; showWorkshop(result.value); loadRegistrations() }
                is ApiResult.Failure -> { details.removeAllViews(); details.addView(Ui.body(this, result.message)); details.addView(Ui.button(this, "Reintentar") { load() }) }
            }
        }
    }

    private fun showWorkshop(workshop: Workshop) {
        details.removeAllViews()
        details.addView(Ui.title(this, workshop.name))
        details.addView(Ui.gap(this, 8))
        details.addView(Ui.body(this, workshop.category ?: "Otros", 13f))
        details.addView(Ui.gap(this, 8))
        details.addView(Ui.body(this, workshop.description))
        details.addView(Ui.gap(this, 12))
        details.addView(Ui.body(this, "A cargo de ${workshop.instructor}"))
        details.addView(Ui.body(this, formatDate(workshop.dateTime)))
        details.addView(Ui.body(this, "${workshop.registeredPeople} de ${workshop.maxCapacity} inscritos · ${workshop.availableSlots} cupos libres"))
        details.addView(Ui.gap(this, 12))
        if (workshop.status == "ACTIVE" && workshop.availableSlots > 0) {
            details.addView(Ui.button(this, "Inscribir participante") {
                startActivity(Intent(this, RegistrationFormActivity::class.java)
                    .putExtra("workshop_id", workshop.id).putExtra("workshop_name", workshop.name))
            })
        } else {
            details.addView(Ui.body(this, if (workshop.status == "ACTIVE") "Este taller ya no tiene cupos." else "Este taller está cancelado."))
        }
        details.addView(Ui.gap(this, 8))
        details.addView(Ui.button(this, "Compartir taller", Ui.ink) { shareWorkshop(workshop) })
        if (workshop.status == "ACTIVE") {
            details.addView(Ui.gap(this, 8))
            details.addView(Ui.button(this, "Editar taller", Ui.green) {
                startActivity(Intent(this, WorkshopFormActivity::class.java).putExtra("workshop_id", workshop.id))
            })
            details.addView(Ui.gap(this, 8))
            details.addView(Ui.button(this, "Cancelar taller", Ui.orange) { confirmCancel(workshop) })
        }
    }

    private fun confirmCancel(workshop: Workshop) {
        AlertDialog.Builder(this)
            .setTitle("¿Cancelar el taller?")
            .setMessage("“${workshop.name}” dejará de aceptar inscripciones.")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Cancelar taller") { _, _ ->
                WorkshopsRepository.cancelWorkshop(workshop.id) { result ->
                    when (result) {
                        is ApiResult.Success -> { Ui.toast(this, "Taller cancelado."); load() }
                        is ApiResult.Failure -> Ui.toast(this, result.message)
                    }
                }
            }.show()
    }

    private fun loadRegistrations() {
        registrations.removeAllViews()
        registrations.addView(Ui.body(this, "Cargando inscripciones…"))
        WorkshopsRepository.registrations(workshopId) { result ->
            registrations.removeAllViews()
            when (result) {
                is ApiResult.Success -> { allRegistrations = result.value; showRegistrations(allRegistrations) }
                is ApiResult.Failure -> {
                    registrations.addView(Ui.body(this, result.message))
                    registrations.addView(Ui.button(this, "Reintentar") { loadRegistrations() })
                }
            }
        }
    }

    private fun showRegistrations(items: List<Registration>) {
        registrations.removeAllViews()
        if (items.isEmpty()) {
            registrations.addView(Ui.body(this, "Todavía no hay personas inscritas."))
            return
        }
        val query = registrationFilter.text?.toString()?.trim().orEmpty()
        val matching = if (query.isBlank()) items else items.filter {
            it.beneficiaryName.contains(query, ignoreCase = true) ||
                it.beneficiaryDocument.contains(query, ignoreCase = true)
        }
        if (matching.isEmpty()) {
            registrations.addView(Ui.body(this, "No encontramos personas con ese nombre o documento."))
            return
        }
        matching.forEach { person ->
            registrations.addView(Ui.body(this, "${person.beneficiaryName}  ·  ${person.beneficiaryDocument}"))
            val attendanceText = when {
                person.status != "ACTIVE" -> "Inscripción cancelada · cupo liberado"
                person.attended -> "Asistencia marcada"
                else -> "Inscripción activa"
            }
            registrations.addView(Ui.body(this, attendanceText, 13f))
            if (person.status == "ACTIVE") {
                if (!person.attended) {
                    registrations.addView(Ui.button(this, "Marcar asistencia", Ui.green) {
                        confirmAttendance(person)
                    })
                    registrations.addView(Ui.button(this, "Cancelar inscripción", Ui.orange) {
                        confirmCancelRegistration(person)
                    })
                }
            }
            registrations.addView(Ui.gap(this, 10))
        }
    }

    private fun confirmCancelRegistration(person: Registration) {
        AlertDialog.Builder(this)
            .setTitle("¿Cancelar esta inscripción?")
            .setMessage("La inscripción de ${person.beneficiaryName} se conservará como cancelada y su cupo quedará disponible.")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Cancelar inscripción") { _, _ ->
                WorkshopsRepository.cancelRegistration(workshopId, person.beneficiaryDocument) { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            Ui.toast(this, "Inscripción cancelada; cupo liberado.")
                            load()
                        }
                        is ApiResult.Failure -> Ui.toast(this, result.message)
                    }
                }
            }.show()
    }

    private fun confirmAttendance(person: Registration) {
        AlertDialog.Builder(this)
            .setTitle("¿Marcar asistencia?")
            .setMessage("${person.beneficiaryName} quedará marcado como asistente. El cupo seguirá reservado.")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Marcar asistencia") { _, _ ->
                WorkshopsRepository.markAttendance(person.id) { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            Ui.toast(this, "Asistencia registrada.")
                            load()
                        }
                        is ApiResult.Failure -> Ui.toast(this, result.message)
                    }
                }
            }.show()
    }

    private fun shareWorkshop(workshop: Workshop) {
        val text = buildString {
            appendLine(workshop.name)
            appendLine(workshop.category ?: "Otros")
            appendLine(formatDate(workshop.dateTime))
            appendLine("A cargo de ${workshop.instructor}")
            appendLine("${workshop.availableSlots} cupos disponibles")
            append(workshop.description)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, workshop.name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir taller"))
    }

    private fun formatDate(value: String): String = runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM · h:mm a", Locale("es", "CO")))
    }.getOrDefault(value.replace('T', ' '))
}
