package com.example.data.remote

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String = "auto",
    @Json(name = "messages") val messages: List<OpenAiMessage>,
    @Json(name = "temperature") val temperature: Double = 0.3,
    @Json(name = "max_tokens") val maxTokens: Int = 1200
)

@JsonClass(generateAdapter = true)
data class Choice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: OpenAiMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<Choice>? = null
)

data class ParsedToolCall(
    val name: String,
    val arguments: JSONObject
)

data class AiModelTurnResult(
    val replyText: String?,
    val toolCalls: List<ParsedToolCall> = emptyList()
)

interface OpenAiApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

object OpenAiClient {
    const val DEFAULT_BASE_URL = "https://ai.drakor.pp.ua/v1/"
    const val DEFAULT_API_KEY = "freellmapi-f657ed0085e8e45b7282037af89d6712e8fb0db6860fcf90"
    const val DEFAULT_MODEL = "auto"

    private const val GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"

    @Volatile
    private var runtimeBaseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var runtimeApiKey: String = DEFAULT_API_KEY

    @Volatile
    private var runtimeModel: String = DEFAULT_MODEL

    @Volatile
    private var runtimeGeminiKey: String = ""

    @Volatile
    private var cachedServiceUrl: String? = null

    @Volatile
    private var cachedService: OpenAiApiService? = null

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun normalizeBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().ifBlank { DEFAULT_BASE_URL }
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    fun updateConfig(
        baseUrl: String,
        apiKey: String,
        model: String = DEFAULT_MODEL,
        geminiApiKey: String = ""
    ) {
        runtimeBaseUrl = normalizeBaseUrl(baseUrl)
        runtimeApiKey = apiKey.trim()
        runtimeModel = model.trim().ifBlank { DEFAULT_MODEL }
        runtimeGeminiKey = geminiApiKey.trim()
    }

    fun syncFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences("consigntrack_settings", Context.MODE_PRIVATE)
        val url = prefs.getString("ai_base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        val key = prefs.getString("ai_api_key", DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        val model = prefs.getString("ai_model_name", DEFAULT_MODEL) ?: DEFAULT_MODEL
        val geminiKey = prefs.getString("gemini_api_key", "") ?: ""
        updateConfig(baseUrl = url, apiKey = key, model = model, geminiApiKey = geminiKey)
    }

    fun getBaseUrl(): String = runtimeBaseUrl

    fun getApiKey(): String = runtimeApiKey

    fun getPreferredModel(): String = runtimeModel

    fun getGeminiApiKey(): String = runtimeGeminiKey

    private fun getOrBuildService(): OpenAiApiService {
        val targetUrl = getBaseUrl()
        val existing = cachedService
        if (existing != null && cachedServiceUrl == targetUrl) {
            return existing
        }
        return synchronized(this) {
            val recheck = cachedService
            if (recheck != null && cachedServiceUrl == targetUrl) {
                recheck
            } else {
                val built = Retrofit.Builder()
                    .baseUrl(targetUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(OpenAiApiService::class.java)
                cachedServiceUrl = targetUrl
                cachedService = built
                built
            }
        }
    }

    /**
     * Calls Gemini with native functionDeclarations if Gemini API Key is configured in Settings,
     * otherwise calls the configured OpenAI-compatible endpoint and parses structured tool calls + reply.
     */
    suspend fun generateAgentTurn(
        systemContext: String,
        userQuery: String,
        functionDeclarations: JSONArray,
        enableTools: Boolean
    ): AiModelTurnResult? = withContext(Dispatchers.IO) {
        val geminiKey = getGeminiApiKey()
        if (geminiKey.isNotBlank()) {
            val geminiResult = callGeminiWithTools(
                apiKey = geminiKey,
                systemContext = systemContext,
                userQuery = userQuery,
                functionDeclarations = functionDeclarations,
                enableTools = enableTools
            )
            if (geminiResult != null) return@withContext geminiResult
        }

        // Primary OpenAI-compatible endpoint from Settings
        val rawReply = generateAnswer(
            systemContext = systemContext,
            userQuery = userQuery,
            preferredModel = getPreferredModel()
        ) ?: return@withContext null

        val extractedCalls = if (enableTools) extractToolCallsFromText(rawReply) else emptyList()
        val cleanedText = stripToolCallBlocks(rawReply).trim()

        AiModelTurnResult(
            replyText = cleanedText.ifBlank { null },
            toolCalls = extractedCalls
        )
    }

    private fun callGeminiWithTools(
        apiKey: String,
        systemContext: String,
        userQuery: String,
        functionDeclarations: JSONArray,
        enableTools: Boolean
    ): AiModelTurnResult? {
        return try {
            val payload = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemContext)))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userQuery)))
                }))
                if (enableTools && functionDeclarations.length() > 0) {
                    put("tools", JSONArray().put(JSONObject().apply {
                        put("functionDeclarations", functionDeclarations)
                    }))
                }
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                })
            }

            val req = Request.Builder()
                .url("$GEMINI_ENDPOINT?key=$apiKey")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string() ?: return null
                val root = JSONObject(bodyStr)
                val candidates = root.optJSONArray("candidates") ?: return null
                val firstCandidate = candidates.optJSONObject(0) ?: return null
                val content = firstCandidate.optJSONObject("content") ?: return null
                val parts = content.optJSONArray("parts") ?: return null

                val toolCalls = mutableListOf<ParsedToolCall>()
                val textBuilder = StringBuilder()

                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val fnCall = part.optJSONObject("functionCall")
                    if (fnCall != null) {
                        val fnName = fnCall.optString("name")
                        val fnArgs = fnCall.optJSONObject("args") ?: JSONObject()
                        if (fnName.isNotBlank()) {
                            toolCalls.add(ParsedToolCall(name = fnName, arguments = fnArgs))
                        }
                    }
                    val partText = part.optString("text")
                    if (partText.isNotBlank()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(partText)
                    }
                }

                val rawText = textBuilder.toString()
                val extraCalls = if (enableTools && toolCalls.isEmpty()) {
                    extractToolCallsFromText(rawText)
                } else {
                    emptyList()
                }
                val finalCalls = toolCalls + extraCalls
                val cleanedText = stripToolCallBlocks(rawText).trim()

                AiModelTurnResult(
                    replyText = cleanedText.ifBlank { null },
                    toolCalls = finalCalls
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("OpenAiClient", "Gemini tool call failed: ${e.message}")
            null
        }
    }

    /**
     * Extracts `<tool_call>{"name": "...", "arguments": {...}}</tool_call>` or JSON tool_calls blocks
     * emitted by OpenAI-compatible or open-weight models.
     */
    fun extractToolCallsFromText(text: String): List<ParsedToolCall> {
        val results = mutableListOf<ParsedToolCall>()

        // 1. Match <tool_call>...</tool_call> tags
        val tagRegex = Regex("<tool_call>\\s*(\\{[\\s\\S]*?\\})\\s*</tool_call>", RegexOption.IGNORE_CASE)
        tagRegex.findAll(text).forEach { match ->
            parseSingleToolJson(match.groupValues[1], results)
        }

        // 2. Match ```tool_call ... ``` or ```json ... ``` blocks containing "tool_calls" or "name" + "arguments"
        if (results.isEmpty()) {
            val fenceRegex = Regex("```(?:tool_call|json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            fenceRegex.findAll(text).forEach { match ->
                parseSingleToolJson(match.groupValues[1].trim(), results)
            }
        }

        // 3. If the entire response is a JSON object with "tool_calls" or "name"+"arguments"
        if (results.isEmpty()) {
            val trimmed = text.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                parseSingleToolJson(trimmed, results)
            }
        }

        return results
    }

    private fun parseSingleToolJson(jsonStr: String, out: MutableList<ParsedToolCall>) {
        runCatching {
            val obj = JSONObject(jsonStr)
            if (obj.has("tool_calls")) {
                val arr = obj.optJSONArray("tool_calls") ?: return
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val name = item.optString("name").ifBlank {
                        item.optJSONObject("function")?.optString("name") ?: ""
                    }
                    val args = item.optJSONObject("arguments")
                        ?: item.optJSONObject("args")
                        ?: item.optJSONObject("function")?.optJSONObject("arguments")
                        ?: JSONObject()
                    if (name.isNotBlank()) {
                        out.add(ParsedToolCall(name = name, arguments = args))
                    }
                }
            } else if (obj.has("name")) {
                val name = obj.optString("name")
                val args = obj.optJSONObject("arguments") ?: obj.optJSONObject("args") ?: JSONObject()
                if (name.isNotBlank()) {
                    out.add(ParsedToolCall(name = name, arguments = args))
                }
            }
        }
    }

    fun stripToolCallBlocks(text: String): String {
        var cleaned = text
            .replace(Regex("<tool_call>[\\s\\S]*?</tool_call>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```tool_call[\\s\\S]*?```", RegexOption.IGNORE_CASE), "")
        cleaned = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).replace(cleaned) { match ->
            val inner = match.groupValues[1].trim()
            if (inner.contains("\"tool_calls\"") || (inner.contains("\"name\"") && inner.contains("\"arguments\""))) {
                ""
            } else {
                match.value
            }
        }
        val trimmed = cleaned.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}") &&
            (trimmed.contains("\"tool_calls\"") || (trimmed.contains("\"name\"") && trimmed.contains("\"arguments\"")))
        ) {
            return ""
        }
        return cleaned.trim()
    }

    /**
     * Ask the remote OpenAI-compatible model with system instructions & user query.
     */
    suspend fun generateAnswer(
        systemContext: String,
        userQuery: String,
        preferredModel: String = getPreferredModel()
    ): String? {
        val apiKey = getApiKey()
        val authHeader = "Bearer $apiKey"
        val apiService = getOrBuildService()

        val messages = listOf(
            OpenAiMessage(role = "system", content = systemContext),
            OpenAiMessage(role = "user", content = userQuery)
        )

        val modelsToTry = listOf(preferredModel.ifBlank { "auto" }, "auto", "qwen-3.8-27b", "fusion").distinct()
        for (model in modelsToTry) {
            try {
                val req = ChatCompletionRequest(
                    model = model,
                    messages = messages,
                    temperature = 0.3,
                    maxTokens = 1200
                )
                val response = apiService.createChatCompletion(authHeader, req)
                val reply = response.choices?.firstOrNull()?.message?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    return reply
                }
            } catch (e: Exception) {
                android.util.Log.w("OpenAiClient", "Failed calling model $model at ${getBaseUrl()}: ${e.message}")
            }
        }
        return null
    }
}
