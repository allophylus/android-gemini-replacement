package com.openclaw.assistant;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

/**
 * Local TTS Management.
 * Currently uses Android System TTS as fallback, 
 * but configured to support Sherpa-ONNX (Piper) for custom local voices.
 */
public class VoiceManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "VoiceManager";
    private TextToSpeech tts;
    private boolean isReady = false;

    public VoiceManager(Context context) {
        this.tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                isReady = true;
            }
        } else {
            Log.e(TAG, "TTS Initialization failed");
        }
    }

    public void speak(String text) {
        if (isReady && text != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MateSpeechID");
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
