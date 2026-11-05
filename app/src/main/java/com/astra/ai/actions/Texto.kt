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
