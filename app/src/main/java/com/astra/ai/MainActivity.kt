package com.astra.ai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var speaker: Speaker
    private lateinit var voz: VoiceListener
    private lateinit var router: ActionRouter
    private lateinit var orb: OrbView
    private lateinit var hud: HudView
    private lateinit var estado: TextView
    private lateinit var tuTexto: TextView
    private lateinit var astraTexto: TextView
    private lateinit var botonMic: TextView
    private lateinit var botonVoz: TextView
    private lateinit var entrada: EditText

    private var densidad = 1f
    private var ocupado = false
    private var hablando = false
    private var vozActiva = true
    private val textoInicial = "Toca el micrófono o escribe una orden"

    private val permisos = Permisos.lista()

    private fun dp(v: Float) = (v * densidad).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        densidad = resources.displayMetrics.density
        window.statusBarColor = Color.parseColor("#050814")
        window.navigationBarColor = Color.parseColor("#0E1A3F")

        val prefs = getSharedPreferences("astra", MODE_PRIVATE)
        vozActiva = prefs.getBoolean("voz_activa", true)

        router = ActionRouter(this)
        speaker = Speaker(this) { h -> runOnUiThread { alCambiarHabla(h) } }
        voz = VoiceListener(
            this,
            alTerminar = { texto -> procesar(texto) },
            alCambiarEstado = { estado.text = it },
            alCambiarNivel = { orb.ponerNivel(it) },
            alFallar = { terminarActividad() }
        )

        construirPantalla()

        if (!prefs.getBoolean("permisos_v2", false)) {
            prefs.edit().putBoolean("permisos_v2", true).apply()
            requestPermissions(permisos, 1)
        }
    }

    private fun construirPantalla() {
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

        // --- Barra superior ---
        val titulo = TextView(this).apply {
            text = "ASTRA AI"
            textSize = 18f
            letterSpacing = 0.25f
            setTextColor(Tema.texto)
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        }
        botonVoz = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            background = Tema.panel(densidad, 22f)
            layoutParams = LinearLayout.LayoutParams(dp(44f), dp(44f))
            setOnClickListener { alternarVoz() }
        }
        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titulo)
            addView(botonVoz)
        }

        // --- Orbe con HUD ---
        val alto = resources.displayMetrics.heightPixels
        val tamOrbe = minOf(dp(300f), (alto * 0.36f).toInt())
        hud = HudView(this)
        orb = OrbView(this)
        val marcoOrbe = FrameLayout(this).apply {
            addView(hud, FrameLayout.LayoutParams(match, match))
            addView(orb, FrameLayout.LayoutParams(match, match))
        }

        // --- Estado ---
        estado = TextView(this).apply {
            text = textoInicial
            textSize = 13f
            letterSpacing = 0.05f
            gravity = Gravity.CENTER
            setTextColor(Tema.cian)
            background = Tema.panel(densidad, 20f)
            setPadding(dp(18f), dp(8f), dp(18f), dp(8f))
        }

        // --- Conversación ---
        tuTexto = TextView(this).apply {
            textSize = 14f
            setTextColor(Tema.textoSuave)
        }
        astraTexto = TextView(this).apply {
            text = "Hola, soy Astra. Dime o escribe una orden."
            textSize = 17f
            setTextColor(Tema.texto)
            setPadding(0, dp(6f), 0, 0)
        }
        val panelChat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Tema.panel(densidad, 20f, borde = Tema.violeta, bordeAlpha = 120)
            setPadding(dp(18f), dp(14f), dp(18f), dp(14f))
            minimumHeight = dp(96f)
            addView(tuTexto)
            addView(astraTexto)
        }

        // --- Micrófono / cancelar ---
        botonMic = TextView(this).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { if (ocupado) cancelar() else pedirYEscuchar() }
        }

        // --- Barra de escribir ---
        entrada = EditText(this).apply {
            hint = "Escribe una orden..."
            setHintTextColor(Tema.textoSuave)
            setTextColor(Tema.texto)
            textSize = 16f
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = Tema.panel(densidad, 26f)
            setPadding(dp(20f), 0, dp(20f), 0)
            setOnEditorActionListener { _, accion, _ ->
                if (accion == EditorInfo.IME_ACTION_SEND) { enviarTexto(); true } else false
            }
        }
        val botonEnviar = TextView(this).apply {
            text = "➤"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(Tema.violeta, Tema.azul)
            ).apply { shape = GradientDrawable.OVAL }
            setOnClickListener { enviarTexto() }
        }
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(entrada, LinearLayout.LayoutParams(0, dp(52f), 1f))
            addView(
                botonEnviar,
                LinearLayout.LayoutParams(dp(52f), dp(52f)).apply { leftMargin = dp(10f) }
            )
        }

        // --- Contenido ---
        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20f), dp(16f), dp(20f), dp(16f))
            addView(barra, LinearLayout.LayoutParams(match, wrap))
            addView(marcoOrbe, LinearLayout.LayoutParams(tamOrbe, tamOrbe).apply { topMargin = dp(8f) })
            addView(estado, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = dp(12f) })
            addView(panelChat, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(14f) })
            addView(View(this@MainActivity), LinearLayout.LayoutParams(match, 0, 1f))
            addView(botonMic, LinearLayout.LayoutParams(dp(76f), dp(76f)).apply { topMargin = dp(12f) })
            addView(fila, LinearLayout.LayoutParams(match, wrap).apply { topMargin = dp(14f) })
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(contenido, ViewGroup.LayoutParams(match, match))
        }

        setContentView(FrameLayout(this).apply {
            addView(FondoView(this@MainActivity), FrameLayout.LayoutParams(match, match))
            addView(scroll, FrameLayout.LayoutParams(match, match))
        })

        actualizarMic()
        actualizarBotonVoz()
    }

    // ---------- Estado visual ----------

    private fun estadoOrbe(e: OrbState) {
        orb.ponerEstado(e)
        hud.ponerEstado(e)
    }

    private fun actualizarMic() {
        botonMic.text = if (ocupado) "✕" else "🎤"
        val colores = if (ocupado) intArrayOf(Tema.peligro, Color.parseColor("#B8325A"))
        else intArrayOf(Tema.violeta, Tema.azul)
        botonMic.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, colores).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(2f), Color.argb(150, 79, 216, 196))
        }
    }

    private fun actualizarBotonVoz() {
        botonVoz.text = if (vozActiva) "🔊" else "🔇"
    }

    private fun alCambiarHabla(h: Boolean) {
        hablando = h
        if (h) {
            estadoOrbe(OrbState.HABLANDO)
        } else {
            ocupado = false
            estadoOrbe(OrbState.QUIETO)
            estado.text = textoInicial
        }
        actualizarMic()
    }

    private fun terminarActividad() {
        ocupado = false
        estadoOrbe(OrbState.QUIETO)
        actualizarMic()
    }

    // ---------- Acciones ----------

    private fun procesar(texto: String) {
        tuTexto.text = "Tú: $texto"
        val respuesta = try {
            router.ejecutar(texto) ?: "Todavía no sé hacer eso"
        } catch (e: Exception) {
            "Algo salió mal al ejecutar esa orden"
        }
        astraTexto.text = respuesta
        estado.text = "Listo"
        if (vozActiva && speaker.hablar(respuesta)) {
            ocupado = true
        } else {
            ocupado = false
            estadoOrbe(OrbState.QUIETO)
        }
        actualizarMic()
    }

    private fun enviarTexto() {
        val texto = entrada.text.toString().trim()
        if (texto.isEmpty()) return
        entrada.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(entrada.windowToken, 0)
        if (ocupado) {
            voz.cancelar()
            speaker.callar()
            hablando = false
            ocupado = false
        }
        procesar(texto)
    }

    private fun cancelar() {
        voz.cancelar()
        speaker.callar()
        hablando = false
        ocupado = false
        estadoOrbe(OrbState.QUIETO)
        estado.text = "Cancelado"
        actualizarMic()
    }

    private fun alternarVoz() {
        vozActiva = !vozActiva
        getSharedPreferences("astra", MODE_PRIVATE).edit().putBoolean("voz_activa", vozActiva).apply()
        if (!vozActiva && hablando) {
            speaker.callar()
            hablando = false
            ocupado = false
            estadoOrbe(OrbState.QUIETO)
            actualizarMic()
        }
        estado.text = if (vozActiva) "Voz activada" else "Solo texto"
        actualizarBotonVoz()
    }

    private fun pedirYEscuchar() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            empezarAEscuchar()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2)
        }
    }

    private fun empezarAEscuchar() {
        ocupado = true
        estadoOrbe(OrbState.ESCUCHANDO)
        actualizarMic()
        voz.empezar()
    }

    override fun onRequestPermissionsResult(code: Int, permisos: Array<out String>, resultados: IntArray) {
        super.onRequestPermissionsResult(code, permisos, resultados)
        if (code == 2) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                empezarAEscuchar()
            } else {
                estado.text = "Sin permiso de micrófono. Actívalo en Ajustes > Aplicaciones > Astra"
            }
        }
    }

    override fun onDestroy() {
        voz.cerrar()
        speaker.cerrar()
        super.onDestroy()
    }
}
