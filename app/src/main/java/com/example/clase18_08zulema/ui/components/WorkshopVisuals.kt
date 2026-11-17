package com.example.clase18_08zulema.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.clase18_08zulema.data.Workshop
import com.example.clase18_08zulema.ui.kit.CategoryStyle
import com.example.clase18_08zulema.ui.kit.Glyph
import com.example.clase18_08zulema.ui.kit.Motion
import com.example.clase18_08zulema.ui.kit.TextRole
import com.example.clase18_08zulema.ui.kit.card
import com.example.clase18_08zulema.ui.kit.colors
import com.example.clase18_08zulema.ui.kit.dp
import com.example.clase18_08zulema.ui.kit.dpf
import com.example.clase18_08zulema.ui.kit.glyph
import com.example.clase18_08zulema.ui.kit.horizontal
import com.example.clase18_08zulema.ui.kit.pill
import com.example.clase18_08zulema.ui.kit.rounded
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Nombre compartido entre la tarjeta y el detalle para la transición "container transform". */
fun transitionNameFor(workshopId: Long) = "workshop_$workshopId"

/** Estado de disponibilidad compartido por la tarjeta y el detalle. */
data class Availability(val label: String, val foreground: Int, val background: Int)

fun Context.availability(workshop: Workshop, enrolled: Boolean = false): Availability {
    val c = colors
    return when {
        !workshop.isActive -> Availability("Cancelado", c.danger, c.dangerContainer)
        enrolled -> Availability("Inscrito", c.success, c.successContainer)
        workshop.availableSlots == 0L -> Availability("Sin cupos", c.danger, c.dangerContainer)
        workshop.availableSlots <= 3 -> Availability("Últimos ${workshop.availableSlots}", c.warning, c.warningContainer)
        else -> Availability(Formats.slots(workshop.availableSlots), c.success, c.successContainer)
    }
}

/** Portada: imagen HTTPS con fundido o, si no hay, un degradado por categoría con su inicial. */
fun Context.workshopCover(workshop: Workshop, heightPx: Int, cornerRadiusPx: Float = 0f): FrameLayout =
    FrameLayout(this).apply {
        val style = CategoryStyle.of(workshop.category)
        val placeholder = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(style.start, style.end))
            .apply { cornerRadius = cornerRadiusPx }
        background = placeholder
        val imageUrl = workshop.imageUrl?.takeIf { it.startsWith("https://", ignoreCase = true) }
        if (imageUrl != null) {
            val image = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Imagen del taller ${workshop.name}"
            }
            addView(image, FrameLayout.LayoutParams(-1, -1))
            Glide.with(context).load(imageUrl).transition(DrawableTransitionOptions.withCrossFade(260)).into(image)
        } else {
            addView(context.text((workshop.category ?: "T").take(1).uppercase(), TextRole.DISPLAY, Color.WHITE).apply {
                textSize = 120f
                alpha = 0.16f
            }, FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.BOTTOM).apply {
                marginEnd = dp(12); bottomMargin = -dp(18)
            })
        }
        // Velo inferior para que el texto sobre la imagen siempre sea legible.
        addView(View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00000000, 0x1A000000, 0x8C000000.toInt()))
        }, FrameLayout.LayoutParams(-1, -1))
        layoutParams = LinearLayout.LayoutParams(-1, heightPx)
    }

/** Bloque de fecha estilo calendario (día grande + mes). */
fun Context.dateTile(dateTime: String): LinearLayout = vertical(Gravity.CENTER).apply {
    setPadding(dp(10), dp(6), dp(10), dp(7))
    background = rounded(Color.WHITE, dpf(14))
    elevation = dpf(2)
    addView(context.text(Formats.month(dateTime), TextRole.OVERLINE, CategoryStyle.of(null).end).apply { gravity = Gravity.CENTER })
    addView(context.text(Formats.day(dateTime), TextRole.NUMBER, 0xFF0E1A22.toInt()).apply { gravity = Gravity.CENTER; textSize = 22f })
}

fun Context.capacityBar(workshop: Workshop): LinearProgressIndicator = LinearProgressIndicator(this).apply {
    val c = context.colors
    max = 100
    trackThickness = dp(6)
    trackCornerRadius = dp(3)
    trackColor = c.surfaceVariant
    setIndicatorColor(when {
        !workshop.isActive -> c.inkSubtle
        workshop.fillRatio >= 1f -> c.danger
        workshop.fillRatio >= 0.8f -> c.warning
        else -> c.primary
    })
    isIndeterminate = false
}

/** Tarjeta de la lista principal. */
fun Context.workshopCard(workshop: Workshop, enrolled: Boolean, onClick: (View) -> Unit): View {
    val c = colors
    val card = card(radiusDp = 26)
    card.transitionName = transitionNameFor(workshop.id)
    card.isClickable = true
    card.isFocusable = true
    card.setOnClickListener { onClick(it) }
    Motion.pressable(card, 0.98f)

    val column = vertical()
    val cover = workshopCover(workshop, dp(172))
    cover.addView(dateTile(workshop.dateTime), FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
        leftMargin = dp(14); topMargin = dp(14)
    })
    val state = availability(workshop, enrolled)
    cover.addView(pill(state.label, state.foreground, state.background, if (enrolled) Glyph.CHECK else null),
        FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { rightMargin = dp(14); topMargin = dp(16) })
    cover.addView(pill(workshop.category ?: "Comunidad", Color.WHITE, 0x33FFFFFF, Glyph.TAG),
        FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply { leftMargin = dp(14); bottomMargin = dp(14) })
    column.addView(cover)

    val body = vertical().apply { setPadding(dp(18), dp(16), dp(18), dp(18)) }
    body.addView(text(workshop.name, TextRole.TITLE_LARGE).apply { maxLines = 2 })
    body.addView(horizontal().apply {
        addView(android.widget.ImageView(context).apply { setImageDrawable(context.glyph(Glyph.USER, c.inkSubtle, 16)) },
            LinearLayout.LayoutParams(dp(16), dp(16)))
        addView(context.text(workshop.instructor, TextRole.BODY_SMALL, c.inkMuted).apply { maxLines = 1 },
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        addView(android.widget.ImageView(context).apply { setImageDrawable(context.glyph(Glyph.CLOCK, c.inkSubtle, 16)) },
            LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginStart = dp(10) })
        addView(context.text("${Formats.weekday(workshop.dateTime)} · ${Formats.time(workshop.dateTime)}", TextRole.BODY_SMALL, c.inkMuted),
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
    }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

    body.addView(horizontal().apply {
        addView(context.text("${workshop.registeredPeople} de ${workshop.maxCapacity} inscritos", TextRole.CAPTION, c.inkMuted),
            LinearLayout.LayoutParams(0, -2, 1f))
        addView(context.text("${(workshop.fillRatio * 100).toInt()}%", TextRole.CAPTION, c.ink))
    }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
    val bar = capacityBar(workshop)
    body.addView(bar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    // La barra se llena con animación cuando la tarjeta aparece.
    bar.post { bar.setProgressCompat((workshop.fillRatio * 100).toInt(), true) }

    column.addView(body)
    card.addView(column)
    return card
}
