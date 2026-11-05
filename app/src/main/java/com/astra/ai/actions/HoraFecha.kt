package com.astra.ai.actions

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HoraFecha : Accion {

    override fun intentar(orden: String): String? = when {
        orden.contains("que hora") || orden.contains("la hora") -> {
            val ahora = LocalTime.now()
            "Son las ${ahora.hour}:${"%02d".format(ahora.minute)}"
        }
        orden.contains("que dia") || orden.contains("fecha") -> {
            val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            "Hoy es ${LocalDate.now().format(formato)}"
        }
        else -> null
    }
}
