package com.astra.ai

import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.astra.ai.device.DeviceEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cerebro de IA de Astra (Gemini a través de Firebase AI Logic, plan gratuito Spark).
 *
 * Sirve para: conversación básica, preguntas con internet (búsqueda de Google)
 * y preguntas sobre el teléfono. NO ejecuta acciones: eso lo hace ActionRouter.
 */
class AstraCerebro(context: Context) {

    companion object {
        private const val TAG = "AstraIA"

        // Modelo gratuito, rápido y sin "pensar" de más (respuestas cortas).
        // Si algún día Google lo retira, cambia solo este nombre.
        private const val MODELO = "gemini-2.5-flash-lite"

        // Nombre comercial del teléfono (el sistema solo entrega un código como "X6xxx").
        private const val NOMBRE_COMERCIAL = "Infinix Hot 60 Pro"

        // Permite a Gemini buscar en internet datos actuales (clima, noticias, resultados...).
        private const val USAR_BUSQUEDA = true

        private const val MAX_TURNOS = 8          // turnos de conversación que recuerda
        private const val TIMEOUT_MS = 25_000L    // espera máxima por respuesta
    }

    private val appContext = context.applicationContext
    private val dispositivo = DeviceEngine(appContext)
    private val historial = ArrayList<Content>()

    /** Detalle técnico del último fallo (vacío si todo salió bien). Sirve para depurar. */
    @Volatile
    var ultimoDetalle: String = ""
        private set

    /** Siempre devuelve un texto listo para mostrar y decir en voz alta. */
    suspend fun responder(pregunta: String): String = withContext(Dispatchers.Default) {
        ultimoDetalle = ""
        try {
            withTimeout(TIMEOUT_MS) {
                val chat = crearModelo().startChat(copiaHistorial())
                val respuesta = chat.sendMessage(pregunta)
                val texto = limpiar(respuesta.text.orEmpty())
                if (texto.isBlank()) {
                    "No encontré una respuesta para eso."
                } else {
                    guardar(pregunta, texto)
                    texto
                }
            }
        } catch (e: TimeoutCancellationException) {
            ultimoDetalle = "Timeout"
            "La IA tardó demasiado. Intenta de nuevo."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error de IA", e)
            ultimoDetalle = "${e.javaClass.simpleName}: ${e.message.orEmpty().take(140)}"
            mensajeError(e)
        }
    }

    /** Olvida la conversación actual. */
    fun reiniciar() {
        synchronized(historial) { historial.clear() }
    }

    // ---------------------------
    // Modelo
    // ---------------------------

    private fun crearModelo() = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = MODELO,
            generationConfig = generationConfig {
                maxOutputTokens = 256
                temperature = 0.6f
            },
            tools = if (USAR_BUSQUEDA) listOf(Tool.googleSearch()) else null,
            systemInstruction = content { text(instrucciones()) }
        )

    private fun instrucciones(): String = """
Eres Astra, el asistente de voz de este teléfono Android. Hablas con tu dueño.

REGLAS:
- Responde siempre en español, en 1 a 3 frases cortas. Tus respuestas se leen en voz alta.
- No uses markdown, listas, emojis, asteriscos ni enlaces.
- Si no sabes algo o no estás seguro, dilo con honestidad. No inventes datos.
- Para datos actuales (clima, noticias, resultados, precios, fechas) usa la búsqueda de Google.
- Tú solo conversas y respondes. NO puedes ejecutar acciones del teléfono (llamar, mensajes, alarmas, temporizadores, linterna, volumen, brillo, música, mapas, abrir apps). Si te lo piden, di que lo pida como orden directa, por ejemplo: "Astra, enciende la linterna".
- Para preguntas sobre el teléfono usa solo la ficha de abajo. Si el dato no aparece, di que no lo sabes.

NOMBRE COMERCIAL DEL TELÉFONO: $NOMBRE_COMERCIAL

${dispositivo.getAssistantContext()}
DATOS ACTUALES:
${datosEnVivo()}
""".trimIndent()

    private fun datosEnVivo(): String {
        val sb = StringBuilder()

        try {
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val porcentaje = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val intent = appContext.registerReceiver(
                null,
                IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val estado = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val cargando = estado == BatteryManager.BATTERY_STATUS_CHARGING ||
                estado == BatteryManager.BATTERY_STATUS_FULL
            sb.appendLine("Batería: $porcentaje %" + if (cargando) " (cargando)" else "")
        } catch (e: Exception) {
        }

        try {
            val st = StatFs(Environment.getDataDirectory().path)
            val gb = 1024.0 * 1024.0 * 1024.0
            sb.appendLine(
                "Almacenamiento: %.1f GB libres de %.1f GB".format(
                    Locale.US,
                    st.availableBytes / gb,
                    st.totalBytes / gb
                )
            )
        } catch (e: Exception) {
        }

        try {
            val formato = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy, HH:mm", Locale("es"))
            sb.appendLine("Fecha y hora: ${formato.format(Date())}")
        } catch (e: Exception) {
        }

        return sb.toString().trim()
    }

    // ---------------------------
    // Historial
    // ---------------------------

    private fun copiaHistorial(): List<Content> =
        synchronized(historial) { historial.toList() }

    private fun guardar(pregunta: String, respuesta: String) {
        synchronized(historial) {
            historial.add(content("user") { text(pregunta) })
            historial.add(content("model") { text(respuesta) })
            while (historial.size > MAX_TURNOS * 2) {
                historial.removeAt(0)
                historial.removeAt(0)
            }
        }
    }

    // ---------------------------
    // Texto
    // ---------------------------

    /** Quita lo que suena mal al leerlo en voz alta. */
    private fun limpiar(texto: String): String = texto
        .replace(Regex("https?://\\S+"), "")
        .replace(Regex("(?m)^\\s*[-•]\\s+"), "")
        .replace(Regex("[*_`#>~]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun mensajeError(e: Exception): String {
        val clase = e.javaClass.simpleName.lowercase()
        val msg = e.message.orEmpty().lowercase()
        val todo = "$clase $msg"

        return when {
            "app check" in todo || "appcheck" in todo || "app_check" in todo ->
                "Falta registrar el token de App Check en Firebase."

            "quota" in todo || "429" in todo || "exhausted" in todo ->
                "Llegué al límite gratuito de la IA. Intenta de nuevo en un minuto."

            "servicedisabled" in todo || "403" in todo || "permission" in todo ||
                "not enabled" in todo || "has not been used" in todo || "disabled" in todo ->
                "Firebase rechazó la solicitud de la IA. Revisa App Check y que la IA esté activada."

            "apikey" in todo || "api key" in todo || "api_key" in todo ->
                "La clave de la IA no es válida."

            "location" in todo ->
                "La IA no está disponible en tu ubicación."

            "unknownhost" in todo || "unable to resolve" in todo || "connect" in todo ||
                "network" in todo || "timeout" in todo || "ioexception" in todo ->
                "No tengo internet ahora mismo."

            "404" in todo || "not found" in todo ->
                "El modelo de IA no está disponible."

            else -> "No pude conectar con la IA."
        }
    }
}
