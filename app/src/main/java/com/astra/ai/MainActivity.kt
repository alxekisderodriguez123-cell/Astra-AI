package com.astra.ai

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var speaker: Speaker
    private lateinit var voz: VoiceListener
    private lateinit var estado: TextView
    private lateinit var escuchado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = Speaker(this)
        voz = VoiceListener(
            this,
            alTerminar = { texto ->
                escuchado.text = "Dijiste: $texto"
                estado.text = "Listo"
                speaker.hablar("Dijiste $texto")
            },
            alCambiarEstado = { estado.text = it }
        )

        val titulo = TextView(this).apply {
            text = "Astra"
            textSize = 40f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        estado = TextView(this).apply {
            text = "Toca el botón para hablar"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 20)
        }
        escuchado = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 60)
        }
        val boton = Button(this).apply {
            text = "🎤 Hablar"
            setOnClickListener { pedirYEscuchar() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0A0A14"))
            addView(titulo)
            addView(estado)
            addView(escuchado)
            addView(boton)
        })
    }

    private fun pedirYEscuchar() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voz.empezar()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onRequestPermissionsResult(code: Int, permisos: Array<out String>, resultados: IntArray) {
        super.onRequestPermissionsResult(code, permisos, resultados)
        if (code == 1 && resultados.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            voz.empezar()
        } else {
            estado.text = "Sin permiso de micrófono"
        }
    }

    override fun onDestroy() {
        voz.cerrar()
        speaker.cerrar()
        super.onDestroy()
    }
}
