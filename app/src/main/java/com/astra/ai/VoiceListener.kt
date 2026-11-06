package com.astra.ai

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceListener(
    private val context: Context,
    private val alTerminar: (String) -> Unit,
    private val alCambiarEstado: (String) -> Unit,
    private val alCambiarNivel: (Float) -> Unit = {},
    private val alFallar: () -> Unit = {}
) {

    private var recognizer: SpeechRecognizer? = null
    private var intentActual: Intent? = null
    private var activo = false

    private fun crearIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    private fun crearReconocedor(): SpeechRecognizer? = when {
        Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) ->
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        SpeechRecognizer.isRecognitionAvailable(context) ->
            SpeechRecognizer.createSpeechRecognizer(context)
        else -> null
    }

    private fun mensajeError(codigo: Int) = when (codigo) {
        1, 2 -> "Sin internet. Descarga el idioma sin conexión o escribe tu orden"
        3 -> "Problema con el micrófono"
        4 -> "Error del servicio de voz"
        6 -> "No te oí, intenta de nuevo"
        7 -> "No te entendí"
        8 -> "El reconocimiento está ocupado, intenta de nuevo"
        9 -> "Falta el permiso del micrófono"
        10 -> "Demasiadas peticiones, espera un momento"
        11 -> "El servicio de voz se desconectó"
        12, 13 -> "Falta el idioma sin conexión (código $codigo)"
        else -> "No te entendí (código $codigo)"
    }

    fun empezar() {
        cancelar()
        val r = crearReconocedor()
        if (r == null) {
            alCambiarEstado("Tu teléfono no tiene reconocimiento de voz disponible")
            alFallar()
            return
        }
        recognizer = r
        activo = true
        val intent = crearIntent()
        intentActual = intent

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = alCambiarEstado("Te escucho...")
            override fun onBeginningOfSpeech() = alCambiarEstado("Escuchando...")
            override fun onRmsChanged(rmsdB: Float) = alCambiarNivel(rmsdB)
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = alCambiarEstado("Procesando...")
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onError(error: Int) {
                if (!activo) return
                activo = false
                if ((error == 12 || error == 13) && Build.VERSION.SDK_INT >= 33) {
                    try {
                        recognizer?.triggerModelDownload(intentActual!!)
                        alCambiarEstado("Descargando el idioma sin conexión... intenta de nuevo en un momento")
                    } catch (e: Exception) {
                        alCambiarEstado(mensajeError(error))
                    }
                } else {
                    alCambiarEstado(mensajeError(error))
                }
                alFallar()
            }

            override fun onResults(results: Bundle?) {
                if (!activo) return
                activo = false
                val texto = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (texto.isNullOrBlank()) {
                    alCambiarEstado("No te entendí")
                    alFallar()
                } else {
                    alTerminar(texto)
                }
            }
        })
        r.startListening(intent)
    }

    fun cancelar() {
        activo = false
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (e: Exception) {
        }
        recognizer = null
    }

    fun cerrar() = cancelar()
}
