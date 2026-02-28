package com.openclaw.assistant;

import android.service.voice.VoiceInteractionService;
import android.util.Log;
import java.util.Locale;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.content.Intent;

public class AssistantService extends VoiceInteractionService {
    private static final String TAG = "Mate";

    @Override
    public void onReady() {
        super.onReady();
        Log.d(TAG, "Assistant Service Ready. Hotword detector requires system app privileges.");
    }
}
