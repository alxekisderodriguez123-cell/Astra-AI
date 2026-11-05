package com.astra.ai.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class Llamar(private val context: Context) : Accion {

    private val verbos = listOf("llama", "llamar", "llamale", "marca", "marcar", "telefonea")

    override fun intentar(orden: String): String? {
        val palabras = orden.split(" ")
        val primera = palabras.firstOrNull() ?: return null
        if (primera !in verbos) return null

        val objetivo = palabras.drop(1).joinToString(" ")
            .replace(Regex("^(a |al |a la |a el )+"), "")
            .trim()
        if (objetivo.isEmpty()) return "¿A quién quieres llamar?"

        val soloDigitos = objetivo.replace(" ", "")
        val nombre: String
        val numero: String
        if (soloDigitos.matches(Regex("\\d{3,15}"))) {
            nombre = soloDigitos
            numero = soloDigitos
        } else {
            if (!Contactos.tienePermiso(context)) {
                return "Necesito permiso de contactos. Actívalo en Ajustes, Aplicaciones, Astra, Permisos"
            }
            val contacto = Contactos.buscar(context, objetivo)
                ?: return "No encontré a $objetivo en tus contactos"
            nombre = contacto.nombre
            numero = contacto.numero
        }

        val limpio = numero.replace(Regex("[^+0-9*#]"), "")
        val puedeLlamar = context.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val intent = Intent(
            if (puedeLlamar) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.parse("tel:" + Uri.encode(limpio))
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            if (puedeLlamar) "Llamando a $nombre" else "Marcando a $nombre. Toca el botón de llamar"
        } catch (e: Exception) {
            "No pude iniciar la llamada"
        }
    }
}
