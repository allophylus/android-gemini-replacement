package com.abettergemini.assistant;

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
    private PreferencesManager prefs;

    public VoiceManager(Context context, PreferencesManager prefs) {
        this.prefs = prefs;
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
                applyVoiceGender();
            }
        } else {
            Log.e(TAG, "TTS Initialization failed");
        }
    }

    private void applyVoiceGender() {
        if (!isReady || tts == null)
            return;

        try {
            String targetGender = prefs.getVoiceGender().toLowerCase();
            for (android.speech.tts.Voice tmpVoice : tts.getVoices()) {
                if (tmpVoice.getName().toLowerCase().contains(targetGender)
                        || tmpVoice.getName().toLowerCase().contains("-" + targetGender.substring(0, 1))) {
                    tts.setVoice(tmpVoice);
                    Log.d(TAG, "Applied TTS Voice: " + tmpVoice.getName());
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply TTS gender", e);
        }
    }

    public void speak(String text) {
        if (isReady && text != null) {
            applyVoiceGender(); // Re-apply in case user changed it in Settings
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
