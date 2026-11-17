package com.example.clase18_08zulema.ui.kit

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Selector de opciones con una "píldora" que se desliza hasta la opción elegida.
 * Reemplaza a un TabLayout/ToggleGroup genérico con un acabado más propio.
 */
@SuppressLint("ViewConstructor")
class SegmentedControl(
    context: Context,
    private val options: List<String>,
    initial: Int = 0,
    private val onSelected: (Int) -> Unit
) : FrameLayout(context) {
    private companion object { const val SEGMENT_HEIGHT_DP = 42 }

    private val palette = context.colors
    private val thumb = View(context)
    private val labels = mutableListOf<TextView>()
    var selected: Int = initial
        private set
    private var thumbAnimating = false

    init {
        val pad = context.dp(4)
        setPadding(pad, pad, pad, pad)
        background = rounded(palette.surfaceVariant, context.dpf(26))
        thumb.background = rounded(palette.surface, context.dpf(22), context.dp(1), palette.outline)
        thumb.elevation = context.dpf(1)
        // Altura fija: con MATCH_PARENT, dentro de un ScrollView la píldora ocupaba toda la pantalla.
        addView(thumb, LayoutParams(0, context.dp(SEGMENT_HEIGHT_DP)))

        val row = context.horizontal()
        options.forEachIndexed { index, option ->
            val label = context.text(option, TextRole.LABEL, colorFor(index == selected)).apply {
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener { select(index) }
            }
            labels += label
            row.addView(label, LinearLayout.LayoutParams(0, context.dp(SEGMENT_HEIGHT_DP), 1f))
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        // La píldora debe dibujarse por encima del fondo pero detrás del texto.
        row.elevation = context.dpf(2)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val inner = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val segment = inner / options.size.coerceAtLeast(1)
        (thumb.layoutParams as LayoutParams).width = segment
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!thumbAnimating) thumb.translationX = (segment * selected).toFloat()
    }

    fun select(index: Int, notify: Boolean = true) {
        if (index == selected || index !in options.indices) return
        val previous = selected
        selected = index
        val target = (thumb.width * index).toFloat()
        thumbAnimating = true
        thumb.animate().translationX(target).setDuration(Motion.MEDIUM).setInterpolator(Motion.emphasized)
            .withEndAction { thumbAnimating = false }.start()
        animateColor(labels[previous], colorFor(true), colorFor(false))
        animateColor(labels[index], colorFor(false), colorFor(true))
        if (notify) onSelected(index)
    }

    private fun colorFor(active: Boolean) = if (active) palette.ink else palette.inkMuted

    private fun animateColor(view: TextView, from: Int, to: Int) {
        ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
            duration = Motion.MEDIUM
            addUpdateListener { view.setTextColor(it.animatedValue as Int) }
            start()
        }
    }
}
