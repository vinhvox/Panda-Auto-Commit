package com.viotech.auto

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class GeminiRequest(
    val system_instruction: SystemInstruction?,
    val contents: List<Content>
)

data class SystemInstruction(val parts: Part)
data class Content(val parts: List<Part>)
data class Part(val text: String)

data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?)

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun generateCommitMessage(apiKey: String, rolePrompt: String, gitDiff: String): String {
        val cleanApiKey = apiKey.trim()
        if (cleanApiKey.isBlank()) {
            throw Exception("The API key is blank. Please configure it in Settings.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=$cleanApiKey".trim()        // 3. Xây dựng payload (dữ liệu gửi đi)
        val requestData = GeminiRequest(
            system_instruction = SystemInstruction(Part(text = rolePrompt)),
            contents = listOf(Content(listOf(Part(text = "Here is the git diff:\n$gitDiff"))))
        )

        val jsonBody = gson.toJson(requestData)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                throw Exception("API Failed: Code ${response.code}\nDetails: $errorBody")
            }

            val responseBody = response.body?.string() ?: throw Exception("Response body is null")

            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)

            return geminiResponse.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
                ?: throw Exception("Content could not be extracted from Gemini's response.")
        }
    }
}