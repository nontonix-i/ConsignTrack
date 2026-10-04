package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    @Json(name = "temperature") val temperature: Double = 0.5,
    @Json(name = "max_tokens") val maxTokens: Int = 800
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

interface OpenAiApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

object OpenAiClient {
    private const val DEFAULT_BASE_URL = "https://ai.drakor.pp.ua/v1/"
    private const val DEFAULT_API_KEY = "freellmapi-f657ed0085e8e45b7282037af89d6712e8fb0db6860fcf90"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun getBaseUrl(): String {
        val configured = try {
            BuildConfig.OPENAI_BASE_URL
        } catch (_: Exception) {
            DEFAULT_BASE_URL
        }
        val url = if (configured.isNullOrBlank()) DEFAULT_BASE_URL else configured.trim()
        return if (url.endsWith("/")) url else "$url/"
    }

    fun getApiKey(): String {
        val configured = try {
            BuildConfig.OPENAI_API_KEY
        } catch (_: Exception) {
            DEFAULT_API_KEY
        }
        return if (configured.isNullOrBlank()) DEFAULT_API_KEY else configured.trim()
    }

    val service: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiApiService::class.java)
    }

    /**
     * Ask the remote OpenAI-compatible model with system instructions & user query.
     */
    suspend fun generateAnswer(systemContext: String, userQuery: String, preferredModel: String = "auto"): String? {
        val apiKey = getApiKey()
        val authHeader = "Bearer $apiKey"

        val messages = listOf(
            OpenAiMessage(role = "system", content = systemContext),
            OpenAiMessage(role = "user", content = userQuery)
        )

        // Try primary model (auto), if null or fails try "qwen-3.8-27b"
        val modelsToTry = listOf(preferredModel, "qwen-3.8-27b", "fusion")
        for (model in modelsToTry) {
            try {
                val req = ChatCompletionRequest(
                    model = model,
                    messages = messages,
                    temperature = 0.5,
                    maxTokens = 800
                )
                val response = service.createChatCompletion(authHeader, req)
                val reply = response.choices?.firstOrNull()?.message?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    return reply
                }
            } catch (e: Exception) {
                android.util.Log.w("OpenAiClient", "Failed calling model $model: ${e.message}")
            }
        }
        return null
    }
}
