package com.astra.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class HudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val inicio = System.nanoTime()
    private var objetivo = 0f
    private var mezcla = 0f

    private val cian = Color.parseColor("#4FD8C4")
    private val violeta = Color.parseColor("#8E7CF8")
    private val azul = Color.parseColor("#6C95FF")

    private val pTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.012f; strokeCap = Paint.Cap.ROUND
    }
    private val pArco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.02f; strokeCap = Paint.Cap.ROUND
    }
    private val pAro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.006f
    }

    private val rect1 = RectF(-0.93f, -0.93f, 0.93f, 0.93f)
    private val rect2 = RectF(-0.87f, -0.87f, 0.87f, 0.87f)
    private val n = 72
    private val cosT = FloatArray(n) { cos(it * 2f * PI.toFloat() / n) }
    private val sinT = FloatArray(n) { sin(it * 2f * PI.toFloat() / n) }

    fun ponerEstado(e: OrbState) {
        objetivo = when (e) {
            OrbState.HABLANDO -> 1f
            OrbState.ESCUCHANDO -> 0.5f
            OrbState.QUIETO -> 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        val t = (System.nanoTime() - inicio) / 1_000_000_000f
        mezcla += (objetivo - mezcla) * 0.06f

        val s = minOf(width, height) / 2f * 0.97f
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(s, s)

        pAro.color = cian
        pAro.alpha = (35 + 40 * mezcla).toInt()
        canvas.drawCircle(0f, 0f, 0.99f, pAro)

        canvas.save()
        canvas.rotate(t * (2f + 6f * mezcla))
        pTick.color = azul
        for (i in 0 until n) {
            val largo = i % 6 == 0
            val r0 = if (largo) 0.94f else 0.97f
            pTick.alpha = if (largo) (90 + 80 * mezcla).toInt() else (45 + 40 * mezcla).toInt()
            canvas.drawLine(cosT[i] * r0, sinT[i] * r0, cosT[i] * 1f, sinT[i] * 1f, pTick)
        }
        canvas.restore()

        val a1 = t * (25f + 110f * mezcla)
        pArco.color = violeta
        pArco.alpha = (150 + 90 * mezcla).toInt()
        canvas.drawArc(rect1, a1, 75f, false, pArco)
        canvas.drawArc(rect1, a1 + 180f, 25f, false, pArco)

        val a2 = 180f - t * (18f + 80f * mezcla)
        pArco.color = cian
        pArco.alpha = (130 + 100 * mezcla).toInt()
        canvas.drawArc(rect2, a2, 110f, false, pArco)

        canvas.restore()
        postInvalidateOnAnimation()
    }
}
