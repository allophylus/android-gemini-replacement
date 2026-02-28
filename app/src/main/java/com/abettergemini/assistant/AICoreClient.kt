package com.abettergemini.assistant

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Interface for On-Device Gemini Nano using MediaPipe Tasks GenAI.
 */
class AICoreClient(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private var llmInference: LlmInference? = null
    private var isInitializing = false
    private var isDownloading = false
    private var downloadProgress = 0

    init {
        initializeLlm()
    }

    private fun initializeLlm() {
        if (isInitializing || llmInference != null) return
        isInitializing = true

        scope.launch(Dispatchers.IO) {
            try {
                // Use internal storage to avoid needing file system permissions
                val modelFile = java.io.File(context.filesDir, "gemma-2b.bin")
                
                if (!modelFile.exists() || modelFile.length() < 1000000) { // Redownload if corrupt or empty
                    if (isUnmeteredNetwork()) {
                        downloadModel(modelFile)
                    } else {
                        // Throw specifically so MainActivity can catch it and show the cellular prompt
                        throw Exception("CELLULAR_DOWNLOAD_REQUIRED")
                    }
                }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTemperature(0.7f)
                    .setResultListener { partialResult, done ->
                        // Streaming results
                    }
                    .setErrorListener { error ->
                        Log.e(TAG, "LlmInference Error: " + error.message)
                    }
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                Log.d(TAG, "MediaPipe LlmInference initialized for on-device inference.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LlmInference (MediaPipe): " + e.message, e)
                throw e // Propagate error up to surface in MainActivity checks
            } finally {
                isInitializing = false
            }
        }
    }

    /**
     * Checks network state. If on Wi-Fi or Ethernet, returns true. 
     * If on Cellular, returns false so MainActivity can prompt the user.
     */
    fun isUnmeteredNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || 
               capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Public method to explicitly start the download (e.g. after user agrees to cellular warning)
     */
    fun startDownloadExplicitly() {
        val modelFile = java.io.File(context.filesDir, "gemma-2b.bin")
        scope.launch(Dispatchers.IO) {
            downloadModel(modelFile)
        }
    }

    interface DownloadProgressListener {
        fun onProgress(percent: Int)
    }

    var progressListener: DownloadProgressListener? = null

    private fun downloadModel(targetFile: java.io.File) {
        isDownloading = true
        downloadProgress = 0
        Log.d(TAG, "Downloading Gemini Nano model to internal storage...")
        
        // Google's official MediaPipe format for 2B
        val modelUrl = java.net.URL("https://storage.googleapis.com/mediapipe-models/llm_inference/gemma_2b_en/float32/1/gemma_2b_en.bin") 
        
        try {
            val connection = modelUrl.openConnection() as java.net.HttpURLConnection
            connection.connect()
            
            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error " + connection.responseCode)
            }
            
            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = java.io.FileOutputStream(targetFile)
            
            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val newProgress = (total * 100 / fileLength).toInt()
                    if (newProgress != downloadProgress) {
                        downloadProgress = newProgress
                        progressListener?.let { listener ->
                            scope.launch(Dispatchers.Main) {
                                listener.onProgress(downloadProgress)
                            }
                        }
                    }
                }
                output.write(data, 0, count)
            }
            
            output.flush()
            output.close()
            input.close()
            Log.d(TAG, "Download complete!")
        } catch (e: Exception) {
            targetFile.delete() // Cleanup partial
            throw e
        } finally {
            isDownloading = false
            // Final update to complete progress bar setup
            progressListener?.let { listener ->
                scope.launch(Dispatchers.Main) {
                    listener.onProgress(100)
                }
            }
        }
    }

    interface ResponseCallback {
        fun onSuccess(response: String)
        fun onError(t: Throwable)
    }

    fun generateResponse(userPrompt: String, screenContext: String?, callback: ResponseCallback) {
        generateResponse(userPrompt, screenContext, null, callback)
    }

    fun generateResponse(userPrompt: String, screenContext: String?, image: Bitmap?, callback: ResponseCallback) {
        val currentLlm = llmInference
        if (currentLlm == null) {
            if (isDownloading) {
                callback.onError(Exception("Downloading LLM... $downloadProgress% complete. Please wait."))
            } else if (isInitializing) {
                callback.onError(Exception("Initializing LLM Engine..."))
            } else {
                callback.onError(Exception("MediaPipe LlmInference failed to initialize. Check logs."))
            }
            return
        }

        val promptBuilder = StringBuilder()
        promptBuilder.append(prefs.generateSystemPrompt())
        promptBuilder.append("\n\n")

        if (!screenContext.isNullOrEmpty()) {
            promptBuilder.append("CURRENT SCREEN CONTEXT:\n")
            promptBuilder.append(screenContext)
            promptBuilder.append("\n\n")
        }

        promptBuilder.append("USER: ")
        promptBuilder.append(userPrompt)

        scope.launch(Dispatchers.IO) {
            try {
                val response = currentLlm.generateResponse(promptBuilder.toString())
                scope.launch(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaPipe Generate Error", e)
                scope.launch(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AICoreClient"
    }
}
