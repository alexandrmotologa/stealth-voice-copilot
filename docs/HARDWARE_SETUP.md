# Hardware and Gestures Setup Guide

Configuration steps for running Stealth Voice Copilot on the Google Pixel 6 Pro running Android 14 or Android 15.

## 1. Google Pixel 6 Pro Profile

The Pixel 6 Pro utilizes the Google Tensor G1 system-on-chip with an ARM Mali-G78 MP20 GPU and an integrated EdgeTPU. The 12 GB LPDDR5 RAM allocation allows holding quantized 1B to 2B language models resident in memory without triggering low-memory killer (LMK) eviction.

### Recommended System Settings
1. **Display & Dark Theme**: Enable system Dark Theme. Stealth Voice Copilot uses true black (`#000000`), turning off OLED emitters for power conservation.
2. **Lockscreen Display**: Go to `Settings -> Display -> Lock screen` and ensure "Always show time and info" is turned on if you want zero-wake status checks.
3. **Battery Optimization Exemption**:
   * Navigate to `Settings -> Apps -> Stealth Voice Copilot -> Battery`.
   * Set battery usage to **Unrestricted**.
   * This prevents Android Doze and battery saver from terminating the `CopilotForegroundService` audio capture loop.

## 2. Lockscreen Overlay Configuration

Stealth Voice Copilot presents prompt suggestions directly on the lockscreen window without requiring biometric or PIN unlocking.

### Required Permissions
1. **Microphone**: Allow "While using the app" or "All the time".
2. **Notifications**: Allow notifications for foreground audio capture service persistence.
3. **Display over other apps**:
   * Navigate to `Settings -> Apps -> Special app access -> Display over other apps`.
   * Toggle **Allowed** for Stealth Voice Copilot.
   * This enables the secondary floating mini HUD overlay mode.

## 3. Pixel Quick Tap (Back Tap) Setup

You can launch or summon the copilot instantly by double-tapping the back glass of the phone.

1. Open `Settings -> System -> Gestures -> Quick Tap to start actions`.
2. Turn **Use Quick Tap** on.
3. Select **Open app**, tap the gear icon next to it, and choose **Stealth Voice Copilot**.
4. Check "Require stronger taps" if you experience accidental launches during handling.

## 4. Quick Settings Tile Configuration

The application registers `CopilotTileService` as an Android Quick Settings tile for immediate toggle access:

1. Swipe down twice from the top of the screen to expand Quick Settings.
2. Tap the pencil (Edit) icon at the bottom right.
3. Scroll down to find the **Copilot** tile.
4. Drag and drop the tile into the top 4 positions of your Quick Settings grid.
5. Tap the back button to save.

Once configured, swiping down on the lockscreen reveals the tile. A single tap switches the audio engine between **Listening** and **Standby**.

## 5. Physical Hardware Key Listeners

When `StealthActivity` is on screen, the physical volume buttons provide covert input:

* **Volume Up (Double Click)**:
  * Resets conversation context and clears turn history.
  * Triggers a triple haptic pulse to confirm the reset.
* **Volume Down (Long Press)**:
  * Toggles Romanian translation output on or off.
  * In OFF mode, the inference engine skips Romanian token generation, shortening response latency to under 1.5 seconds.
