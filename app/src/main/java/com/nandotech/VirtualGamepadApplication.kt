package com.nandotech

import android.app.Application

class VirtualGamepadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Here we can initialize analytics, crashes, log tracing or other PC bridge global items
    }
}
