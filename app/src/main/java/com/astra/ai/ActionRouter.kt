package com.astra.ai

import android.content.Context
import com.astra.ai.actions.AbrirApp
import com.astra.ai.actions.Accion
import com.astra.ai.actions.Alarma
import com.astra.ai.actions.Brillo
import com.astra.ai.actions.HoraFecha
import com.astra.ai.actions.Linterna
import com.astra.ai.actions.Llamar
import com.astra.ai.actions.Mapas
import com.astra.ai.actions.Mensaje
import com.astra.ai.actions.Musica
import com.astra.ai.actions.Temporizador
import com.astra.ai.actions.TokenAppCheck
import com.astra.ai.actions.Volumen
import com.astra.ai.actions.normalizar

class ActionRouter(context: Context) {

    private var ultimoOriginal = ""

    private val acciones: List<Accion> = listOf(
        TokenAppCheck(context),
        Llamar(context),
        Mensaje(context) { ultimoOriginal },
        Alarma(context),
        Temporizador(context),
        Linterna(context),
        Volumen(context),
        Brillo(context),
        HoraFecha(),
        Mapas(context),
        Musica(context),
        AbrirApp(context)
    )

    /** Devuelve lo que Astra debe decir, o null si ninguna acción entendió la orden. */
    fun ejecutar(orden: String): String? {
        ultimoOriginal = orden
        val limpia = normalizar(orden)
        for (accion in acciones) {
            val respuesta = accion.intentar(limpia)
            if (respuesta != null) return respuesta
        }
        return null
    }
}
