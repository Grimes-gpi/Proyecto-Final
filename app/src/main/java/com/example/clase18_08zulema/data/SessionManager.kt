package com.example.clase18_08zulema.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * Guarda el JWT y el perfil en preferencias privadas de la app.
 * Se inicializa una sola vez desde [com.example.clase18_08zulema.TalleresApp].
 */
object SessionManager {
    private const val PREFS = "talleres_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER = "user"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    private val main = Handler(Looper.getMainLooper())

    /** La Activity visible se suscribe para volver al login si el servidor rechaza el token. */
    @Volatile var onSessionExpired: (() -> Unit)? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)

    val user: UserProfile?
        get() = prefs.getString(KEY_USER, null)?.let { runCatching { gson.fromJson(it, UserProfile::class.java) }.getOrNull() }

    val isLoggedIn: Boolean get() = token?.let { !isExpired(it) } == true && user != null

    val isInstructor: Boolean get() = user?.isInstructor == true

    fun save(auth: AuthResponse) {
        prefs.edit().putString(KEY_TOKEN, auth.token).putString(KEY_USER, gson.toJson(auth.user)).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Llamado desde el hilo de red cuando la API responde 401 a una petición autenticada. */
    fun expire() {
        clear()
        main.post { onSessionExpired?.invoke() }
    }

    /** Lee "exp" del JWT para no usar un token vencido al abrir la app (la firma la valida el servidor). */
    private fun isExpired(jwt: String): Boolean = runCatching {
        val payload = jwt.split('.')[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        val exp = JsonParser.parseString(json).asJsonObject.get("exp").asLong
        System.currentTimeMillis() / 1000 >= exp
    }.getOrDefault(true)
}
