# Downloading the Gemini Nano Edge Model

To get the MediaPipe `LlmInference` API working on your Samsung Fold (or any Android 14+ device), you need to push the Gemini Nano `.bin` model file directly to the device's local `/data/local/tmp` directory as specified in the updated code.

1. **Download the Model**
   You can grab the official quantized MediaPipe Gemini Nano weights from Google's Kaggle repository:
   - [MediaPipe Gemini Nano GGUF/BIN Models](https://www.kaggle.com/models/google/gemini/frameworks/tensorFlowLite/variations/gemini-nano)

2. **Push to Device**
   Once you download the `.bin` file (usually named `gemini-nano.bin`), use `adb` to push it to the path the `AICoreClient` expects:
   ```bash
   adb push path/to/gemini-nano.bin /data/local/tmp/gemini-nano.bin
   ```

3. **Restart the App**
   Restart the Mate app. The `checkAiCoreStatus()` will now find the binary natively and load it into RAM without needing `com.google.android.aicore` service binding privileges!
