package com.example.clase18_08zulema.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.clase18_08zulema.data.SessionManager
import com.example.clase18_08zulema.data.UserProfile
import com.example.clase18_08zulema.ui.auth.LoginActivity

abstract class SecureActivity : AppCompatActivity() {
    private val expiryListener: () -> Unit = { goToLogin(expired = true) }

    protected val currentUser: UserProfile? get() = SessionManager.user

    /** Llamar al inicio de onCreate (después de super). Devuelve false si se redirigió al login. */
    protected fun ensureSession(instructorOnly: Boolean = false): Boolean {
        if (!SessionManager.isLoggedIn) {
            goToLogin(expired = SessionManager.token != null)
            return false
        }
        if (instructorOnly && !SessionManager.isInstructor) {
            finish()
            return false
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        SessionManager.onSessionExpired = expiryListener
    }

    override fun onStop() {
        if (SessionManager.onSessionExpired === expiryListener) SessionManager.onSessionExpired = null
        super.onStop()
    }

    final override fun onResume() {
        super.onResume()
        if (isFinishing) return
        if (SessionManager.isLoggedIn) onAuthenticatedResume() else goToLogin(expired = true)
    }

    /** Equivale a onResume, pero solo se llama con sesión válida. */
    protected open fun onAuthenticatedResume() = Unit

    protected fun goToLogin(expired: Boolean = false) {
        if (isFinishing) return
        startActivity(Intent(this, LoginActivity::class.java)
            .putExtra(LoginActivity.EXTRA_EXPIRED, expired)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
