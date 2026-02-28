package com.openclaw.assistant;

import android.content.Context;
import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import android.graphics.Bitmap;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Interface for Google AI Edge SDK / Gemini Nano via AICore.
 */
public class AICoreClient {
    private static final String TAG = "AICoreClient";
    
    // Switch to Google AI Edge SDK (ML Kit) naming convention for AICore
    private static final String MODEL_NAME = "gemini-nano"; 
    
    private final GenerativeModel model;
    private final GenerativeModelFutures modelFutures;
    private final Executor executor;
    private final PreferencesManager prefs;

    public AICoreClient(Context context) {
        this.prefs = new PreferencesManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        
        // IMPORTANT: On-device AICore is accessed via a specific initialization 
        // that doesn't require an API Key, but if the SDK falls back to the cloud 
        // (due to model missing or wrong naming), it throws 'unregistered caller'.
        this.model = new GenerativeModel(MODEL_NAME, "unused"); 
        this.modelFutures = GenerativeModelFutures.from(model);
    }

    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(Throwable t);
    }

    public void generateResponse(String userPrompt, String screenContext, ResponseCallback callback) {
        generateResponse(userPrompt, screenContext, null, callback);
    }

    public void generateResponse(String userPrompt, String screenContext, Bitmap image, ResponseCallback callback) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(prefs.generateSystemPrompt());
        promptBuilder.append("\n\n");
        
        if (screenContext != null && !screenContext.isEmpty()) {
            promptBuilder.append("CURRENT SCREEN CONTEXT:\n");
            promptBuilder.append(screenContext);
            promptBuilder.append("\n\n");
        }
        
        promptBuilder.append("USER: ");
        promptBuilder.append(userPrompt);

        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.addText(promptBuilder.toString());
        
        if (image != null) {
            contentBuilder.addImage(image);
        }

        Content content = contentBuilder.build();
        
        // This call MUST be intercepted by the Android AICore service.
        // If it throws "unregistered caller", it means the SDK is trying to hit 
        // Google's Cloud Gemini API instead of the local NPU.
        ListenableFuture<GenerateContentResponse> responseFuture = modelFutures.generateContent(content);
        
        responseFuture.addListener(() -> {
            try {
                GenerateContentResponse response = responseFuture.get();
                String text = response.getText();
                if (text != null) {
                    callback.onSuccess(text);
                } else {
                    callback.onError(new Exception("AICore returned empty. Check model download status."));
                }
            } catch (Exception e) {
                Log.e(TAG, "AICore/Gemini Nano Error: ", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("unregistered callers")) {
                    callback.onError(new Exception("Hardware Block: The app is trying to hit the Cloud. On Samsung, you must enable 'Process data only on device' and 'Settings > Developer options > AICore > Enable On-Device GenAI'."));
                } else {
                    callback.onError(e);
                }
            }
        }, executor);
    }
}
