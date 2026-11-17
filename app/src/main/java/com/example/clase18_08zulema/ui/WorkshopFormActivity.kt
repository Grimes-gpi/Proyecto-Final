package com.example.clase18_08zulema.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopRequest
import com.example.clase18_08zulema.data.WorkshopsRepository
import com.example.clase18_08zulema.ui.kit.ButtonTone
import com.example.clase18_08zulema.ui.kit.Buttons
import com.example.clase18_08zulema.ui.kit.CategoryStyle
import com.example.clase18_08zulema.ui.kit.Field
import com.example.clase18_08zulema.ui.kit.Fields
import com.example.clase18_08zulema.ui.kit.Glyph
import com.example.clase18_08zulema.ui.kit.Motion
import com.example.clase18_08zulema.ui.kit.Skeleton
import com.example.clase18_08zulema.ui.kit.TextRole
import com.example.clase18_08zulema.ui.kit.Tone
import com.example.clase18_08zulema.ui.kit.applySystemInsets
import com.example.clase18_08zulema.ui.kit.card
import com.example.clase18_08zulema.ui.kit.colors
import com.example.clase18_08zulema.ui.kit.divider
import com.example.clase18_08zulema.ui.kit.dp
import com.example.clase18_08zulema.ui.kit.dpf
import com.example.clase18_08zulema.ui.kit.glyph
import com.example.clase18_08zulema.ui.kit.horizontal
import com.example.clase18_08zulema.ui.kit.iconButton
import com.example.clase18_08zulema.ui.kit.matchWrap
import com.example.clase18_08zulema.ui.kit.pill
import com.example.clase18_08zulema.ui.kit.snack
import com.example.clase18_08zulema.ui.kit.space
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.example.clase18_08zulema.ui.kit.weighted
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formulario dual: sin ID hace POST, con ID carga el taller y hace PUT. Solo para instructores. */
class WorkshopFormActivity : SecureActivity() {
    companion object {
        const val EXTRA_ID = "workshop_id"
        const val EXTRA_MESSAGE = "result_message"
        private val SUGGESTED_CATEGORIES = listOf(
            "Tecnología", "Cocina", "Oficios", "Medio ambiente", "Emprendimiento",
            "Arte y cultura", "Salud y bienestar", "Idiomas", "Educación"
        )
    }

    private val c by lazy { colors }
    private val locale = Locale("es", "CO")
    private var workshopId: Long? = null
    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private lateinit var scroll: NestedScrollView
    private lateinit var form: LinearLayout
    private lateinit var loading: View
    private lateinit var coverPreview: FrameLayout
    private lateinit var coverImage: ImageView
    private lateinit var name: Field
    private lateinit var category: Field
    private lateinit var instructor: Field
    private lateinit var description: Field
    private lateinit var date: Field
    private lateinit var time: Field
    private lateinit var capacity: Field
    private lateinit var imageUrl: Field
    private lateinit var save: MaterialButton
    private var coverTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureSession(instructorOnly = true)) return
        workshopId = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }
        enableEdgeToEdge()

        val root = FrameLayout(this).apply { setBackgroundColor(c.background) }
        val page = vertical().apply {
            setPadding(dp(20), 0, dp(20), 0)
            applySystemInsets(top = true, extraTopDp = 12, extraBottomDp = 120)
        }
        page.addView(header())

        val body = FrameLayout(this)
        form = vertical()
        loading = Skeleton.detail(this).apply { visibility = View.GONE }
        body.addView(form)
        body.addView(loading)
        page.addView(body, matchWrap(20, this))

        buildForm()

        scroll = NestedScrollView(this).apply { clipToPadding = false; addView(page) }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        root.addView(bottomBar(), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        val id = workshopId
        if (id == null) {
            instructor.edit.setText(currentUser?.fullName.orEmpty())
            Motion.stagger((0 until form.childCount).map(form::getChildAt), step = 55)
        } else {
            form.visibility = View.GONE
            loading.visibility = View.VISIBLE
            loadWorkshop(id)
        }
    }

    private fun header(): View = horizontal().apply {
        addView(context.iconButton(Glyph.BACK, "Volver") { finish() })
        addView(context.vertical().apply {
            addView(context.text(if (workshopId == null) "Nuevo taller" else "Editar taller", TextRole.HEADLINE))
            addView(context.text(
                if (workshopId == null) "Comparte lo que sabes con tu comunidad." else "Los cambios se verifican al guardar.",
                TextRole.BODY_SMALL, c.inkMuted), matchWrap(4, context))
        }, weighted().apply { marginStart = dp(14) })
    }

    private fun section(title: String, subtitle: String? = null, content: LinearLayout.() -> Unit): View =
        card(radiusDp = 24).apply {
            addView(vertical().apply {
                setPadding(dp(18), dp(18), dp(18), dp(20))
                addView(context.text(title, TextRole.TITLE))
                subtitle?.let { addView(context.text(it, TextRole.BODY_SMALL, c.inkSubtle), matchWrap(4, context)) }
                addView(context.space(14))
                content()
            })
        }

    private fun buildForm() {
        coverPreview = FrameLayout(this).apply {
            val style = CategoryStyle.of(null)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(style.start, style.end)).apply {
                cornerRadius = dpf(24)
            }
            clipToOutline = true
            addView(vertical(Gravity.CENTER).apply {
                addView(ImageView(context).apply { setImageDrawable(context.glyph(Glyph.IMAGE, Color.WHITE, 34, 1.6f)) },
                    LinearLayout.LayoutParams(dp(34), dp(34)))
                addView(context.text("Vista previa de la portada", TextRole.LABEL, Color.WHITE), matchWrap(10, context))
                addView(context.text("Pega un enlace HTTPS en la sección Portada", TextRole.CAPTION, 0xCCFFFFFF.toInt()), matchWrap(4, context))
            }, FrameLayout.LayoutParams(-1, -1))
            coverImage = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; visibility = View.GONE }
            addView(coverImage, FrameLayout.LayoutParams(-1, -1))
        }
        form.addView(coverPreview, LinearLayout.LayoutParams(-1, dp(168)))

        name = Fields.create(this, "Nombre del taller", Glyph.SPARKLE, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, maxLength = 120)
        category = Fields.create(this, "Categoría", Glyph.TAG, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, maxLength = 60)
        instructor = Fields.create(this, "Persona instructora", Glyph.USER, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, maxLength = 120)
        form.addView(section("Lo esencial", "Así aparecerá el taller en la agenda.") {
            addView(name.layout)
            addView(category.layout, matchWrap(12, context))
            addView(categorySuggestions(), matchWrap(10, context))
            addView(instructor.layout, matchWrap(14, context))
        }, matchWrap(16, this))

        description = Fields.create(this, "¿Qué aprenderán las personas?", null,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, maxLength = 255)
        description.edit.minLines = 3
        description.edit.gravity = Gravity.TOP or Gravity.START
        form.addView(section("Descripción") { addView(description.layout) }, matchWrap(16, this))

        date = pickerField("Fecha", Glyph.CALENDAR) { chooseDate() }
        time = pickerField("Hora", Glyph.CLOCK) { chooseTime() }
        capacity = Fields.create(this, "Cupo máximo", Glyph.USERS, InputType.TYPE_CLASS_NUMBER, maxLength = 4)
        capacity.layout.isCounterEnabled = false
        form.addView(section("Cuándo y cuántos") {
            addView(context.horizontal(Gravity.TOP).apply {
                addView(date.layout, weighted())
                addView(time.layout, weighted().apply { marginStart = dp(10) })
            })
            addView(context.horizontal().apply {
                addView(capacity.layout, weighted())
                addView(context.iconButton(Glyph.MINUS, "Restar cupo") { stepCapacity(-1) },
                    LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(10) })
                addView(context.iconButton(Glyph.PLUS, "Sumar cupo", c.onPrimary, c.primary, strokeColor = Color.TRANSPARENT) { stepCapacity(1) },
                    LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(8) })
            }, matchWrap(12, context))
        }, matchWrap(16, this))

        imageUrl = Fields.create(this, "Enlace de imagen HTTPS", Glyph.IMAGE, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            helper = "Opcional. Si lo dejas vacío usaremos una portada por categoría.", maxLength = 255)
        imageUrl.layout.isCounterEnabled = false
        imageUrl.edit.addTextChangedListener(afterChange {
            coverTask?.let(coverPreview::removeCallbacks)
            val task = Runnable { updateCoverPreview() }
            coverTask = task
            coverPreview.postDelayed(task, 450)
        })
        form.addView(section("Portada", "Una buena imagen ayuda a que más personas se animen.") { addView(imageUrl.layout) }, matchWrap(16, this))
    }

    private fun categorySuggestions(): View = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(context.horizontal().apply {
            SUGGESTED_CATEGORIES.forEachIndexed { index, value ->
                addView(context.pill(value, c.inkMuted, c.surfaceVariant).apply {
                    isClickable = true
                    isFocusable = true
                    Motion.pressable(this, 0.94f)
                    setOnClickListener { category.edit.setText(value); category.edit.setSelection(value.length) }
                }, LinearLayout.LayoutParams(-2, -2).apply { if (index > 0) marginStart = dp(6) })
            }
        })
    }

    /** Campo de solo lectura que abre un selector al tocarlo. */
    private fun pickerField(label: String, glyph: Glyph, onPick: () -> Unit): Field =
        Fields.create(this, label, glyph, InputType.TYPE_NULL).apply {
            edit.isFocusable = false
            edit.isCursorVisible = false
            edit.keyListener = null
            edit.setOnClickListener { onPick() }
        }

    private fun bottomBar(): View = vertical().apply {
        setBackgroundColor(c.surface)
        elevation = dpf(8)
        addView(context.divider())
        save = Buttons.create(context, if (workshopId == null) "Publicar taller" else "Guardar cambios", ButtonTone.PRIMARY, Glyph.CHECK) { save() }
        addView(FrameLayout(context).apply {
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                view.setPadding(dp(20), dp(14), dp(20), dp(14) + maxOf(bars.bottom, ime.bottom))
                insets
            }
            addView(save, FrameLayout.LayoutParams(-1, -2))
        })
    }

    // ---------- Selectores ----------

    private fun chooseDate() {
        val selection = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        MaterialDatePicker.Builder.datePicker()
            .setTitleText("¿Qué día será el taller?")
            .setSelection(selection)
            .setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
            .build()
            .apply {
                addOnPositiveButtonClickListener { millis ->
                    selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    renderDateTime()
                }
                show(supportFragmentManager, "workshop-date")
            }
    }

    private fun chooseTime() {
        MaterialTimePicker.Builder()
            .setTitleText("¿A qué hora empieza?")
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(selectedTime?.hour ?: 9)
            .setMinute(selectedTime?.minute ?: 0)
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    selectedTime = LocalTime.of(hour, minute)
                    renderDateTime()
                }
                show(supportFragmentManager, "workshop-time")
            }
    }

    private fun renderDateTime() {
        selectedDate?.let {
            date.edit.setText(it.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale)).replaceFirstChar { ch -> ch.titlecase(locale) })
            date.layout.error = null
        }
        selectedTime?.let {
            time.edit.setText(it.format(DateTimeFormatter.ofPattern("h:mm a", locale)))
            time.layout.error = null
        }
    }

    private fun stepCapacity(delta: Int) {
        val next = ((capacity.value.toIntOrNull() ?: 0) + delta).coerceIn(1, 9999)
        capacity.edit.setText(next.toString())
        capacity.edit.setSelection(capacity.edit.length())
    }

    private fun updateCoverPreview() {
        val url = imageUrl.value
        if (url.startsWith("https://", true) && Patterns.WEB_URL.matcher(url).matches()) {
            Glide.with(this).load(url).transition(DrawableTransitionOptions.withCrossFade(300)).into(coverImage)
            if (coverImage.visibility != View.VISIBLE) Motion.crossfade(coverImage)
        } else if (coverImage.visibility == View.VISIBLE) {
            coverImage.animate().alpha(0f).setDuration(Motion.SHORT).withEndAction {
                coverImage.visibility = View.GONE; coverImage.alpha = 1f; coverImage.setImageDrawable(null)
            }.start()
        }
    }

    // ---------- Datos ----------

    private fun loadWorkshop(id: Long) {
        WorkshopsRepository.workshop(id) { result ->
            if (isFinishing || isDestroyed) return@workshop
            when (result) {
                is ApiResult.Success -> {
                    fill(result.value)
                    Motion.crossfade(form, loading)
                    Motion.stagger((0 until form.childCount).map(form::getChildAt), step = 45)
                }
                is ApiResult.Failure -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun fill(workshop: Workshop) {
        name.edit.setText(workshop.name)
        description.edit.setText(workshop.description)
        instructor.edit.setText(workshop.instructor)
        category.edit.setText(workshop.category ?: "Otros")
        imageUrl.edit.setText(workshop.imageUrl.orEmpty())
        capacity.edit.setText(workshop.maxCapacity.toString())
        runCatching { LocalDateTime.parse(workshop.dateTime) }.getOrNull()?.let {
            selectedDate = it.toLocalDate()
            selectedTime = it.toLocalTime()
            renderDateTime()
        }
        updateCoverPreview()
    }

    private fun save() {
        val errors = mutableListOf<Field>()
        fun require(field: Field, ok: Boolean, message: String) { if (!ok) { field.error(message); errors += field } }

        val max = capacity.value.toIntOrNull()
        val image = imageUrl.value
        require(name, name.value.length >= 3, "Escribe un nombre de al menos 3 letras")
        require(category, category.value.isNotEmpty(), "Elige o escribe una categoría")
        require(instructor, instructor.value.isNotEmpty(), "¿Quién dictará el taller?")
        require(description, description.value.length >= 10, "Cuenta un poco más (mínimo 10 caracteres)")
        require(date, selectedDate != null, "Elige el día")
        require(time, selectedTime != null, "Elige la hora")
        require(capacity, max != null && max > 0, "Debe ser mayor que cero")
        require(imageUrl, image.isEmpty() || (image.startsWith("https://", true) && Patterns.WEB_URL.matcher(image).matches()),
            "Debe ser un enlace HTTPS válido")

        if (errors.isNotEmpty()) {
            scrollTo(errors.first().layout)
            snack("Revisa los campos marcados.", Tone.ERROR, save)
            return
        }

        val dateTime = selectedDate!!.atTime(selectedTime!!).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val request = WorkshopRequest(name.value, description.value, instructor.value, category.value, image.ifBlank { null }, max!!, dateTime)
        Buttons.setLoading(save, true, if (workshopId == null) "Publicando…" else "Guardando…")
        WorkshopsRepository.saveWorkshop(workshopId, request) { result ->
            if (isFinishing || isDestroyed) return@saveWorkshop
            Buttons.setLoading(save, false)
            when (result) {
                is ApiResult.Success -> {
                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_MESSAGE,
                        if (workshopId == null) "Taller publicado." else "Cambios guardados y verificados."))
                    finish()
                }
                is ApiResult.Failure -> { Motion.shake(save); snack(result.message, Tone.ERROR, save) }
            }
        }
    }

    private fun scrollTo(target: View) {
        val targetLocation = IntArray(2).also(target::getLocationInWindow)
        val scrollLocation = IntArray(2).also(scroll::getLocationInWindow)
        val y = scroll.scrollY + targetLocation[1] - scrollLocation[1] - dp(96)
        scroll.smoothScrollTo(0, y.coerceAtLeast(0))
    }

    private fun afterChange(block: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = block()
    }
}
