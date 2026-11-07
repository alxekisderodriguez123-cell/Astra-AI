package com.astra.ai

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.astra.ai.actions.normalizar
import kotlin.math.ceil

object Biblioteca {

    data class Pista(
        val id: Long,
        val titulo: String,
        val artista: String,
        val uri: Uri,
        val clave: String
    )

    private val relleno = setOf("de", "la", "el", "los", "las", "del", "y", "en", "un", "una", "mi", "a", "al", "lo")

    fun tienePermiso(context: Context): Boolean {
        val permiso = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return context.checkSelfPermission(permiso) == PackageManager.PERMISSION_GRANTED
    }

    fun cargar(context: Context): List<Pista> {
        val lista = mutableListOf<Pista>()
        val columnas = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val filtro = "${MediaStore.Audio.Media.DURATION} >= 30000"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columnas, filtro, null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val titulo = c.getString(1) ?: ""
                var artista = c.getString(2) ?: ""
                val archivo = c.getString(3) ?: ""
                if (artista == "<unknown>") artista = ""
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                lista.add(
                    Pista(
                        id, titulo.ifBlank { archivo }, artista, uri,
                        normalizar("$titulo $artista $archivo")
                    )
                )
            }
        }
        return lista
    }

    /** Devuelve las pistas que mejor coinciden, la más parecida primero. */
    fun buscar(pistas: List<Pista>, consulta: String): List<Pista> {
        val palabras = normalizar(consulta).split(" ").filter { it.isNotEmpty() && it !in relleno }
        if (palabras.isEmpty()) return emptyList()
        val minimo = if (palabras.size <= 2) palabras.size else ceil(palabras.size * 0.6).toInt()
        return pistas
            .map { p -> p to palabras.count { p.clave.contains(it) } }
            .filter { it.second >= minimo }
            .sortedWith(compareByDescending<Pair<Pista, Int>> { it.second }.thenBy { it.first.clave.length })
            .map { it.first }
    }
}
