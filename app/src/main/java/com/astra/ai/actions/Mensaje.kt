package com.astra.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri

class Mensaje(
    private val context: Context,
    private val original: () -> String
) : Accion {

    private val verbos = listOf("manda", "mandale", "envia", "enviale", "escribe", "escribele", "dile", "avisale")
    private val relleno = setOf(
        "manda", "mandale", "envia", "enviale", "escribe", "escribele", "dile", "avisale",
        "un", "una", "mensaje", "mensajito", "whatsapp", "wasap", "guasap", "sms", "texto",
        "a", "al", "le", "para", "por", "en"
    )

    override fun intentar(orden: String): String? {
        val primera = orden.split(" ").firstOrNull() ?: return null
        if (primera !in verbos) return null

        // Separamos "a quién" de "qué decir" usando el texto original (con tildes y mayúsculas)
        val orig = original().trim()
        val corte = Regex(
            """\b(diciendo|que diga|que dice|que le diga|con el mensaje|dile que|que)\b""",
            RegexOption.IGNORE_CASE
        ).find(orig)
        val antes = if (corte != null) orig.substring(0, corte.range.first) else orig
        val mensaje = if (corte != null) {
            orig.substring(corte.range.last + 1).trim().trimEnd('.')
                .replaceFirstChar { it.uppercase() }
        } else ""

        val antesNorm = normalizar(antes).replace("mensaje de texto", " ")
        val nombre = antesNorm.split(" ").filter { it.isNotEmpty() && it !in relleno }.joinToString(" ")
        if (nombre.isEmpty()) return "¿A quién se lo mando?"

        val usaWhatsApp = when {
            antesNorm.contains("whatsapp") || antesNorm.contains("wasap") || antesNorm.contains("guasap") -> true
            antesNorm.contains("sms") || normalizar(antes).contains("mensaje de texto") -> false
            else -> Config.CANAL_POR_DEFECTO == "whatsapp"
        }

        val destinatario: String
        val numero: String
        if (nombre.replace(" ", "").matches(Regex("\\d{6,15}"))) {
            destinatario = nombre.replace(" ", "")
            numero = destinatario
        } else {
            if (!Contactos.tienePermiso(context)) {
                return "Necesito permiso de contactos. Actívalo en Ajustes, Aplicaciones, Astra, Permisos"
            }
            val contacto = Contactos.buscar(context, nombre)
                ?: return "No encontré a $nombre en tus contactos"
            destinatario = contacto.nombre
            numero = contacto.numero
        }

        return try {
            if (usaWhatsApp) {
                val digitos = paraWhatsApp(numero)
                val url = "https://wa.me/$digitos" + if (mensaje.isNotEmpty()) "?text=" + Uri.encode(mensaje) else ""
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                val aviso = if (Config.CODIGO_PAIS.isBlank() && !numero.trim().startsWith("+"))
                    ". Si no abre el chat, configura el código de país" else ""
                "Abriendo WhatsApp con $destinatario. Toca enviar$aviso"
            } else {
                val limpio = numero.replace(Regex("[^+0-9]"), "")
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$limpio"))
                    .putExtra("sms_body", mensaje)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Abriendo el mensaje para $destinatario. Toca enviar"
            }
        } catch (e: Exception) {
            "No pude abrir la aplicación de mensajes"
        }
    }

    private fun paraWhatsApp(numero: String): String {
        val limpio = numero.replace(Regex("[^+0-9]"), "")
        if (limpio.startsWith("+")) return limpio.drop(1)
        val sinCero = limpio.trimStart('0')
        val codigo = Config.CODIGO_PAIS
        return if (codigo.isNotBlank() && !(sinCero.length >= 12 && sinCero.startsWith(codigo))) {
            codigo + sinCero
        } else sinCero
    }
}
