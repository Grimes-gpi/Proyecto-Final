package com.example.clase18_08zulema.ui.kit

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView

/**
 * Escala tipográfica. Usa las familias del sistema (Roboto / Google Sans según el equipo),
 * con interlineado y tracking ajustados para que la jerarquía sea clara.
 */
enum class TextRole(
    val sizeSp: Float,
    val family: String,
    val style: Int,
    val tracking: Float,
    val lineMultiplier: Float,
    val allCaps: Boolean = false
) {
    DISPLAY(30f, "sans-serif", Typeface.BOLD, -0.025f, 1.08f),
    HEADLINE(24f, "sans-serif", Typeface.BOLD, -0.015f, 1.12f),
    TITLE_LARGE(19f, "sans-serif-medium", Typeface.NORMAL, -0.005f, 1.18f),
    TITLE(17f, "sans-serif-medium", Typeface.NORMAL, 0f, 1.2f),
    BODY(15f, "sans-serif", Typeface.NORMAL, 0f, 1.42f),
    BODY_SMALL(13.5f, "sans-serif", Typeface.NORMAL, 0.005f, 1.35f),
    LABEL(14f, "sans-serif-medium", Typeface.NORMAL, 0.01f, 1.1f),
    CAPTION(12f, "sans-serif-medium", Typeface.NORMAL, 0.01f, 1.2f),
    OVERLINE(11f, "sans-serif-medium", Typeface.NORMAL, 0.12f, 1.1f, allCaps = true),
    NUMBER(26f, "sans-serif", Typeface.BOLD, -0.02f, 1f)
}

fun TextView.style(role: TextRole, color: Int? = null): TextView = apply {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, role.sizeSp)
    typeface = Typeface.create(role.family, role.style)
    letterSpacing = role.tracking
    setLineSpacing(0f, role.lineMultiplier)
    isAllCaps = role.allCaps
    includeFontPadding = false
    color?.let(::setTextColor)
}

fun Context.text(value: CharSequence, role: TextRole, color: Int = colors.ink): TextView =
    TextView(this).apply {
        text = value
        style(role, color)
    }
