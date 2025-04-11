package com.github.shishaomeng.searcheverywhereplus.service

import com.intellij.openapi.components.Service
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 百炼LLM服务
 * <pre>
 * 采用 OpenAI兼容的HTTP方式调用
 * </pre>
 *
 * @author shishaomeng
 */
@Service
class BLLLMService() {

    private val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).build()

    private val mediaType = "application/json".toMediaType()

    private val url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    /*
     * 智能搜索
     *
     * @param query 搜索提示词
     * @return 响应对象
     */
    fun search(query: String): ChatCompletionResponse {
        val request = Request.Builder()
            .url(url)
            .method("POST", createRequestBody(query))
            .addHeader("Content-Type", mediaType.toString())
            .addHeader("Authorization", "Bearer sk-c60370ed9e654ea2936a2f57fbabecaf")
            .build()

        return try {
            val resp = client.newCall(request).execute()
            if (!resp.isSuccessful) {
                throw IOException("Http request failed: ${resp.code} - ${resp.message}")
            }
            parseChatCompletionResponse(resp.body?.string() ?: throw IOException("Empty response body"))
        } catch (e: IOException) {
            throw IOException("Failed to call: ${e.message}", e)
        }
    }

    /*
     * 创建请求体
     * @param prompt 提示词
     * @return 请求体
     */
    private fun createRequestBody(prompt: String) = """
        {
          "model": "deepseek-v3",
          "messages": [
            {
              "content": "$prompt",
              "role": "user"
            }
          ]
        }
    """.trimIndent().toRequestBody(mediaType)


    /*
     * 解析百炼模型服务响应
     *
     * @param jsonString 响应字符串
     * @return 响应对象
     */
    private fun parseChatCompletionResponse(jsonString: String): ChatCompletionResponse {
        return try {
            Json.decodeFromString<ChatCompletionResponse>(jsonString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse DeepSeek JSON response: ${e.message}", e)
        }
    }
}