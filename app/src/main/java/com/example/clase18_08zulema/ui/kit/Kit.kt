package com.example.clase18_08zulema.ui.kit

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

// ---------- Medidas y contenedores ----------

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
fun Context.dpf(value: Int): Float = value * resources.displayMetrics.density

fun rounded(color: Int, radius: Float, strokeWidth: Int = 0, strokeColor: Int = Color.TRANSPARENT) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

fun Context.vertical(gravity: Int = Gravity.NO_GRAVITY): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    this.gravity = gravity
}

fun Context.horizontal(gravity: Int = Gravity.CENTER_VERTICAL): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    this.gravity = gravity
}

fun Context.space(heightDp: Int): View = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
}

fun matchWrap(topDp: Int = 0, context: Context? = null) = LinearLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
).apply { if (context != null) topMargin = context.dp(topDp) }

fun weighted(weight: Float = 1f) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)

/** Aplica las barras del sistema (y el teclado) como padding: necesario con edge-to-edge. */
fun View.applySystemInsets(
    top: Boolean = false,
    bottom: Boolean = false,
    extraTopDp: Int = 0,
    extraBottomDp: Int = 0,
    includeIme: Boolean = false
) {
    val start = paddingStart
    val end = paddingEnd
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = if (includeIme) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        view.setPaddingRelative(
            start,
            (if (top) bars.top else 0) + context.dp(extraTopDp),
            end,
            (if (bottom) maxOf(bars.bottom, ime) else 0) + context.dp(extraBottomDp)
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

// ---------- Botones ----------

enum class ButtonTone { PRIMARY, TONAL, OUTLINED, GHOST, DANGER, DANGER_TONAL, ON_DARK }

object Buttons {
    fun create(
        context: Context,
        label: String,
        tone: ButtonTone = ButtonTone.PRIMARY,
        icon: Glyph? = null,
        heightDp: Int = 54,
        onClick: (() -> Unit)? = null
    ): MaterialButton {
        val c = context.colors
        val (bg, fg, stroke) = when (tone) {
            ButtonTone.PRIMARY -> Triple(c.primary, c.onPrimary, Color.TRANSPARENT)
            ButtonTone.TONAL -> Triple(c.primaryContainer, c.onPrimaryContainer, Color.TRANSPARENT)
            ButtonTone.OUTLINED -> Triple(Color.TRANSPARENT, c.ink, c.outlineStrong)
            ButtonTone.GHOST -> Triple(Color.TRANSPARENT, c.primary, Color.TRANSPARENT)
            ButtonTone.DANGER -> Triple(c.danger, Color.WHITE, Color.TRANSPARENT)
            ButtonTone.DANGER_TONAL -> Triple(c.dangerContainer, c.danger, Color.TRANSPARENT)
            ButtonTone.ON_DARK -> Triple(Color.WHITE, c.heroEnd, Color.TRANSPARENT)
        }
        return MaterialButton(context).apply {
            text = label
            isAllCaps = false
            style(TextRole.LABEL, fg)
            setTextColor(ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(ColorUtils.setAlphaComponent(fg, 110), fg)
            ))
            backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(if (bg == Color.TRANSPARENT) bg else ColorUtils.setAlphaComponent(bg, 90), bg)
            )
            rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(if (bg == Color.TRANSPARENT || tone == ButtonTone.ON_DARK) c.primary else Color.WHITE, 48))
            strokeColor = ColorStateList.valueOf(stroke)
            strokeWidth = if (stroke == Color.TRANSPARENT) 0 else context.dp(1)
            cornerRadius = context.dp(heightDp / 2)
            insetTop = 0
            insetBottom = 0
            minHeight = context.dp(heightDp)
            minimumHeight = context.dp(heightDp)
            elevation = 0f
            stateListAnimator = null
            setPadding(context.dp(20), 0, context.dp(20), 0)
            icon?.let {
                this.icon = context.glyph(it, fg, 18, 2f)
                iconPadding = context.dp(8)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            }
            Motion.pressable(this, 0.97f)
            onClick?.let { action -> setOnClickListener { if (!isLoading()) action() } }
        }
    }

    private class LoadingState(val label: CharSequence, val icon: Drawable?, val iconGravity: Int)

    /** Muestra un indicador circular dentro del botón y bloquea toques repetidos. */
    fun setLoading(button: MaterialButton, loading: Boolean, loadingLabel: String? = null) {
        if (loading == button.isLoading()) return
        if (loading) {
            button.tag = LoadingState(button.text, button.icon, button.iconGravity)
            val color = button.currentTextColor
            val spec = CircularProgressIndicatorSpec(button.context, null).apply {
                indicatorColors = intArrayOf(color)
                indicatorSize = button.context.dp(18)
                trackThickness = button.context.dp(2)
                trackColor = Color.TRANSPARENT
            }
            button.icon = IndeterminateDrawable.createCircularDrawable(button.context, spec)
            button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            loadingLabel?.let { button.text = it }
        } else {
            val state = button.tag as LoadingState
            button.text = state.label
            button.icon = state.icon
            button.iconGravity = state.iconGravity
            button.tag = null
        }
    }

    private fun MaterialButton.isLoading() = tag is LoadingState
}

/** Botón circular solo con ícono (barra superior, acciones de fila). */
fun Context.iconButton(
    glyph: Glyph,
    description: String,
    tint: Int = colors.ink,
    fill: Int = colors.surface,
    sizeDp: Int = 44,
    strokeColor: Int = colors.outline,
    onClick: () -> Unit
): FrameLayout = FrameLayout(this).apply {
    contentDescription = description
    isClickable = true
    isFocusable = true
    background = RippleDrawable(
        ColorStateList.valueOf(ColorUtils.setAlphaComponent(colors.primary, 40)),
        rounded(fill, dpf(sizeDp / 2), if (strokeColor == Color.TRANSPARENT) 0 else dp(1), strokeColor),
        null
    )
    addView(ImageView(context).apply { setImageDrawable(context.glyph(glyph, tint, 20)) },
        FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
    layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    Motion.pressable(this, 0.92f)
    setOnClickListener { onClick() }
}

// ---------- Campos de texto ----------

class Field(val layout: TextInputLayout, val edit: TextInputEditText) {
    val value: String get() = edit.text?.toString()?.trim().orEmpty()
    fun error(message: String?) { layout.error = message; if (message != null) Motion.shake(layout) }
}

object Fields {
    fun create(
        context: Context,
        label: String,
        icon: Glyph? = null,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        helper: String? = null,
        maxLength: Int? = null
    ): Field {
        val c = context.colors
        val radius = context.dpf(16)
        val layout = TextInputLayout(context).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(radius, radius, radius, radius)
            // En modo oscuro la casilla necesita un relleno distinto al de la tarjeta para distinguirse.
            setBoxBackgroundColor(if (c.isDark) c.surfaceVariant else c.surface)
            boxStrokeWidth = context.dp(1)
            boxStrokeWidthFocused = context.dp(2)
            setBoxStrokeColorStateList(ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(c.primary, c.outlineStrong)
            ))
            hintTextColor = ColorStateList.valueOf(c.primary)
            defaultHintTextColor = ColorStateList.valueOf(c.inkMuted)
            icon?.let {
                setStartIconDrawable(context.glyph(it, c.inkMuted, 20))
                setStartIconTintList(ColorStateList.valueOf(c.inkMuted))
            }
            helper?.let { helperText = it }
            maxLength?.let { isCounterEnabled = true; counterMaxLength = it }
            setErrorIconDrawable(null)
        }
        val edit = TextInputEditText(layout.context).apply {
            this.inputType = inputType
            setTextColor(c.ink)
            textSize = 15.5f
            if (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0) setSingleLine(true)
            maxLength?.let { filters = arrayOf<InputFilter>(InputFilter.LengthFilter(it)) }
        }
        layout.addView(edit)
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (layout.error != null) layout.error = null
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        return Field(layout, edit)
    }
}

// ---------- Superficies ----------

fun Context.card(radiusDp: Int = 24, fill: Int = colors.surface, stroke: Boolean = true): MaterialCardView =
    MaterialCardView(this).apply {
        radius = dpf(radiusDp)
        cardElevation = 0f
        setCardBackgroundColor(fill)
        strokeColor = colors.outline
        strokeWidth = if (stroke) dp(1) else 0
        rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colors.primary, 28))
    }

/** Etiqueta compacta de estado o categoría. */
fun Context.pill(label: String, foreground: Int, fill: Int, icon: Glyph? = null): TextView =
    text(label, TextRole.CAPTION, foreground).apply {
        setPadding(dp(10), dp(5), dp(10), dp(5))
        background = rounded(fill, dpf(20))
        gravity = Gravity.CENTER_VERTICAL
        icon?.let {
            setCompoundDrawablesRelative(context.glyph(it, foreground, 14, 2f), null, null, null)
            compoundDrawablePadding = dp(5)
        }
    }

/** Avatar circular con iniciales (no requiere imagen del usuario). */
fun Context.avatar(name: String, sizeDp: Int = 44, fill: Int = colors.primaryContainer, foreground: Int = colors.onPrimaryContainer): TextView =
    TextView(this).apply {
        text = initials(name)
        style(if (sizeDp >= 64) TextRole.HEADLINE else TextRole.LABEL, foreground)
        gravity = Gravity.CENTER
        background = rounded(fill, dpf(sizeDp / 2))
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Fila de información: ícono en cápsula + etiqueta + valor. */
fun Context.infoRow(glyph: Glyph, label: String, value: String, tint: Int = colors.primary, container: Int = colors.primaryContainer): LinearLayout =
    horizontal().apply {
        addView(FrameLayout(context).apply {
            background = rounded(container, dpf(14))
            addView(ImageView(context).apply { setImageDrawable(context.glyph(glyph, tint, 20)) },
                FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(context.vertical().apply {
            addView(context.text(label, TextRole.CAPTION, context.colors.inkSubtle))
            addView(context.text(value, TextRole.TITLE, context.colors.ink), matchWrap(3, context))
        }, weighted().apply { marginStart = dp(14) })
    }

fun Context.sectionHeader(title: String, trailing: View? = null): LinearLayout = horizontal().apply {
    addView(context.text(title, TextRole.TITLE_LARGE), weighted())
    trailing?.let(::addView)
}

fun Context.divider(): View = View(this).apply {
    setBackgroundColor(colors.outline)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
}

/** Estado vacío o de error con ícono, mensaje y acciones. */
fun Context.stateView(
    glyph: Glyph,
    title: String,
    body: String,
    tone: Int = colors.primary,
    toneContainer: Int = colors.primaryContainer,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null
): LinearLayout = vertical(Gravity.CENTER_HORIZONTAL).apply {
    setPadding(dp(24), dp(32), dp(24), dp(28))
    background = rounded(colors.surface, dpf(28), dp(1), colors.outline)
    val badge = FrameLayout(context).apply {
        background = rounded(toneContainer, dpf(36))
        addView(ImageView(context).apply { setImageDrawable(context.glyph(glyph, tone, 32, 1.7f)) },
            FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
    }
    addView(badge, LinearLayout.LayoutParams(dp(72), dp(72)))
    addView(context.text(title, TextRole.TITLE_LARGE).apply { gravity = Gravity.CENTER }, matchWrap(18, context))
    addView(context.text(body, TextRole.BODY_SMALL, context.colors.inkMuted).apply { gravity = Gravity.CENTER }, matchWrap(8, context))
    primary?.let { (label, action) ->
        addView(Buttons.create(context, label, ButtonTone.PRIMARY, heightDp = 48, onClick = action), matchWrap(22, context))
    }
    secondary?.let { (label, action) ->
        addView(Buttons.create(context, label, ButtonTone.GHOST, heightDp = 44, onClick = action), matchWrap(4, context))
    }
    Motion.pop(badge, 80)
}

// ---------- Feedback ----------

enum class Tone { INFO, SUCCESS, ERROR }

fun Activity.snack(message: String, tone: Tone = Tone.INFO, anchor: View? = null) {
    val root = findViewById<View>(android.R.id.content) ?: return
    val c = colors
    Snackbar.make(root, message, Snackbar.LENGTH_LONG).apply {
        setBackgroundTint(when (tone) { Tone.INFO -> if (c.isDark) c.surfaceVariant else c.ink; Tone.SUCCESS -> c.success; Tone.ERROR -> c.danger })
        setTextColor(if (tone == Tone.INFO && c.isDark) c.ink else Color.WHITE)
        setTextMaxLines(4)
        anchor?.let { anchorView = it }
        show()
    }
}
