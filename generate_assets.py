import os
import math
from PIL import Image, ImageDraw, ImageFont

IMG_DIR = os.path.join(os.path.dirname(__file__), "docs", "images")
os.makedirs(IMG_DIR, exist_ok=True)

# Font setup
def get_font(size, bold=False, mono=False):
    try:
        if mono:
            path = "C:/Windows/Fonts/consolab.ttf" if bold else "C:/Windows/Fonts/consola.ttf"
        else:
            path = "C:/Windows/Fonts/seguisb.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf"
        if not os.path.exists(path):
            path = "C:/Windows/Fonts/arial.ttf"
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

def draw_header(draw, title, subtitle, width=2880):
    draw.rectangle([(0, 0), (width, 140)], fill=(10, 10, 12))
    draw.line([(0, 140), (width, 140)], fill=(30, 30, 36), width=2)
    
    font_title = get_font(42, bold=True)
    font_sub = get_font(24)
    font_badge = get_font(20, mono=True)
    
    draw.text((80, 32), title, font=font_title, fill=(240, 240, 245))
    draw.text((80, 88), subtitle, font=font_sub, fill=(140, 140, 150))
    
    # Status pills on right
    draw.rounded_rectangle([(width - 480, 48), (width - 320, 92)], radius=6, fill=(18, 38, 24), outline=(46, 125, 50))
    draw.text((width - 460, 58), "PIXEL 6 PRO", font=font_badge, fill=(61, 220, 132))
    
    draw.rounded_rectangle([(width - 300, 48), (width - 80, 92)], radius=6, fill=(20, 24, 34), outline=(50, 70, 110))
    draw.text((width - 280, 58), "OFFLINE ON-DEVICE", font=font_badge, fill=(130, 170, 255))

def render_screenshot_1():
    # Live AMOLED Lockscreen Feed (True Black)
    w, h = 2880, 1800
    img = Image.new("RGB", (w, h), (4, 4, 6))
    draw = ImageDraw.Draw(img)
    draw_header(draw, "Stealth Voice Copilot", "Lockscreen AMOLED Live Feed | Google Pixel 6 Pro (1440x3120 OLED)")
    
    # Phone frame representation
    phone_w, phone_h = 880, 1540
    px = (w - phone_w) // 2
    py = 180
    
    # Phone border & subtle glow
    draw.rounded_rectangle([(px - 4, py - 4), (px + phone_w + 4, py + phone_h + 4)], radius=48, fill=(35, 35, 40), outline=(50, 50, 60), width=2)
    draw.rounded_rectangle([(px, py), (px + phone_w, py + phone_h)], radius=44, fill=(0, 0, 0)) # AMOLED true black
    
    # Status bar inside phone
    draw.ellipse([(px + 40, py + 36), (px + 54, py + 50)], fill=(61, 220, 132))
    draw.text((px + 66, py + 34), "LISTENING", font=get_font(20, bold=True, mono=True), fill=(61, 220, 132))
    draw.text((px + 210, py + 36), "210ms", font=get_font(18, mono=True), fill=(90, 90, 90))
    
    draw.rounded_rectangle([(px + phone_w - 320, py + 26), (px + phone_w - 230, py + 60)], radius=6, fill=(25, 25, 25))
    draw.text((px + phone_w - 305, py + 32), "HYBRID", font=get_font(18, mono=True), fill=(160, 160, 160))
    
    draw.rounded_rectangle([(px + phone_w - 215, py + 26), (px + phone_w - 110, py + 60)], radius=6, fill=(40, 40, 25))
    draw.text((px + phone_w - 200, py + 32), "RO: ON", font=get_font(18, mono=True), fill=(180, 180, 140))
    
    draw.rounded_rectangle([(px + phone_w - 95, py + 26), (px + phone_w - 30, py + 60)], radius=6, fill=(25, 25, 25))
    draw.text((px + phone_w - 82, py + 32), "HUD", font=get_font(18, mono=True), fill=(210, 210, 210))
    
    # Quick scenario chips
    chips = ["DETENTION", "TONU", "RATE CON", "LAYOVER", "HOS & ELD", "OS&D", "LUMPER"]
    cx = px + 40
    for chip in chips:
        bw = len(chip) * 14 + 20
        draw.rounded_rectangle([(cx, py + 86), (cx + bw, py + 124)], radius=8, fill=(16, 16, 16))
        draw.text((cx + 10, py + 94), chip, font=get_font(16, mono=True), fill=(110, 110, 110))
        cx += bw + 12
    
    # Turn Card 1 (Detention)
    cy = py + 160
    draw.rounded_rectangle([(px + 36, cy), (px + phone_w - 36, cy + 540)], radius=16, fill=(10, 10, 12))
    draw.text((px + 64, cy + 28), "Interviewer Question:", font=get_font(20, bold=True), fill=(130, 130, 130))
    draw.text((px + 64, cy + 62), "\"What is your standard policy when a driver is delayed at a facility?\"", font=get_font(24), fill=(160, 160, 160))
    draw.text((px + 64, cy + 106), "Traducere intrebare:", font=get_font(18, bold=True), fill=(120, 120, 95))
    draw.text((px + 64, cy + 134), "\"Care este politica ta standard cand un sofer este intarziat la incarcare sau descarcare?\"", font=get_font(21), fill=(145, 145, 120))
    
    draw.line([(px + 64, cy + 185), (px + phone_w - 64, cy + 185)], fill=(28, 28, 32), width=1)
    
    draw.text((px + 64, cy + 205), "Recommended English Answer:", font=get_font(22, bold=True), fill=(215, 215, 215))
    ans_text1 = "Standard industry policy allows two free hours for loading or unloading. I contact the broker at the 90-minute mark to notify them detention is starting, make sure the driver gets their BOL stamped with exact in and out times, and submit our GPS arrival logs for hourly compensation."
    
    # Word wrap for answer
    words = ans_text1.split(" ")
    lines = []
    cur = []
    for w_word in words:
        cur.append(w_word)
        if len(" ".join(cur)) > 52:
            lines.append(" ".join(cur[:-1]))
            cur = [w_word]
    if cur: lines.append(" ".join(cur))
    
    ay = cy + 245
    for l in lines:
        draw.text((px + 64, ay), l, font=get_font(25, bold=True), fill=(245, 245, 245))
        ay += 36
        
    draw.text((px + 64, ay + 14), "Explicatie Romana:", font=get_font(18, bold=True), fill=(130, 130, 100))
    draw.text((px + 64, ay + 42), "Standardul este de 2 ore libere. Notifici brokerul la 1.5 ore, ceri stampila cu ora exacta pe BOL si atasezi log-urile GPS pentru plata detentiei ($50-$75/ora).", font=get_font(20), fill=(155, 155, 125))
    
    draw.text((px + 64, cy + 495), "DETENTION POLICY", font=get_font(18, bold=True, mono=True), fill=(80, 80, 80))
    draw.text((px + phone_w - 240, cy + 495), "210ms | 96% conf", font=get_font(18, mono=True), fill=(80, 80, 80))
    
    # Turn Card 2 (TONU)
    cy2 = cy + 565
    draw.rounded_rectangle([(px + 36, cy2), (px + phone_w - 36, cy2 + 480)], radius=16, fill=(10, 10, 12))
    draw.text((px + 64, cy2 + 28), "Interviewer Question:", font=get_font(20, bold=True), fill=(130, 130, 130))
    draw.text((px + 64, cy2 + 62), "\"The broker cancels the load while the driver is on site. What is your TONU fee?\"", font=get_font(24), fill=(160, 160, 160))
    
    draw.line([(px + 64, cy2 + 115), (px + phone_w - 64, cy2 + 115)], fill=(28, 28, 32), width=1)
    draw.text((px + 64, cy2 + 135), "Recommended English Answer:", font=get_font(22, bold=True), fill=(215, 215, 215))
    ans_text2 = "If a load cancels after the driver is dispatched or has arrived at the facility, I immediately request a revised rate confirmation with the standard TONU fee of $150 to $250. Once secured, I book a replacement load to minimize deadhead."
    words2 = ans_text2.split(" ")
    lines2 = []
    cur2 = []
    for w_word in words2:
        cur2.append(w_word)
        if len(" ".join(cur2)) > 52:
            lines2.append(" ".join(cur2[:-1]))
            cur2 = [w_word]
    if cur2: lines2.append(" ".join(cur2))
    
    ay2 = cy2 + 175
    for l in lines2:
        draw.text((px + 64, ay2), l, font=get_font(25, bold=True), fill=(245, 245, 245))
        ay2 += 36
        
    draw.text((px + 64, ay2 + 14), "Explicatie Romana:", font=get_font(18, bold=True), fill=(130, 130, 100))
    draw.text((px + 64, ay2 + 42), "Ceri imediat Rate Con actualizat cu taxa TONU ($150-$250) si cauti o noua cursa pentru a reduce mersul in gol.", font=get_font(20), fill=(155, 155, 125))
    
    draw.text((px + 64, cy2 + 435), "TONU CLAIM", font=get_font(18, bold=True, mono=True), fill=(80, 80, 80))
    draw.text((px + phone_w - 240, cy2 + 435), "185ms | 94% conf", font=get_font(18, mono=True), fill=(80, 80, 80))
    
    # Bottom Controls Row
    ctrl_y = py + phone_h - 130
    draw.line([(px, ctrl_y - 10), (px + phone_w, ctrl_y - 10)], fill=(20, 20, 20), width=1)
    
    # Delete icon circle
    draw.ellipse([(px + 80, ctrl_y + 15), (px + 150, ctrl_y + 85)], fill=(25, 25, 25))
    draw.text((px + 100, ctrl_y + 36), "CLR", font=get_font(20, bold=True, mono=True), fill=(120, 120, 120))
    
    # Main Mic circle
    draw.ellipse([(px + phone_w // 2 - 50, ctrl_y), (px + phone_w // 2 + 50, ctrl_y + 100)], fill=(61, 220, 132))
    draw.text((px + phone_w // 2 - 25, ctrl_y + 34), "MIC", font=get_font(26, bold=True, mono=True), fill=(0, 0, 0))
    
    # Test bench circle
    draw.ellipse([(px + phone_w - 150, ctrl_y + 15), (px + phone_w - 80, ctrl_y + 85)], fill=(25, 25, 25))
    draw.text((px + phone_w - 132, ctrl_y + 36), "TEST", font=get_font(18, bold=True, mono=True), fill=(120, 120, 120))

    img.save(os.path.join(IMG_DIR, "stealth-voice-copilot-1.png"), quality=95)

def render_screenshot_2():
    # Floating System HUD Mode over Zoom Call
    w, h = 2880, 1800
    img = Image.new("RGB", (w, h), (14, 16, 22))
    draw = ImageDraw.Draw(img)
    draw_header(draw, "Stealth Voice Copilot", "Floating System HUD Overlay | Remote Dispatcher Interview Call")
    
    # Main Video Conference Window UI
    win_x, win_y = 120, 180
    win_w, win_h = 2640, 1540
    draw.rounded_rectangle([(win_x, win_y), (win_x + win_w, win_y + win_h)], radius=24, fill=(20, 22, 28), outline=(40, 44, 56), width=2)
    
    # Top bar of meeting
    draw.rectangle([(win_x, win_y), (win_x + win_w, win_y + 70)], fill=(28, 30, 38))
    draw.text((win_x + 40, win_y + 20), "Zoom Meeting: Freight Operations Logistics Coordinator Interview", font=get_font(24, bold=True), fill=(220, 225, 235))
    draw.rounded_rectangle([(win_x + win_w - 220, win_y + 16), (win_x + win_w - 40, win_y + 54)], radius=8, fill=(180, 40, 40))
    draw.text((win_x + win_w - 170, win_y + 24), "LEAVE", font=get_font(18, bold=True), fill=(255, 255, 255))
    
    # Video grid (Interviewer Tile & Candidate Tile)
    gw = (win_w - 60) // 2
    gh = win_h - 260
    
    # Tile 1: Interviewer
    draw.rounded_rectangle([(win_x + 20, win_y + 90), (win_x + 20 + gw, win_y + 90 + gh)], radius=16, fill=(12, 14, 18))
    draw.rectangle([(win_x + 20, win_y + 90 + gh - 60), (win_x + 20 + gw, win_y + 90 + gh)], fill=(18, 20, 26))
    draw.text((win_x + 40, win_y + 90 + gh - 44), "Michael Vance (Freight Operations Director, Chicago Logistics LLC)", font=get_font(22), fill=(200, 200, 210))
    # Simulated camera waveform
    draw.text((win_x + 180, win_y + 360), "[Speaking...]", font=get_font(36, bold=True), fill=(100, 120, 150))
    draw.text((win_x + 180, win_y + 420), "\"What is your exact process when you see a discrepancy on the BOL at pickup?\"", font=get_font(30), fill=(180, 190, 210))
    
    # Tile 2: Candidate
    draw.rounded_rectangle([(win_x + 40 + gw, win_y + 90), (win_x + 40 + gw * 2, win_y + 90 + gh)], radius=16, fill=(16, 18, 22))
    draw.rectangle([(win_x + 40 + gw, win_y + 90 + gh - 60), (win_x + 40 + gw * 2, win_y + 90 + gh)], fill=(18, 20, 26))
    draw.text((win_x + 60 + gw, win_y + 90 + gh - 44), "Alexandr Motologa (Candidate)", font=get_font(22), fill=(200, 200, 210))
    draw.text((win_x + 60 + gw + 220, win_y + 420), "[Video Live - Listening attentively]", font=get_font(28), fill=(110, 130, 160))
    
    # Floating Stealth HUD Bubble at the bottom
    hud_w = 2100
    hud_h = 130
    hud_x = (w - hud_w) // 2
    hud_y = win_y + win_h - 180
    
    # Ultra-translucent stealth overlay
    draw.rounded_rectangle([(hud_x, hud_y), (hud_x + hud_w, hud_y + hud_h)], radius=28, fill=(8, 8, 12), outline=(61, 220, 132), width=2)
    
    # Green live pulse dot
    draw.ellipse([(hud_x + 36, hud_y + 46), (hud_x + 56, hud_y + 66)], fill=(61, 220, 132))
    draw.text((hud_x + 72, hud_y + 42), "STEALTH HUD", font=get_font(20, bold=True, mono=True), fill=(61, 220, 132))
    
    hud_ans = "Never sign a clean BOL. Contact dispatch immediately, annotate exact piece shortage on the document, take photos, and notify broker before departure."
    draw.text((hud_x + 250, hud_y + 38), hud_ans, font=get_font(26, bold=True), fill=(245, 245, 245))
    draw.text((hud_x + 250, hud_y + 78), "Nu semneaza curat. Noteaza lipsurile pe BOL, fa poze si anunta brokerul inainte de plecare.", font=get_font(20), fill=(160, 160, 130))
    draw.text((hud_x + hud_w - 220, hud_y + 50), "175ms | 98%", font=get_font(20, mono=True), fill=(100, 100, 100))

    img.save(os.path.join(IMG_DIR, "stealth-voice-copilot-2.png"), quality=95)

def render_screenshot_3():
    # Audio Pipeline & Silero VAD Diagnostic Dashboard
    w, h = 2880, 1800
    img = Image.new("RGB", (w, h), (8, 9, 12))
    draw = ImageDraw.Draw(img)
    draw_header(draw, "Stealth Voice Copilot", "Audio Conditioning, Silero VAD v5 & Acoustic Normalization Diagnostic")
    
    # 4 diagnostic cards
    # Card 1: 16kHz PCM Stream & Noise Suppression
    cw = 1300
    ch = 720
    
    # Top Left
    draw.rounded_rectangle([(100, 180), (100 + cw, 180 + ch)], radius=16, fill=(14, 15, 20), outline=(28, 30, 40))
    draw.text((140, 215), "16kHz Mono PCM Audio Stream with Hardware AEC & Noise Suppression", font=get_font(26, bold=True), fill=(230, 230, 240))
    draw.text((140, 255), "AudioRecord buffer: 512 samples/frame | AudioSource: VOICE_RECOGNITION", font=get_font(18, mono=True), fill=(130, 140, 160))
    
    # Simulated Waveform
    for i in range(1200):
        t = i / 120.0
        val = math.sin(t * 8) * math.sin(t * 1.5) * 110 + math.sin(t * 22) * 25
        if 400 < i < 700:
            val *= 0.15 # silence gap
        draw.line([(140 + i, 460 - val), (140 + i, 460 + val)], fill=(61, 220, 132) if abs(val) > 25 else (50, 70, 60), width=1)
        
    draw.text((140, 620), "RMS Signal Level: 0.042 (Normal Speech Range)", font=get_font(20, mono=True), fill=(200, 200, 210))
    draw.text((140, 660), "Acoustic Echo Canceler: ACTIVE | Noise Suppressor: ACTIVE (-18dB attenuation)", font=get_font(18, mono=True), fill=(100, 190, 130))

    # Top Right: Silero VAD v5 Evaluation
    draw.rounded_rectangle([(1480, 180), (1480 + cw, 180 + ch)], radius=16, fill=(14, 15, 20), outline=(28, 30, 40))
    draw.text((1520, 215), "Silero VAD v5 Recurrent Neural Probability Output", font=get_font(26, bold=True), fill=(230, 230, 240))
    draw.text((1520, 255), "Input: 512 samples PCM float | Threshold: 0.50 | Hangover: 800ms silence", font=get_font(18, mono=True), fill=(130, 140, 160))
    
    # VAD Step Probability Graph
    draw.line([(1520, 560), (1520 + 1200, 560)], fill=(40, 44, 56), width=1)
    draw.line([(1520, 420), (1520 + 1200, 420)], fill=(80, 50, 50), width=1) # 0.5 threshold line
    draw.text((1520 + 1120, 400), "Threshold 0.50", font=get_font(16, mono=True), fill=(200, 100, 100))
    
    points = []
    for i in range(1200):
        prob = 0.92 if (i < 400 or i > 750) else 0.12
        points.append((1520 + i, 560 - prob * 260))
    for i in range(len(points) - 1):
        draw.line([points[i], points[i+1]], fill=(61, 220, 132), width=3)
        
    draw.text((1520, 620), "State: SPEECH_RELEASED (800ms silence detected) -> Triggering STT Ingestion", font=get_font(20, mono=True), fill=(200, 200, 210))
    draw.text((1520, 660), "Accumulated Speech Segment: 3.84s (61,440 float samples)", font=get_font(18, mono=True), fill=(100, 190, 130))

    # Bottom Left: Sherpa-ONNX STT & Lexicon Normalizer
    draw.rounded_rectangle([(100, 940), (100 + cw, 940 + ch)], radius=16, fill=(14, 15, 20), outline=(28, 30, 40))
    draw.text((140, 975), "Sherpa-ONNX Whisper Base EN & Lexicon Normalizer", font=get_font(26, bold=True), fill=(230, 230, 240))
    draw.text((140, 1015), "Acoustic Normalization Engine | Latency: 320ms on Tensor G1", font=get_font(18, mono=True), fill=(130, 140, 160))
    
    draw.text((140, 1080), "Raw Acoustic Transcript:", font=get_font(20, bold=True), fill=(160, 160, 170))
    draw.text((140, 1115), "\"make sure the driver gets the ray con and b o l signed with no toe new\"", font=get_font(24, mono=True), fill=(210, 160, 160))
    
    draw.text((140, 1180), "Normalized Logistics Transcript (Rules Applied):", font=get_font(20, bold=True), fill=(160, 160, 170))
    draw.text((140, 1215), "\"make sure the driver gets the Rate Confirmation and BOL signed with no TONU\"", font=get_font(24, mono=True), fill=(140, 220, 160))
    
    draw.text((140, 1290), "Replacements: [ray con -> Rate Confirmation] [b o l -> BOL] [toe new -> TONU]", font=get_font(18, mono=True), fill=(100, 180, 240))

    # Bottom Right: Latency & Memory Profile
    draw.rounded_rectangle([(1480, 940), (1480 + cw, 940 + ch)], radius=16, fill=(14, 15, 20), outline=(28, 30, 40))
    draw.text((1520, 975), "Execution Latency & Memory Footprint Breakdown", font=get_font(26, bold=True), fill=(230, 230, 240))
    draw.text((1520, 1015), "Profile on Pixel 6 Pro (12 GB LPDDR5 RAM / Google Tensor G1)", font=get_font(18, mono=True), fill=(130, 140, 160))
    
    table_rows = [
        ("Pipeline Stage", "Time", "RAM Usage", "Status"),
        ("AudioRecord Ingestion (512 samples)", "32.0 ms", "8.2 MB", "ACTIVE"),
        ("Silero VAD Evaluation (ONNX)", "1.4 ms", "14.5 MB", "PASS"),
        ("Sherpa-ONNX Whisper Base EN", "320.0 ms", "142.0 MB", "PASS"),
        ("Freight Lexicon Normalizer", "1.2 ms", "1.1 MB", "PASS"),
        ("Deterministic Expert Fallback", "2.4 ms", "4.8 MB", "INSTANT"),
        ("Local Llama 3.2 1B (Q4_K_M)", "1320.0 ms", "740.0 MB", "RESIDENT"),
        ("AMOLED UI Render", "5.6 ms", "18.0 MB", "OPTIMAL")
    ]
    
    ty = 1080
    for idx, (col1, col2, col3, col4) in enumerate(table_rows):
        font_t = get_font(20, bold=(idx == 0), mono=True)
        color_t = (255, 255, 255) if idx == 0 else (190, 195, 205)
        draw.text((1520, ty), col1, font=font_t, fill=color_t)
        draw.text((1980, ty), col2, font=font_t, fill=color_t)
        draw.text((2200, ty), col3, font=font_t, fill=color_t)
        draw.text((2460, ty), col4, font=font_t, fill=(61, 220, 132) if col4 in ["PASS", "ACTIVE", "OPTIMAL", "INSTANT", "RESIDENT"] else color_t)
        ty += 52

    img.save(os.path.join(IMG_DIR, "stealth-voice-copilot-3.png"), quality=95)

def render_screenshot_4():
    # US Freight Logistics Knowledge & Scenario Matrix
    w, h = 2880, 1800
    img = Image.new("RGB", (w, h), (8, 9, 12))
    draw = ImageDraw.Draw(img)
    draw_header(draw, "Stealth Voice Copilot", "US Freight Dispatching & Billing Grounding Knowledge Matrix")
    
    cards = [
        ("Rate Confirmation Review", "Gross rate, mileage, appointment windows, commodity, detention & TONU clauses.", "Verificare pret total, ore de incarcare/descarcare si clauze de detentie."),
        ("Detention Policy ($50-$75/hr)", "2 free hours standard. Notify broker at 90 minutes. Stamped BOL and GPS arrival logs.", "2 ore libere. Notificare broker la 1.5h, stampila pe BOL cu ora exacta intrare/iesire."),
        ("TONU Claim ($150-$250)", "Load cancelled after driver is dispatched or on site. Request updated rate con immediately.", "Cursa anulata dupa plecarea soferului. Ceri imediat Rate Con actualizat cu TONU."),
        ("Layover Procedures ($200-$300)", "Facility unable to unload until subsequent day. Secure written layover authorization.", "Descarcare amanata pe a doua zi. Negociere taxa layover $200-$300 plus cazare."),
        ("Factoring & NOA Invoicing", "Complete packet: Rate Con, signed clean POD, lumper receipts submitted for same-day ACH.", "Pachet complet de facturare: Rate Con, POD stampilat si bonuri lumper pentru plata rapida."),
        ("Hours of Service (HOS) & ELD", "11h driving, 14h window, 10h break, 30m break at 8h, 70h/8 days with 34h restart.", "Reguli FMCSA: maxim 11h condus, 14h tura, 10h pauza obligatorie, verificare pe ELD.")
    ]
    
    col_w = 1300
    row_h = 440
    for idx, (title, en_text, ro_text) in enumerate(cards):
        col = idx % 2
        row = idx // 2
        bx = 100 + col * 1380
        by = 180 + row * 490
        
        draw.rounded_rectangle([(bx, by), (bx + col_w, by + row_h)], radius=16, fill=(13, 14, 18), outline=(32, 34, 44))
        draw.text((bx + 40, by + 35), title, font=get_font(28, bold=True), fill=(240, 240, 250))
        
        draw.text((bx + 40, by + 100), "Standard Response Logic (English):", font=get_font(20, bold=True), fill=(160, 160, 180))
        draw.text((bx + 40, by + 140), en_text, font=get_font(23), fill=(215, 215, 225))
        
        draw.text((bx + 40, by + 230), "Context si explicatii (Romana):", font=get_font(20, bold=True), fill=(140, 140, 110))
        draw.text((bx + 40, by + 270), ro_text, font=get_font(22), fill=(175, 175, 145))
        
        draw.rounded_rectangle([(bx + 40, by + 360), (bx + 260, by + 400)], radius=6, fill=(20, 22, 30))
        draw.text((bx + 55, by + 370), "DETERMINISTIC + LLM", font=get_font(16, mono=True), fill=(61, 220, 132))

    img.save(os.path.join(IMG_DIR, "stealth-voice-copilot-4.png"), quality=95)

def render_screenshot_5():
    # Pixel Quick Settings Tile & Hardware Controls
    w, h = 2880, 1800
    img = Image.new("RGB", (w, h), (10, 11, 14))
    draw = ImageDraw.Draw(img)
    draw_header(draw, "Stealth Voice Copilot", "Quick Settings Tile & Physical Covert Hardware Trigger Controls")
    
    # Left Card: Quick Settings Shade
    draw.rounded_rectangle([(120, 180), (1380, 1680)], radius=24, fill=(16, 17, 22), outline=(35, 38, 48))
    draw.text((180, 230), "Android Lockscreen Quick Settings Shade", font=get_font(30, bold=True), fill=(240, 240, 250))
    draw.text((180, 280), "Single pull-down gesture provides one-tap toggle access without unlocking", font=get_font(20), fill=(150, 150, 165))
    
    tiles = [
        ("Internet", "Connected", False),
        ("Bluetooth", "On", False),
        ("Copilot", "Listening (Active)", True),
        ("Flashlight", "Off", False),
        ("Do Not Disturb", "On", False),
        ("Auto-rotate", "Off", False)
    ]
    
    ty = 360
    for i, (t_name, t_sub, active) in enumerate(tiles):
        row = i // 2
        col = i % 2
        tx = 180 + col * 550
        tile_y = ty + row * 200
        
        bg_col = (46, 125, 50) if active else (26, 28, 36)
        txt_col = (255, 255, 255) if active else (180, 180, 195)
        sub_col = (200, 255, 200) if active else (120, 120, 135)
        
        draw.rounded_rectangle([(tx, tile_y), (tx + 500, tile_y + 160)], radius=20, fill=bg_col)
        draw.ellipse([(tx + 30, tile_y + 40), (tx + 100, tile_y + 110)], fill=(0, 0, 0, 80))
        draw.text((tx + 120, tile_y + 45), t_name, font=get_font(26, bold=True), fill=txt_col)
        draw.text((tx + 120, tile_y + 85), t_sub, font=get_font(20), fill=sub_col)

    # Right Card: Hardware Controls
    draw.rounded_rectangle([(1500, 180), (2760, 1680)], radius=24, fill=(16, 17, 22), outline=(35, 38, 48))
    draw.text((1560, 230), "Covert Physical & Gesture Triggers", font=get_font(30, bold=True), fill=(240, 240, 250))
    draw.text((1560, 280), "Zero-touch and pocket controls engineered for discrete operation", font=get_font(20), fill=(150, 150, 165))
    
    triggers = [
        ("Pixel Quick Tap (Back Tap)", "Double-tapping the back glass summons or focuses the copilot directly over the lockscreen."),
        ("Volume Up Double-Click", "Instantly clears conversation dialogue history and resets context. Triggers triple subtle haptic pulse."),
        ("Volume Down Long-Press", "Toggles Romanian translations ON or OFF without touching screen. OFF cuts generation time to < 1.5s."),
        ("180s Silence Watchdog", "Auto-standby watchdog halts microphone capture after 180s of silence to conserve battery.")
    ]
    
    t_y = 380
    for name, desc in triggers:
        draw.rounded_rectangle([(1560, t_y), (2700, t_y + 240)], radius=18, fill=(22, 24, 32), outline=(40, 44, 56))
        draw.text((1600, t_y + 35), name, font=get_font(26, bold=True), fill=(61, 220, 132))
        
        # Word wrap
        w_list = desc.split(" ")
        d_lines = []
        cur = []
        for w_word in w_list:
            cur.append(w_word)
            if len(" ".join(cur)) > 55:
                d_lines.append(" ".join(cur[:-1]))
                cur = [w_word]
        if cur: d_lines.append(" ".join(cur))
        
        dy = t_y + 85
        for dl in d_lines:
            draw.text((1600, dy), dl, font=get_font(22), fill=(190, 195, 205))
            dy += 36
            
        t_y += 290

    img.save(os.path.join(IMG_DIR, "stealth-voice-copilot-5.png"), quality=95)

def render_demo_gif():
    # 8 distinct animation frames capturing the live speech-to-text -> inference loop
    frames = []
    w, h = 960, 1440
    
    steps = [
        ("STANDBY", (70, 70, 70), "STANDBY - WAITING FOR AUDIO", None, None, None),
        ("LISTENING", (61, 220, 132), "SPEECH DETECTED (VAD prob 0.88)", "Interviewer speaking...", None, None),
        ("TRANSCRIBING", (100, 180, 255), "SHERPA-ONNX TRANSCRIBING", "\"What is your standard policy for handling detention?\"", None, None),
        ("INFERRING", (255, 167, 38), "INFERRING RESPONSE (HYBRID)", "\"What is your standard policy for handling detention?\"", "Evaluating freight knowledge matrix...", None),
        ("ANSWER READY", (61, 220, 132), "RESPONSE READY (210ms)", "\"What is your standard policy for handling detention?\"", "Standard policy is two free hours for loading or unloading. I contact the broker at 90 minutes and secure stamped BOL timestamps.", "2 ore libere. Notificare broker la 1.5h si stampila pe BOL."),
        ("ANSWER READY", (61, 220, 132), "RESPONSE READY (210ms)", "\"What is your standard policy for handling detention?\"", "Standard policy is two free hours for loading or unloading. I contact the broker at 90 minutes and secure stamped BOL timestamps.", "2 ore libere. Notificare broker la 1.5h si stampila pe BOL."),
        ("NEXT QUESTION", (61, 220, 132), "NEW PROMPT DETECTED", "\"Broker cancels load after driver arrives. What do you do?\"", "Request revised Rate Con with $150-$250 TONU fee and rebook driver immediately.", "Ceri TONU $150-$250 pe Rate Con si cauti alta cursa."),
        ("NEXT QUESTION", (61, 220, 132), "NEW PROMPT DETECTED", "\"Broker cancels load after driver arrives. What do you do?\"", "Request revised Rate Con with $150-$250 TONU fee and rebook driver immediately.", "Ceri TONU $150-$250 pe Rate Con si cauti alta cursa.")
    ]
    
    for status, status_color, header_sub, q_text, a_text, ro_text in steps:
        frame = Image.new("RGB", (w, h), (0, 0, 0)) # AMOLED true black
        draw = ImageDraw.Draw(frame)
        
        # Phone top bar
        draw.ellipse([(40, 46), (56, 62)], fill=status_color)
        draw.text((70, 42), status, font=get_font(20, bold=True, mono=True), fill=status_color)
        draw.text((w - 180, 42), "RO: ON", font=get_font(18, mono=True), fill=(180, 180, 140))
        
        draw.text((40, 100), header_sub, font=get_font(18, mono=True), fill=(110, 110, 110))
        draw.line([(40, 135), (w - 40, 135)], fill=(25, 25, 25), width=1)
        
        if q_text:
            draw.rounded_rectangle([(36, 170), (w - 36, 320)], radius=14, fill=(12, 12, 15))
            draw.text((60, 195), "Interviewer Question:", font=get_font(18, bold=True), fill=(130, 130, 130))
            draw.text((60, 235), q_text, font=get_font(22), fill=(190, 190, 190))
            
        if a_text:
            draw.rounded_rectangle([(36, 350), (w - 36, 850)], radius=16, fill=(10, 10, 12), outline=(40, 40, 50))
            draw.text((60, 385), "Recommended Response (Read Aloud):", font=get_font(20, bold=True), fill=(210, 210, 210))
            
            # wrap
            words = a_text.split(" ")
            lines = []
            cur = []
            for wr in words:
                cur.append(wr)
                if len(" ".join(cur)) > 36:
                    lines.append(" ".join(cur[:-1]))
                    cur = [wr]
            if cur: lines.append(" ".join(cur))
            
            ay = 435
            for l in lines:
                draw.text((60, ay), l, font=get_font(25, bold=True), fill=(250, 250, 250))
                ay += 38
                
            if ro_text:
                draw.text((60, ay + 20), "Nota explicativa (Romana):", font=get_font(18, bold=True), fill=(140, 140, 110))
                draw.text((60, ay + 55), ro_text, font=get_font(21), fill=(170, 170, 140))
                
            draw.text((60, 790), "CONFIDENCE: 96% | ON-DEVICE INFERENCE", font=get_font(16, mono=True), fill=(80, 80, 80))
            
        # Bottom controls
        draw.line([(0, h - 140), (w, h - 140)], fill=(20, 20, 20), width=1)
        draw.ellipse([(w // 2 - 45, h - 115), (w // 2 + 45, h - 25)], fill=(61, 220, 132) if status != "STANDBY" else (30, 30, 30))
        draw.text((w // 2 - 22, h - 80), "MIC", font=get_font(22, bold=True, mono=True), fill=(0, 0, 0) if status != "STANDBY" else (150, 150, 150))
        
        frames.append(frame)
        
    gif_path = os.path.join(IMG_DIR, "stealth_voice_copilot_demo.gif")
    frames[0].save(
        gif_path,
        save_all=True,
        append_images=frames[1:],
        duration=1200,
        loop=0
    )

if __name__ == "__main__":
    print("Generating screenshot 1...")
    render_screenshot_1()
    print("Generating screenshot 2...")
    render_screenshot_2()
    print("Generating screenshot 3...")
    render_screenshot_3()
    print("Generating screenshot 4...")
    render_screenshot_4()
    print("Generating screenshot 5...")
    render_screenshot_5()
    print("Generating animated demo GIF...")
    render_demo_gif()
    print("Assets generated successfully!")
