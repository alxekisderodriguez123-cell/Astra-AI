package com.astra.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceListener(
    private val context: Context,
    private val alTerminar: (String) -> Unit,
    private val alCambiarEstado: (String) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null

    fun empezar() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            alCambiarEstado("Tu teléfono no tiene reconocimiento de voz disponible")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = alCambiarEstado("Te escucho...")
                override fun onBeginningOfSpeech() = alCambiarEstado("Escuchando...")
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() = alCambiarEstado("Procesando...")
                override fun onError(error: Int) = alCambiarEstado("No te entendí (código $error)")
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    val texto = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (texto.isNullOrBlank()) alCambiarEstado("No te entendí")
                    else alTerminar(texto)
                }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }
        recognizer?.startListening(intent)
    }

    fun cerrar() {
        recognizer?.destroy()
        recognizer = null
    }
}
