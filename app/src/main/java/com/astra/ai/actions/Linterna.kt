package com.astra.ai.actions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class Linterna(context: Context) : Accion {

    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val camaraId: String? = try {
        cm.cameraIdList.firstOrNull { id ->
            val c = cm.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: Exception) {
        null
    }

    override fun intentar(orden: String): String? {
        if (!orden.contains("linterna") && !orden.contains("flash")) return null
        val id = camaraId ?: return "Este teléfono no tiene linterna disponible"
        val apagar = orden.contains("apag") || orden.contains("desactiv")
        return try {
            cm.setTorchMode(id, !apagar)
            if (apagar) "Linterna apagada" else "Linterna encendida"
        } catch (e: Exception) {
            "No pude usar la linterna"
        }
    }
}
