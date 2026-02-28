package com.openclaw.assistant;

import android.content.Context;
import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Interface for Google AI Edge SDK / Gemini Nano.
 * Note: Requires a compatible device (Pixel 8+, S24+) with AICore installed.
 */
public class AICoreClient {
    private static final String TAG = "AICoreClient";
    private static final String MODEL_NAME = "gemini-nano";
    
    private final GenerativeModel model;
    private final GenerativeModelFutures modelFutures;
    private final Executor executor;
    private final PreferencesManager prefs;

    public AICoreClient(Context context) {
        this.prefs = new PreferencesManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        
        // Setup local model (Gemini Nano via AICore)
        // In a real implementation, we use GenerativeModel.Builder
        this.model = new GenerativeModel(MODEL_NAME, ""); // API Key is empty for on-device Nano
        this.modelFutures = GenerativeModelFutures.from(model);
    }

    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(Throwable t);
    }

    public void generateResponse(String userPrompt, String screenContext, ResponseCallback callback) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 1. Inject Personalized Persona
        promptBuilder.append(prefs.generateSystemPrompt());
        promptBuilder.append("\n\n");
        
        // 2. Inject Screen Awareness (AssistStructure)
        if (screenContext != null && !screenContext.isEmpty()) {
            promptBuilder.append("CURRENT SCREEN CONTEXT:\n");
            promptBuilder.append(screenContext);
            promptBuilder.append("\n\n");
        }
        
        // 3. The User Ask
        promptBuilder.append("USER: ");
        promptBuilder.append(userPrompt);

        Content content = new Content.Builder()
                .addText(promptBuilder.toString())
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = modelFutures.generateContent(content);
        
        responseFuture.addListener(() -> {
            try {
                GenerateContentResponse response = responseFuture.get();
                String text = response.getText();
                if (text != null) {
                    callback.onSuccess(text);
                } else {
                    callback.onError(new Exception("Empty response from AI"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini Nano Error: ", e);
                callback.onError(e);
            }
        }, executor);
    }
}
