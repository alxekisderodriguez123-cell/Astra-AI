package com.astra.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder

class MusicService : Service() {

    companion object {
        const val CANAL = "astra_musica"
        const val ID_NOTIF = 77
        const val A_INICIAR = "com.astra.ai.INICIAR"
        const val A_TOGGLE = "com.astra.ai.TOGGLE"
        const val A_SIGUIENTE = "com.astra.ai.SIGUIENTE"
        const val A_ANTERIOR = "com.astra.ai.ANTERIOR"
        const val A_DETENER = "com.astra.ai.DETENER"

        @Volatile
        var instancia: MusicService? = null
        private var cola: List<Biblioteca.Pista> = emptyList()
        private var indice = 0

        fun reproducir(context: Context, pistas: List<Biblioteca.Pista>, desde: Int = 0) {
            if (pistas.isEmpty()) return
            cola = pistas
            indice = desde.coerceIn(0, pistas.size - 1)
            context.startForegroundService(
                Intent(context, MusicService::class.java).setAction(A_INICIAR)
            )
        }

        fun hayMusica(): Boolean = instancia != null
        fun sonando(): Boolean = instancia?.estaSonando() == true
        fun alternar() { instancia?.alternarPausa() }
        fun siguiente() { instancia?.saltar(1) }
        fun anterior() { instancia?.saltar(-1) }
        fun detener() { instancia?.parar() }
        fun atenuar(si: Boolean) { instancia?.ponerAtenuado(si) }
        fun pistaActual(): Biblioteca.Pista? = instancia?.actual()
    }

    private var mp: MediaPlayer? = null
    private var activo = false
    private var atenuado = false
    private var bajoPorFoco = false
    private var pausadoPorFoco = false
    private var fallos = 0
    private lateinit var audio: AudioManager
    private var foco: AudioFocusRequest? = null

    private val atributos = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val oyenteFoco = AudioManager.OnAudioFocusChangeListener { cambio ->
        when (cambio) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (estaSonando()) { mp?.pause(); mostrarNotificacion() }
                pausadoPorFoco = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (estaSonando()) { mp?.pause(); pausadoPorFoco = true; mostrarNotificacion() }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                bajoPorFoco = true; aplicarVolumen()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                bajoPorFoco = false; aplicarVolumen()
                if (pausadoPorFoco) { pausadoPorFoco = false; mp?.start(); mostrarNotificacion() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instancia = this
        activo = true
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val canal = NotificationChannel(CANAL, "Música de Astra", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(canal)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mostrarNotificacion()
        when (intent?.action) {
            A_INICIAR -> reproducirIndice(indice)
            A_TOGGLE -> alternarPausa()
            A_SIGUIENTE -> saltar(1)
            A_ANTERIOR -> saltar(-1)
            A_DETENER -> parar()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        activo = false
        liberar()
        instancia = null
        super.onDestroy()
    }

    // ---------- Reproducción ----------

    private fun actual(): Biblioteca.Pista? = cola.getOrNull(indice)

    private fun estaSonando(): Boolean = try { mp?.isPlaying == true } catch (e: IllegalStateException) { false }

    private fun reproducirIndice(i: Int) {
        if (cola.isEmpty()) { parar(); return }
        indice = ((i % cola.size) + cola.size) % cola.size
        liberar()
        pedirFoco()
        val pista = cola[indice]
        val nuevo = MediaPlayer()
        try {
            nuevo.setAudioAttributes(atributos)
            nuevo.setDataSource(this, pista.uri)
            nuevo.setOnPreparedListener {
                fallos = 0
                it.start()
                aplicarVolumen()
                mostrarNotificacion()
            }
            nuevo.setOnCompletionListener { saltar(1) }
            nuevo.setOnErrorListener { _, _, _ ->
                fallos++
                if (fallos < cola.size) saltar(1) else parar()
                true
            }
            mp = nuevo
            nuevo.prepareAsync()
        } catch (e: Exception) {
            nuevo.release()
            mp = null
            fallos++
            if (fallos < cola.size) saltar(1) else parar()
        }
        mostrarNotificacion()
    }

    private fun saltar(d: Int) = reproducirIndice(indice + d)

    private fun alternarPausa() {
        val m = mp ?: return
        try {
            if (m.isPlaying) {
                m.pause()
            } else {
                pedirFoco()
                m.start()
            }
        } catch (e: IllegalStateException) {
        }
        pausadoPorFoco = false
        mostrarNotificacion()
    }

    private fun parar() {
        activo = false
        liberar()
        foco?.let { audio.abandonAudioFocusRequest(it) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun liberar() {
        try { mp?.stop() } catch (e: Exception) { }
        try { mp?.release() } catch (e: Exception) { }
        mp = null
    }

    private fun ponerAtenuado(si: Boolean) {
        atenuado = si
        aplicarVolumen()
    }

    private fun aplicarVolumen() {
        val v = if (atenuado || bajoPorFoco) 0.25f else 1f
        try { mp?.setVolume(v, v) } catch (e: Exception) { }
    }

    private fun pedirFoco() {
        val peticion = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(atributos)
            .setOnAudioFocusChangeListener(oyenteFoco)
            .build()
        foco = peticion
        audio.requestAudioFocus(peticion)
    }

    // ---------- Notificación ----------

    private fun accion(icono: Int, titulo: String, accion: String, codigo: Int): Notification.Action {
        val pi = PendingIntent.getService(
            this, codigo,
            Intent(this, MusicService::class.java).setAction(accion),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Action.Builder(Icon.createWithResource(this, icono), titulo, pi).build()
    }

    private fun construirNotificacion(): Notification {
        val p = actual()
        val sonando = estaSonando()
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val texto = p?.artista?.takeIf { it.isNotBlank() } ?: "Reproduciendo con Astra"
        return Notification.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(p?.titulo ?: "Astra")
            .setContentText(texto)
            .setContentIntent(abrir)
            .setOngoing(sonando)
            .addAction(accion(android.R.drawable.ic_media_previous, "Anterior", A_ANTERIOR, 1))
            .addAction(
                accion(
                    if (sonando) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (sonando) "Pausa" else "Reproducir", A_TOGGLE, 2
                )
            )
            .addAction(accion(android.R.drawable.ic_media_next, "Siguiente", A_SIGUIENTE, 3))
            .setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun mostrarNotificacion() {
        if (!activo) return
        val n = construirNotificacion()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID_NOTIF, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(ID_NOTIF, n)
        }
    }
}
