package com.example.clase18_08zulema.ui.kit

import android.content.Context
import android.content.res.Configuration

/** Tokens de color de la app. Toda la UI los lee de aquí para que claro/oscuro sea consistente. */
data class Colors(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val accent: Int,
    val accentContainer: Int,
    val success: Int,
    val successContainer: Int,
    val warning: Int,
    val warningContainer: Int,
    val danger: Int,
    val dangerContainer: Int,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val outline: Int,
    val outlineStrong: Int,
    val ink: Int,
    val inkMuted: Int,
    val inkSubtle: Int,
    val shimmerBase: Int,
    val shimmerHighlight: Int,
    val heroStart: Int,
    val heroEnd: Int,
    val isDark: Boolean
)

object Palette {
    private val light = Colors(
        primary = 0xFF0A6C8F.toInt(),
        onPrimary = 0xFFFFFFFF.toInt(),
        primaryContainer = 0xFFD9F0F8.toInt(),
        onPrimaryContainer = 0xFF00384C.toInt(),
        accent = 0xFFE8603F.toInt(),
        accentContainer = 0xFFFFE6DE.toInt(),
        success = 0xFF167A4F.toInt(),
        successContainer = 0xFFDDF3E6.toInt(),
        warning = 0xFFA4660E.toInt(),
        warningContainer = 0xFFFCEFD5.toInt(),
        danger = 0xFFB93A26.toInt(),
        dangerContainer = 0xFFFBE4DF.toInt(),
        background = 0xFFF4F6F8.toInt(),
        surface = 0xFFFFFFFF.toInt(),
        surfaceVariant = 0xFFECF0F3.toInt(),
        outline = 0xFFE2E7EB.toInt(),
        outlineStrong = 0xFFC7D0D7.toInt(),
        ink = 0xFF0E1A22.toInt(),
        inkMuted = 0xFF56656F.toInt(),
        inkSubtle = 0xFF8794A0.toInt(),
        shimmerBase = 0xFFE7ECF0.toInt(),
        shimmerHighlight = 0x99FFFFFF.toInt(),
        heroStart = 0xFF0B7FA6.toInt(),
        heroEnd = 0xFF09506C.toInt(),
        isDark = false
    )

    private val dark = Colors(
        primary = 0xFF6CCDEE.toInt(),
        onPrimary = 0xFF00293A.toInt(),
        primaryContainer = 0xFF0F3D50.toInt(),
        onPrimaryContainer = 0xFFCDEFFA.toInt(),
        accent = 0xFFFF8E72.toInt(),
        accentContainer = 0xFF4A2419.toInt(),
        success = 0xFF6AD6A1.toInt(),
        successContainer = 0xFF12392A.toInt(),
        warning = 0xFFEDBB62.toInt(),
        warningContainer = 0xFF3B2C12.toInt(),
        danger = 0xFFFF907C.toInt(),
        dangerContainer = 0xFF4A1E17.toInt(),
        background = 0xFF0A1015.toInt(),
        surface = 0xFF121A20.toInt(),
        surfaceVariant = 0xFF1A242B.toInt(),
        outline = 0xFF222E36.toInt(),
        outlineStrong = 0xFF52687A.toInt(),
        ink = 0xFFE6EDF1.toInt(),
        inkMuted = 0xFF9EB0BB.toInt(),
        inkSubtle = 0xFF6D808C.toInt(),
        shimmerBase = 0xFF1A242B.toInt(),
        shimmerHighlight = 0x1FFFFFFF,
        heroStart = 0xFF0E5A75.toInt(),
        heroEnd = 0xFF07303F.toInt(),
        isDark = true
    )

    fun of(context: Context): Colors {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (night == Configuration.UI_MODE_NIGHT_YES) dark else light
    }
}

val Context.colors: Colors get() = Palette.of(this)

/** Degradado e inicial para las portadas de talleres que no tienen imagen. */
object CategoryStyle {
    data class Style(val start: Int, val end: Int)

    fun of(category: String?): Style {
        val value = category.orEmpty().lowercase()
        return when {
            "tecno" in value -> Style(0xFF3757D6.toInt(), 0xFF5B3CC4.toInt())
            "cocin" in value -> Style(0xFFE8603F.toInt(), 0xFFC2364F.toInt())
            "ambiente" in value || "huerta" in value -> Style(0xFF2E9461.toInt(), 0xFF176B5B.toInt())
            "arte" in value || "cultura" in value -> Style(0xFFC13A86.toInt(), 0xFF7A35B5.toInt())
            "emprend" in value -> Style(0xFFD48A14.toInt(), 0xFFB5531B.toInt())
            "salud" in value || "bienestar" in value -> Style(0xFF149A9A.toInt(), 0xFF0B6E8F.toInt())
            "idioma" in value -> Style(0xFF4C5BD4.toInt(), 0xFF2A7DC1.toInt())
            "educa" in value -> Style(0xFF2B86C9.toInt(), 0xFF1D5FA0.toInt())
            "oficio" in value -> Style(0xFF8A5A3B.toInt(), 0xFF5E3D2B.toInt())
            else -> Style(0xFF0B7FA6.toInt(), 0xFF09506C.toInt())
        }
    }
}
