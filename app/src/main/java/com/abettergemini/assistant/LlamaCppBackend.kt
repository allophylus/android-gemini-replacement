package com.abettergemini.assistant

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * llama.cpp inference backend for GGUF models.
 * Uses JNI to call native llama.cpp functions.
 */
class LlamaCppBackend(private val context: Context) : InferenceBackend {

    companion object {
        private const val TAG = "LlamaCppBackend"
        private var isLibraryLoaded = false

        init {
            try {
                System.loadLibrary("llama-android")
                isLibraryLoaded = true
                Log.d(TAG, "llama-android native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load llama-android native library", e)
                isLibraryLoaded = false
            }
        }
    }

    private var modelPtr: Long = 0   // Native pointer to llama_model
    private var contextPtr: Long = 0 // Native pointer to llama_context
    private val scope = CoroutineScope(Dispatchers.IO)

    // JNI native methods
    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeCreateContext(modelPtr: Long, nCtx: Int): Long
    private external fun nativeGenerate(contextPtr: Long, modelPtr: Long, prompt: String, maxTokens: Int): String
    private external fun nativeFreeModel(modelPtr: Long)
    private external fun nativeFreeContext(contextPtr: Long)

    /**
     * Load a GGUF model file from the given path.
     */
    fun loadModel(modelPath: String): Boolean {
        if (!isLibraryLoaded) {
            Log.e(TAG, "Native library not loaded, cannot load model")
            return false
        }

        try {
            modelPtr = nativeLoadModel(modelPath)
            if (modelPtr == 0L) {
                Log.e(TAG, "Failed to load model: $modelPath")
                return false
            }

            contextPtr = nativeCreateContext(modelPtr, 2048)
            if (contextPtr == 0L) {
                Log.e(TAG, "Failed to create context")
                nativeFreeModel(modelPtr)
                modelPtr = 0
                return false
            }

            Log.d(TAG, "Model loaded successfully: $modelPath")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
            return false
        }
    }

    override fun generateResponse(prompt: String, callback: InferenceBackend.ResponseCallback) {
        if (!isReady()) {
            callback.onError(Exception("llama.cpp model not loaded"))
            return
        }

        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val response = nativeGenerate(contextPtr, modelPtr, prompt, 512)
                val elapsed = System.currentTimeMillis() - startTime

                launch(Dispatchers.Main) {
                    callback.onSuccess(response, elapsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Generation error", e)
                launch(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    override fun isReady(): Boolean {
        return isLibraryLoaded && modelPtr != 0L && contextPtr != 0L
    }

    override fun close() {
        if (contextPtr != 0L) {
            nativeFreeContext(contextPtr)
            contextPtr = 0
        }
        if (modelPtr != 0L) {
            nativeFreeModel(modelPtr)
            modelPtr = 0
        }
        Log.d(TAG, "llama.cpp model unloaded from RAM")
    }
}
