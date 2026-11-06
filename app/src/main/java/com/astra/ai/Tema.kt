package com.astra.ai

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object Tema {
    val cian = Color.parseColor("#4FD8C4")
    val violeta = Color.parseColor("#8E7CF8")
    val azul = Color.parseColor("#6C95FF")
    val texto = Color.parseColor("#E6EEFF")
    val textoSuave = Color.parseColor("#8FA0C8")
    val peligro = Color.parseColor("#FF5C7A")

    fun panel(
        d: Float,
        radio: Float = 18f,
        relleno: Int = Color.argb(205, 14, 22, 48),
        borde: Int = cian,
        bordeAlpha: Int = 110
    ): GradientDrawable = GradientDrawable().apply {
        cornerRadius = radio * d
        setColor(relleno)
        setStroke(
            (1.3f * d).toInt().coerceAtLeast(1),
            Color.argb(bordeAlpha, Color.red(borde), Color.green(borde), Color.blue(borde))
        )
    }
}
