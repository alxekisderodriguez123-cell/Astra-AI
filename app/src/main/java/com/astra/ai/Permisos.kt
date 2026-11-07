package com.astra.ai

import android.Manifest
import android.os.Build

object Permisos {
    fun lista(): Array<String> {
        val l = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= 33) {
            l.add(Manifest.permission.READ_MEDIA_AUDIO)
            l.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            l.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return l.toTypedArray()
    }
}
