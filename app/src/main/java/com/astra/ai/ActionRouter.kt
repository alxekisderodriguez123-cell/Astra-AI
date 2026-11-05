package com.astra.ai

import android.content.Context
import com.astra.ai.actions.AbrirApp
import com.astra.ai.actions.Accion
import com.astra.ai.actions.Brillo
import com.astra.ai.actions.HoraFecha
import com.astra.ai.actions.Linterna
import com.astra.ai.actions.Volumen
import com.astra.ai.actions.normalizar

class ActionRouter(context: Context) {

    private val acciones: List<Accion> = listOf(
        Linterna(context),
        Volumen(context),
        Brillo(context),
        HoraFecha(),
        AbrirApp(context)
    )

    /** Devuelve lo que Astra debe decir, o null si ninguna acción entendió la orden. */
    fun ejecutar(orden: String): String? {
        val limpia = normalizar(orden)
        for (accion in acciones) {
            val respuesta = accion.intentar(limpia)
            if (respuesta != null) return respuesta
        }
        return null
    }
}
