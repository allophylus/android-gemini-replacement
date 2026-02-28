package com.abettergemini.assistant;

/**
 * Abstraction layer for on-device LLM inference.
 * Implementations: MediaPipeBackend (TFLite), LlamaCppBackend (GGUF).
 */
public interface InferenceBackend {

    interface ResponseCallback {
        void onSuccess(String response, long generationTimeMs);
        void onError(Throwable t);
    }

    /**
     * Generate a text response from the model.
     * Must be called on a background thread.
     */
    void generateResponse(String prompt, ResponseCallback callback);

    /**
     * Check if the backend is ready to generate.
     */
    boolean isReady();

    /**
     * Release model resources from RAM.
     */
    void close();
}
