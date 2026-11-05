package com.astra.ai.actions

interface Accion {
    /** Devuelve lo que Astra debe decir si la orden era para esta acción, o null si no. */
    fun intentar(orden: String): String?
}
