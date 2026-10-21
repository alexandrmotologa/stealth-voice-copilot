package com.copilot.stealth.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import com.copilot.stealth.R
import com.copilot.stealth.data.repository.SessionExportManager
import com.copilot.stealth.service.CopilotForegroundService
import com.copilot.stealth.ui.StealthActivity

/**
 * On-device AMOLED lockscreen and homescreen widget for Stealth Voice Copilot.
 * Supports covert glance answer inspection, one-tap mic toggling, and emergency panic wiping.
 */
class StealthLockScreenWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_MIC -> {
                val serviceIntent = Intent(context, CopilotForegroundService::class.java).apply {
                    action = if (isCurrentlyListening) {
                        CopilotForegroundService.ACTION_STOP_LISTENING
                    } else {
                        CopilotForegroundService.ACTION_START_LISTENING
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                isCurrentlyListening = !isCurrentlyListening
                updateAll(
                    context = context,
                    isListening = isCurrentlyListening,
                    category = if (isCurrentlyListening) "LISTENING FOR PROMPT" else "STANDBY",
                    answerEn = if (isCurrentlyListening) "Listening to interviewer speech..." else lastAnswerEn,
                    answerRo = if (isCurrentlyListening) "Ascultă întrebarea brokerului..." else lastAnswerRo,
                    latencyMs = lastLatencyMs,
                    isBtConnected = lastIsBt
                )
            }
            ACTION_PANIC_WIPE -> {
                try {
                    SessionExportManager(context).panicWipe()
                } catch (_: Exception) {}
                isCurrentlyListening = false
                lastAnswerEn = "SESSION BUFFER WIPED • ZERO DATA PERSISTED"
                lastAnswerRo = "Date șterse din memorie • Securizat"
                lastCategory = "EMERGENCY WIPE"

                updateAll(
                    context = context,
                    isListening = false,
                    category = "EMERGENCY WIPE",
                    answerEn = "SESSION BUFFER WIPED • ZERO DATA PERSISTED",
                    answerRo = "Date șterse din memorie • Securizat",
                    latencyMs = 0L,
                    isBtConnected = lastIsBt
                )
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_MIC = "com.copilot.stealth.ACTION_WIDGET_TOGGLE_MIC"
        const val ACTION_PANIC_WIPE = "com.copilot.stealth.ACTION_WIDGET_PANIC_WIPE"

        var isCurrentlyListening = false
        var lastCategory = "STEALTH VOICE COPILOT"
        var lastAnswerEn = "Ready for US freight dispatching & billing interview prompts."
        var lastAnswerRo = "Tap microphone or start listening."
        var lastLatencyMs = 53L
        var lastIsBt = false

        fun updateAll(
            context: Context,
            isListening: Boolean = isCurrentlyListening,
            category: String = lastCategory,
            answerEn: String = lastAnswerEn,
            answerRo: String? = lastAnswerRo,
            latencyMs: Long = lastLatencyMs,
            isBtConnected: Boolean = lastIsBt
        ) {
            isCurrentlyListening = isListening
            lastCategory = category
            lastAnswerEn = answerEn
            if (answerRo != null) lastAnswerRo = answerRo
            lastLatencyMs = latencyMs
            lastIsBt = isBtConnected

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, StealthLockScreenWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return

            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_stealth_lockscreen)

            // Header state
            if (isCurrentlyListening) {
                views.setTextViewText(R.id.widget_status_text, "● LISTENING")
                views.setTextColor(R.id.widget_status_text, Color.parseColor("#3DDC84"))
                views.setTextViewText(R.id.widget_btn_toggle_mic, "■ STOP")
                views.setTextColor(R.id.widget_btn_toggle_mic, Color.parseColor("#FF4444"))
            } else {
                views.setTextViewText(R.id.widget_status_text, "● STANDBY")
                views.setTextColor(R.id.widget_status_text, Color.parseColor("#888888"))
                views.setTextViewText(R.id.widget_btn_toggle_mic, "● MIC")
                views.setTextColor(R.id.widget_btn_toggle_mic, Color.parseColor("#3DDC84"))
            }

            views.setTextViewText(
                R.id.widget_latency_text,
                if (lastLatencyMs > 0) "${lastLatencyMs}ms" else "--"
            )

            views.setTextViewText(
                R.id.widget_route_badge,
                if (lastIsBt) "BT: BUDS" else "MIC: INT"
            )

            // Content
            views.setTextViewText(R.id.widget_category_label, lastCategory.uppercase())
            views.setTextViewText(R.id.widget_answer_text, lastAnswerEn)
            views.setTextViewText(R.id.widget_ro_guidance, lastAnswerRo)

            // PendingIntent for Open App (launches directly over lockscreen)
            val openAppIntent = Intent(context, StealthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_open, openAppPendingIntent)

            // PendingIntent for Toggle Mic
            val toggleMicIntent = Intent(context, StealthLockScreenWidget::class.java).apply {
                action = ACTION_TOGGLE_MIC
            }
            val toggleMicPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                toggleMicIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_toggle_mic, toggleMicPendingIntent)

            // PendingIntent for Panic Wipe
            val panicIntent = Intent(context, StealthLockScreenWidget::class.java).apply {
                action = ACTION_PANIC_WIPE
            }
            val panicPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                panicIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_panic, panicPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
