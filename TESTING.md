# Local Testing & Development Guide

This guide explains how to build, install, and test the **OpenClaw Assistant Replacement** on your local machine using Android Studio and a supported device.

## Prerequisites
- **Android Studio** (Hedgehog or newer recommended).
- **Physical Device**: Pixel 8/9, Galaxy S24/S25 (devices with **AICore/Gemini Nano**).
- **Emulator**: Use an "API 34" image with Google Play Services.

## Step 1: Clone and Open
```bash
git clone https://github.com/allophylus/android-gemini-replacement.git
cd android-gemini-replacement
```
1. Open Android Studio.
2. Select **File > Open** and choose this project folder.

## Step 2: Build and Install
1. Connect your device via USB (ensure **USB Debugging** is on).
2. Click **Run > Run 'app'** in Android Studio.

## Step 3: Enable as Default Assistant
Once installed, you must manually grant the app assistant permissions:
1. Open **Settings** on your Android device.
2. Go to **Apps > Default apps > Digital assistant app**.
3. Tap **Default digital assistant app**.
4. Select **OpenClaw Assistant**.

## Step 4: Testing the Logic
1. Open any app (e.g., a shopping app or browser).
2. **Long-press the Home button** or **Swipe from a bottom corner**.
3. Watch the **Logcat** in Android Studio (filter by `OpenClawAssistant`).
4. You should see the extracted screen text in the logs.

## Step 5: AICore Verification
To verify your device supports the local LLM:
1. Ensure the **AICore** app is updated in the Play Store.
2. Run the provided `scripts/check_aicore.sh` (once implemented) to verify model availability.
