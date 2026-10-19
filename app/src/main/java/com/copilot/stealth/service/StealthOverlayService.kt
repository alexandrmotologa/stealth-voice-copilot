package com.copilot.stealth.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Floating system overlay service displaying a minimalist, gesture-interactive HUD
 * over conference applications (Zoom, Teams, Google Meet) with dynamic transparency.
 */
class StealthOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var hudTextView: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var isMinimized = false

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 140
        }
        params = layoutParams

        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.argb(210, 8, 8, 8)) // 82% true black
            setStroke(2, Color.argb(100, 40, 40, 40))
        }

        val container = TextView(this).apply {
            background = backgroundDrawable
            setTextColor(Color.argb(240, 220, 220, 220))
            textSize = 12.5f
            setPadding(40, 20, 40, 20)
            text = "Stealth HUD • Standby"
            gravity = Gravity.CENTER
            maxWidth = 850
        }

        hudTextView = container
        overlayView = container

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastClickTime = 0L

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 350) {
                        // Double tap: toggle micro-pill minimized state
                        toggleMinimizedState(container, layoutParams)
                        lastClickTime = 0L
                    } else {
                        lastClickTime = currentTime
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY - (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, layoutParams)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (_: Exception) {}
    }

    private fun toggleMinimizedState(view: TextView, layoutParams: WindowManager.LayoutParams) {
        isMinimized = !isMinimized
        if (isMinimized) {
            view.text = "•"
            view.setPadding(20, 10, 20, 10)
        } else {
            view.text = "Stealth HUD Active"
            view.setPadding(40, 20, 40, 20)
        }
        windowManager?.updateViewLayout(view, layoutParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val updateText = intent?.getStringExtra(EXTRA_HUD_TEXT)
        if (!updateText.isNullOrBlank()) {
            if (!isMinimized) {
                hudTextView?.text = updateText
            }
        }

        val alpha = intent?.getFloatExtra(EXTRA_ALPHA, -1f) ?: -1f
        if (alpha > 0f) {
            params?.alpha = alpha
            params?.let { windowManager?.updateViewLayout(overlayView, it) }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        hudTextView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_HUD_TEXT = "extra_hud_text"
        const val EXTRA_ALPHA = "extra_hud_alpha"

        fun updateHudText(context: Context, text: String) {
            val intent = Intent(context, StealthOverlayService::class.java).apply {
                putExtra(EXTRA_HUD_TEXT, text)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun updateHudAlpha(context: Context, alpha: Float) {
            val intent = Intent(context, StealthOverlayService::class.java).apply {
                putExtra(EXTRA_ALPHA, alpha)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
