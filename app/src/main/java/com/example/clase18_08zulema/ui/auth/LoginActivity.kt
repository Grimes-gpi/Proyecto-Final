package com.example.clase18_08zulema.ui.auth

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.example.clase18_08zulema.MainActivity
import com.example.clase18_08zulema.data.ApiResult
import com.example.clase18_08zulema.data.AuthRepository
import com.example.clase18_08zulema.data.RegisterRequest
import com.example.clase18_08zulema.data.Role
import com.example.clase18_08zulema.data.SessionManager
import com.example.clase18_08zulema.data.WorkshopsApi
import com.example.clase18_08zulema.ui.ServerConfig
import com.example.clase18_08zulema.ui.kit.ButtonTone
import com.example.clase18_08zulema.ui.kit.Buttons
import com.example.clase18_08zulema.ui.kit.Field
import com.example.clase18_08zulema.ui.kit.Fields
import com.example.clase18_08zulema.ui.kit.Glyph
import com.example.clase18_08zulema.ui.kit.Motion
import com.example.clase18_08zulema.ui.kit.SegmentedControl
import com.example.clase18_08zulema.ui.kit.TextRole
import com.example.clase18_08zulema.ui.kit.Tone
import com.example.clase18_08zulema.ui.kit.applySystemInsets
import com.example.clase18_08zulema.ui.kit.colors
import com.example.clase18_08zulema.ui.kit.dp
import com.example.clase18_08zulema.ui.kit.dpf
import com.example.clase18_08zulema.ui.kit.glyph
import com.example.clase18_08zulema.ui.kit.matchWrap
import com.example.clase18_08zulema.ui.kit.snack
import com.example.clase18_08zulema.ui.kit.text
import com.example.clase18_08zulema.ui.kit.vertical
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

/** Ingreso y registro en una sola pantalla; el formulario se expande con animación al cambiar de modo. */
class LoginActivity : AppCompatActivity() {
    companion object { const val EXTRA_EXPIRED = "session_expired" }

    private val c by lazy { colors }
    private var registering = false
    private var role = Role.STUDENT

    private lateinit var name: Field
    private lateinit var email: Field
    private lateinit var password: Field
    private lateinit var document: Field
    private lateinit var instructorCode: Field
    private lateinit var roleRow: View
    private lateinit var submit: MaterialButton
    private lateinit var titleView: android.widget.TextView
    private lateinit var subtitleView: android.widget.TextView
    private lateinit var serverLabel: android.widget.TextView

    private val localNetworkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) submit() else snack("Sin el permiso de red local no podemos llegar al servidor.", Tone.ERROR)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn) { openHome(); return }
        enableEdgeToEdge()

        val root = FrameLayout(this).apply { setBackgroundColor(c.background) }
        root.addView(GlowView(this), FrameLayout.LayoutParams(-1, dp(420)))

        val content = vertical().apply {
            setPadding(dp(24), 0, dp(24), 0)
            applySystemInsets(top = true, bottom = true, extraTopDp = 40, extraBottomDp = 24, includeIme = true)
        }
        content.addView(brandMark())
        titleView = text("Bienvenido de vuelta", TextRole.DISPLAY)
        content.addView(titleView, matchWrap(28, this))
        subtitleView = text("Ingresa para descubrir y gestionar los talleres de tu comunidad.", TextRole.BODY, c.inkMuted)
        content.addView(subtitleView, matchWrap(10, this))

        content.addView(SegmentedControl(this, listOf("Ingresar", "Crear cuenta")) { setMode(it == 1) }, matchWrap(28, this))

        val form = vertical().apply {
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = com.example.clase18_08zulema.ui.kit.rounded(c.surface, dpf(28), dp(1), c.outline)
            Motion.smoothLayout(this)
        }
        name = Fields.create(this, "Nombre completo", Glyph.USER, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        email = Fields.create(this, "Correo electrónico", Glyph.MAIL, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        password = Fields.create(this, "Contraseña", Glyph.LOCK, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).apply {
            layout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        document = Fields.create(this, "Documento de identidad", Glyph.ID, InputType.TYPE_CLASS_TEXT,
            helper = "Lo usamos para registrar tus inscripciones.")
        instructorCode = Fields.create(this, "Código de instructor", Glyph.SPARKLE, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            helper = "Te lo entrega la coordinación de talleres.")

        form.addView(name.layout, matchWrap(0, this))
        form.addView(email.layout, matchWrap(12, this))
        form.addView(password.layout, matchWrap(12, this))
        roleRow = vertical().apply {
            addView(context.text("¿Cómo participarás?", TextRole.CAPTION, c.inkMuted))
            addView(SegmentedControl(context, listOf(Role.STUDENT.label, Role.INSTRUCTOR.label)) { index ->
                role = if (index == 0) Role.STUDENT else Role.INSTRUCTOR
                applyRoleFields()
            }, matchWrap(8, context))
        }
        form.addView(roleRow, matchWrap(18, this))
        form.addView(document.layout, matchWrap(14, this))
        form.addView(instructorCode.layout, matchWrap(14, this))
        content.addView(form, matchWrap(16, this))

        submit = Buttons.create(this, "Ingresar", ButtonTone.PRIMARY, Glyph.ARROW_RIGHT) { submit() }
        submit.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        content.addView(submit, matchWrap(20, this))

        serverLabel = text("", TextRole.CAPTION, c.inkSubtle).apply { gravity = Gravity.CENTER }
        content.addView(Buttons.create(this, "Configurar servidor", ButtonTone.GHOST, Glyph.SERVER, heightDp = 44) {
            ServerConfig.showDialog(this) { refreshServerLabel(); snack("Servidor actualizado.", Tone.SUCCESS) }
        }, matchWrap(18, this))
        content.addView(serverLabel, matchWrap(2, this))
        refreshServerLabel()

        root.addView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // Estado inicial: modo ingreso, sin animar los campos ocultos.
        form.layoutTransition = null
        setMode(false)
        Motion.smoothLayout(form)
        Motion.stagger((0 until content.childCount).map(content::getChildAt), step = 55)

        if (intent.getBooleanExtra(EXTRA_EXPIRED, false)) {
            content.post { snack("Tu sesión expiró. Vuelve a ingresar.", Tone.INFO) }
        }
    }

    private fun setMode(register: Boolean) {
        registering = register
        val visibility = if (register) View.VISIBLE else View.GONE
        name.layout.visibility = visibility
        roleRow.visibility = visibility
        applyRoleFields()
        titleView.text = if (register) "Crea tu cuenta" else "Bienvenido de vuelta"
        subtitleView.text = if (register) "Inscríbete como estudiante o administra talleres como instructor."
        else "Ingresa para descubrir y gestionar los talleres de tu comunidad."
        submit.text = if (register) "Crear cuenta" else "Ingresar"
        listOf(name, email, password, document, instructorCode).forEach { it.layout.error = null }
    }

    private fun applyRoleFields() {
        document.layout.visibility = if (registering && role == Role.STUDENT) View.VISIBLE else View.GONE
        instructorCode.layout.visibility = if (registering && role == Role.INSTRUCTOR) View.VISIBLE else View.GONE
    }

    private fun submit() {
        if (!validate()) return
        if (ServerConfig.requiresLocalNetworkPermission(this)) {
            localNetworkPermission.launch(ServerConfig.LOCAL_NETWORK_PERMISSION)
            return
        }
        Buttons.setLoading(submit, true, if (registering) "Creando cuenta…" else "Ingresando…")
        val done: (ApiResult<com.example.clase18_08zulema.data.UserProfile>) -> Unit = { result ->
            if (!isFinishing) {
                Buttons.setLoading(submit, false)
                when (result) {
                    is ApiResult.Success -> openHome()
                    is ApiResult.Failure -> {
                        Motion.shake(submit)
                        snack(result.message, Tone.ERROR)
                    }
                }
            }
        }
        if (registering) {
            AuthRepository.register(RegisterRequest(
                fullName = name.value,
                email = email.value,
                password = password.edit.text?.toString().orEmpty(),
                role = role,
                document = document.value.takeIf { role == Role.STUDENT },
                instructorCode = instructorCode.value.takeIf { role == Role.INSTRUCTOR }
            ), done)
        } else {
            AuthRepository.login(email.value, password.edit.text?.toString().orEmpty(), done)
        }
    }

    private fun validate(): Boolean {
        var firstError: Field? = null
        fun check(field: Field, message: String?) {
            if (message != null) { field.error(message); if (firstError == null) firstError = field }
        }
        val pass = password.edit.text?.toString().orEmpty()
        if (registering) check(name, if (name.value.length < 3) "Escribe tu nombre completo" else null)
        check(email, if (!Patterns.EMAIL_ADDRESS.matcher(email.value).matches()) "Escribe un correo válido" else null)
        check(password, when {
            pass.isEmpty() -> "Escribe tu contraseña"
            registering && pass.length < 8 -> "Usa al menos 8 caracteres"
            else -> null
        })
        if (registering && role == Role.STUDENT) check(document, if (document.value.isEmpty()) "El documento es obligatorio" else null)
        if (registering && role == Role.INSTRUCTOR) check(instructorCode, if (instructorCode.value.isEmpty()) "Escribe el código" else null)
        firstError?.edit?.requestFocus()
        return firstError == null
    }

    private fun refreshServerLabel() {
        serverLabel.text = WorkshopsApi.baseUrl
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun brandMark(): View = FrameLayout(this).apply {
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c.heroStart, c.heroEnd)).apply {
            cornerRadius = dpf(22)
        }
        elevation = dpf(6)
        addView(ImageView(context).apply { setImageDrawable(context.glyph(Glyph.SPARKLE, Color.WHITE, 30, 2f)) },
            FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
    }

    /** Halo decorativo detrás del encabezado (dos degradados radiales suaves). */
    private class GlowView(context: android.content.Context) : View(context) {
        private val palette = context.colors
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            paint.shader = RadialGradient(w * 0.15f, 0f, w * 0.9f,
                ColorUtils.setAlphaComponent(palette.primary, if (palette.isDark) 70 else 46), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, height.toFloat(), paint)
            paint.shader = RadialGradient(w, height * 0.35f, w * 0.7f,
                ColorUtils.setAlphaComponent(palette.accent, if (palette.isDark) 46 else 30), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, height.toFloat(), paint)
        }
    }
}
