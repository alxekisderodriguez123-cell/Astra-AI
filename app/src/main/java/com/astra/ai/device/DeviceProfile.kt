package com.astra.ai.device

data class ScreenInfo(
    val widthPixels: Int,
    val heightPixels: Int,
    val density: Float,
    val refreshRates: List<Float>
)

data class DeviceFeature(
    val id: String,
    val name: String,
    val available: Boolean
)

data class DeviceProfile(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val board: String,

    val androidRelease: String,
    val androidApi: Int,
    val securityPatch: String,
    val buildDisplay: String,

    val supportedAbis: List<String>,
    val cpuCores: Int,
    val totalRamMb: Long,

    val screen: ScreenInfo,
    val features: List<DeviceFeature>,
    val sensors: List<String>,
    val locale: String
) {

    fun toAssistantContext(): String = buildString {
        appendLine("INFORMACIÓN DEL DISPOSITIVO")
        appendLine("Fabricante: $manufacturer")
        appendLine("Marca: $brand")
        appendLine("Modelo: $model")
        appendLine("Código del dispositivo: $device")
        appendLine("Producto: $product")
        appendLine("Hardware: $hardware")
        appendLine("Placa: $board")

        appendLine("Android: $androidRelease")
        appendLine("API Android: $androidApi")
        appendLine("Parche de seguridad: $securityPatch")
        appendLine("Versión de compilación: $buildDisplay")

        appendLine("Arquitecturas: ${supportedAbis.joinToString()}")
        appendLine("Núcleos CPU: $cpuCores")
        appendLine("RAM física: $totalRamMb MB")

        appendLine(
            "Pantalla: ${screen.widthPixels}x${screen.heightPixels} px"
        )

        appendLine(
            "Frecuencias disponibles: ${
                screen.refreshRates.joinToString { "${it.toInt()} Hz" }
            }"
        )

        appendLine("Idioma: $locale")

        appendLine("FUNCIONES DETECTADAS:")
        features.filter { it.available }.forEach {
            appendLine("- ${it.name}")
        }

        appendLine("SENSORES DISPONIBLES:")
        sensors.forEach {
            appendLine("- $it")
        }
    }
}
