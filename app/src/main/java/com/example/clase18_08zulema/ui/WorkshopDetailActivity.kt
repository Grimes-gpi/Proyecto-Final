package com.example.clase18_08zulema.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.Registration
import com.example.clase18_08zulema.data.UserProfile
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopsRepository
import com.example.clase18_08zulema.ui.components.Formats
import com.example.clase18_08zulema.ui.components.Sheets
import com.example.clase18_08zulema.ui.components.availability
import com.example.clase18_08zulema.ui.components.capacityBar
import com.example.clase18_08zulema.ui.components.transitionNameFor
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
import com.example.clase18_08zulema.ui.kit.avatar
import com.example.clase18_08zulema.ui.kit.card
import com.example.clase18_08zulema.ui.kit.colors
import com.example.clase18_08zulema.ui.kit.divider
import com.example.clase18_08zulema.ui.kit.dp
import com.example.clase18_08zulema.ui.kit.dpf
import com.example.clase18_08zulema.ui.kit.glyph
import com.example.clase18_08zulema.ui.kit.horizontal
import com.example.clase18_08zulema.ui.kit.iconButton
import com.example.clase18_08zulema.ui.kit.infoRow
import com.example.clase18_08zulema.ui.kit.matchWrap
import com.example.clase18_08zulema.ui.kit.pill
import com.example.clase18_08zulema.ui.kit.rounded
import com.example.clase18_08zulema.ui.kit.sectionHeader
import com.example.clase18_08zulema.ui.kit.snack
import com.example.clase18_08zulema.ui.kit.stateView
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.example.clase18_08zulema.ui.kit.weighted
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

class WorkshopDetailActivity : SecureActivity() {
    companion object {
        const val EXTRA_ID = "workshop_id"
        private const val STUDENT_BANNER = "student_banner"
    }

    private val c by lazy { colors }
    private lateinit var user: UserProfile
    private var workshopId: Long = -1
    private var current: Workshop? = null
    private var allRegistrations: List<Registration> = emptyList()
    /** Inscripción activa del estudiante en este taller; null si no está inscrito. */
    private var myRegistration: Registration? = null
    private var myRegistrationLoaded = false

    private lateinit var scroll: NestedScrollView
    private lateinit var hero: FrameLayout
    private lateinit var topBar: FrameLayout
    private lateinit var topTitle: TextView
    private lateinit var moreButton: View
    private lateinit var content: FrameLayout
    private lateinit var skeleton: View
    private lateinit var details: LinearLayout
    private lateinit var registrationsSection: LinearLayout
    private lateinit var registrationsList: LinearLayout
    private lateinit var registrationsCount: TextView
    private lateinit var registrationsSummary: LinearLayout
    private lateinit var registrationFilter: Field
    private lateinit var bottomBar: LinearLayout
    private lateinit var bottomCaption: TextView
    private lateinit var bottomValue: TextView
    private lateinit var cta: MaterialButton

    private val heroHeight by lazy { dp(300) }

    private val childLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(WorkshopFormActivity.EXTRA_MESSAGE)?.let { snack(it, Tone.SUCCESS, bottomBar) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        workshopId = intent.getLongExtra(EXTRA_ID, -1)
        setupContainerTransform()
        super.onCreate(savedInstanceState)
        if (!ensureSession()) return
        user = currentUser!!
        enableEdgeToEdge()

        val root = FrameLayout(this).apply { setBackgroundColor(c.background) }
        val column = vertical()

        hero = FrameLayout(this).apply { background = rounded(c.heroStart, 0f) }
        column.addView(hero, LinearLayout.LayoutParams(-1, heroHeight))

        val sheet = vertical().apply {
            setPadding(dp(22), dp(26), dp(22), dp(24))
            background = com.google.android.material.shape.MaterialShapeDrawable(
                com.google.android.material.shape.ShapeAppearanceModel.builder()
                    .setTopLeftCornerSize(dpf(28)).setTopRightCornerSize(dpf(28)).build()
            ).apply { fillColor = android.content.res.ColorStateList.valueOf(c.background) }
        }
        content = FrameLayout(this)
        skeleton = Skeleton.detail(this)
        details = vertical().apply { visibility = View.GONE }
        content.addView(details)
        content.addView(skeleton)
        sheet.addView(content)
        if (user.isInstructor) {
            registrationsSection = buildRegistrationsSection().apply { visibility = View.GONE }
            sheet.addView(registrationsSection, matchWrap(32, this))
        }
        column.addView(sheet, LinearLayout.LayoutParams(-1, -2).apply { topMargin = -dp(28) })

        scroll = NestedScrollView(this).apply {
            clipToPadding = false
            addView(column)
        }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        // Solo la franja superior: sin LayoutParams el FrameLayout la estiraría a toda la pantalla.
        root.addView(buildTopBar(), FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        root.addView(buildBottomBar(), FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            // Deja espacio para la barra inferior fija.
            view.setPadding(0, 0, 0, bottomBar.height.takeIf { it > 0 } ?: dp(110))
            insets
        }
        bottomBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (scroll.paddingBottom != bottomBar.height) scroll.setPadding(0, 0, 0, bottomBar.height)
        }
        scroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, y, _, _ -> onScrolled(y) })
    }

    /** Configura la expansión desde la tarjeta de la lista (MaterialContainerTransform de Material 3). */
    private fun setupContainerTransform() {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        findViewById<View>(android.R.id.content).transitionName = transitionNameFor(workshopId)
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        val background = colors.background
        window.sharedElementEnterTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 420
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(background)
            interpolator = Motion.emphasized
        }
        window.sharedElementReturnTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 320
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(background)
            interpolator = Motion.emphasized
        }
    }

    override fun onAuthenticatedResume() {
        if (workshopId > 0 && ::content.isInitialized) load()
    }

    // ---------- Carga ----------

    private fun load() {
        if (current == null) Motion.crossfade(skeleton, details)
        WorkshopsRepository.workshop(workshopId) { result ->
            if (isFinishing || isDestroyed) return@workshop
            when (result) {
                is ApiResult.Success -> {
                    val firstTime = current == null
                    current = result.value
                    showWorkshop(result.value, animate = firstTime)
                    if (user.isInstructor) loadRegistrations() else loadMyRegistration()
                }
                is ApiResult.Failure -> showError(result.message)
            }
        }
    }

    private fun loadMyRegistration() {
        WorkshopsRepository.myRegistrations { result ->
            if (isFinishing || isDestroyed) return@myRegistrations
            if (result is ApiResult.Success) {
                myRegistration = result.value.firstOrNull { it.workshopId == workshopId && it.isActive }
            }
            myRegistrationLoaded = true
            // Redibuja para reflejar "Inscrito" en la etiqueta, el aviso y el botón principal.
            current?.let { showWorkshop(it, animate = false) }
        }
    }

    private fun loadRegistrations() {
        if (allRegistrations.isEmpty()) {
            registrationsList.removeAllViews()
            registrationsList.addView(Skeleton.rows(this))
        }
        WorkshopsRepository.registrations(workshopId) { result ->
            if (isFinishing || isDestroyed) return@registrations
            when (result) {
                is ApiResult.Success -> { allRegistrations = result.value; showRegistrations() }
                is ApiResult.Failure -> {
                    registrationsList.removeAllViews()
                    registrationsList.addView(stateView(Glyph.ALERT, "No pudimos cargar las inscripciones", result.message,
                        c.danger, c.dangerContainer, primary = "Reintentar" to { loadRegistrations() }))
                }
            }
        }
    }

    // ---------- Taller ----------

    private fun canManage(workshop: Workshop) = user.isInstructor && (workshop.ownerId == null || workshop.ownerId == user.id)

    private fun showWorkshop(workshop: Workshop, animate: Boolean) {
        hero.removeAllViews()
        hero.addView(workshopCover(workshop, heroHeight), FrameLayout.LayoutParams(-1, -1))
        topTitle.text = workshop.name
        moreButton.visibility = if (canManage(workshop) && workshop.isActive) View.VISIBLE else View.GONE

        details.removeAllViews()
        val state = availability(workshop, enrolled = myRegistration != null)
        details.addView(horizontal().apply {
            addView(context.pill(workshop.category ?: "Comunidad", c.primary, c.primaryContainer, Glyph.TAG))
            addView(context.pill(state.label, state.foreground, state.background),
                LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        })
        details.addView(text(workshop.name, TextRole.HEADLINE), matchWrap(14, this))

        details.addView(horizontal().apply {
            addView(context.avatar(workshop.instructor, 40, c.accentContainer, c.accent))
            addView(context.vertical().apply {
                addView(context.text("A cargo de", TextRole.CAPTION, c.inkSubtle))
                addView(context.text(workshop.instructor, TextRole.TITLE), matchWrap(2, context))
            }, weighted().apply { marginStart = dp(12) })
        }, matchWrap(18, this))

        val info = card(radiusDp = 24).apply {
            addView(vertical().apply {
                setPadding(dp(18), dp(18), dp(18), dp(18))
                addView(context.infoRow(Glyph.CALENDAR, "Fecha", Formats.longDate(workshop.dateTime)))
                addView(context.infoRow(Glyph.CLOCK, "Hora", Formats.time(workshop.dateTime)), matchWrap(16, context))
                addView(context.infoRow(Glyph.USERS, "Ocupación", "${workshop.registeredPeople} de ${workshop.maxCapacity} personas",
                    c.success, c.successContainer), matchWrap(16, context))
                val bar = context.capacityBar(workshop)
                addView(bar, matchWrap(14, context))
                bar.post { bar.setProgressCompat((workshop.fillRatio * 100).toInt(), true) }
            })
        }
        details.addView(info, matchWrap(22, this))

        details.addView(text("Acerca del taller", TextRole.TITLE_LARGE), matchWrap(26, this))
        details.addView(text(workshop.description, TextRole.BODY, c.inkMuted), matchWrap(10, this))

        if (user.isInstructor && !canManage(workshop)) {
            details.addView(banner(Glyph.ALERT, "Este taller pertenece a otro instructor. Puedes consultarlo, pero no modificarlo.",
                c.warning, c.warningContainer), matchWrap(22, this))
        }
        renderStudentBanner()
        renderBottomBar(workshop)

        Motion.crossfade(details, skeleton)
        if (animate) Motion.stagger((0 until details.childCount).map(details::getChildAt), step = 45)
        if (user.isInstructor) registrationsSection.visibility = View.VISIBLE
    }

    private fun renderStudentBanner() {
        if (user.isInstructor) return
        details.findViewWithTag<View>(STUDENT_BANNER)?.let(details::removeView)
        if (myRegistration != null) {
            details.addView(banner(Glyph.CHECK, "Estás inscrito en este taller. Te esperamos el ${Formats.longDate(current?.dateTime.orEmpty()).lowercase()}.",
                c.success, c.successContainer).apply { tag = STUDENT_BANNER }, minOf(4, details.childCount), matchWrap(18, this))
        }
    }

    private fun banner(glyph: Glyph, message: String, tint: Int, fill: Int): View = horizontal(Gravity.TOP).apply {
        setPadding(dp(14), dp(14), dp(16), dp(14))
        background = rounded(fill, dpf(18))
        addView(ImageView(context).apply { setImageDrawable(context.glyph(glyph, tint, 20)) }, LinearLayout.LayoutParams(dp(20), dp(20)))
        addView(context.text(message, TextRole.BODY_SMALL, c.ink), weighted().apply { marginStart = dp(12) })
    }

    private fun showError(message: String) {
        details.removeAllViews()
        details.addView(stateView(Glyph.ALERT, "No pudimos abrir el taller", message, c.danger, c.dangerContainer,
            primary = "Reintentar" to { load() }))
        Motion.crossfade(details, skeleton)
        bottomBar.visibility = View.GONE
    }

    // ---------- Barras ----------

    private fun buildTopBar(): View {
        topBar = FrameLayout(this).apply {
            setPadding(dp(16), 0, dp(16), dp(10))
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                view.setPadding(dp(16), top + dp(8), dp(16), dp(10))
                insets
            }
        }
        val translucent = ColorUtils.setAlphaComponent(c.surface, 235)
        topBar.addView(iconButton(Glyph.BACK, "Volver", fill = translucent, strokeColor = Color.TRANSPARENT) { onBackPressedDispatcher.onBackPressed() },
            FrameLayout.LayoutParams(dp(44), dp(44), Gravity.START or Gravity.CENTER_VERTICAL))
        topTitle = text("", TextRole.TITLE).apply { alpha = 0f; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        topBar.addView(topTitle, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(58); marginEnd = dp(108)
        })
        val actions = horizontal()
        actions.addView(iconButton(Glyph.SHARE, "Compartir taller", fill = translucent, strokeColor = Color.TRANSPARENT) {
            current?.let(::shareWorkshop)
        })
        moreButton = iconButton(Glyph.MORE, "Más opciones", fill = translucent, strokeColor = Color.TRANSPARENT) { showMoreMenu(moreButton) }
            .apply { visibility = View.GONE }
        actions.addView(moreButton, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(8) })
        topBar.addView(actions, FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL))
        return topBar
    }

    private fun onScrolled(y: Int) {
        hero.translationY = y * 0.4f
        val fadeStart = heroHeight * 0.45f
        val progress = ((y - fadeStart) / (heroHeight * 0.4f)).coerceIn(0f, 1f)
        topBar.setBackgroundColor(ColorUtils.setAlphaComponent(c.background, (progress * 255).toInt()))
        topBar.elevation = if (progress >= 1f) dpf(2) else 0f
        topTitle.alpha = progress
    }

    private fun buildBottomBar(): View {
        bottomBar = vertical().apply {
            setBackgroundColor(c.surface)
            elevation = dpf(8)
        }
        bottomBar.addView(divider())
        val row = horizontal().apply {
            setPadding(dp(22), dp(14), dp(22), dp(14))
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                view.setPadding(dp(22), dp(14), dp(22), dp(14) + bottom)
                insets
            }
        }
        bottomCaption = text("Cupos disponibles", TextRole.CAPTION, c.inkSubtle)
        bottomValue = text("—", TextRole.TITLE_LARGE)
        row.addView(vertical().apply {
            addView(bottomCaption)
            addView(bottomValue, matchWrap(2, context))
        }, weighted())
        cta = Buttons.create(this, "Cargando…", ButtonTone.PRIMARY, heightDp = 52) { onCtaClicked() }
        cta.isEnabled = false
        row.addView(cta, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(16) })
        bottomBar.addView(row)
        return bottomBar
    }

    private enum class CtaAction { ENROLL, UNENROLL, REGISTER_OTHER, SHARE, NONE }
    private var ctaAction = CtaAction.NONE

    private fun renderBottomBar(workshop: Workshop) {
        bottomBar.visibility = View.VISIBLE
        bottomCaption.text = if (workshop.isActive) "Cupos disponibles" else "Estado"
        bottomValue.text = if (workshop.isActive) "${workshop.availableSlots} de ${workshop.maxCapacity}" else "Cancelado"
        bottomValue.setTextColor(if (!workshop.isActive || workshop.availableSlots == 0L) c.danger else c.ink)

        val (label, action, tone) = when {
            user.isInstructor && !canManage(workshop) -> Triple("Compartir", CtaAction.SHARE, ButtonTone.TONAL)
            user.isInstructor && workshop.isActive && workshop.availableSlots > 0 ->
                Triple("Inscribir persona", CtaAction.REGISTER_OTHER, ButtonTone.PRIMARY)
            user.isInstructor -> Triple("Compartir", CtaAction.SHARE, ButtonTone.TONAL)
            !myRegistrationLoaded -> Triple("Cargando…", CtaAction.NONE, ButtonTone.PRIMARY)
            myRegistration != null -> Triple("Cancelar inscripción", CtaAction.UNENROLL, ButtonTone.DANGER_TONAL)
            !workshop.isActive -> Triple("No disponible", CtaAction.NONE, ButtonTone.PRIMARY)
            workshop.availableSlots == 0L -> Triple("Sin cupos", CtaAction.NONE, ButtonTone.PRIMARY)
            else -> Triple("Inscribirme", CtaAction.ENROLL, ButtonTone.PRIMARY)
        }
        ctaAction = action
        // Se reemplaza el botón para aplicar el tono nuevo con una transición suave.
        val parent = cta.parent as LinearLayout
        val replacement = Buttons.create(this, label, tone, if (action == CtaAction.ENROLL) Glyph.ARROW_RIGHT else null, heightDp = 52) { onCtaClicked() }
        if (action == CtaAction.ENROLL) replacement.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        replacement.isEnabled = action != CtaAction.NONE
        val index = parent.indexOfChild(cta)
        parent.removeView(cta)
        parent.addView(replacement, index, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(16) })
        replacement.alpha = 0f
        replacement.animate().alpha(1f).setDuration(Motion.MEDIUM).start()
        cta = replacement
    }

    private fun onCtaClicked() {
        val workshop = current ?: return
        when (ctaAction) {
            CtaAction.ENROLL -> confirmEnroll(workshop)
            CtaAction.UNENROLL -> confirmUnenroll(workshop)
            CtaAction.REGISTER_OTHER -> childLauncher.launch(Intent(this, RegistrationFormActivity::class.java)
                .putExtra(RegistrationFormActivity.EXTRA_ID, workshop.id))
            CtaAction.SHARE -> shareWorkshop(workshop)
            CtaAction.NONE -> Unit
        }
    }

    // ---------- Acciones del estudiante ----------

    private fun confirmEnroll(workshop: Workshop) {
        Sheets.confirm(this, Glyph.TICKET, "Confirma tu inscripción",
            "Reservaremos un cupo a tu nombre en “${workshop.name}”.", "Confirmar inscripción",
            details = {
                addView(context.card(radiusDp = 20, fill = c.surfaceVariant, stroke = false).apply {
                    addView(context.vertical().apply {
                        setPadding(dp(16), dp(16), dp(16), dp(16))
                        addView(context.infoRow(Glyph.USER, "Participante", user.fullName))
                        addView(context.infoRow(Glyph.ID, "Documento", user.document ?: "—"), matchWrap(12, context))
                        addView(context.infoRow(Glyph.CALENDAR, "Fecha", "${Formats.longDate(workshop.dateTime)} · ${Formats.time(workshop.dateTime)}"),
                            matchWrap(12, context))
                    })
                }, matchWrap(20, context))
            }
        ) { dialog, button ->
            Buttons.setLoading(button, true, "Inscribiendo…")
            WorkshopsRepository.enrollMyself(workshop.id) { result ->
                if (isFinishing || isDestroyed) return@enrollMyself
                Buttons.setLoading(button, false)
                when (result) {
                    is ApiResult.Success -> {
                        dialog.dismiss()
                        myRegistration = result.value
                        snack("¡Listo! Quedaste inscrito.", Tone.SUCCESS, bottomBar)
                        load()
                    }
                    is ApiResult.Failure -> { Motion.shake(button); snack(result.message, Tone.ERROR) }
                }
            }
        }
    }

    private fun confirmUnenroll(workshop: Workshop) {
        val document = user.document ?: return
        Sheets.confirm(this, Glyph.CLOSE, "¿Cancelar tu inscripción?",
            "Liberarás tu cupo en “${workshop.name}” para otra persona. Podrás volver a inscribirte si hay cupos.",
            "Sí, cancelar", ButtonTone.DANGER
        ) { dialog, button ->
            Buttons.setLoading(button, true, "Cancelando…")
            WorkshopsRepository.cancelRegistration(workshop.id, document) { result ->
                if (isFinishing || isDestroyed) return@cancelRegistration
                Buttons.setLoading(button, false)
                when (result) {
                    is ApiResult.Success -> {
                        dialog.dismiss()
                        myRegistration = null
                        snack("Inscripción cancelada; el cupo quedó libre.", Tone.INFO, bottomBar)
                        load()
                    }
                    is ApiResult.Failure -> { Motion.shake(button); snack(result.message, Tone.ERROR) }
                }
            }
        }
    }

    // ---------- Acciones del instructor ----------

    private fun showMoreMenu(anchor: View) {
        val workshop = current ?: return
        PopupMenu(this, anchor, Gravity.END).apply {
            menu.add(0, 1, 0, "Editar taller")
            menu.add(0, 2, 1, "Cancelar taller")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> childLauncher.launch(Intent(this@WorkshopDetailActivity, WorkshopFormActivity::class.java)
                        .putExtra(WorkshopFormActivity.EXTRA_ID, workshop.id))
                    2 -> confirmCancelWorkshop(workshop)
                }
                true
            }
            show()
        }
    }

    private fun confirmCancelWorkshop(workshop: Workshop) {
        Sheets.confirm(this, Glyph.ALERT, "¿Cancelar el taller?",
            "“${workshop.name}” dejará de aceptar inscripciones. Las personas inscritas conservarán su registro.",
            "Cancelar taller", ButtonTone.DANGER
        ) { dialog, button ->
            Buttons.setLoading(button, true, "Cancelando…")
            WorkshopsRepository.cancelWorkshop(workshop.id) { result ->
                if (isFinishing || isDestroyed) return@cancelWorkshop
                Buttons.setLoading(button, false)
                when (result) {
                    is ApiResult.Success -> { dialog.dismiss(); snack("Taller cancelado.", Tone.INFO, bottomBar); load() }
                    is ApiResult.Failure -> { Motion.shake(button); snack(result.message, Tone.ERROR) }
                }
            }
        }
    }

    private fun buildRegistrationsSection(): LinearLayout = vertical().apply {
        registrationsCount = context.pill("0", c.primary, c.primaryContainer)
        addView(context.sectionHeader("Personas inscritas", registrationsCount))
        registrationsSummary = context.horizontal()
        addView(registrationsSummary, matchWrap(12, context))
        registrationFilter = Fields.create(context, "Buscar por nombre o documento", Glyph.SEARCH,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        registrationFilter.edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = showRegistrations()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        addView(registrationFilter.layout, matchWrap(16, context))
        registrationsList = context.vertical()
        Motion.smoothLayout(registrationsList)
        addView(registrationsList, matchWrap(16, context))
    }

    private fun showRegistrations() {
        val workshop = current ?: return
        val active = allRegistrations.count { it.isActive }
        val attended = allRegistrations.count { it.isActive && it.attended }
        val cancelled = allRegistrations.count { !it.isActive }
        registrationsCount.text = active.toString()
        registrationsSummary.removeAllViews()
        listOf(
            Triple("$active activas", c.primary, c.primaryContainer),
            Triple("$attended asistieron", c.success, c.successContainer),
            Triple("$cancelled canceladas", c.inkMuted, c.surfaceVariant)
        ).forEachIndexed { index, (label, fg, bg) ->
            registrationsSummary.addView(pill(label, fg, bg), LinearLayout.LayoutParams(-2, -2).apply { if (index > 0) marginStart = dp(8) })
        }

        registrationsList.layoutTransition = null
        registrationsList.removeAllViews()
        if (allRegistrations.isEmpty()) {
            registrationsList.addView(stateView(Glyph.USERS, "Todavía no hay personas inscritas",
                "Cuando alguien se inscriba aparecerá aquí."))
            return
        }
        val query = registrationFilter.value
        val matching = if (query.isBlank()) allRegistrations else allRegistrations.filter {
            it.beneficiaryName.contains(query, ignoreCase = true) || it.beneficiaryDocument.contains(query, ignoreCase = true)
        }
        if (matching.isEmpty()) {
            registrationsList.addView(stateView(Glyph.SEARCH, "Sin coincidencias",
                "No encontramos personas con ese nombre o documento."))
            return
        }
        val manage = canManage(workshop)
        matching.forEachIndexed { index, person ->
            if (index > 0) registrationsList.addView(divider(), matchWrap(14, this))
            registrationsList.addView(registrationRow(person, manage), matchWrap(if (index == 0) 0 else 14, this))
        }
        Motion.smoothLayout(registrationsList)
    }

    private fun registrationRow(person: Registration, manage: Boolean): View = vertical().apply {
        val (status, fg, bg) = when {
            !person.isActive -> Triple("Cancelada", c.inkMuted, c.surfaceVariant)
            person.attended -> Triple("Asistió", c.success, c.successContainer)
            else -> Triple("Inscrita", c.primary, c.primaryContainer)
        }
        addView(context.horizontal().apply {
            addView(context.avatar(person.beneficiaryName, 42,
                if (person.isActive) c.primaryContainer else c.surfaceVariant,
                if (person.isActive) c.onPrimaryContainer else c.inkSubtle))
            addView(context.vertical().apply {
                addView(context.text(person.beneficiaryName, TextRole.TITLE, if (person.isActive) c.ink else c.inkMuted))
                addView(context.text("Doc. ${person.beneficiaryDocument}", TextRole.BODY_SMALL, c.inkSubtle), matchWrap(3, context))
            }, weighted().apply { marginStart = dp(12) })
            addView(context.pill(status, fg, bg, if (person.attended) Glyph.CHECK else null))
        })
        if (manage && person.isActive && !person.attended) {
            addView(context.horizontal().apply {
                addView(Buttons.create(context, "Marcar asistencia", ButtonTone.TONAL, Glyph.CHECK, heightDp = 40) {
                    confirmAttendance(person)
                }, weighted())
                addView(Buttons.create(context, "Cancelar", ButtonTone.GHOST, heightDp = 40) {
                    confirmCancelRegistration(person)
                }.apply { setTextColor(c.danger) }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
            }, matchWrap(12, context).apply { marginStart = dp(54) })
        }
    }

    private fun confirmAttendance(person: Registration) {
        Sheets.confirm(this, Glyph.CHECK, "¿Marcar asistencia?",
            "${person.beneficiaryName} quedará como asistente. El cupo seguirá reservado.", "Marcar asistencia"
        ) { dialog, button ->
            Buttons.setLoading(button, true, "Guardando…")
            WorkshopsRepository.markAttendance(person.id) { result ->
                if (isFinishing || isDestroyed) return@markAttendance
                Buttons.setLoading(button, false)
                when (result) {
                    is ApiResult.Success -> { dialog.dismiss(); snack("Asistencia registrada.", Tone.SUCCESS, bottomBar); load() }
                    is ApiResult.Failure -> { Motion.shake(button); snack(result.message, Tone.ERROR) }
                }
            }
        }
    }

    private fun confirmCancelRegistration(person: Registration) {
        Sheets.confirm(this, Glyph.CLOSE, "¿Cancelar esta inscripción?",
            "La inscripción de ${person.beneficiaryName} se conservará como cancelada y su cupo quedará disponible.",
            "Cancelar inscripción", ButtonTone.DANGER
        ) { dialog, button ->
            Buttons.setLoading(button, true, "Cancelando…")
            WorkshopsRepository.cancelRegistration(workshopId, person.beneficiaryDocument) { result ->
                if (isFinishing || isDestroyed) return@cancelRegistration
                Buttons.setLoading(button, false)
                when (result) {
                    is ApiResult.Success -> { dialog.dismiss(); snack("Inscripción cancelada; cupo liberado.", Tone.INFO, bottomBar); load() }
                    is ApiResult.Failure -> { Motion.shake(button); snack(result.message, Tone.ERROR) }
                }
            }
        }
    }

    private fun shareWorkshop(workshop: Workshop) {
        val text = buildString {
            appendLine(workshop.name)
            appendLine(workshop.category ?: "Otros")
            appendLine("${Formats.longDate(workshop.dateTime)} · ${Formats.time(workshop.dateTime)}")
            appendLine("A cargo de ${workshop.instructor}")
            appendLine("${workshop.availableSlots} cupos disponibles")
            append(workshop.description)
        }
        lateinit var sheet: com.google.android.material.bottomsheet.BottomSheetDialog
        sheet = Sheets.build(this) {
            addView(context.text("Compartir taller", TextRole.HEADLINE))
            addView(context.text("El mensaje está listo. Puedes copiarlo o abrir el selector de aplicaciones.",
                TextRole.BODY, c.inkMuted), matchWrap(8, context))
            addView(context.card(radiusDp = 20, fill = c.surfaceVariant, stroke = false).apply {
                addView(context.text(text, TextRole.BODY_SMALL, c.ink).apply {
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    maxLines = 7
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
            }, matchWrap(18, context))
            addView(Buttons.create(context, "Elegir aplicación", ButtonTone.PRIMARY, Glyph.SHARE) {
                sheet.dismiss(); openShareChooser(workshop.name, text)
            }, matchWrap(20, context))
            addView(Buttons.create(context, "Copiar mensaje", ButtonTone.OUTLINED, Glyph.TICKET) {
                sheet.dismiss(); copyShareText(text)
                snack("Mensaje copiado. Ya puedes pegarlo donde quieras compartirlo.", Tone.SUCCESS, bottomBar)
            }, matchWrap(10, context))
        }
        sheet.show()
    }

    private fun openShareChooser(subject: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            startActivity(Intent.createChooser(shareIntent, "Compartir taller"))
        }.onFailure {
            copyShareText(text)
            snack("No fue posible abrir las aplicaciones. El mensaje fue copiado.", Tone.INFO, bottomBar)
        }
    }

    private fun copyShareText(text: String) {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Taller para compartir", text))
    }
}
