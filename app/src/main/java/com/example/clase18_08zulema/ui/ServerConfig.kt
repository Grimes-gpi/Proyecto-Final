package com.example.clase18_08zulema.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.InputType
import com.example.clase18_08zulema.data.WorkshopsApi
import com.example.clase18_08zulema.ui.kit.Fields
import com.example.clase18_08zulema.ui.kit.Glyph
import com.example.clase18_08zulema.ui.kit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Dirección del backend configurable (emulador vs. teléfono físico) y permiso de red local de Android 17. */
object ServerConfig {
    private const val PREFS = "api_connection"
    private const val KEY = "base_url"
    const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    fun read(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY, WorkshopsApi.DEFAULT_BASE_URL)
        ?.replace("http://10.0.2.2:8080/", WorkshopsApi.DEFAULT_BASE_URL)
        ?: WorkshopsApi.DEFAULT_BASE_URL

    fun normalize(value: String): String? {
        val candidate = value.trim().let { if (it.endsWith("/")) it else "$it/" }
        val parsed = runCatching { java.net.URI(candidate) }.getOrNull() ?: return null
        if (parsed.scheme !in listOf("http", "https") || parsed.host.isNullOrBlank() || parsed.userInfo != null) return null
        if (parsed.path !in listOf("", "/")) return null
        return candidate
    }

    fun requiresLocalNetworkPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(LOCAL_NETWORK_PERMISSION) != PackageManager.PERMISSION_GRANTED

    fun showDialog(activity: Activity, onSaved: () -> Unit) {
        val field = Fields.create(activity, "Dirección de la API", Glyph.SERVER,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            helper = "Emulador: 10.0.2.2 · Teléfono: IP local del computador")
        field.edit.setText(read(activity))
        val container = android.widget.FrameLayout(activity).apply {
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), 0)
            addView(field.layout)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("Conectar con el servidor")
            .setMessage("El teléfono y el computador deben estar en la misma red.")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val url = normalize(field.value)
                        if (url == null) {
                            field.error("Usa una dirección http:// o https:// válida, sin ruta")
                            return@setOnClickListener
                        }
                        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, url).apply()
                        WorkshopsApi.configure(url)
                        dialog.dismiss()
                        onSaved()
                    }
                }
                dialog.show()
            }
    }
}
