package com.copilot.stealth.engine.web

/**
 * Embedded single-page web application served to laptop/desktop browsers.
 * Designed for positioning directly underneath laptop webcams during Zoom, Meet, or Teams interviews.
 */
object WebClientHtml {

    val HTML_CONTENT = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Stealth Teleprompter | Web Companion</title>
    <style>
        :root {
            --bg-color: #000000;
            --card-bg: #0A0A0A;
            --card-border: #1E1E1E;
            --text-main: #E6EDF3;
            --text-question: #7D8590;
            --text-ro: #A3E635;
            --text-accent: #38BDF8;
            --status-active: #10B981;
            --status-process: #F59E0B;
            --btn-bg: #141414;
            --btn-border: #2A2A2A;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            -webkit-font-smoothing: antialiased;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-main);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        /* Top Bar */
        header {
            position: sticky;
            top: 0;
            z-index: 100;
            background: rgba(0, 0, 0, 0.95);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid #161616;
            padding: 10px 18px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 13px;
        }

        .header-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .status-dot {
            width: 9px;
            height: 9px;
            border-radius: 50%;
            background-color: #555555;
            transition: all 0.3s ease;
        }

        .status-dot.active {
            background-color: var(--status-active);
            box-shadow: 0 0 10px rgba(16, 185, 129, 0.7);
            animation: pulse 2s infinite;
        }

        .status-dot.processing {
            background-color: var(--status-process);
            box-shadow: 0 0 10px rgba(245, 158, 11, 0.7);
        }

        @keyframes pulse {
            0% { transform: scale(0.95); opacity: 0.8; }
            50% { transform: scale(1.15); opacity: 1; }
            100% { transform: scale(0.95); opacity: 0.8; }
        }

        .brand-title {
            font-weight: 700;
            letter-spacing: 0.8px;
            font-size: 12px;
            color: #888888;
            text-transform: uppercase;
        }

        .pill {
            display: inline-flex;
            align-items: center;
            padding: 3px 9px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            user-select: none;
            transition: background 0.2s;
            border: 1px solid var(--btn-border);
            background: var(--btn-bg);
            color: #AAAAAA;
        }

        .pill.active-pill {
            background: #102418;
            border-color: #1E482C;
            color: var(--status-active);
        }

        .pill.engine-pill {
            background: #0F1E2E;
            border-color: #1B3654;
            color: var(--text-accent);
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        button.action-btn {
            background: var(--btn-bg);
            color: #AAAAAA;
            border: 1px solid var(--btn-border);
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 12px;
            cursor: pointer;
            transition: all 0.15s ease;
        }

        button.action-btn:hover {
            color: #FFFFFF;
            border-color: #444444;
            background: #1C1C1C;
        }

        /* Partial / Real-Time Transcript HUD */
        #partial-container {
            display: none;
            margin: 12px 18px 4px 18px;
            padding: 10px 14px;
            background: #061118;
            border: 1px dashed #1B3F58;
            border-radius: 6px;
            font-size: 14px;
            color: #38BDF8;
            font-family: monospace;
            animation: fadeIn 0.2s ease;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-4px); }
            to { opacity: 1; transform: translateY(0); }
        }

        /* Turns Container */
        main {
            flex: 1;
            padding: 14px 18px 90px 18px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            max-width: 980px;
            margin: 0 auto;
            width: 100%;
        }

        .turn-card {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 8px;
            padding: 18px 20px;
            display: flex;
            flex-direction: column;
            gap: 10px;
            transition: border-color 0.2s ease;
        }

        .turn-card.latest {
            border-color: #26382B;
            box-shadow: 0 0 20px rgba(16, 185, 129, 0.05);
        }

        .turn-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 11px;
            color: #555555;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .question-text {
            color: var(--text-question);
            font-size: 15px;
            font-weight: 500;
            line-height: 1.4;
        }

        .question-ro {
            color: #607274;
            font-size: 13px;
            font-style: italic;
            margin-top: -4px;
        }

        .answer-container {
            margin-top: 4px;
        }

        .answer-en {
            color: var(--text-main);
            font-size: var(--prompter-font-size, 22px);
            font-weight: 600;
            line-height: 1.45;
            white-space: pre-wrap;
            letter-spacing: -0.2px;
        }

        .answer-ro {
            margin-top: 10px;
            padding-top: 10px;
            border-top: 1px solid #141414;
            color: var(--text-ro);
            font-size: 14px;
            line-height: 1.45;
            font-style: italic;
        }

        /* Quick Micro-Actions */
        .quick-actions {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            margin-top: 10px;
        }

        .micro-pill {
            background: #111111;
            border: 1px solid #242424;
            color: #999999;
            font-size: 12px;
            padding: 4px 10px;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.15s;
        }

        .micro-pill:hover {
            background: #1A1A1A;
            color: #FFFFFF;
            border-color: #38BDF8;
        }

        /* Bottom Fixed Input Bar */
        footer {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: rgba(0, 0, 0, 0.95);
            backdrop-filter: blur(12px);
            border-top: 1px solid #161616;
            padding: 10px 18px;
            display: flex;
            justify-content: center;
            z-index: 100;
        }

        .input-wrapper {
            max-width: 980px;
            width: 100%;
            display: flex;
            gap: 8px;
        }

        #query-input {
            flex: 1;
            background: #0E0E0E;
            border: 1px solid #222222;
            color: #FFFFFF;
            font-size: 14px;
            padding: 9px 14px;
            border-radius: 6px;
            outline: none;
            transition: border-color 0.2s;
        }

        #query-input:focus {
            border-color: #38BDF8;
        }

        #send-btn {
            background: #142430;
            border: 1px solid #22445E;
            color: #80D8FF;
            font-size: 13px;
            font-weight: 600;
            padding: 0 16px;
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.15s;
        }

        #send-btn:hover {
            background: #1A3448;
            border-color: #38BDF8;
            color: #FFFFFF;
        }

        .empty-placeholder {
            text-align: center;
            margin-top: 60px;
            color: #444444;
            font-size: 14px;
            line-height: 1.6;
        }
    </style>
</head>
<body>

    <header>
        <div class="header-left">
            <div id="status-dot" class="status-dot" title="Connection Status"></div>
            <span class="brand-title">Stealth Prompter</span>
            <div id="lang-pill" class="pill active-pill" onclick="cycleLanguage()" title="Click to cycle speech recognition language">RO➔EN</div>
            <div id="engine-pill" class="pill engine-pill">HYBRID</div>
        </div>
        <div class="header-actions">
            <button class="action-btn" onclick="adjustFontSize(-2)" title="Smaller text">A-</button>
            <button class="action-btn" onclick="adjustFontSize(2)" title="Larger text">A+</button>
            <button class="action-btn" onclick="clearHistory()">Clear</button>
        </div>
    </header>

    <div id="partial-container">
        <strong>Listening:</strong> <span id="partial-text"></span>
    </div>

    <main id="turns-container">
        <div id="empty-state" class="empty-placeholder">
            <p><strong>Covert Teleprompter Ready</strong></p>
            <p>Microphone is listening on your phone. Spoken questions and answers will appear here in real-time directly beneath your laptop webcam.</p>
        </div>
    </main>

    <footer>
        <div class="input-wrapper">
            <input type="text" id="query-input" placeholder="Type a question or topic directly and press Enter..." onkeydown="if(event.key==='Enter') sendManualQuery()" />
            <button id="send-btn" onclick="sendManualQuery()">Send</button>
        </div>
    </footer>

    <script>
        let ws = null;
        let currentFontSize = 22;
        let reconnectTimer = null;

        function connectWebSocket() {
            const loc = window.location;
            const wsUri = (loc.protocol === "https:" ? "wss://" : "ws://") + loc.host + "/ws";
            console.log("Connecting to WebSocket:", wsUri);

            ws = new WebSocket(wsUri);

            ws.onopen = function() {
                console.log("WebSocket connected successfully.");
                document.getElementById("status-dot").className = "status-dot active";
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                }
            };

            ws.onclose = function() {
                console.log("WebSocket disconnected. Reconnecting in 2s...");
                document.getElementById("status-dot").className = "status-dot";
                reconnectTimer = setTimeout(connectWebSocket, 2000);
            };

            ws.onerror = function(err) {
                console.error("WebSocket error:", err);
                ws.close();
            };

            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    if (data.type === "STATE_UPDATE") {
                        handleStateUpdate(data);
                    }
                } catch (e) {
                    console.error("Error parsing incoming message:", e);
                }
            };
        }

        function handleStateUpdate(state) {
            // Status dot
            const dot = document.getElementById("status-dot");
            if (state.isProcessing) {
                dot.className = "status-dot processing";
            } else if (state.isListening) {
                dot.className = "status-dot active";
            } else {
                dot.className = "status-dot";
            }

            // Language pill
            const langPill = document.getElementById("lang-pill");
            const isRo = state.sttLanguageMode === "ROMANIAN" || state.sttLanguageMode === "BILINGUAL_AUTO";
            langPill.textContent = isRo ? "RO➔EN" : "EN ONLY";
            langPill.className = isRo ? "pill active-pill" : "pill";

            // Engine pill
            const enginePill = document.getElementById("engine-pill");
            enginePill.textContent = state.activeEngine || "HYBRID";

            // Partial transcript
            const partialContainer = document.getElementById("partial-container");
            const partialText = document.getElementById("partial-text");
            if (state.partialTranscript && state.partialTranscript.trim().length > 0) {
                partialText.textContent = state.partialTranscript;
                partialContainer.style.display = "block";
            } else {
                partialContainer.style.display = "none";
            }

            // Turns
            renderTurns(state.turns || []);
        }

        function renderTurns(turns) {
            const container = document.getElementById("turns-container");
            const emptyState = document.getElementById("empty-state");

            if (!turns || turns.length === 0) {
                emptyState.style.display = "block";
                const cards = container.querySelectorAll(".turn-card");
                cards.forEach(c => c.remove());
                return;
            }

            emptyState.style.display = "none";
            container.innerHTML = "";

            turns.forEach((turn, index) => {
                const card = document.createElement("div");
                card.className = "turn-card" + (index === turns.length - 1 ? " latest" : "");

                let questionHtml = '<div class="turn-header"><span>Turn #' + (index + 1) + ' &bull; ' + escapeHtml(turn.category || "QUESTION") + '</span><span>' + (turn.latencyMs || 0) + 'ms</span></div>';
                questionHtml += '<div class="question-text">' + escapeHtml(turn.transcriptEn) + '</div>';

                if (turn.transcriptRo && turn.transcriptRo !== turn.transcriptEn) {
                    questionHtml += '<div class="question-ro">"' + escapeHtml(turn.transcriptRo) + '"</div>';
                }

                let answerHtml = '<div class="answer-container"><div class="answer-en" style="font-size: ' + currentFontSize + 'px;">' + formatAnswer(turn.answerEn) + '</div>';

                if (turn.answerRo && turn.answerRo.trim().length > 0) {
                    answerHtml += '<div class="answer-ro">' + escapeHtml(turn.answerRo) + '</div>';
                }
                answerHtml += '</div>';

                // Quick Micro-Actions
                let actionsHtml = '<div class="quick-actions">';
                actionsHtml += '<button class="micro-pill" onclick="triggerFollowUp(' + index + ', \'example\')">+ Example</button>';
                actionsHtml += '<button class="micro-pill" onclick="triggerFollowUp(' + index + ', \'tradeoff\')">+ Tradeoff</button>';
                actionsHtml += '<button class="micro-pill" onclick="triggerFollowUp(' + index + ', \'simpler\')">Simpler</button>';
                actionsHtml += '<button class="micro-pill" onclick="triggerFollowUp(' + index + ', \'deep_dive\')">Deep Dive</button>';
                actionsHtml += '</div>';

                card.innerHTML = questionHtml + answerHtml + actionsHtml;
                container.appendChild(card);
            });

            // Smoothly keep latest card in view
            window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
        }

        function formatAnswer(text) {
            if (!text) return "";
            return escapeHtml(text).replace(/\n/g, "<br/>");
        }

        function escapeHtml(str) {
            if (!str) return "";
            return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
        }

        function cycleLanguage() {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ action: "cycle_language" }));
            }
        }

        function clearHistory() {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ action: "clear_history" }));
            }
        }

        function triggerFollowUp(turnIndex, actionType) {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ action: "follow_up", turnIndex: turnIndex, type: actionType }));
            }
        }

        function sendManualQuery() {
            const input = document.getElementById("query-input");
            const text = input.value.trim();
            if (!text) return;
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ action: "query", text: text }));
                input.value = "";
            }
        }

        function adjustFontSize(delta) {
            currentFontSize = Math.max(16, Math.min(36, currentFontSize + delta));
            document.querySelectorAll(".answer-en").forEach(el => {
                el.style.fontSize = currentFontSize + "px";
            });
        }

        // Initialize on page load
        window.addEventListener("DOMContentLoaded", connectWebSocket);
    </script>
</body>
</html>
    """.trimIndent()
}
