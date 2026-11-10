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

/** Una combinación de modelo + búsqueda de Google que Astra puede intentar. */
private data class ConfigIA(val modelo: String, val busqueda: Boolean)

/**
 * Cerebro de IA de Astra (Gemini a través de Firebase AI Logic, plan gratuito Spark).
 *
 * Sirve para: conversación básica, preguntas con internet (búsqueda de Google, si el plan la permite)
 * y preguntas sobre el teléfono. NO ejecuta acciones: eso lo hace ActionRouter.
 */
class AstraCerebro(context: Context) {

    companion object {
        private const val TAG = "AstraIA"

        // Se prueban en orden. Si una falla por modelo o por búsqueda no disponible,
        // Astra pasa a la siguiente y recuerda cuál funcionó.
        // Los modelos 2.5 ya no se entregan a proyectos nuevos, por eso no están aquí.
        private val CONFIGS = listOf(
            ConfigIA("gemini-3.5-flash-lite", true),
            ConfigIA("gemini-3.5-flash-lite", false),
            ConfigIA("gemini-3.1-flash-lite", false)
        )

        // Nombre comercial del teléfono (el sistema solo entrega un código como "X6xxx").
        private const val NOMBRE_COMERCIAL = "Infinix Hot 60 Pro"

        private const val MAX_TURNOS = 8          // turnos de conversación que recuerda
        private const val TIMEOUT_MS = 30_000L    // espera máxima por respuesta
    }

    private val appContext = context.applicationContext
    private val dispositivo = DeviceEngine(appContext)
    private val historial = ArrayList<Content>()

    @Volatile
    private var indiceOk = 0

    /** Detalle técnico del último fallo o del modo usado (vacío si todo salió normal). */
    @Volatile
    var ultimoDetalle: String = ""
        private set

    /** Siempre devuelve un texto listo para mostrar y decir en voz alta. */
    suspend fun responder(pregunta: String): String = withContext(Dispatchers.Default) {
        ultimoDetalle = ""
        try {
            withTimeout(TIMEOUT_MS) {
                var ultimoError: Exception? = null
                var i = indiceOk

                while (i < CONFIGS.size) {
                    val cfg = CONFIGS[i]
                    try {
                        val chat = crearModelo(cfg).startChat(copiaHistorial())
                        val respuesta = chat.sendMessage(pregunta)
                        val texto = limpiar(respuesta.text.orEmpty())

                        indiceOk = i
                        if (i > 0) {
                            ultimoDetalle = cfg.modelo +
                                if (cfg.busqueda) "" else " (sin búsqueda web)"
                        }

                        return@withTimeout if (texto.isBlank()) {
                            "No encontré una respuesta para eso."
                        } else {
                            guardar(pregunta, texto)
                            texto
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Falló ${cfg.modelo} (búsqueda=${cfg.busqueda})", e)
                        ultimoError = e
                        if (!esRecuperable(e, cfg)) throw e
                        i++
                    }
                }

                throw ultimoError ?: IllegalStateException("Sin configuración de IA")
            }
        } catch (e: TimeoutCancellationException) {
            ultimoDetalle = "Timeout"
            "La IA tardó demasiado. Intenta de nuevo."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error de IA", e)
            ultimoDetalle = "${e.javaClass.simpleName}: ${e.message.orEmpty().take(160)}"
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

    private fun crearModelo(cfg: ConfigIA) = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = cfg.modelo,
            generationConfig = generationConfig {
                maxOutputTokens = 300
                temperature = 0.6f
            },
            tools = if (cfg.busqueda) listOf(Tool.googleSearch()) else null,
            systemInstruction = content { text(instrucciones(cfg.busqueda)) }
        )

    private fun instrucciones(conBusqueda: Boolean): String {
        val datosActuales = if (conBusqueda) {
            "- Para datos actuales (clima, noticias, resultados, precios, fechas) usa la búsqueda de Google."
        } else {
            "- No tienes acceso a internet. Si piden datos actuales (clima, noticias, resultados, precios), di con honestidad que ahora no puedes consultarlos."
        }

        return """
Eres Astra, el asistente de voz de este teléfono Android. Hablas con tu dueño.

REGLAS:
- Responde siempre en español, en 1 a 3 frases cortas. Tus respuestas se leen en voz alta.
- No uses markdown, listas, emojis, asteriscos ni enlaces.
- Si no sabes algo o no estás seguro, dilo con honestidad. No inventes datos.
$datosActuales
- Tú solo conversas y respondes. NO puedes ejecutar acciones del teléfono (llamar, mensajes, alarmas, temporizadores, linterna, volumen, brillo, música, mapas, abrir apps). Si te lo piden, di que lo pida como orden directa, por ejemplo: "Astra, enciende la linterna".
- Para preguntas sobre el teléfono usa solo la ficha de abajo. Si el dato no aparece, di que no lo sabes.

NOMBRE COMERCIAL DEL TELÉFONO: $NOMBRE_COMERCIAL

${dispositivo.getAssistantContext()}
DATOS ACTUALES:
${datosEnVivo()}
""".trimIndent()
    }

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
    // Texto y errores
    // ---------------------------

    /** Quita lo que suena mal al leerlo en voz alta. */
    private fun limpiar(texto: String): String = texto
        .replace(Regex("https?://\\S+"), "")
        .replace(Regex("(?m)^\\s*[-•]\\s+"), "")
        .replace(Regex("[*_`#>~]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun textoError(e: Exception): String =
        "${e.javaClass.simpleName} ${e.message.orEmpty()}".lowercase()

    /**
     * Decide si vale la pena probar la siguiente opción.
     * Nunca se reintenta con App Check o sin internet. El límite de cuota solo se reintenta
     * si la opción usaba búsqueda (la búsqueda puede tener su propio límite).
     */
    private fun esRecuperable(e: Exception, cfg: ConfigIA): Boolean {
        val t = textoError(e)

        val appCheckOInternet = "app check" in t || "appcheck" in t || "app_check" in t ||
            "unknownhost" in t || "unable to resolve" in t || "network" in t ||
            "timeout" in t || "ioexception" in t
        if (appCheckOInternet) return false

        val cuota = "quota" in t || "429" in t || "exhausted" in t
        return if (cuota) cfg.busqueda else true
    }

    private fun mensajeError(e: Exception): String {
        val todo = textoError(e)

        return when {
            "app check" in todo || "appcheck" in todo || "app_check" in todo ->
                "Falta registrar el token de App Check en Firebase."

            "quota" in todo || "429" in todo || "exhausted" in todo ->
                "Llegué al límite gratuito de la IA. Intenta de nuevo en un minuto."

            "no longer available" in todo || "404" in todo || "not found" in todo ->
                "El modelo de IA ya no está disponible. Hay que cambiarlo."

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

            else -> "No pude conectar con la IA."
        }
    }
}
