package com.example.clase18_08zulema

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.AuthRepository
import com.example.clase18_08zulema.data.UserProfile
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.data.WorkshopsRepository
import com.example.clase18_08zulema.ui.SecureActivity
import com.example.clase18_08zulema.ui.ServerConfig
import com.example.clase18_08zulema.ui.WorkshopDetailActivity
import com.example.clase18_08zulema.ui.WorkshopFormActivity
import com.example.clase18_08zulema.ui.components.Sheets
import com.example.clase18_08zulema.ui.components.transitionNameFor
import com.example.clase18_08zulema.ui.components.workshopCard
import com.example.clase18_08zulema.ui.kit.Field
import com.example.clase18_08zulema.ui.kit.Fields
import com.example.clase18_08zulema.ui.kit.Glyph
import com.example.clase18_08zulema.ui.kit.Motion
import com.example.clase18_08zulema.ui.kit.SegmentedControl
import com.example.clase18_08zulema.ui.kit.Skeleton
import com.example.clase18_08zulema.ui.kit.TextRole
import com.example.clase18_08zulema.ui.kit.Tone
import com.example.clase18_08zulema.ui.kit.applySystemInsets
import com.example.clase18_08zulema.ui.kit.avatar
import com.example.clase18_08zulema.ui.kit.colors
import com.example.clase18_08zulema.ui.kit.dp
import com.example.clase18_08zulema.ui.kit.dpf
import com.example.clase18_08zulema.ui.kit.glyph
import com.example.clase18_08zulema.ui.kit.horizontal
import com.example.clase18_08zulema.ui.kit.matchWrap
import com.example.clase18_08zulema.ui.kit.rounded
import com.example.clase18_08zulema.ui.kit.snack
import com.example.clase18_08zulema.ui.kit.stateView
import com.example.clase18_08zulema.ui.kit.style
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.example.clase18_08zulema.ui.kit.weighted
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import java.time.LocalTime
import java.util.Locale

class MainActivity : SecureActivity() {
    private val c by lazy { colors }
    private lateinit var user: UserProfile

    private lateinit var scroll: NestedScrollView
    private lateinit var search: Field
    private lateinit var chips: ChipGroup
    private lateinit var count: TextView
    private lateinit var stats: LinearLayout
    private lateinit var results: FrameLayout
    private lateinit var list: LinearLayout
    private lateinit var skeleton: View
    private lateinit var stateHolder: FrameLayout
    private var fab: ExtendedFloatingActionButton? = null

    private var workshops: List<Workshop> = emptyList()
    private var enrolledIds: Set<Long> = emptySet()
    private var categories: List<String> = listOf("Cocina", "Tecnología")
    private var selectedCategory: String? = null
    /** 0 = todos los talleres; 1 = "Mis inscripciones" (estudiante) o "Mis talleres" (instructor). */
    private var scope = 0
    private var searchTask: Runnable? = null
    private var returningFromDetail = false
    private var loadedOnce = false
    private var renderingChips = false

    private val formLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(WorkshopFormActivity.EXTRA_MESSAGE)?.let { snack(it, Tone.SUCCESS, fab) }
        }
    }

    private val localNetworkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) load(silent = false) else showPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transición "container transform": la tarjeta se expande hasta convertirse en el detalle.
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementsUseOverlay = false
        super.onCreate(savedInstanceState)
        if (!ensureSession()) return
        user = currentUser!!
        enableEdgeToEdge()

        val root = FrameLayout(this).apply { setBackgroundColor(c.background) }
        val page = vertical().apply {
            setPadding(dp(20), 0, dp(20), 0)
            applySystemInsets(top = true, bottom = true, extraTopDp = 12, extraBottomDp = if (user.isInstructor) 104 else 32)
        }

        page.addView(header())
        page.addView(text(headline(), TextRole.DISPLAY), matchWrap(22, this))
        page.addView(text(
            if (user.isInstructor) "Crea, edita y sigue la asistencia de tus talleres."
            else "Encuentra un espacio para aprender y compartir con tu barrio.",
            TextRole.BODY, c.inkMuted
        ), matchWrap(8, this))

        search = Fields.create(this, "Buscar talleres", Glyph.SEARCH, InputType.TYPE_CLASS_TEXT).apply {
            val pill = dpf(28)
            layout.setBoxCornerRadii(pill, pill, pill, pill)
            layout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        }
        page.addView(search.layout, matchWrap(22, this))
        search.edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchTask?.let(page::removeCallbacks)
                val task = Runnable { load(silent = false) }
                searchTask = task
                page.postDelayed(task, 350)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val scopes = if (user.isInstructor) listOf("Todos", "Mis talleres") else listOf("Explorar", "Mis inscripciones")
        page.addView(SegmentedControl(this, scopes) { scope = it; render(animate = true) }, matchWrap(16, this))

        stats = horizontal().apply { visibility = if (user.isInstructor) View.VISIBLE else View.GONE }
        page.addView(stats, matchWrap(16, this))

        chips = ChipGroup(this).apply {
            isSingleLine = true
            isSingleSelection = true
            isSelectionRequired = true
            chipSpacingHorizontal = dp(8)
            setOnCheckedStateChangeListener { group, ids ->
                if (renderingChips) return@setOnCheckedStateChangeListener
                val label = ids.firstOrNull()?.let { group.findViewById<Chip>(it)?.text?.toString() }
                val newCategory = label?.takeUnless { it == ALL }
                if (newCategory != selectedCategory) {
                    selectedCategory = newCategory
                    load(silent = false)
                }
            }
        }
        page.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(chips)
        }, matchWrap(18, this))
        renderChips()

        count = text("", TextRole.CAPTION, c.inkMuted)
        page.addView(horizontal().apply {
            addView(context.text(if (user.isInstructor) "Agenda de talleres" else "Próximos talleres", TextRole.TITLE_LARGE), weighted())
            addView(count)
        }, matchWrap(22, this))

        results = FrameLayout(this)
        list = vertical()
        skeleton = Skeleton.workshopList(this)
        stateHolder = FrameLayout(this).apply { visibility = View.GONE }
        results.addView(list)
        results.addView(skeleton)
        results.addView(stateHolder)
        page.addView(results, matchWrap(14, this))

        scroll = NestedScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
            addView(page)
        }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        if (user.isInstructor) root.addView(createFab())
        setContentView(root)

        Motion.stagger((0 until page.childCount).map(page::getChildAt).take(6), step = 45)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        returningFromDetail = true
    }

    override fun onAuthenticatedResume() {
        if (!::results.isInitialized) return
        if (returningFromDetail) {
            // Espera a que termine la transición de regreso antes de reemplazar las tarjetas.
            returningFromDetail = false
            scroll.postDelayed({ load(silent = true) }, 480)
        } else {
            load(silent = loadedOnce)
        }
    }

    // ---------- Datos ----------

    private fun load(silent: Boolean) {
        if (ServerConfig.requiresLocalNetworkPermission(this)) { showPermissionState(); return }
        if (!silent) Motion.crossfade(skeleton, list, stateHolder)
        if (!user.isInstructor) loadMyRegistrations()
        WorkshopsRepository.workshops(search.value, selectedCategory) { result ->
            if (isFinishing || isDestroyed) return@workshops
            when (result) {
                is ApiResult.Success -> {
                    workshops = result.value.sortedWith(compareBy<Workshop> { !it.isActive }.thenBy { it.dateTime })
                    mergeCategories(result.value)
                    render(animate = !silent || !loadedOnce)
                    loadedOnce = true
                }
                is ApiResult.Failure -> if (!silent || workshops.isEmpty()) showError(result.message)
                else snack(result.message, Tone.ERROR, fab)
            }
        }
    }

    private fun loadMyRegistrations() {
        WorkshopsRepository.myRegistrations { result ->
            if (isFinishing || isDestroyed) return@myRegistrations
            if (result is ApiResult.Success) {
                val ids = result.value.filter { it.isActive }.map { it.workshopId }.toSet()
                if (ids != enrolledIds) {
                    enrolledIds = ids
                    if (loadedOnce) render(animate = false)
                }
            }
        }
    }

    private fun mergeCategories(items: List<Workshop>) {
        val locale = Locale("es", "CO")
        val merged = (categories + items.mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) })
            .distinctBy { it.lowercase(locale) }
            .sortedBy { it.lowercase(locale) }
        if (merged != categories) {
            categories = merged
            renderChips()
        }
    }

    // ---------- Render ----------

    private fun render(animate: Boolean) {
        val visible = when {
            scope == 0 -> workshops
            user.isInstructor -> workshops.filter { it.ownerId == user.id }
            else -> workshops.filter { it.id in enrolledIds }
        }
        count.text = if (visible.size == 1) "1 taller" else "${visible.size} talleres"
        renderStats(visible, animate)

        if (visible.isEmpty()) {
            showEmpty()
            return
        }
        list.removeAllViews()
        val cards = visible.map { workshop ->
            workshopCard(workshop, enrolled = workshop.id in enrolledIds) { card -> openDetail(card, workshop) }.also {
                list.addView(it, matchWrap(if (list.childCount == 0) 0 else 18, this))
            }
        }
        Motion.crossfade(list, skeleton, stateHolder)
        if (animate) Motion.stagger(cards, step = 60, maxDelay = 360)
    }

    private fun renderStats(items: List<Workshop>, animate: Boolean) {
        if (!user.isInstructor) return
        val active = items.filter { it.isActive }
        val values = listOf(
            Triple("Activos", active.size.toLong(), c.primary),
            Triple("Inscritos", active.sumOf { it.registeredPeople }, c.success),
            Triple("Cupos libres", active.sumOf { it.availableSlots }, c.accent)
        )
        stats.removeAllViews()
        values.forEachIndexed { index, (label, value, tint) ->
            val number = text("0", TextRole.NUMBER, tint)
            stats.addView(vertical().apply {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = rounded(c.surface, dpf(20), dp(1), c.outline)
                addView(number)
                addView(context.text(label, TextRole.CAPTION, c.inkMuted), matchWrap(4, context))
            }, weighted().apply { if (index > 0) marginStart = dp(10) })
            if (animate) countUp(number, value) else number.text = value.toString()
        }
    }

    private fun countUp(view: TextView, target: Long) {
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 900
            interpolator = Motion.emphasizedDecelerate
            addUpdateListener { view.text = (it.animatedValue as Float).toLong().toString() }
            start()
        }
    }

    private fun renderChips() {
        renderingChips = true
        chips.removeAllViews()
        (listOf(ALL) + categories).forEach { label ->
            chips.addView(Chip(this).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                isCheckedIconVisible = false
                style(TextRole.LABEL)
                val checked = intArrayOf(android.R.attr.state_checked)
                chipBackgroundColor = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(c.ink, c.surface))
                setTextColor(ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(c.surface, c.inkMuted)))
                chipStrokeColor = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(c.ink, c.outline))
                chipStrokeWidth = dpf(1)
                chipCornerRadius = dpf(20)
                chipMinHeight = dpf(40)
                rippleColor = ColorStateList.valueOf(Color.argb(30, 0, 0, 0))
                isChecked = if (selectedCategory == null) label == ALL else label.equals(selectedCategory, ignoreCase = true)
            })
        }
        renderingChips = false
    }

    private fun showEmpty() {
        val (title, body) = when {
            scope == 1 && user.isInstructor -> "Aún no tienes talleres" to "Crea tu primer taller con el botón “Nuevo taller”."
            scope == 1 -> "Todavía no tienes inscripciones" to "Explora la agenda e inscríbete en el taller que te interese."
            else -> "No encontramos talleres" to "Prueba con otra búsqueda o con otra categoría."
        }
        val filtered = selectedCategory != null || search.value.isNotEmpty()
        showState(stateView(Glyph.SEARCH, title, body,
            secondary = if (filtered) "Limpiar filtros" to { clearFilters() } else null))
    }

    private fun showError(message: String) {
        showState(stateView(Glyph.ALERT, "No logramos llegar a la API", message, c.danger, c.dangerContainer,
            primary = "Volver a intentar" to { load(silent = false) },
            secondary = "Configurar servidor" to { ServerConfig.showDialog(this) { load(silent = false) } }))
    }

    private fun showPermissionState() {
        showState(stateView(Glyph.SERVER, "Conecta con tu comunidad",
            "Android necesita tu permiso para consultar el servidor de talleres en la red local.",
            primary = "Permitir conexión local" to { localNetworkPermission.launch(ServerConfig.LOCAL_NETWORK_PERMISSION) }))
    }

    private fun showState(view: View) {
        stateHolder.removeAllViews()
        stateHolder.addView(view)
        Motion.crossfade(stateHolder, skeleton, list)
    }

    private fun clearFilters() {
        selectedCategory = null
        renderChips()
        search.edit.setText("")
        load(silent = false)
    }

    // ---------- Encabezado y navegación ----------

    private fun header(): View = horizontal().apply {
        val greeting = when (LocalTime.now().hour) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }
        addView(context.vertical().apply {
            addView(context.text(greeting, TextRole.CAPTION, c.inkMuted))
            addView(context.text(user.firstName, TextRole.TITLE_LARGE), matchWrap(2, context))
        }, weighted())
        addView(context.avatar(user.fullName, 46).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Abrir perfil"
            Motion.pressable(this, 0.9f)
            setOnClickListener { openProfile() }
        })
    }

    private fun headline() = if (user.isInstructor) "Tus talleres,\nen un vistazo." else "Aprende algo\nnuevo esta semana."

    private fun openProfile() {
        Sheets.profile(this, user,
            onServer = { ServerConfig.showDialog(this) { load(silent = false) } },
            onLogout = {
                AuthRepository.logout()
                goToLogin()
            })
    }

    private fun openDetail(card: View, workshop: Workshop) {
        val options = ActivityOptions.makeSceneTransitionAnimation(this, card, transitionNameFor(workshop.id))
        startActivity(
            Intent(this, WorkshopDetailActivity::class.java).putExtra(WorkshopDetailActivity.EXTRA_ID, workshop.id),
            options.toBundle()
        )
    }

    private fun createFab(): ExtendedFloatingActionButton = ExtendedFloatingActionButton(this).apply {
        text = "Nuevo taller"
        isAllCaps = false
        icon = context.glyph(Glyph.PLUS, c.onPrimary, 22, 2.2f)
        setTextColor(c.onPrimary)
        backgroundTintList = ColorStateList.valueOf(c.primary)
        iconTint = ColorStateList.valueOf(c.onPrimary)
        shapeAppearanceModel = shapeAppearanceModel.withCornerSize(dpf(20))
        setOnClickListener { formLauncher.launch(Intent(this@MainActivity, WorkshopFormActivity::class.java)) }
        layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END)
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (view.layoutParams as FrameLayout.LayoutParams).setMargins(0, 0, dp(20) + bars.right, dp(20) + bars.bottom)
            view.requestLayout()
            insets
        }
        fab = this
        scroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, y, _, oldY ->
            if (y > oldY + 6 && isExtended) shrink() else if (y < oldY - 6 && !isExtended) extend()
        })
        Motion.pop(this, 350)
    }

    private companion object { const val ALL = "Todos" }
}
