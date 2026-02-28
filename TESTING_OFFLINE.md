# Testing Mate Assistant in Android Studio

To test the **Mate Assistant** offline using the Android Emulator, follow these steps to ensure the on-device AI (Gemini Nano) and Assistant triggers are functioning correctly.

## 1. Prerequisites (Emulator Setup)
Because Mate relies on **AICore (Gemini Nano)**, the standard emulator needs specific configuration:
- **Device Profile**: Use a **Pixel 8** or **Pixel 8 Pro** system image (API 34+).
- **RAM**: Ensure the emulator has at least **8GB of RAM** allocated (Gemini Nano is memory-intensive).
- **Google Play Services**: Use a system image that includes "Google Play APIs."

## 2. Deployment
1. Open the project in **Android Studio**.
2. Connect your emulator.
3. Click **Run 'app'** (Shift + F10).
4. The **MainActivity** should open, showing the "Mate Assistant Personalization" sliders.

## 3. Registering Mate as the System Assistant
Android only allows one active "Digital Assistant App." You must manually switch to Mate:
1. In the Emulator, go to **Settings > Apps > Default apps**.
2. Tap **Digital assistant app**.
3. Tap **Default digital assistant app**.
4. Select **Mate** from the list and confirm the "Trust this app?" prompt.

## 4. Triggering the Assistant
Once registered, you can trigger Mate's overlay UI:
- **Long-press the Home Button** (or Swipe up from the corner if using gesture navigation).
- **Command Line Trigger**: Run this in your terminal to simulate a voice trigger:
  ```bash
  adb shell am start-voice-interaction
  ```

## 5. Testing Offline Features
To verify the **Local-First** nature of the app:
1. **Toggle Airplane Mode** on the emulator.
2. Trigger the assistant (Long-press Home).
3. **Verify UI**: You should see the "Cozy Office" background (Day/Night depending on your system clock).
4. **Verify Vision**: Open any app (e.g., Contacts or a Browser) and trigger Mate. Check the **Logcat** in Android Studio for the tag `Mate` to see the extracted screen text:
   ```text
   D/Mate: Assist triggered. Parsing screen content...
   D/Mate: Extracted Context: [Text from your screen here]
   ```
5. **Verify Memory**: Adjust the sliders in the Main Activity, then trigger the assistant. In Logcat, you will see the generated system prompt based on your traits:
   ```text
   You are Mate, a personalized AI assistant. Be very detailed and thorough. Maintain a highly professional...
   ```

## 6. Troubleshooting AICore (Gemini Nano)
On an emulator, the `AICoreClient` might return an error if the Gemini Nano model isn't pre-downloaded. 
- **Check Logcat**: Filter for `AICoreClient` or `Gemini Nano Error`.
- **Note**: If testing on a physical device (Pixel 8+, S24+), ensure "AICore" is updated in the Google Play Store.

## 7. Verifying Bargain Hunter
1. Open a website or app showing a price (e.g., "$199.99").
2. Trigger Mate.
3. Check Logcat for:
   ```text
   D/Mate: Potential bargain/price detected in context.
   ```
4. This confirms the logic has flagged the price and logged it to the local SQLite `mate_memory.db`.
