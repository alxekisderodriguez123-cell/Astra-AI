package com.astra.ai

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var speaker: Speaker
    private lateinit var voz: VoiceListener
    private lateinit var orb: OrbView
    private lateinit var estado: TextView
    private lateinit var escuchado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density

        orb = OrbView(this).apply {
            layoutParams = LinearLayout.LayoutParams((300 * dp).toInt(), (300 * dp).toInt())
        }
        val titulo = TextView(this).apply {
            text = "ASTRA AI"
            textSize = 22f
            letterSpacing = 0.2f
            setTextColor(Color.parseColor("#DCE6FF"))
            gravity = Gravity.CENTER
        }
        estado = TextView(this).apply {
            text = "Toca el botón para hablar"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, (24 * dp).toInt(), 0, (8 * dp).toInt())
        }
        escuchado = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#7FE8D8"))
            gravity = Gravity.CENTER
            setPadding((32 * dp).toInt(), (8 * dp).toInt(), (32 * dp).toInt(), (32 * dp).toInt())
        }
        val boton = Button(this).apply {
            text = "🎤 Hablar"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 100f * dp
                setColor(Color.parseColor("#6C5CE7"))
            }
            setPadding((36 * dp).toInt(), 0, (36 * dp).toInt(), 0)
            setOnClickListener { pedirYEscuchar() }
        }

        speaker = Speaker(this) { hablando ->
            runOnUiThread {
                orb.ponerEstado(if (hablando) OrbState.HABLANDO else OrbState.QUIETO)
                if (!hablando) estado.text = "Toca el botón para hablar"
            }
        }
        voz = VoiceListener(
            this,
            alTerminar = { texto ->
                escuchado.text = "Dijiste: $texto"
                estado.text = "Listo"
                speaker.hablar("Dijiste $texto")
            },
            alCambiarEstado = { estado.text = it },
            alCambiarNivel = { orb.ponerNivel(it) },
            alFallar = { orb.ponerEstado(OrbState.QUIETO) }
        )

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1B2435"))
            addView(orb)
            addView(titulo)
            addView(estado)
            addView(escuchado)
            addView(boton)
        })
    }

    private fun pedirYEscuchar() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            orb.ponerEstado(OrbState.ESCUCHANDO)
            voz.empezar()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onRequestPermissionsResult(code: Int, permisos: Array<out String>, resultados: IntArray) {
        super.onRequestPermissionsResult(code, permisos, resultados)
        if (code == 1 && resultados.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            orb.ponerEstado(OrbState.ESCUCHANDO)
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
