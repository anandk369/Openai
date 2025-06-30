
package com.mcqautomation.app

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class GeminiAPI(private val context: Context) {
    
    companion object {
        private const val TAG = "GeminiAPI"
        // Replace with your actual Gemini API endpoint
        private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
        // Replace with your API key from local.properties
        private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    private val gson = Gson()
    private val answerCache = AnswerCache(context)
    
    data class GeminiRequest(
        val contents: List<Content>
    )
    
    data class Content(
        val parts: List<Part>
    )
    
    data class Part(
        val text: String
    )
    
    data class GeminiResponse(
        val candidates: List<Candidate>
    )
    
    data class Candidate(
        val content: Content,
        @SerializedName("finishReason") val finishReason: String?
    )
    
    suspend fun getAnswer(mcqData: OCRHelper.MCQData): String {
        return withContext(Dispatchers.IO) {
            try {
                val questionHash = mcqData.hashCode().toString()
                
                // Check cache first
                val cachedAnswer = answerCache.getAnswer(questionHash)
                if (cachedAnswer != null) {
                    Log.d(TAG, "Using cached answer: $cachedAnswer")
                    return@withContext cachedAnswer
                }
                
                // Create prompt
                val prompt = buildPrompt(mcqData)
                Log.d(TAG, "Sending prompt to Gemini: $prompt")
                
                // Make API call
                val answer = callGeminiAPI(prompt)
                
                // Cache the answer
                answerCache.saveAnswer(questionHash, answer)
                
                Log.d(TAG, "Received answer from Gemini: $answer")
                answer
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting answer from Gemini", e)
                // Return random answer as fallback
                listOf("A", "B", "C", "D").random()
            }
        }
    }
    
    private fun buildPrompt(mcqData: OCRHelper.MCQData): String {
        return buildString {
            append("Answer with only one letter (A, B, C, or D): ")
            append(mcqData.question)
            append("\n")
            mcqData.options.forEachIndexed { index, option ->
                val letter = ('A' + index)
                append("$letter) $option\n")
            }
        }
    }
    
    private suspend fun callGeminiAPI(prompt: String): String {
        val requestBody = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(prompt))
                )
            )
        )
        
        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$API_URL?key=$API_KEY")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("API call failed: ${response.code} ${response.message}")
        }
        
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response body")
        
        val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
        val answer = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No answer in response")
        
        // Extract single letter from response
        val letterMatch = Regex("[A-D]").find(answer.uppercase())
        return letterMatch?.value ?: "A"
    }
}
