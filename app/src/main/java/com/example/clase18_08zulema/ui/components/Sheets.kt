package com.example.clase18_08zulema.ui.components

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.clase18_08zulema.data.UserProfile
import com.example.clase18_08zulema.ui.kit.ButtonTone
import com.example.clase18_08zulema.ui.kit.Buttons
import com.example.clase18_08zulema.ui.kit.Glyph
import com.example.clase18_08zulema.ui.kit.Motion
import com.example.clase18_08zulema.ui.kit.TextRole
import com.example.clase18_08zulema.ui.kit.avatar
import com.example.clase18_08zulema.ui.kit.colors
import com.example.clase18_08zulema.ui.kit.dp
import com.example.clase18_08zulema.ui.kit.dpf
import com.example.clase18_08zulema.ui.kit.glyph
import com.example.clase18_08zulema.ui.kit.infoRow
import com.example.clase18_08zulema.ui.kit.matchWrap
import com.example.clase18_08zulema.ui.kit.pill
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/** Hojas inferiores con el mismo acabado: esquinas de 28dp, asa y contenido en columna. */
object Sheets {
    fun build(activity: Activity, content: LinearLayout.() -> Unit): BottomSheetDialog {
        val c = activity.colors
        val dialog = BottomSheetDialog(activity)
        val column = activity.vertical().apply {
            setPadding(activity.dp(24), activity.dp(12), activity.dp(24), activity.dp(28))
            background = MaterialShapeDrawable(
                ShapeAppearanceModel.builder().setTopLeftCornerSize(activity.dpf(28)).setTopRightCornerSize(activity.dpf(28)).build()
            ).apply { fillColor = android.content.res.ColorStateList.valueOf(c.surface) }
            addView(View(activity).apply { background = com.example.clase18_08zulema.ui.kit.rounded(c.outlineStrong, activity.dpf(3)) },
                LinearLayout.LayoutParams(activity.dp(36), activity.dp(5)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = activity.dp(18) })
            content()
        }
        dialog.setContentView(column)
        dialog.setOnShowListener {
            dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    fun profile(activity: Activity, user: UserProfile, onServer: () -> Unit, onLogout: () -> Unit) {
        val c = activity.colors
        lateinit var dialog: BottomSheetDialog
        dialog = build(activity) {
            gravity = Gravity.CENTER_HORIZONTAL
            val avatar = activity.avatar(user.fullName, 72)
            addView(avatar)
            Motion.pop(avatar, 60)
            addView(activity.text(user.fullName, TextRole.HEADLINE).apply { gravity = Gravity.CENTER }, matchWrap(14, activity))
            addView(activity.text(user.email, TextRole.BODY_SMALL, c.inkMuted).apply { gravity = Gravity.CENTER }, matchWrap(4, activity))
            val roleTone = if (user.isInstructor) c.primary to c.primaryContainer else c.success to c.successContainer
            addView(activity.pill(user.role.label, roleTone.first, roleTone.second, if (user.isInstructor) Glyph.SPARKLE else Glyph.TICKET),
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = activity.dp(12) })
            user.document?.let {
                addView(activity.infoRow(Glyph.ID, "Documento", it), matchWrap(24, activity))
            }
            addView(Buttons.create(activity, "Configurar servidor", ButtonTone.OUTLINED, Glyph.SERVER) {
                dialog.dismiss(); onServer()
            }, matchWrap(24, activity))
            addView(Buttons.create(activity, "Cerrar sesión", ButtonTone.DANGER_TONAL, Glyph.LOGOUT) {
                dialog.dismiss(); onLogout()
            }, matchWrap(10, activity))
        }
        dialog.show()
    }

    /** Confirmación con resumen: devuelve el botón para poder mostrar su estado de carga. */
    fun confirm(
        activity: Activity,
        glyph: Glyph,
        title: String,
        message: String,
        confirmLabel: String,
        tone: ButtonTone = ButtonTone.PRIMARY,
        details: (LinearLayout.() -> Unit)? = null,
        onConfirm: (dialog: BottomSheetDialog, button: MaterialButton) -> Unit
    ): BottomSheetDialog {
        val c = activity.colors
        lateinit var dialog: BottomSheetDialog
        dialog = build(activity) {
            val danger = tone == ButtonTone.DANGER || tone == ButtonTone.DANGER_TONAL
            addView(FrameLayout(activity).apply {
                background = com.example.clase18_08zulema.ui.kit.rounded(if (danger) c.dangerContainer else c.primaryContainer, activity.dpf(20))
                addView(ImageView(activity).apply { setImageDrawable(activity.glyph(glyph, if (danger) c.danger else c.primary, 26)) },
                    FrameLayout.LayoutParams(activity.dp(26), activity.dp(26), Gravity.CENTER))
            }, LinearLayout.LayoutParams(activity.dp(56), activity.dp(56)))
            addView(activity.text(title, TextRole.HEADLINE), matchWrap(18, activity))
            addView(activity.text(message, TextRole.BODY, c.inkMuted), matchWrap(8, activity))
            details?.invoke(this)
            lateinit var confirm: MaterialButton
            confirm = Buttons.create(activity, confirmLabel, tone) { onConfirm(dialog, confirm) }
            addView(confirm, matchWrap(24, activity))
            addView(Buttons.create(activity, "Volver", ButtonTone.GHOST, heightDp = 48) { dialog.dismiss() }, matchWrap(6, activity))
        }
        dialog.show()
        return dialog
    }
}
