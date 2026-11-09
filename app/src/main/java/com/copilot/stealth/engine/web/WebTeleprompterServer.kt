package com.copilot.stealth.engine.web

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Embedded HTTP and WebSocket server running locally on Android device.
 * Serves an AMOLED true-black web teleprompter mirror accessible by laptop/desktop browsers
 * positioned under the webcam on the same Wi-Fi or hotspot.
 */
class WebTeleprompterServer(
    port: Int = DEFAULT_PORT
) : NanoWSD(port) {

    companion object {
        const val DEFAULT_PORT = 8765
        private const val TAG = "WebTeleprompterServer"
    }

    // Callbacks to ViewModel
    var onClientQuery: ((query: String) -> Unit)? = null
    var onCycleLanguage: (() -> Unit)? = null
    var onClearHistory: (() -> Unit)? = null
    var onFollowUpAction: ((turnIndex: Int, actionType: String) -> Unit)? = null

    // State cache to send immediately upon client connection
    private var lastStateJson: String? = null

    private val connectedSockets = CopyOnWriteArraySet<TeleprompterSocket>()

    override fun openWebSocket(handshake: NanoHTTPD.IHTTPSession): WebSocket {
        return TeleprompterSocket(handshake)
    }

    override fun serveHttp(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        if (uri == "/" || uri == "/index.html") {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "text/html; charset=UTF-8",
                WebClientHtml.HTML_CONTENT
            )
        }
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
    }

    fun broadcastState(
        isListening: Boolean,
        isProcessing: Boolean,
        sttLanguageMode: String,
        activeEngine: String,
        partialTranscript: String?,
        turns: List<com.copilot.stealth.data.models.CopilotTurn>
    ) {
        try {
            val root = JSONObject().apply {
                put("type", "STATE_UPDATE")
                put("isListening", isListening)
                put("isProcessing", isProcessing)
                put("sttLanguageMode", sttLanguageMode)
                put("activeEngine", activeEngine)
                put("partialTranscript", partialTranscript ?: "")

                val turnsArray = JSONArray()
                for (t in turns) {
                    val turnObj = JSONObject().apply {
                        put("transcriptEn", t.transcriptEn)
                        put("transcriptRo", t.transcriptRo ?: "")
                        put("answerEn", t.answerEn)
                        put("answerRo", t.answerRo ?: "")
                        put("latencyMs", t.latencyMs)
                        put("category", t.matchedCategory ?: "DISPATCH INTENT")
                        put("isQuestion", t.isQuestion)
                        val followUpsArr = JSONArray()
                        t.followUpQuestions.forEach { followUpsArr.put(it) }
                        put("followUps", followUpsArr)
                    }
                    turnsArray.put(turnObj)
                }
                put("turns", turnsArray)
            }

            val jsonString = root.toString()
            lastStateJson = jsonString

            for (socket in connectedSockets) {
                if (socket.isOpen) {
                    try {
                        socket.send(jsonString)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed sending state update to socket: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding broadcast state: ${e.message}", e)
        }
    }

    inner class TeleprompterSocket(handshakeRequest: NanoHTTPD.IHTTPSession) : WebSocket(handshakeRequest) {

        override fun onOpen() {
            Log.d(TAG, "WebSocket client connected: ${handshakeRequest.remoteIpAddress}")
            connectedSockets.add(this)

            // Send current state immediately so new client doesn't wait
            lastStateJson?.let {
                try {
                    send(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending initial state: ${e.message}")
                }
            }
        }

        override fun onClose(
            code: NanoWSD.WebSocketFrame.CloseCode?,
            reason: String?,
            initiatedByRemote: Boolean
        ) {
            Log.d(TAG, "WebSocket client closed: code=$code, reason=$reason, remote=$initiatedByRemote")
            connectedSockets.remove(this)
        }

        override fun onMessage(message: NanoWSD.WebSocketFrame) {
            try {
                val text = message.textPayload ?: return
                Log.d(TAG, "WebSocket received: $text")
                val json = JSONObject(text)
                when (json.optString("action")) {
                    "query" -> {
                        val q = json.optString("text")
                        if (q.isNotBlank()) onClientQuery?.invoke(q)
                    }
                    "cycle_language" -> onCycleLanguage?.invoke()
                    "clear_history" -> onClearHistory?.invoke()
                    "follow_up" -> {
                        val index = json.optInt("turnIndex", -1)
                        val type = json.optString("type", "example")
                        onFollowUpAction?.invoke(index, type)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing WebSocket incoming message: ${e.message}", e)
            }
        }

        override fun onPong(pong: NanoWSD.WebSocketFrame?) {}

        override fun onException(exception: IOException?) {
            Log.w(TAG, "WebSocket exception: ${exception?.message}")
            connectedSockets.remove(this)
        }
    }
}
