package com.copilot.stealth

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Application class initializing global configurations and logging for Stealth Voice Copilot.
 */
class StealthApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (_: Exception) {}
    }
}
