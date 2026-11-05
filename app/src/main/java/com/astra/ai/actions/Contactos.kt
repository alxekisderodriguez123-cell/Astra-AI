package com.astra.ai.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone

object Contactos {

    data class Contacto(val nombre: String, val numero: String)

    fun tienePermiso(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun buscar(context: Context, consulta: String): Contacto? {
        val q = normalizar(consulta).replace(Regex("^(mi |mis )"), "").trim()
        if (q.isEmpty()) return null

        val todos = mutableListOf<Pair<String, Contacto>>()
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val nombre = cursor.getString(0) ?: continue
                val numero = cursor.getString(1) ?: continue
                todos.add(normalizar(nombre) to Contacto(nombre, numero))
            }
        }

        return todos.firstOrNull { it.first == q }?.second
            ?: todos.filter { it.first.startsWith(q) }.minByOrNull { it.first.length }?.second
            ?: todos.filter { it.first.contains(q) }.minByOrNull { it.first.length }?.second
            ?: todos.filter { par -> q.split(" ").all { par.first.contains(it) } }
                .minByOrNull { it.first.length }?.second
    }
}
