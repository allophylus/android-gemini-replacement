package com.abettergemini.assistant

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Remote inference backend using OpenAI-compatible API (OpenClaw, Ollama, etc.)
 * Connects over HTTPS with Bearer token auth.
 */
class OpenClawBackend(
    private val encryptedPrefs: EncryptedPrefsManager
) : InferenceBackend {

    companion object {
        private const val TAG = "OpenClawBackend"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 120_000  // remote models can be slow
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun generateResponse(prompt: String, callback: InferenceBackend.ResponseCallback) {
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val response = callChatCompletions(prompt)
                val elapsed = System.currentTimeMillis() - startTime

                launch(Dispatchers.Main) {
                    callback.onSuccess(response, elapsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenClaw generation error", e)
                launch(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    override fun isReady(): Boolean {
        val url = encryptedPrefs.getOpenClawUrl()
        val key = encryptedPrefs.getOpenClawApiKey()
        return url.isNotEmpty() && key.isNotEmpty()
    }

    override fun close() {
        // No persistent resources to release for HTTP client
        Log.d(TAG, "OpenClaw backend closed")
    }

    /**
     * Call the /v1/chat/completions endpoint.
     * The prompt is expected to already be in the user's message format.
     */
    private fun callChatCompletions(prompt: String): String {
        val baseUrl = encryptedPrefs.getOpenClawUrl().trimEnd('/')
        val apiKey = encryptedPrefs.getOpenClawApiKey()
        var model = encryptedPrefs.getOpenClawModel()
        if (model.isEmpty()) model = "default"

        val endpoint = "$baseUrl/v1/chat/completions"
        Log.d(TAG, "Calling: $endpoint with model: $model")

        // Build the JSON request body
        val messagesArray = JSONArray()

        // Parse ChatML-formatted prompt into messages array
        val parts = prompt.split("<|im_start|>")
        for (part in parts) {
            if (part.isBlank()) continue
            val endIdx = part.indexOf("<|im_end|>")
            val content = if (endIdx >= 0) part.substring(0, endIdx) else part

            val newlineIdx = content.indexOf('\n')
            if (newlineIdx < 0) continue

            val role = content.substring(0, newlineIdx).trim()
            val text = content.substring(newlineIdx + 1).trim()

            if (role.isNotEmpty() && text.isNotEmpty()) {
                val msgObj = JSONObject()
                msgObj.put("role", role)
                msgObj.put("content", text)
                messagesArray.put(msgObj)
            } else if (role == "assistant" && text.isEmpty()) {
                // The final <|im_start|>assistant\n tag — skip, API handles this
            }
        }

        // If parsing failed, fallback to single user message
        if (messagesArray.length() == 0) {
            val msgObj = JSONObject()
            msgObj.put("role", "user")
            msgObj.put("content", prompt)
            messagesArray.put(msgObj)
        }

        val requestBody = JSONObject()
        requestBody.put("model", model)
        requestBody.put("messages", messagesArray)
        requestBody.put("max_tokens", 1024)
        requestBody.put("temperature", 0.7)

        // Make HTTPS request
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpsURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true

            // Write request body
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorStream = connection.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "No error body"
                }
                throw Exception("OpenClaw API error $responseCode: $errorBody")
            }

            // Read response
            val responseBody = BufferedReader(
                InputStreamReader(connection.inputStream, "UTF-8")
            ).use { it.readText() }

            // Parse OpenAI-compatible response
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                return message.getString("content").trim()
            }

            return "No response from OpenClaw"
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Test the connection by calling /v1/models endpoint.
     * Returns the list of available model names, or throws on failure.
     */
    fun testConnection(): List<String> {
        val baseUrl = encryptedPrefs.getOpenClawUrl().trimEnd('/')
        val apiKey = encryptedPrefs.getOpenClawApiKey()

        val endpoint = "$baseUrl/v1/models"
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpsURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw Exception("Connection failed: HTTP $responseCode")
            }

            val responseBody = BufferedReader(
                InputStreamReader(connection.inputStream, "UTF-8")
            ).use { it.readText() }

            val json = JSONObject(responseBody)
            val data = json.getJSONArray("data")
            val models = mutableListOf<String>()
            for (i in 0 until data.length()) {
                models.add(data.getJSONObject(i).getString("id"))
            }

            Log.d(TAG, "OpenClaw models available: $models")
            return models
        } finally {
            connection.disconnect()
        }
    }
}
