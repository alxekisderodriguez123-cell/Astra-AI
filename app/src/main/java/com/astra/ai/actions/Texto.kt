package com.astra.ai.actions

import java.text.Normalizer
import java.util.Locale

/** Minúsculas, sin tildes ni signos: "¿Qué hora es?" -> "que hora es" */
fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(Locale("es")), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private val unidades = mapOf(
    "cero" to 0, "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3,
    "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8,
    "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
    "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17,
    "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21,
    "veintidos" to 22, "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25,
    "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29
)
private val decenas = mapOf("treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60)

/** "siete y media" -> "7 y media", "treinta y cinco" -> "35". Úsalo solo donde se esperan números. */
fun numerosEnPalabras(texto: String): String {
    var r = texto
    val dec = decenas.keys.joinToString("|")
    val uni = unidades.filterValues { it in 1..9 }.keys.joinToString("|")
    r = Regex("\\b($dec) y ($uni)\\b").replace(r) {
        (decenas.getValue(it.groupValues[1]) + unidades.getValue(it.groupValues[2])).toString()
    }
    for ((palabra, valor) in decenas + unidades) {
        r = r.replace(Regex("\\b$palabra\\b"), valor.toString())
    }
    return r
}
