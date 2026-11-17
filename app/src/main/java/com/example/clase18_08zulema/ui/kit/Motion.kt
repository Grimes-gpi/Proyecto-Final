package com.example.clase18_08zulema.ui.kit

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.StateListAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

/** Curvas y duraciones de Material 3 Motion, más utilidades de animación reutilizables. */
object Motion {
    val emphasized = PathInterpolator(0.2f, 0f, 0f, 1f)
    val emphasizedDecelerate = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAccelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    const val SHORT = 160L
    const val MEDIUM = 300L
    const val LONG = 450L

    /** Entrada: aparece desde abajo con desvanecido. */
    fun enter(view: View, delay: Long = 0L, distanceDp: Int = 18) {
        view.alpha = 0f
        view.translationY = view.context.dp(distanceDp).toFloat()
        view.animate().alpha(1f).translationY(0f)
            .setStartDelay(delay).setDuration(LONG).setInterpolator(emphasizedDecelerate).start()
    }

    /** Escalona la entrada de varias vistas (listas, secciones). */
    fun stagger(views: List<View>, step: Long = 50L, maxDelay: Long = 320L, initialDelay: Long = 0L) {
        views.forEachIndexed { index, view -> enter(view, (initialDelay + index * step).coerceAtMost(maxDelay)) }
    }

    /** Cambia entre estados (carga / contenido / error) sin saltos. */
    fun crossfade(show: View, vararg hide: View) {
        hide.filter { it.visibility == View.VISIBLE && it !== show }.forEach { view ->
            view.animate().alpha(0f).setDuration(SHORT).setInterpolator(emphasizedAccelerate)
                .withEndAction { view.visibility = View.GONE; view.alpha = 1f }.start()
        }
        if (show.visibility != View.VISIBLE) {
            show.alpha = 0f
            show.visibility = View.VISIBLE
        }
        show.animate().alpha(1f).setDuration(MEDIUM).setInterpolator(emphasizedDecelerate).start()
    }

    /** Escala sutil al presionar: da sensación táctil sin depender de la elevación. */
    fun pressable(view: View, pressedScale: Float = 0.97f) {
        fun animator(scale: Float) = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, scale),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, scale)
        ).apply { duration = SHORT; interpolator = emphasized }
        view.stateListAnimator = StateListAnimator().apply {
            addState(intArrayOf(android.R.attr.state_pressed), animator(pressedScale))
            addState(intArrayOf(), animator(1f))
        }
    }

    /** Sacudida horizontal para señalar un error de validación. */
    fun shake(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 14f, -12f, 9f, -6f, 3f, 0f).apply {
            duration = 420
            start()
        }
    }

    /** "Pop" de confirmación (íconos de éxito, insignias). */
    fun pop(view: View, delay: Long = 0L) {
        view.scaleX = 0.4f
        view.scaleY = 0.4f
        view.alpha = 0f
        view.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay)
            .setDuration(LONG).setInterpolator(OvershootInterpolator(2.2f)).start()
    }

    /** Anima automáticamente las vistas que aparecen, desaparecen o cambian de tamaño en un contenedor. */
    fun smoothLayout(group: ViewGroup) {
        group.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setDuration(MEDIUM)
            setInterpolator(LayoutTransition.CHANGING, emphasized)
            setInterpolator(LayoutTransition.CHANGE_APPEARING, emphasized)
            setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, emphasized)
        }
    }
}
