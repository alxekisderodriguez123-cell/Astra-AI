package com.astra.ai.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.io.File

/**
 * Orden de depuración: "token de app check".
 * Copia al portapapeles el token de depuración de App Check para registrarlo en Firebase.
 */
class TokenAppCheck(private val context: Context) : Accion {

    private val uuid = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )

    override fun intentar(orden: String): String? {
        val pide = "token" in orden && ("app check" in orden || "appcheck" in orden)
        if (!pide) return null

        val token = buscarToken()
            ?: return "Todavía no hay token. Conéctate a internet, reinicia Astra y vuelve a pedírmelo."

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Token App Check", token))
        return "Listo, copié el token de App Check. Pégalo en la consola de Firebase."
    }

    private fun buscarToken(): String? {
        val carpeta = File(context.applicationInfo.dataDir, "shared_prefs")
        val archivos = carpeta.listFiles { f -> f.name.endsWith(".xml") } ?: return null

        for (archivo in archivos) {
            if (!archivo.name.contains("appcheck", true)) continue
            val prefs = context.getSharedPreferences(
                archivo.name.removeSuffix(".xml"),
                Context.MODE_PRIVATE
            )
            for (valor in prefs.all.values) {
                if (valor is String && uuid.matches(valor)) return valor
            }
        }
        return null
    }
}
