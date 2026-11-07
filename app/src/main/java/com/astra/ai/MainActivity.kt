
package com.astra.ai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.inputmethod.InputMethodManager
import com.astra.ai.chat.ChatScreen
import com.astra.ai.chat.ChatStore
import com.google.firebase.Firebase
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MainActivity : Activity() {

    private lateinit var speaker: Speaker
    private lateinit var voz: VoiceListener
    private lateinit var router: ActionRouter
    private lateinit var chat: ChatScreen
    private lateinit var chatStore: ChatStore

    private var ocupado = false
    private var hablando = false
    private var vozActiva = true

    private val permisos = Permisos.lista()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializar Firebase y App Check
Firebase.initialize(context = this)

Firebase.appCheck.installAppCheckProviderFactory(
    DebugAppCheckProviderFactory.getInstance()
)

        window.statusBarColor = Color.parseColor("#050814")
        window.navigationBarColor = Color.parseColor("#0E1A3F")

        val prefs = getSharedPreferences("astra", MODE_PRIVATE)
        vozActiva = prefs.getBoolean("voz_activa", true)

        router = ActionRouter(this)
        chatStore = ChatStore(this)

        speaker = Speaker(this) { h ->
            runOnUiThread {
                alCambiarHabla(h)
            }
        }

        voz = VoiceListener(
            this,

            alTerminar = { texto ->
                runOnUiThread {
                    procesar(texto)
                }
            },

            alCambiarEstado = { estado ->
                runOnUiThread {
                    chat.setStatus(estado)
                }
            },

            alCambiarNivel = { nivel ->
                runOnUiThread {
                    chat.setOrbLevel(nivel)
                }
            },

            alFallar = {
                runOnUiThread {
                    terminarActividad()
                }
            }
        )

        chat = ChatScreen(
            context = this,
            store = chatStore,

            onSend = { texto ->
                if (ocupado) cancelar()
                procesar(texto)
            },

            onVoice = {
                if (ocupado) {
                    cancelar()
                } else {
                    pedirYEscuchar()
                }
            }
        )

        val root = FrameLayout(this)

        root.addView(
            FondoView(this),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            chat,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        if (!prefs.getBoolean("permisos_v2", false)) {
            prefs.edit()
                .putBoolean("permisos_v2", true)
                .apply()

            requestPermissions(permisos, 1)
        }
    }

    // ---------------------------
    // Procesamiento de mensajes
    // ---------------------------

    private fun procesar(texto: String) {

        ocupado = false
        chat.setStatus("Procesando...")

        chat.addMessage("user", texto)

        val respuesta = try {
            router.ejecutar(texto)
                ?: "Todavía no sé hacer eso. Puedes preguntarme otra cosa."
        } catch (e: Exception) {
            "No pude completar esa acción. Inténtalo nuevamente."
        }

        chat.addMessage("assistant", respuesta)

        if (vozActiva && speaker.hablar(respuesta)) {
            ocupado = true
            chat.setOrbState(OrbState.HABLANDO)
            chat.setStatus("Astra está respondiendo...")
        } else {
            ocupado = false
            chat.setOrbState(OrbState.QUIETO)
            chat.setStatus("Listo")
        }
    }

    // ---------------------------
    // Voz
    // ---------------------------

    private fun alCambiarHabla(h: Boolean) {

        hablando = h
        MusicService.atenuar(h)

        if (h) {
            chat.setOrbState(OrbState.HABLANDO)
            chat.setStatus("Astra está hablando...")
        } else {
            ocupado = false
            chat.setOrbState(OrbState.QUIETO)
            chat.setStatus("Listo")
        }
    }

    private fun pedirYEscuchar() {

        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            empezarAEscuchar()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                2
            )
        }
    }

    private fun empezarAEscuchar() {

        ocupado = true

        chat.setOrbState(OrbState.ESCUCHANDO)
        chat.setStatus("Te escucho...")

        voz.empezar()
    }

    private fun cancelar() {

        voz.cancelar()
        speaker.callar()

        hablando = false
        ocupado = false

        chat.setOrbState(OrbState.QUIETO)
        chat.setStatus("Cancelado")
    }

    private fun terminarActividad() {

        ocupado = false

        chat.setOrbState(OrbState.QUIETO)
    }

    // ---------------------------
    // Configuración de voz
    // ---------------------------

    fun establecerVozActiva(activa: Boolean) {

        vozActiva = activa

        getSharedPreferences("astra", MODE_PRIVATE)
            .edit()
            .putBoolean("voz_activa", activa)
            .apply()

        if (!activa && hablando) {
            speaker.callar()
            hablando = false
            ocupado = false

            chat.setOrbState(OrbState.QUIETO)
            chat.setStatus("Solo texto")
        }
    }

    // ---------------------------
    // Permisos
    // ---------------------------

    override fun onRequestPermissionsResult(
        code: Int,
        permisos: Array<out String>,
        resultados: IntArray
    ) {
        super.onRequestPermissionsResult(
            code,
            permisos,
            resultados
        )

        if (code == 2) {

            if (
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                empezarAEscuchar()
            } else {
                chat.setStatus(
                    "Activa el permiso del micrófono en Ajustes"
                )
            }
        }
    }

    override fun onDestroy() {

        voz.cerrar()
        speaker.cerrar()
        chatStore.close()

        super.onDestroy()
    }
}
