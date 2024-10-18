package com.example.learnme.activity

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.learnme.R
import com.example.learnme.config.GPTConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class GPT4Activity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gpt)

        val etQuestion = findViewById<EditText>(R.id.etQuestion)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val txtAnswer = findViewById<TextView>(R.id.txtAnswer)

        btnSubmit.setOnClickListener {
            val question = etQuestion.text.toString()
            Toast.makeText(this, question, Toast.LENGTH_SHORT).show()
            // Lógica para enviar la pregunta a la API de GPT-4 y obtener la respuesta
            getResponse(question) {response ->
                runOnUiThread {
                    txtAnswer.text = response
                }
            }
        }
    }



    // Función para codificar una imagen en Base64
    fun encodeImage(imagePath: String): String {
        val imageFile = File(imagePath)
        val fileInputStream = FileInputStream(imageFile)
        val imageBytes = fileInputStream.readBytes()
        fileInputStream.close()

        // Codifica la imagen en Base64
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    fun getResponse(question: String, callback: (String) -> Unit) {
        val URL = GPTConfig.BASE_URL
        val API_KEY = GPTConfig.CHAT_GPT_API_KEY

//        val base64Image = encodeImage(imagePath)

        val requestBody = """ {
            "model": "gpt-4o-mini",
            "messages": [
              {
                "role": "system",
                "content": [
                  {
                    "type": "text",
                    "text": "$question"

                  }
                ]
              }
            ],
            "max_tokens": 300
        } """.trimIndent()

        val request = Request.Builder()
            .url(URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", "API FAILED", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    Log.v("data", body)
                } else {
                    Log.v("data", "empty")
                }

                val jsonObject = JSONObject(body)

//                // Verifica si "choices" existe en la respuesta antes de acceder a él. Falta cachear el error
//                if (jsonObject.has("choices")) {
//                    val choices = jsonObject.getJSONArray("choices")
//                    // Aquí manejas el resultado como lo hacías antes.
//                } else {
//                    println("Error: La API de OpenAI ha retornado un error.")
//                }

                val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                val textResult = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                callback(textResult)
            }
        })
    }


}