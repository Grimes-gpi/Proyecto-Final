package com.example.clase18_08zulema.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.RegistrationRequest
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopsRepository
import com.example.clase18_08zulema.ui.components.Formats
import com.example.clase18_08zulema.ui.components.capacityBar
import com.example.clase18_08zulema.ui.components.workshopCover
import com.example.clase18_08zulema.ui.kit.ButtonTone
import com.example.clase18_08zulema.ui.kit.Buttons
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
import com.example.clase18_08zulema.ui.kit.rounded
import com.example.clase18_08zulema.ui.kit.snack
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.example.clase18_08zulema.ui.kit.weighted
import com.google.android.material.button.MaterialButton

/** Un instructor inscribe a otra persona (nombre + documento) en uno de sus talleres. */
class RegistrationFormActivity : SecureActivity() {
    companion object { const val EXTRA_ID = "workshop_id" }

    private val c by lazy { colors }
    private var workshopId: Long = -1
    private lateinit var summary: FrameLayout
    private lateinit var formColumn: LinearLayout
    private lateinit var success: LinearLayout
    private lateinit var beneficiary: Field
    private lateinit var document: Field
    private lateinit var submit: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureSession(instructorOnly = true)) return
        workshopId = intent.getLongExtra(EXTRA_ID, -1)
        enableEdgeToEdge()

        val root = FrameLayout(this).apply { setBackgroundColor(c.background) }
        val page = vertical().apply {
            setPadding(dp(20), 0, dp(20), 0)
            applySystemInsets(top = true, extraTopDp = 12, extraBottomDp = 120)
        }
        page.addView(horizontal().apply {
            addView(context.iconButton(Glyph.BACK, "Volver") { finish() })
            addView(context.vertical().apply {
                addView(context.text("Inscribir persona", TextRole.HEADLINE))
                addView(context.text("Reserva un cupo con su nombre y documento.", TextRole.BODY_SMALL, c.inkMuted), matchWrap(4, context))
            }, weighted().apply { marginStart = dp(14) })
        })

        summary = FrameLayout(this).apply { addView(Skeleton.rows(context, 1)) }
        page.addView(summary, matchWrap(22, this))

        formColumn = vertical()
        beneficiary = Fields.create(this, "Nombre completo", Glyph.USER, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, maxLength = 120)
        document = Fields.create(this, "Documento de identidad", Glyph.ID, InputType.TYPE_CLASS_TEXT,
            helper = "Evita inscripciones duplicadas en el mismo taller.", maxLength = 30)
        formColumn.addView(card(radiusDp = 24).apply {
            addView(vertical().apply {
                setPadding(dp(18), dp(18), dp(18), dp(20))
                addView(context.text("Datos de la persona", TextRole.TITLE))
                addView(beneficiary.layout, matchWrap(16, context))
                addView(document.layout, matchWrap(12, context))
            })
        })
        page.addView(formColumn, matchWrap(16, this))

        success = buildSuccess().apply { visibility = View.GONE }
        page.addView(success, matchWrap(40, this))

        root.addView(NestedScrollView(this).apply { clipToPadding = false; addView(page) }, FrameLayout.LayoutParams(-1, -1))
        root.addView(bottomBar(), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        Motion.stagger(listOf(summary, formColumn), step = 70)
        loadSummary()
    }

    private fun loadSummary() {
        WorkshopsRepository.workshop(workshopId) { result ->
            if (isFinishing || isDestroyed) return@workshop
            summary.removeAllViews()
            when (result) {
                is ApiResult.Success -> summary.addView(summaryCard(result.value))
                is ApiResult.Failure -> summary.addView(errorText(result.message))
            }
            Motion.enter(summary, distanceDp = 8)
        }
    }

    private fun errorText(message: String): View = text(message, TextRole.BODY_SMALL, c.danger)

    private fun summaryCard(workshop: Workshop): View = card(radiusDp = 24).apply {
        addView(horizontal().apply {
            setPadding(dp(12), dp(12), dp(16), dp(12))
            addView(context.workshopCover(workshop, dp(72), dpf(16)).apply { clipToOutline = true },
                LinearLayout.LayoutParams(dp(72), dp(72)))
            addView(context.vertical().apply {
                addView(context.text(workshop.name, TextRole.TITLE).apply { maxLines = 2 })
                addView(context.text("${Formats.weekday(workshop.dateTime)} ${Formats.day(workshop.dateTime)} ${Formats.month(workshop.dateTime).lowercase()} · ${Formats.time(workshop.dateTime)}",
                    TextRole.BODY_SMALL, c.inkMuted), matchWrap(4, context))
                val bar = context.capacityBar(workshop)
                addView(bar, matchWrap(10, context))
                bar.post { bar.setProgressCompat((workshop.fillRatio * 100).toInt(), true) }
                addView(context.text("Quedan ${Formats.slots(workshop.availableSlots)}", TextRole.CAPTION,
                    if (workshop.availableSlots <= 3) c.warning else c.success), matchWrap(6, context))
            }, weighted().apply { marginStart = dp(14) })
        })
    }

    private fun bottomBar(): View = vertical().apply {
        setBackgroundColor(c.surface)
        elevation = dpf(8)
        addView(context.divider())
        submit = Buttons.create(context, "Confirmar inscripción", ButtonTone.PRIMARY, Glyph.CHECK) { register() }
        addView(FrameLayout(context).apply {
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                view.setPadding(dp(20), dp(14), dp(20), dp(14) + maxOf(bars.bottom, ime.bottom))
                insets
            }
            addView(submit, FrameLayout.LayoutParams(-1, -2))
        })
    }

    private fun buildSuccess(): LinearLayout = vertical(Gravity.CENTER_HORIZONTAL).apply {
        addView(FrameLayout(context).apply {
            tag = "badge"
            background = rounded(c.success, dpf(44))
            addView(ImageView(context).apply { setImageDrawable(context.glyph(Glyph.CHECK, Color.WHITE, 40, 2.6f)) },
                FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(88), dp(88)))
        addView(context.text("¡Inscripción confirmada!", TextRole.HEADLINE).apply { gravity = Gravity.CENTER }, matchWrap(20, context))
        addView(context.text("", TextRole.BODY, c.inkMuted).apply { gravity = Gravity.CENTER; tag = "message" }, matchWrap(8, context))
    }

    private fun register() {
        var valid = true
        if (beneficiary.value.length < 3) { beneficiary.error("Escribe el nombre completo"); valid = false }
        if (document.value.isEmpty()) { document.error("El documento es obligatorio"); valid = false }
        if (!valid || workshopId < 1) return

        Buttons.setLoading(submit, true, "Inscribiendo…")
        WorkshopsRepository.register(RegistrationRequest(workshopId, beneficiary.value, document.value)) { result ->
            if (isFinishing || isDestroyed) return@register
            when (result) {
                is ApiResult.Success -> showSuccess(result.value.beneficiaryName)
                is ApiResult.Failure -> {
                    Buttons.setLoading(submit, false)
                    Motion.shake(submit)
                    snack(result.message, Tone.ERROR, submit)
                }
            }
        }
    }

    private fun showSuccess(name: String) {
        success.findViewWithTag<android.widget.TextView>("message").text = "$name ya tiene su cupo reservado."
        Motion.crossfade(success, formColumn, summary)
        Motion.pop(success.findViewWithTag("badge"), 120)
        submit.isEnabled = false
        setResult(Activity.RESULT_OK, Intent().putExtra(WorkshopFormActivity.EXTRA_MESSAGE, "Inscripción confirmada para $name."))
        success.postDelayed({ if (!isFinishing) finish() }, 1400)
    }
}
