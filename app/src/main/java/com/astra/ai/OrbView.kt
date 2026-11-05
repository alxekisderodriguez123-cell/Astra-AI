package com.astra.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState { QUIETO, ESCUCHANDO, HABLANDO }

class OrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val violeta = Color.parseColor("#8E7CF8")
    private val azul = Color.parseColor("#6C95FF")
    private val turquesa = Color.parseColor("#4FD8C4")

    private var estado = OrbState.QUIETO
    private var nivel = 0f
    private var nivelObjetivo = 0f
    private var mezcla = 0f
    private val inicio = System.nanoTime()

    private val gradLetra = LinearGradient(
        -0.6f, -0.6f, 0.6f, 0.65f,
        intArrayOf(violeta, azul, turquesa), floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    private val gradAnillo = LinearGradient(
        -0.95f, 0.2f, 0.95f, -0.2f,
        intArrayOf(violeta, azul, turquesa), null, Shader.TileMode.CLAMP
    )

    private val pLetra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.14f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        shader = gradLetra
    }
    private val pInterior = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.04f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        shader = gradLetra; alpha = 150
    }
    private val pAnillo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.05f
        strokeCap = Paint.Cap.ROUND; shader = gradAnillo
    }
    private val pDestello = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.065f
        strokeCap = Paint.Cap.ROUND; color = Color.argb(210, 220, 235, 255)
    }
    private val pBrillo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0.05f, 0.95f,
            intArrayOf(Color.argb(120, 142, 124, 248), Color.argb(50, 79, 216, 196), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
    }
    private val pOnda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.022f
    }
    private val pEstrella = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2F7FF")
    }
    private val pAura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1.8f,
            intArrayOf(Color.argb(150, 190, 210, 255), Color.argb(0, 190, 210, 255)),
            null, Shader.TileMode.CLAMP
        )
    }
    private val pPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val letra = Path().apply {
        moveTo(-0.58f, 0.62f)
        lineTo(-0.17f, -0.42f)
        cubicTo(-0.10f, -0.60f, 0.10f, -0.60f, 0.17f, -0.42f)
        lineTo(0.58f, 0.62f)
    }
    private val interior = Path().apply {
        moveTo(-0.30f, 0.40f)
        lineTo(-0.10f, -0.14f)
        cubicTo(-0.06f, -0.26f, 0.06f, -0.26f, 0.10f, -0.14f)
        lineTo(0.30f, 0.40f)
    }
    private val estrella = Path().apply {
        moveTo(0f, -1f)
        quadTo(0.1f, -0.1f, 1f, 0f)
        quadTo(0.1f, 0.1f, 0f, 1f)
        quadTo(-0.1f, 0.1f, -1f, 0f)
        quadTo(-0.1f, -0.1f, 0f, -1f)
        close()
    }
    private val onda = Path()
    private val ovalo = RectF(-0.95f, -0.3f, 0.95f, 0.3f)
    private val puntos = arrayOf(
        floatArrayOf(-0.62f, -0.22f, 0.014f),
        floatArrayOf(-0.30f, 0.05f, 0.011f),
        floatArrayOf(0.30f, 0.30f, 0.013f),
        floatArrayOf(0.12f, -0.50f, 0.010f),
        floatArrayOf(0.55f, 0.05f, 0.012f)
    )

    fun ponerEstado(nuevo: OrbState) {
        estado = nuevo
        if (nuevo == OrbState.QUIETO) nivelObjetivo = 0f
    }

    fun ponerNivel(rmsDb: Float) {
        if (estado == OrbState.ESCUCHANDO) {
            nivelObjetivo = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val t = (System.nanoTime() - inicio) / 1_000_000_000f

        val objetivoMezcla = if (estado == OrbState.HABLANDO) 1f else 0f
        mezcla += (objetivoMezcla - mezcla) * 0.08f
        if (estado == OrbState.HABLANDO) {
            nivelObjetivo = 0.35f + 0.65f * abs(sin(t * 7f) * sin(t * 2.9f))
        }
        nivel += (nivelObjetivo - nivel) * 0.15f

        val s = minOf(width, height) / 2f
        val respira = 1f + 0.018f * sin(t * 1.3f) + 0.03f * nivel
        val escala = s * 0.9f * respira

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(escala, escala)

        // Brillo de fondo
        val energia = (nivel * 0.6f + mezcla * 0.4f).coerceIn(0f, 1f)
        pBrillo.alpha = (110 + 140 * energia).toInt().coerceIn(0, 255)
        canvas.drawCircle(0f, 0.05f, 0.95f, pBrillo)

        // Ondas al hablar
        if (mezcla > 0.02f) {
            for (i in 0..2) {
                val p = (t * 0.45f + i / 3f) % 1f
                val radio = 0.52f + p * 0.5f
                val amp = 0.025f + 0.05f * nivel
                onda.reset()
                val n = 90
                for (k in 0..n) {
                    val ang = (k.toFloat() / n) * 2f * PI.toFloat()
                    val r = radio * (1f + amp * sin(ang * 5f + t * 3f + i * 2f))
                    val x = r * cos(ang)
                    val y = r * sin(ang)
                    if (k == 0) onda.moveTo(x, y) else onda.lineTo(x, y)
                }
                onda.close()
                pOnda.color = if (i % 2 == 0) violeta else turquesa
                pOnda.alpha = ((1f - p) * 0.75f * mezcla * 255).toInt().coerceIn(0, 255)
                canvas.drawPath(onda, pOnda)
            }
        }

        // Letra A
        canvas.drawPath(letra, pLetra)
        canvas.drawPath(interior, pInterior)

        // Anillo orbital
        canvas.save()
        canvas.translate(0.02f, 0.2f)
        canvas.rotate(-20f + 3f * sin(t * 0.7f))
        canvas.drawOval(ovalo, pAnillo)
        val barrido = (t * (40f + 140f * mezcla)) % 360f
        canvas.drawArc(ovalo, barrido, 50f, false, pDestello)
        canvas.restore()

        // Estrella
        val tam = 0.17f + 0.015f * sin(t * 2f) + 0.07f * nivel
        canvas.save()
        canvas.translate(0.64f, -0.66f)
        canvas.scale(tam, tam)
        canvas.drawCircle(0f, 0f, 1.8f, pAura)
        canvas.rotate(sin(t * 0.8f) * 8f)
        canvas.drawPath(estrella, pEstrella)
        canvas.restore()

        // Partículas
        for ((i, pt) in puntos.withIndex()) {
            pPunto.alpha = (80 + 120 * (0.5f + 0.5f * sin(t * 1.5f + i * 1.7f))).toInt()
            canvas.drawCircle(pt[0], pt[1], pt[2], pPunto)
        }

        canvas.restore()
        postInvalidateOnAnimation()
    }
}
