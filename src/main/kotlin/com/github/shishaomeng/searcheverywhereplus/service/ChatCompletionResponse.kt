package com.github.shishaomeng.searcheverywhereplus.service

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val system_fingerprint: String?= null
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val logprobs: String? = null,
    val finish_reason: String
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
//    val prompt_tokens_details: PromptTokensDetails,
//    val prompt_cache_hit_tokens: Int,
//    val prompt_cache_miss_tokens: Int
)

//@Serializable
//data class PromptTokensDetails(
//    val cached_tokens: Int
//)