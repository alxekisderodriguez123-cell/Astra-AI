package com.astra.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)
    private var listo = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "ES")
            listo = true
        }
    }

    fun hablar(texto: String) {
        if (listo) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "astra")
    }

    fun cerrar() {
        tts.stop()
        tts.shutdown()
    }
}
