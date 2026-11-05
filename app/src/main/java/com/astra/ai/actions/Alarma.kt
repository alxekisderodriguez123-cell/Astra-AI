package com.astra.ai.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.time.LocalTime

class Alarma(private val context: Context) : Accion {

    private val patron = Regex(
        """\ba las? (\d{1,2})(?: y (\d{1,2}|media|cuarto)| (\d{2})| menos (cuarto|\d{1,2}))?"""
    )

    override fun intentar(orden: String): String? {
        if (!orden.contains("alarma") && !orden.contains("despiert")) return null
        if (orden.contains("apaga") || orden.contains("cancela") ||
            orden.contains("elimina") || orden.contains("borra")
        ) return "Todavía no sé cancelar alarmas. Puedes hacerlo desde la app del reloj"

        val texto = numerosEnPalabras(orden)
        val m = patron.find(texto) ?: return "¿A qué hora quieres la alarma?"

        var hora = m.groupValues[1].toInt()
        var minuto = 0
        val despues = m.groupValues[2]
        val directo = m.groupValues[3]
        val menos = m.groupValues[4]
        when {
            despues == "media" -> minuto = 30
            despues == "cuarto" -> minuto = 15
            despues.isNotEmpty() -> minuto = despues.toInt()
            directo.isNotEmpty() -> minuto = directo.toInt()
            menos.isNotEmpty() -> {
                val n = if (menos == "cuarto") 15 else menos.toInt()
                hora = if (hora == 0) 23 else hora - 1
                minuto = 60 - n
            }
        }
        if (hora !in 0..23 || minuto !in 0..59) return "No entendí la hora de la alarma"

        val tarde = texto.contains("de la tarde") || texto.contains("de la noche") ||
            Regex("\\bp m\\b|\\bpm\\b").containsMatchIn(texto)
        val manana = texto.contains("de la manana") || texto.contains("de la madrugada") ||
            Regex("\\ba m\\b|\\bam\\b").containsMatchIn(texto)

        val h24 = when {
            hora > 12 || hora == 0 -> hora
            tarde -> if (hora == 12) (if (texto.contains("de la noche")) 0 else 12) else hora + 12
            manana -> if (hora == 12) 0 else hora
            else -> {
                // Sin indicar mañana o tarde: elegimos la próxima vez que llegue esa hora
                val ahora = LocalTime.now()
                val minAhora = ahora.hour * 60 + ahora.minute
                fun falta(h: Int) = ((h * 60 + minuto) - minAhora + 1440) % 1440
                val a = hora % 12
                val b = a + 12
                if (falta(a) <= falta(b)) a else b
            }
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, h24)
            putExtra(AlarmClock.EXTRA_MINUTES, minuto)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Astra")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            val h12 = if (h24 % 12 == 0) 12 else h24 % 12
            val periodo = when (h24) {
                in 0..11 -> "de la mañana"
                in 12..18 -> "de la tarde"
                else -> "de la noche"
            }
            "Alarma puesta a las $h12:${"%02d".format(minuto)} $periodo"
        } catch (e: Exception) {
            "No encontré una aplicación de reloj para poner la alarma"
        }
    }
}
