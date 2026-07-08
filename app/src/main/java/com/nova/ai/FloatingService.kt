package com.nova.ai

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FloatingService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // Yahan baad me floating bubble ka code aayega
    }

    override fun onDestroy() {
        super.onDestroy()
        // Yahan bubble ko hatane ka code aayega
    }
}
