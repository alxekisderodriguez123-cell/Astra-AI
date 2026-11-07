package com.astra.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri

class Mapas(private val context: Context) : Accion {

    private val reCerca = Regex("\\b(cerca de mi|cerca de aqui|cerca|cercano|cercanos|cercana|cercanas|por aqui|mas cercano|mas cercana)\\b")
    private val reInicioCerca = Regex("^(busca|buscame|muestrame|encuentra|donde|hay|necesito|quiero|llevame|dirigeme|guiame)\\b")
    private val reQuitar = Regex("^(busca|buscame|muestrame|encuentra|donde (hay|queda|esta)|hay|necesito|quiero|llevame|dirigeme|guiame)( un| una| unos| unas| al| a la| el| la| los| las)?\\s*")
    private val reRuta = Regex("^(llevame|guiame|dirigeme|navega|navegar|ruta|dame la ruta|como llego|como puedo llegar|como se llega|indicame como llegar|quiero ir)\\s+(?:a la |al |a |hacia |hasta |para )?(.+)$")
    private val reDonde = Regex("^(donde (queda|esta|hay)|busca en el mapa|buscame en el mapa|muestrame en el mapa)\\s+(.+)$")

    override fun intentar(orden: String): String? {
        // Lugares cerca de ti
        if (reCerca.containsMatchIn(orden) && reInicioCerca.containsMatchIn(orden)) {
            val q = orden.replace(reCerca, " ").replace(reQuitar, "").replace(Regex("\\s+"), " ").trim()
            if (q.isEmpty()) return "¿Qué quieres buscar cerca de ti?"
            return if (abrirBusqueda(q)) "Buscando $q cerca de ti" else "No pude abrir el mapa"
        }

        // Navegación a un destino
        reRuta.find(orden)?.let { m ->
            val destino = m.groupValues[2].trim()
            if (destino.isEmpty()) return "¿A dónde quieres ir?"
            return if (navegar(destino)) "Abriendo la ruta a $destino" else "No pude abrir el mapa"
        }

        // Dónde queda un lugar
        reDonde.find(orden)?.let { m ->
            val lugar = m.groupValues[3].trim()
            return if (abrirBusqueda(lugar)) "Buscando $lugar en el mapa" else "No pude abrir el mapa"
        }
        return null
    }

    private fun navegar(destino: String): Boolean {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(destino)))
                .setPackage("com.google.android.apps.maps")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            return true
        } catch (e: Exception) {
        }
        return abrirBusqueda(destino)
    }

    private fun abrirBusqueda(consulta: String): Boolean {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(consulta))
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .setPackage("com.google.android.apps.maps")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return true
        } catch (e: Exception) {
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }
}
