package com.astra.ai.actions

import android.content.Context
import android.content.Intent

class AbrirApp(private val context: Context) : Accion {

    private val verbos = listOf("abre", "abrir", "abreme", "inicia", "ejecuta", "lanza")
    private val alias = mapOf("configuracion" to "ajustes", "configuraciones" to "ajustes")

    override fun intentar(orden: String): String? {
        val palabras = orden.split(" ")
        if (palabras.isEmpty() || palabras[0] !in verbos) return null

        var nombre = palabras.drop(1).joinToString(" ")
            .replace(Regex("^(la |el )?(aplicacion|app) (de |del )?"), "")
            .replace(Regex("^(la |el |los |las )"), "")
            .trim()
        nombre = alias[nombre] ?: nombre
        if (nombre.isEmpty()) return "¿Qué aplicación quieres abrir?"

        val pm = context.packageManager
        val filtro = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(filtro, 0)
            .map { normalizar(it.loadLabel(pm).toString()) to it }

        val elegida = apps.firstOrNull { it.first == nombre }
            ?: apps.filter { it.first.contains(nombre) }.minByOrNull { it.first.length }
            ?: apps.filter { it.first.length >= 4 && nombre.contains(it.first) }.maxByOrNull { it.first.length }
            ?: return "No encontré la aplicación $nombre"

        val paquete = elegida.second.activityInfo.packageName
        val lanzar = pm.getLaunchIntentForPackage(paquete)
            ?: return "No pude abrir ${elegida.second.loadLabel(pm)}"
        lanzar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(lanzar)
        return "Abriendo ${elegida.second.loadLabel(pm)}"
    }
}
