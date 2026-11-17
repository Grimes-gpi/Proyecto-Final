package com.example.clase18_08zulema.ui.kit

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Íconos lineales dibujados en una cuadrícula de 24×24
 * drawables XML y el trazo se adapta a cualquier tamaño y color.
 */
enum class Glyph {
    BACK, SHARE, SEARCH, CALENDAR, CLOCK, USER, USERS, PLUS, MINUS, EDIT, CLOSE, CHECK, LOGOUT,
    SERVER, MORE, MAIL, LOCK, ID, IMAGE, ARROW_RIGHT, TAG, ALERT, TICKET, SPARKLE
}

class GlyphDrawable(
    private val glyph: Glyph,
    color: Int,
    private val sizePx: Int,
    strokeWidth: Float = 1.8f
) : Drawable() {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color }
    private val path = Path()

    override fun getIntrinsicWidth() = sizePx
    override fun getIntrinsicHeight() = sizePx

    override fun draw(canvas: Canvas) {
        val b = bounds
        val scale = minOf(b.width(), b.height()) / 24f
        canvas.save()
        canvas.translate(b.left + (b.width() - 24f * scale) / 2f, b.top + (b.height() - 24f * scale) / 2f)
        canvas.scale(scale, scale)
        drawGlyph(canvas)
        canvas.restore()
    }

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) = c.drawLine(x1, y1, x2, y2, stroke)

    private fun poly(c: Canvas, vararg p: Float) {
        path.reset()
        path.moveTo(p[0], p[1])
        var i = 2
        while (i < p.size) { path.lineTo(p[i], p[i + 1]); i += 2 }
        c.drawPath(path, stroke)
    }

    private fun rrect(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float) =
        c.drawRoundRect(RectF(l, t, r, b), radius, radius, stroke)

    private fun arc(c: Canvas, l: Float, t: Float, r: Float, b: Float, start: Float, sweep: Float) =
        c.drawArc(RectF(l, t, r, b), start, sweep, false, stroke)

    private fun drawGlyph(c: Canvas) {
        when (glyph) {
            Glyph.BACK -> { line(c, 19f, 12f, 5f, 12f); poly(c, 11f, 6f, 5f, 12f, 11f, 18f) }
            Glyph.ARROW_RIGHT -> { line(c, 5f, 12f, 19f, 12f); poly(c, 13f, 6f, 19f, 12f, 13f, 18f) }
            Glyph.SHARE -> {
                c.drawCircle(18f, 5.5f, 2.5f, stroke); c.drawCircle(6f, 12f, 2.5f, stroke); c.drawCircle(18f, 18.5f, 2.5f, stroke)
                line(c, 8.2f, 10.8f, 15.8f, 6.7f); line(c, 8.2f, 13.2f, 15.8f, 17.3f)
            }
            Glyph.SEARCH -> { c.drawCircle(11f, 11f, 6.5f, stroke); line(c, 16f, 16f, 20f, 20f) }
            Glyph.CALENDAR -> {
                rrect(c, 3.5f, 5f, 20.5f, 20.5f, 2.5f); line(c, 3.5f, 10f, 20.5f, 10f)
                line(c, 8f, 3f, 8f, 6.5f); line(c, 16f, 3f, 16f, 6.5f)
            }
            Glyph.CLOCK -> { c.drawCircle(12f, 12f, 8.5f, stroke); poly(c, 12f, 7.5f, 12f, 12f, 15f, 14f) }
            Glyph.USER -> { c.drawCircle(12f, 8f, 3.8f, stroke); arc(c, 5f, 14f, 19f, 28f, 180f, 180f) }
            Glyph.USERS -> {
                c.drawCircle(9f, 8.5f, 3.3f, stroke); arc(c, 3f, 14.5f, 15f, 26.5f, 180f, 180f)
                c.drawCircle(16.8f, 9.2f, 2.6f, stroke); arc(c, 14f, 15f, 22f, 23f, 200f, 145f)
            }
            Glyph.PLUS -> { line(c, 12f, 5f, 12f, 19f); line(c, 5f, 12f, 19f, 12f) }
            Glyph.MINUS -> line(c, 5f, 12f, 19f, 12f)
            Glyph.EDIT -> {
                path.reset(); path.moveTo(4.5f, 19.5f); path.lineTo(4.5f, 16f); path.lineTo(15f, 5.5f)
                path.lineTo(18.5f, 9f); path.lineTo(8f, 19.5f); path.close(); c.drawPath(path, stroke)
                line(c, 12.8f, 7.7f, 16.3f, 11.2f)
            }
            Glyph.CLOSE -> { line(c, 6.5f, 6.5f, 17.5f, 17.5f); line(c, 17.5f, 6.5f, 6.5f, 17.5f) }
            Glyph.CHECK -> poly(c, 5f, 12.5f, 10f, 17.5f, 19f, 7f)
            Glyph.LOGOUT -> {
                poly(c, 10f, 4f, 6f, 4f, 6f, 20f, 10f, 20f); line(c, 10.5f, 12f, 20f, 12f)
                poly(c, 16f, 8f, 20f, 12f, 16f, 16f)
            }
            Glyph.SERVER -> {
                rrect(c, 4f, 4f, 20f, 10.5f, 2f); rrect(c, 4f, 13.5f, 20f, 20f, 2f)
                c.drawCircle(7.8f, 7.25f, 1f, fill); c.drawCircle(7.8f, 16.75f, 1f, fill)
                line(c, 11.5f, 7.25f, 16f, 7.25f); line(c, 11.5f, 16.75f, 16f, 16.75f)
            }
            Glyph.MORE -> { c.drawCircle(12f, 5.5f, 1.6f, fill); c.drawCircle(12f, 12f, 1.6f, fill); c.drawCircle(12f, 18.5f, 1.6f, fill) }
            Glyph.MAIL -> { rrect(c, 3.5f, 5.5f, 20.5f, 18.5f, 2.5f); poly(c, 4.5f, 7.5f, 12f, 13f, 19.5f, 7.5f) }
            Glyph.LOCK -> {
                rrect(c, 5f, 10.5f, 19f, 20.5f, 2.5f)
                path.reset(); path.moveTo(8f, 10.5f); path.lineTo(8f, 8f)
                path.arcTo(RectF(8f, 4f, 16f, 12f), 180f, 180f); path.lineTo(16f, 10.5f); c.drawPath(path, stroke)
                line(c, 12f, 14.5f, 12f, 16.5f)
            }
            Glyph.ID -> {
                rrect(c, 3f, 5.5f, 21f, 18.5f, 2.5f); c.drawCircle(8.5f, 10.8f, 2f, stroke)
                line(c, 5.8f, 15.3f, 11.2f, 15.3f); line(c, 14f, 9.5f, 18f, 9.5f); line(c, 14f, 13f, 18f, 13f)
            }
            Glyph.IMAGE -> {
                rrect(c, 3.5f, 4.5f, 20.5f, 19.5f, 2.5f); c.drawCircle(9f, 9.5f, 1.6f, stroke)
                poly(c, 4f, 17f, 9.5f, 12f, 13f, 15f, 15.5f, 12.5f, 20f, 17f)
            }
            Glyph.TAG -> {
                path.reset(); path.moveTo(3.5f, 12f); path.lineTo(12f, 3.5f); path.lineTo(20.5f, 3.5f)
                path.lineTo(20.5f, 12f); path.lineTo(12f, 20.5f); path.close(); c.drawPath(path, stroke)
                c.drawCircle(16f, 8f, 1.3f, fill)
            }
            Glyph.ALERT -> { c.drawCircle(12f, 12f, 8.5f, stroke); line(c, 12f, 7.5f, 12f, 12.8f); c.drawCircle(12f, 16.2f, 1.1f, fill) }
            Glyph.TICKET -> {
                path.reset(); path.moveTo(3.5f, 7f); path.lineTo(20.5f, 7f); path.lineTo(20.5f, 10f)
                path.arcTo(RectF(18.5f, 10f, 22.5f, 14f), 270f, -180f); path.lineTo(20.5f, 17f); path.lineTo(3.5f, 17f)
                path.lineTo(3.5f, 14f); path.arcTo(RectF(1.5f, 10f, 5.5f, 14f), 90f, -180f); path.close()
                c.drawPath(path, stroke); line(c, 14f, 9f, 14f, 15f)
            }
            Glyph.SPARKLE -> {
                path.reset(); path.moveTo(12f, 3f); path.quadTo(13f, 11f, 21f, 12f); path.quadTo(13f, 13f, 12f, 21f)
                path.quadTo(11f, 13f, 3f, 12f); path.quadTo(11f, 11f, 12f, 3f); path.close(); c.drawPath(path, stroke)
            }
        }
    }

    override fun setAlpha(alpha: Int) { stroke.alpha = alpha; fill.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { stroke.colorFilter = colorFilter; fill.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

fun Context.glyph(glyph: Glyph, color: Int, sizeDp: Int = 20, strokeWidth: Float = 1.8f): GlyphDrawable =
    GlyphDrawable(glyph, color, dp(sizeDp), strokeWidth).apply { setBounds(0, 0, dp(sizeDp), dp(sizeDp)) }
