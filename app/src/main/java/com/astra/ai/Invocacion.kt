package com.astra.ai

/**
 * Cómo se invocó a Astra:
 *  - IA:      "Ey Astra ..." / "Oye Astra ..."  -> acciones primero y, si no hay ninguna, conversa la IA.
 *  - ACCION:  "Astra ..."                       -> solo acciones (si no entiende, lo dice).
 *  - LIBRE:   sin prefijo (botón o teclado)     -> acciones primero y, si no hay ninguna, conversa la IA.
 */
enum class Modo { IA, ACCION, LIBRE }

data class Orden(val modo: Modo, val texto: String)

object Invocacion {

    private val separadores = "[\\s,.:;!¡¿?\\-]*"

    private val prefijoIA = Regex(
        "^\\s*(?:ey|oye|hey|ei)\\s*,?\\s*astra\\b$separadores",
        RegexOption.IGNORE_CASE
    )

    private val prefijoAccion = Regex(
        "^\\s*astra\\b$separadores",
        RegexOption.IGNORE_CASE
    )

    fun analizar(original: String): Orden {
        prefijoIA.find(original)?.let {
            return Orden(Modo.IA, original.substring(it.range.last + 1).trim())
        }
        prefijoAccion.find(original)?.let {
            return Orden(Modo.ACCION, original.substring(it.range.last + 1).trim())
        }
        return Orden(Modo.LIBRE, original.trim())
    }
}
