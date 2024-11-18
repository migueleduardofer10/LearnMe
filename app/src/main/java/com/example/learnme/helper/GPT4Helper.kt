package com.example.learnme.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.learnme.config.GPTConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException

class GPT4Helper(
    private val client: OkHttpClient
) {
    private val gson = Gson()

    // Codifica la imagen a Base64
    fun encodeImageToBase64(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Envía la imagen a la API de GPT-4 para obtener una etiqueta
    fun sendImageToGPT4(base64Image: String, callback: (String) -> Unit) {
        val url = GPTConfig.BASE_URL
        val apiKey = GPTConfig.CHAT_GPT_API_KEY
        val requestBody = """
            {
                "model": "gpt-4o-mini",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": "Genera un nombre descriptivo de esta imagen en español de no más de 15 caracteres."},
                            {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,$base64Image"}}
                        ]
                    }
                ],
                "max_tokens": 300
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", "API request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                        val choicesArray = jsonResponse.getAsJsonArray("choices")
                        val generatedLabel = choicesArray
                            .get(0)
                            .asJsonObject
                            .getAsJsonObject("message")
                            .get("content")
                            .asString
                        callback(generatedLabel)
                    } catch (e: JsonParseException) {
                        Log.e("Error", "Failed to parse JSON", e)
                    }
                }
            }
        })
    }
}