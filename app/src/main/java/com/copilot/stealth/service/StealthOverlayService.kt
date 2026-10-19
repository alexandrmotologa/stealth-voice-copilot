package com.copilot.stealth.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.copilot.stealth.R

/**
 * Floating system overlay service displaying a minimalist 1-line speech HUD
 * over conference applications (Zoom, Teams, Google Meet).
 */
class StealthOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var hudTextView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        // Programmatic lightweight HUD layout
        val container = TextView(this).apply {
            setBackgroundColor(0xCC000000.toInt())
            setTextColor(0xFFD0D0D0.toInt())
            textSize = 13.0f
            setPadding(32, 16, 32, 16)
            text = "Stealth HUD: Standby"
            gravity = Gravity.CENTER
        }

        hudTextView = container
        overlayView = container

        var initialY = 0
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val updateText = intent?.getStringExtra(EXTRA_HUD_TEXT)
        if (!updateText.isNullOrBlank()) {
            hudTextView?.text = updateText
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_HUD_TEXT = "extra_hud_text"

        fun updateHudText(context: Context, text: String) {
            val intent = Intent(context, StealthOverlayService::class.java).apply {
                putExtra(EXTRA_HUD_TEXT, text)
            }
            context.startService(intent)
        }
    }
}
