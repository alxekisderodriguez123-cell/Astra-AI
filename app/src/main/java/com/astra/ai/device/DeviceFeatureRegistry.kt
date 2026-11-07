
package com.astra.ai.device

import android.content.pm.PackageManager

object DeviceFeatureRegistry {

    private val catalog = listOf(
        "android.hardware.camera.any" to "Cámara",
        "android.hardware.camera.front" to "Cámara frontal",
        "android.hardware.camera.flash" to "Flash LED",

        "android.hardware.microphone" to "Micrófono",

        "android.hardware.wifi" to "Wi-Fi",
        "android.hardware.bluetooth" to "Bluetooth",
        "android.hardware.bluetooth_le" to "Bluetooth LE",
        "android.hardware.nfc" to "NFC",

        "android.hardware.telephony" to "Telefonía móvil",
        "android.hardware.telephony.gsm" to "Red GSM",
        "android.hardware.telephony.cdma" to "Red CDMA",

        "android.hardware.location" to "Ubicación",
        "android.hardware.location.gps" to "GPS",
        "android.hardware.location.network" to "Ubicación por red",

        "android.hardware.fingerprint" to "Lector de huellas",

        "android.hardware.sensor.accelerometer" to "Acelerómetro",
        "android.hardware.sensor.gyroscope" to "Giroscopio",
        "android.hardware.sensor.compass" to "Brújula",
        "android.hardware.sensor.light" to "Sensor de luz",
        "android.hardware.sensor.proximity" to "Sensor de proximidad",
        "android.hardware.sensor.barometer" to "Barómetro",
        "android.hardware.sensor.heartrate" to "Sensor cardíaco",

        "android.hardware.usb.host" to "USB Host",
        "android.hardware.usb.accessory" to "USB Accessory",

        "android.hardware.audio.low_latency" to "Audio de baja latencia",
        "android.hardware.audio.pro" to "Audio profesional"
    )

    fun detect(packageManager: PackageManager): List<DeviceFeature> {
        return catalog.map { (id, name) ->
            DeviceFeature(
                id = id,
                name = name,
                available = packageManager.hasSystemFeature(id)
            )
        }
    }
}
