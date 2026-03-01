package com.abettergemini.assistant

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Interface for On-Device LLM inference via dual backends (MediaPipe + llama.cpp).
 */
class AICoreClient(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private var llmInference: LlmInference? = null
    private var llamaCppBackend: LlamaCppBackend? = null
    private var isInitializing = false
    private var isDownloading = false
    private var isGenerating = false
    private var downloadProgress = 0

    init {
        initializeLlm()
    }

    private fun initializeLlm() {
        if (isInitializing || llmInference != null || llamaCppBackend?.isReady == true) return
        isInitializing = true

        scope.launch(Dispatchers.IO) {
            try {
                val modelConfig = prefs.selectedModelConfig
                // Use external storage to avoid filling up the limited internal app data partition (avoiding ENOSPC)
                val destDir = context.getExternalFilesDir(null) ?: context.filesDir
                val modelFile = java.io.File(destDir, modelConfig.fileName)
                
                // Redownload if corrupt, partial, or empty
                if (!modelFile.exists() || modelFile.length() < modelConfig.minFileSize) {
                    if (modelFile.exists()) {
                        modelFile.delete() // Clean up the corrupted/partial file
                    }
                    if (isUnmeteredNetwork()) {
                        downloadModel(modelFile)
                    } else {
                        throw Exception("CELLULAR_DOWNLOAD_REQUIRED")
                    }
                }

                loadModelFile(modelConfig, modelFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize model: " + e.message, e)
            } finally {
                isInitializing = false
            }
        }
    }

    /**
     * Actually loads a model file into the appropriate backend.
     */
    private fun loadModelFile(modelConfig: ModelConfig, modelFile: java.io.File) {
        when (modelConfig.backend) {
            ModelConfig.Backend.LLAMA_CPP -> {
                val backend = LlamaCppBackend(context)
                if (backend.loadModel(modelFile.absolutePath)) {
                    llamaCppBackend = backend
                    Log.d(TAG, "llama.cpp backend initialized: ${modelConfig.displayName}")
                } else {
                    throw Exception("Failed to load GGUF model via llama.cpp")
                }
            }
            ModelConfig.Backend.MEDIAPIPE -> {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTemperature(0.7f)
                    .setResultListener { partialResult, done -> }
                    .setErrorListener { error ->
                        Log.e(TAG, "LlmInference Error: " + error.message)
                    }
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                Log.d(TAG, "MediaPipe LlmInference initialized: ${modelConfig.displayName}")
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
     * After download completes, automatically loads the model.
     */
    fun startDownloadExplicitly() {
        val modelConfig = prefs.selectedModelConfig
        val destDir = context.getExternalFilesDir(null) ?: context.filesDir
        val modelFile = java.io.File(destDir, modelConfig.fileName)
        scope.launch(Dispatchers.IO) {
            try {
                downloadModel(modelFile)
                loadModelFile(modelConfig, modelFile)
            } catch (e: Exception) {
                Log.e(TAG, "Download/load failed: " + e.message, e)
            }
        }
    }

    /**
     * Unloads the current model and reinitializes with the currently selected one.
     * Call this when the user changes their model selection in Settings.
     */
    fun switchModel() {
        unloadModel()
        initializeLlm()
    }

    /**
     * Unloads the active inference engine from RAM to reclaim system memory.
     */
    fun unloadModel() {
        try {
            llmInference?.close()
            llmInference = null
            llamaCppBackend?.close()
            llamaCppBackend = null
            Log.d(TAG, "Model successfully unloaded from RAM.")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model", e)
        }
    }

    /**
     * Reloads the model into RAM if it was previously unloaded.
     */
    fun reloadModel() {
        if (llmInference == null && llamaCppBackend?.isReady != true && !isInitializing && !isDownloading) {
            initializeLlm()
        }
    }

    interface DownloadProgressListener {
        fun onProgress(percent: Int)
    }

    var progressListener: DownloadProgressListener? = null

    private fun downloadModel(targetFile: java.io.File) {
        isDownloading = true
        downloadProgress = 0
        val modelConfig = prefs.selectedModelConfig
        Log.d(TAG, "Downloading model: ${modelConfig.displayName} to storage...")
        
        var currentUrl = java.net.URL(modelConfig.downloadUrl) 
        var connection = currentUrl.openConnection() as java.net.HttpURLConnection
        connection.instanceFollowRedirects = false
        
        try {
            var redirects = 0
            while (true) {
                connection.connect()
                val code = connection.responseCode
                if (code == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
                    code == java.net.HttpURLConnection.HTTP_MOVED_TEMP || 
                    code == java.net.HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308) {
                    
                    val location = connection.getHeaderField("Location")
                    currentUrl = java.net.URL(currentUrl, location)
                    connection.disconnect()
                    
                    connection = currentUrl.openConnection() as java.net.HttpURLConnection
                    connection.instanceFollowRedirects = false
                    
                    redirects++
                    if (redirects > 10) throw Exception("Too many redirects during model payload resolution")
                } else {
                    break
                }
            }
            
            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error " + connection.responseCode)
            }
            
            val fileLength = connection.contentLength
            val input = connection.inputStream
            
            // Explicitly guard against ENOSPC by verifying partition has at least 2.2GB free
            val stat = android.os.StatFs(targetFile.parentFile?.absolutePath ?: context.filesDir.absolutePath)
            val availableSpace = stat.availableBlocksLong * stat.blockSizeLong
            if (availableSpace < 2200000000L) {
                targetFile.delete()
                throw Exception("ENOSPC")
            }
            
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

    private data class PendingRequest(val prompt: String, val callback: ResponseCallback)
    private val requestQueue = mutableListOf<PendingRequest>()

    fun generateResponse(userPrompt: String, screenContext: String?, callback: ResponseCallback) {
        generateResponse(userPrompt, screenContext, null, callback)
    }

    fun generateResponse(userPrompt: String, screenContext: String?, image: Bitmap?, callback: ResponseCallback) {
        val currentLlm = llmInference
        val currentLlama = llamaCppBackend
        
        if (currentLlm == null && currentLlama?.isReady != true) {
            if (isDownloading) {
                callback.onError(Exception("Downloading LLM... $downloadProgress% complete. Please wait."))
            } else if (isInitializing) {
                callback.onError(Exception("Initializing LLM Engine..."))
            } else {
                callback.onError(Exception("No model loaded. Select and download a model in Settings."))
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
        val finalPrompt = promptBuilder.toString()

        scope.launch(Dispatchers.IO) {
            if (isGenerating) {
                requestQueue.add(PendingRequest(finalPrompt, callback))
                scope.launch(Dispatchers.Main) {
                    callback.onSuccess("\n\n[Added to generation queue...]")
                }
                return@launch
            }

            // Route to the active backend
            if (currentLlama?.isReady == true) {
                isGenerating = true
                currentLlama.generateResponse(finalPrompt, object : InferenceBackend.ResponseCallback {
                    override fun onSuccess(response: String, generationTimeMs: Long) {
                        val durationSecs = generationTimeMs / 1000.0
                        val formatted = response + "\n\n[Generation Time: " + String.format("%.1f", durationSecs) + "s]"
                        callback.onSuccess(formatted)
                        isGenerating = false
                    }
                    override fun onError(t: Throwable) {
                        callback.onError(t)
                        isGenerating = false
                    }
                })
            } else if (currentLlm != null) {
                processQueueItem(currentLlm, finalPrompt, callback)
            }
        }
    }

    private suspend fun processQueueItem(currentLlm: LlmInference, prompt: String, callback: ResponseCallback) {
        isGenerating = true
        try {
            val startTimeMs = System.currentTimeMillis()
            val response = currentLlm.generateResponse(prompt)
            val endTimeMs = System.currentTimeMillis()
            val durationSecs = (endTimeMs - startTimeMs) / 1000.0
            val formattedResponse = response + "\n\n[Generation Time: " + String.format("%.1f", durationSecs) + "s]"

            scope.launch(Dispatchers.Main) {
                callback.onSuccess(formattedResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe Generate Error", e)
            scope.launch(Dispatchers.Main) {
                callback.onError(e)
            }
        } finally {
            isGenerating = false
            if (requestQueue.isNotEmpty()) {
                val nextRequest = requestQueue.removeAt(0)
                processQueueItem(currentLlm, nextRequest.prompt, nextRequest.callback)
            }
        }
    }

    companion object {
        private const val TAG = "AICoreClient"
    }
}
