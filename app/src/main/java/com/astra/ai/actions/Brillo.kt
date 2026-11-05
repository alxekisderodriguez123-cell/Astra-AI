package com.astra.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class Brillo(private val context: Context) : Accion {

    override fun intentar(orden: String): String? {
        if (!orden.contains("brillo")) return null

        if (!Settings.System.canWrite(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Necesito permiso para cambiar el brillo. Actívalo y vuelve a pedírmelo"
        }

        val resolver = context.contentResolver
        val actual = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val paso = if (orden.contains("poco")) 25 else 60

        val nuevo = when {
            orden.contains("maximo") -> 255
            orden.contains("minimo") -> 10
            orden.contains("baj") || orden.contains("menos") || orden.contains("reduc") -> actual - paso
            orden.contains("sub") || orden.contains("mas") || orden.contains("aument") -> actual + paso
            else -> return "¿Quieres más o menos brillo?"
        }.coerceIn(10, 255)

        return try {
            Settings.System.putInt(
                resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, nuevo)
            "Brillo al ${nuevo * 100 / 255} por ciento"
        } catch (e: Exception) {
            "No pude cambiar el brillo"
        }
    }
}
