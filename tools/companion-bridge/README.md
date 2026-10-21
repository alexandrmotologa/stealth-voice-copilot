# PC Companion Bridge

Utility for beaming interview questions from a laptop or desktop computer directly to the phone teleprompter.

## When to use

During video interviews (Google Meet, Zoom, Microsoft Teams) where you wear headphones:
- The phone microphone cannot capture sound coming through your headphones.
- The companion bridge routes questions from your laptop to the phone screen with sub-100ms latency.

## Requirements

- Python 3.8+
- ADB enabled on phone (USB debugging or Wi-Fi debugging)

## Usage

### 1. Interactive terminal
Type any question and hit Enter:
```bash
python pc_companion_bridge.py
```

### 2. Clipboard auto relay
Copy text from meeting chat or notes with Ctrl+C, and it immediately appears on your phone:
```bash
pip install pyperclip
python pc_companion_bridge.py --clipboard
```

### 3. One-off query
```bash
python pc_companion_bridge.py --query "Explain the difference between optimistic and pessimistic locking"
```
