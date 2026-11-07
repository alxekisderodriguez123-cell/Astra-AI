
package com.astra.ai.device

import android.content.Context

class DeviceEngine(context: Context) {

    private val detector = DeviceDetector(context)

    @Volatile
    private var cachedProfile: DeviceProfile? = null

    fun getProfile(): DeviceProfile {
        return cachedProfile ?: synchronized(this) {
            cachedProfile ?: detector.detect().also {
                cachedProfile = it
            }
        }
    }

    fun refresh(): DeviceProfile {
        return synchronized(this) {
            detector.detect().also {
                cachedProfile = it
            }
        }
    }

    fun getAssistantContext(): String {
        return getProfile().toAssistantContext()
    }
}
