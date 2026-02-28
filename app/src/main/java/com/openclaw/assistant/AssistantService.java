package com.openclaw.assistant;

import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.VoiceInteractionService;
import android.util.Log;
import java.util.Locale;

public class AssistantService extends VoiceInteractionService {
    private static final String TAG = "OpenClawAssistant";
    private AlwaysOnHotwordDetector hotwordDetector;

    @Override
    public void onReady() {
        super.onReady();
        Log.d(TAG, "Assistant Service Ready. Initializing Hotword Detector...");
        
        // Attempt to initialize the system's hardware hotword detector
        // Note: This often requires system privileges or specific enrollment on modern Android
        try {
            hotwordDetector = createAlwaysOnHotwordDetector(
                "OpenClaw", 
                Locale.getDefault(), 
                new AlwaysOnHotwordDetector.Callback() {
                    @Override
                    public void onAvailabilityChanged(int status) {
                        Log.d(TAG, "Hotword availability changed: " + status);
                    }

                    @Override
                    public void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload) {
                        Log.d(TAG, "Hotword DETECTED! Awakening assistant...");
                        // Trigger the session to show the UI
                        showSession(null, SHOW_WITH_ASSIST | SHOW_WITH_SCREENSHOT);
                    }

                    @Override
                    public void onError() {
                        Log.e(TAG, "Hotword detector error.");
                    }

                    @Override
                    public void onRecognitionPaused() {
                        Log.d(TAG, "Hotword recognition paused.");
                    }

                    @Override
                    public void onRecognitionResumed() {
                        Log.d(TAG, "Hotword recognition resumed.");
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to create AlwaysOnHotwordDetector: " + e.getMessage());
            // Fallback to a local engine like Porcupine would be triggered here
        }
    }
}
