package com.astra.ai.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.astra.ai.Biblioteca
import com.astra.ai.MusicService

class Musica(private val context: Context) : Accion {

    private val reDetener = Regex("^(deten|detener|apaga|quita|para|cancela|termina|cierra) (la |el )?(musica|cancion|reproductor|reproduccion)$")
    private val rePausa = Regex("^(pausa|pausar|pon pausa|pausa (la |el )?(musica|cancion|reproductor))$")
    private val reReanudar = Regex("^(continua|continuar|sigue|seguir|reanuda|reanudar|play|dale play|reproduce|reproducir|(continua|reanuda|reanudar|sigue) (con )?(la |el )?(musica|cancion))$")
    private val reSiguiente = Regex("^((pasa|ve|cambia) a la |pon la )?(siguiente|proxima)( cancion| tema)?$|^(otra cancion|cambia de cancion|cambia la cancion|salta|salta la cancion|next)$")
    private val reAnterior = Regex("^((pasa|ve|vuelve) a la |pon la )?(anterior|previa)( cancion| tema)?$|^(la cancion anterior|cancion anterior)$")
    private val rePoner = Regex("^(quiero (escuchar|oir)|ponme|ponle|pon|reproduce|reproduceme|tocame|toca|escucha|escuchar|play)\\s+(.+)$")
    private val reGenerica = Regex("^(algo de |un poco de |mi |mis )?(musica|canciones|temas)( aleatoria| al azar| variada)?$")

    override fun intentar(orden: String): String? {
        val hay = MusicService.hayMusica()
        when {
            reDetener.matches(orden) ->
                return if (hay) { MusicService.detener(); "Música detenida" } else "No hay música sonando"
            rePausa.matches(orden) ->
                return if (!hay) "No hay música sonando"
                else if (MusicService.sonando()) { MusicService.alternar(); "Música en pausa" }
                else "La música ya está en pausa"
            reReanudar.matches(orden) ->
                return if (!hay) "No hay música para continuar. Dime qué quieres escuchar"
                else { if (!MusicService.sonando()) MusicService.alternar(); "Continuando" }
            reSiguiente.matches(orden) ->
                return if (hay) { MusicService.siguiente(); "Siguiente canción" } else "No hay música sonando"
            reAnterior.matches(orden) ->
                return if (hay) { MusicService.anterior(); "Canción anterior" } else "No hay música sonando"
            orden.contains("que suena") || orden.contains("que cancion es") || orden.contains("que esta sonando") -> {
                val p = MusicService.pistaActual() ?: return "No hay música sonando en Astra"
                return "Suena ${p.titulo}" + if (p.artista.isNotBlank()) " de ${p.artista}" else ""
            }
        }

        val m = rePoner.find(orden) ?: return null
        val resto = m.groupValues[3].trim()

        val destino = when {
            resto.contains("spotify") -> "spotify"
            resto.contains("youtube music") || resto.contains("yt music") || resto.contains("ytmusic") -> "ytmusic"
            resto.contains("youtube") || Regex("\\bvideos?\\b").containsMatchIn(resto) -> "youtube"
            else -> null
        }

        var consulta = resto
            .replace(Regex("\\b(en|por|desde|con|a traves de|usando)\\s+(youtube music|yt music|ytmusic|youtube|spotify)\\b"), " ")
            .replace(Regex("\\b(youtube music|yt music|ytmusic|youtube|spotify)\\b"), " ")
            .replace(Regex("\\b(en|de|del|desde) (mi |el )?(telefono|celular|movil)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim()

        val generica = consulta.isEmpty() || reGenerica.matches(consulta)
        val esArtista = Regex("^(algo de |un poco de )?(musica|canciones|temas) (de|del) ").containsMatchIn(consulta)
        consulta = consulta
            .replace(Regex("^(algo de |un poco de )?(musica|canciones|temas) (de |del )?"), "")
            .replace(Regex("^(la |el |una |un |lo |los |las )?(cancion|tema|rola|video|videos)( de| del| llamada| llamado| que se llama)?\\s*"), "")
            .trim()

        when (destino) {
            "spotify" -> return if (abrirSpotify(if (generica) "" else consulta))
                "Abriendo Spotify" + (if (generica) "" else " con $consulta") + ". Toca reproducir"
            else "No pude abrir Spotify"
            "ytmusic" -> return if (abrirYoutube(if (generica) "música" else consulta, true))
                "Buscando ${if (generica) "música" else consulta} en YouTube Music"
            else "No pude abrir YouTube"
            "youtube" -> return if (abrirYoutube(if (generica) "música" else consulta, false))
                "Buscando ${if (generica) "música" else consulta} en YouTube"
            else "No pude abrir YouTube"
        }

        // Música guardada en el teléfono, con el reproductor de Astra
        if (!Biblioteca.tienePermiso(context)) {
            return "Necesito permiso para leer tu música. Actívalo en Ajustes, Aplicaciones, Astra, Permisos"
        }
        val todas = Biblioteca.cargar(context)
        if (todas.isEmpty()) return "No encontré canciones en tu teléfono"

        if (generica) {
            return try {
                MusicService.reproducir(context, todas.shuffled())
                "Reproduciendo tu música"
            } catch (e: Exception) {
                "No pude iniciar la música"
            }
        }

        val coincidencias = Biblioteca.buscar(todas, consulta)
        if (coincidencias.isEmpty()) {
            return if (abrirYoutube(consulta, true)) "No encontré $consulta en tu teléfono. La busco en YouTube"
            else "No encontré $consulta en tu teléfono"
        }
        val cola = if (esArtista) coincidencias.shuffled() else coincidencias
        return try {
            MusicService.reproducir(context, cola)
            val p = cola.first()
            "Reproduciendo ${p.titulo}" + if (p.artista.isNotBlank()) " de ${p.artista}" else ""
        } catch (e: Exception) {
            "No pude iniciar la música"
        }
    }

    private fun abrirYoutube(consulta: String, musica: Boolean): Boolean {
        if (musica) {
            try {
                val i = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                    .setPackage("com.google.android.apps.youtube.music")
                    .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                    .putExtra(SearchManager.QUERY, consulta)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                return true
            } catch (e: Exception) {
            }
        }
        try {
            val i = Intent(Intent.ACTION_SEARCH)
                .setPackage("com.google.android.youtube")
                .putExtra("query", consulta)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
            return true
        } catch (e: Exception) {
        }
        return try {
            val url = "https://www.youtube.com/results?search_query=" + Uri.encode(consulta)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun abrirSpotify(consulta: String): Boolean {
        try {
            if (consulta.isBlank()) {
                val i = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                if (i != null) {
                    context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return true
                }
            } else {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(consulta)))
                    .setPackage("com.spotify.music")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                return true
            }
        } catch (e: Exception) {
        }
        return try {
            val url = "https://open.spotify.com/search/" + Uri.encode(consulta)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }
}
