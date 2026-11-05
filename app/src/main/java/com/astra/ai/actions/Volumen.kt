package com.astra.ai.actions

import android.content.Context
import android.media.AudioManager

class Volumen(context: Context) : Accion {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val tipo = AudioManager.STREAM_MUSIC

    override fun intentar(orden: String): String? {
        val silencio = orden.contains("silencia") || orden.contains("silenciar") ||
            orden.contains("mutea") || orden.contains("sin sonido")
        if (!orden.contains("volumen") && !silencio) return null

        val maximo = audio.getStreamMaxVolume(tipo)
        val actual = audio.getStreamVolume(tipo)
        val paso = if (orden.contains("poco")) maxOf(1, maximo / 10) else maxOf(1, maximo / 5)

        val nuevo = when {
            silencio -> 0
            orden.contains("minimo") -> 1
            orden.contains("maximo") -> maximo
            orden.contains("baj") || orden.contains("menos") || orden.contains("reduc") -> actual - paso
            orden.contains("sub") || orden.contains("mas") || orden.contains("aument") -> actual + paso
            else -> return "¿Quieres que suba o baje el volumen?"
        }.coerceIn(0, maximo)

        return try {
            audio.setStreamVolume(tipo, nuevo, AudioManager.FLAG_SHOW_UI)
            if (nuevo == 0) "Volumen en silencio" else "Volumen al ${nuevo * 100 / maximo} por ciento"
        } catch (e: Exception) {
            "No pude cambiar el volumen"
        }
    }
}
