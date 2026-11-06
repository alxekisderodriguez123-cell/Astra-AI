package com.astra.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class Speaker(
    context: Context,
    private val alCambiar: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)
    private var listo = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "ES")
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = alCambiar(true)
                override fun onDone(utteranceId: String?) = alCambiar(false)

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = alCambiar(false)
            })
            listo = true
        }
    }

    /** Devuelve false si todavía no puede hablar. */
    fun hablar(texto: String): Boolean {
        if (!listo) return false
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "astra")
        return true
    }

    fun callar() {
        if (listo) tts.stop()
    }

    fun cerrar() {
        tts.stop()
        tts.shutdown()
    }
}
