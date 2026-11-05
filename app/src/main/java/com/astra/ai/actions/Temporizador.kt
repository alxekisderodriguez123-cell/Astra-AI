package com.astra.ai.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class Temporizador(private val context: Context) : Accion {

    override fun intentar(orden: String): String? {
        if (!orden.contains("temporizador") && !orden.contains("timer") &&
            !orden.contains("cuenta regresiva")
        ) return null

        val texto = numerosEnPalabras(orden)
        var segundos = 0
        Regex("""(\d+) (horas?|minutos?|min|segundos?|seg)\b""").findAll(texto).forEach { m ->
            val n = m.groupValues[1].toInt()
            val unidad = m.groupValues[2]
            segundos += when {
                unidad.startsWith("hora") -> n * 3600
                unidad.startsWith("min") -> n * 60
                else -> n
            }
        }
        if (texto.contains("media hora")) segundos += 1800
        else if (texto.contains("y media") && Regex("\\d+ horas?").containsMatchIn(texto)) segundos += 1800

        if (segundos <= 0) return "¿De cuánto tiempo quieres el temporizador?"
        if (segundos > 86400) return "El temporizador máximo es de 24 horas"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, segundos)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Astra")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            val partes = mutableListOf<String>()
            val h = segundos / 3600
            val min = (segundos % 3600) / 60
            val s = segundos % 60
            if (h > 0) partes.add(if (h == 1) "1 hora" else "$h horas")
            if (min > 0) partes.add(if (min == 1) "1 minuto" else "$min minutos")
            if (s > 0) partes.add(if (s == 1) "1 segundo" else "$s segundos")
            "Temporizador de ${partes.joinToString(" y ")}"
        } catch (e: Exception) {
            "No encontré una aplicación de reloj para el temporizador"
        }
    }
}
