package com.example.clase18_08zulema.ui.kit

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Contenedor que hace pasar un brillo diagonal sobre sus hijos (los "huesos" del esqueleto).
 * Dibuja en una capa propia y usa SRC_ATOP, así el brillo solo aparece sobre los bloques pintados.
 */
class ShimmerLayout(context: Context) : FrameLayout(context) {
    private val palette = context.colors
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
    private val shaderMatrix = Matrix()
    private var shader: LinearGradient? = null
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1350
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { progress = it.animatedValue as Float; invalidate() }
    }

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val band = w * 0.45f
        shader = LinearGradient(
            0f, 0f, band, 0f,
            intArrayOf(Color.TRANSPARENT, palette.shimmerHighlight, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val gradient = shader ?: return
        val band = width * 0.45f
        val x = -band + (width + band * 2) * progress
        shaderMatrix.setTranslate(x, 0f)
        shaderMatrix.postSkew(-0.25f, 0f)
        gradient.setLocalMatrix(shaderMatrix)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!isAttachedToWindow) return
        if (visibility == VISIBLE && isShown) { if (!animator.isStarted) animator.start() } else animator.cancel()
    }
}

/** Esqueletos que imitan la forma real del contenido mientras carga. */
object Skeleton {
    fun bone(context: Context, widthPx: Int, heightDp: Int, radiusDp: Int = 8): View = View(context).apply {
        background = rounded(context.colors.shimmerBase, context.dpf(radiusDp))
        layoutParams = LinearLayout.LayoutParams(widthPx, context.dp(heightDp))
    }

    private fun boneLp(context: Context, widthPx: Int, heightDp: Int, topDp: Int, radiusDp: Int = 8) =
        bone(context, widthPx, heightDp, radiusDp).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, context.dp(heightDp)).apply { topMargin = context.dp(topDp) }
        }

    fun workshopList(context: Context, count: Int = 3): ShimmerLayout = ShimmerLayout(context).apply {
        val column = context.vertical()
        repeat(count) { index ->
            column.addView(context.vertical().apply {
                addView(bone(context, LinearLayout.LayoutParams.MATCH_PARENT, 176, 24))
                addView(boneLp(context, context.dp(220), 18, 16))
                addView(boneLp(context, context.dp(160), 12, 10))
                addView(boneLp(context, LinearLayout.LayoutParams.MATCH_PARENT, 8, 18, 4))
            }, LinearLayout.LayoutParams(-1, -2).apply { if (index > 0) topMargin = context.dp(28) })
        }
        addView(column)
    }

    fun detail(context: Context): ShimmerLayout = ShimmerLayout(context).apply {
        val column = context.vertical()
        column.addView(boneLp(context, context.dp(110), 22, 0, 11))
        column.addView(boneLp(context, LinearLayout.LayoutParams.MATCH_PARENT, 28, 16))
        column.addView(boneLp(context, context.dp(200), 28, 8))
        column.addView(boneLp(context, LinearLayout.LayoutParams.MATCH_PARENT, 150, 24, 20))
        repeat(4) { column.addView(boneLp(context, LinearLayout.LayoutParams.MATCH_PARENT, 12, if (it == 0) 26 else 10)) }
        addView(column)
    }

    fun rows(context: Context, count: Int = 3): ShimmerLayout = ShimmerLayout(context).apply {
        val column = context.vertical()
        repeat(count) { index ->
            column.addView(context.horizontal().apply {
                addView(bone(context, context.dp(40), 40, 20))
                addView(context.vertical().apply {
                    addView(bone(context, context.dp(150), 14))
                    addView(boneLp(context, context.dp(100), 10, 8))
                }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = context.dp(14) })
            }, LinearLayout.LayoutParams(-1, -2).apply { if (index > 0) topMargin = context.dp(18) })
        }
        addView(column)
    }
}
