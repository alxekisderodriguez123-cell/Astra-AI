package com.astra.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.sin

class FondoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val inicio = System.nanoTime()
    private val dens = resources.displayMetrics.density
    private val azar = Random(7)
    private val n = 40
    private val px = FloatArray(n) { azar.nextFloat() }
    private val py = FloatArray(n) { azar.nextFloat() }
    private val vel = FloatArray(n) { 0.006f + azar.nextFloat() * 0.02f }
    private val tam = FloatArray(n) { 0.8f + azar.nextFloat() * 1.8f }

    private val pFondo = Paint()
    private val pBrillo = Paint()
    private val pLinea = Paint().apply { color = Color.argb(16, 120, 160, 255); strokeWidth = 1f }
    private val pPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val altoScan = 70f * dens
    private val pScan = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, altoScan,
            intArrayOf(Color.argb(0, 79, 216, 196), Color.argb(30, 79, 216, 196), Color.argb(0, 79, 216, 196)),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        pFondo.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.parseColor("#050814"), Color.parseColor("#0A1030"), Color.parseColor("#0E1A3F")),
            null, Shader.TileMode.CLAMP
        )
        pBrillo.shader = RadialGradient(
            w / 2f, h * 0.27f, w * 0.85f,
            Color.argb(70, 110, 90, 230), Color.argb(0, 110, 90, 230), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val t = (System.nanoTime() - inicio) / 1_000_000_000f
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, pFondo)
        canvas.drawRect(0f, 0f, w, h, pBrillo)

        // Cuadrícula que se desliza hacia abajo
        val paso = 48f * dens
        var x = 0f
        while (x < w) { canvas.drawLine(x, 0f, x, h, pLinea); x += paso }
        var y = -paso + (t * 6f * dens) % paso
        while (y < h) { canvas.drawLine(0f, y, w, y, pLinea); y += paso }

        // Partículas que suben
        for (i in 0 until n) {
            val yy = (((py[i] - t * vel[i]) % 1f) + 1f) % 1f
            pPunto.alpha = (40 + 90 * (0.5f + 0.5f * sin(t * 1.2f + i))).toInt()
            canvas.drawCircle(px[i] * w, yy * h, tam[i] * dens, pPunto)
        }

        // Línea de escaneo
        val sy = ((t * 0.08f) % 1f) * (h + altoScan) - altoScan
        canvas.save()
        canvas.translate(0f, sy)
        canvas.drawRect(0f, 0f, w, altoScan, pScan)
        canvas.restore()

        postInvalidateOnAnimation()
    }
}
