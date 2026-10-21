#!/usr/bin/env python3
"""
Stealth Voice Copilot - PC Audio & Prompt Companion Bridge
==========================================================
Allows seamless integration between your laptop/desktop meeting
(Google Meet, Zoom, Microsoft Teams) and your phone's teleprompter screen.

Use cases:
1. When wearing headphones on your PC: The phone cannot hear the meeting audio.
   This utility allows instant question relay via clipboard, hotkey, or interactive terminal.
2. Direct ADB bridge: Delivers questions directly to the phone's live teleprompter
   in sub-100ms via USB or Wi-Fi ADB.

Usage:
    python pc_companion_bridge.py               # Interactive CLI mode
    python pc_companion_bridge.py --query "..." # Send single question
    python pc_companion_bridge.py --clipboard   # Watch clipboard for new questions
"""

import sys
import subprocess
import argparse
import time
import shutil

PACKAGE_NAME = "com.copilot.stealth.debug"
ACTIVITY_NAME = ".ui.StealthActivity"

def find_adb():
    adb_path = shutil.which("adb")
    if not adb_path:
        import os
        local_app_data = os.environ.get("LOCALAPPDATA", "")
        candidate = os.path.join(local_app_data, "Android", "Sdk", "platform-tools", "adb.exe")
        if os.path.exists(candidate):
            return candidate
    return adb_path or "adb"

ADB_BIN = find_adb()

def send_query_to_device(query_text: str):
    clean_text = query_text.strip().replace('"', '\\"').replace("'", "\\'")
    if not clean_text:
        return False

    cmd = [
        ADB_BIN, "shell", "am", "start",
        "-n", f"{PACKAGE_NAME}/{ACTIVITY_NAME}",
        "--es", "test_query", clean_text
    ]

    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        print(f"[OK] Question beamed to teleprompter: \"{clean_text[:60]}...\"")
        return True
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] Failed to send query via ADB: {e.stderr}")
        return False
    except Exception as ex:
        print(f"[ERROR] {ex}")
        return False

def interactive_mode():
    print("=" * 65)
    print(" Stealth Voice Copilot - PC Companion Bridge")
    print(f" Connected via ADB: {ADB_BIN}")
    print(" Type any question and press [Enter] to beam it to your phone.")
    print(" Type 'exit' or press Ctrl+C to quit.")
    print("=" * 65)

    while True:
        try:
            query = input("\n[PC -> Phone] Question: ").strip()
            if query.lower() in ("exit", "quit", "q"):
                print("Exiting companion bridge.")
                break
            if query:
                send_query_to_device(query)
        except (KeyboardInterrupt, EOFError):
            print("\nExiting.")
            break

def clipboard_monitor_mode():
    try:
        import pyperclip
    except ImportError:
        print("[!] pyperclip not installed. Run: pip install pyperclip")
        print("Falling back to interactive mode...")
        interactive_mode()
        return

    print("=" * 65)
    print(" Stealth Voice Copilot - Clipboard Auto-Relay")
    print(" Copy any interviewer question (Ctrl+C on PC), and it will")
    print(" automatically appear on your phone's teleprompter!")
    print(" Press Ctrl+C in terminal to stop.")
    print("=" * 65)

    last_text = ""
    while True:
        try:
            current = pyperclip.paste().strip()
            if current and current != last_text and len(current) > 10:
                last_text = current
                print(f"\n[Detected in Clipboard]: {current[:80]}...")
                send_query_to_device(current)
            time.sleep(0.5)
        except KeyboardInterrupt:
            print("\nStopped clipboard monitor.")
            break

def main():
    parser = argparse.ArgumentParser(description="Stealth Voice Copilot PC Bridge")
    parser.add_argument("--query", "-q", type=str, help="Single question to beam to phone")
    parser.add_argument("--clipboard", "-c", action="store_true", help="Monitor clipboard for questions")

    args = parser.parse_args()

    if args.query:
        send_query_to_device(args.query)
    elif args.clipboard:
        clipboard_monitor_mode()
    else:
        interactive_mode()

if __name__ == "__main__":
    main()
