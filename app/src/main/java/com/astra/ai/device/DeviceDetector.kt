
package com.astra.ai.device

import android.app.ActivityManager
import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.view.WindowManager
import java.util.Locale

class DeviceDetector(context: Context) {

    private val appContext = context.applicationContext

    fun detect(): DeviceProfile {

        val activityManager =
            appContext.getSystemService(Context.ACTIVITY_SERVICE)
                as ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMb = memoryInfo.totalMem / (1024L * 1024L)

        val windowManager =
            appContext.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager

        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay

        val metrics = android.util.DisplayMetrics()

        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        val refreshRates = display.supportedModes
            .map { it.refreshRate }
            .filter { it > 0f }
            .distinct()
            .sorted()

        val screen = ScreenInfo(
            widthPixels = metrics.widthPixels,
            heightPixels = metrics.heightPixels,
            density = metrics.density,
            refreshRates = refreshRates
        )

        val sensorManager =
            appContext.getSystemService(Context.SENSOR_SERVICE)
                as SensorManager

        val sensors = sensorManager
            .getSensorList(android.hardware.Sensor.TYPE_ALL)
            .map { it.name }
            .distinct()
            .sorted()

        return DeviceProfile(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            board = Build.BOARD.orEmpty(),

            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            androidApi = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            buildDisplay = Build.DISPLAY.orEmpty(),

            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = totalRamMb,

            screen = screen,

            features = DeviceFeatureRegistry.detect(
                appContext.packageManager
            ),

            sensors = sensors,

            locale = Locale.getDefault().toLanguageTag()
        )
    }
}
