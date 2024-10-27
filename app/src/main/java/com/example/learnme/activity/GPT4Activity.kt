package com.example.learnme.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
            val base64Image = encodeDrawableToBase64(this, R.drawable.manzana)

            Toast.makeText(this, "Imagen codificada en base64", Toast.LENGTH_SHORT).show()
            // Lógica para enviar la pregunta a la API de GPT-4 y obtener la respuesta
            getResponse(this, base64Image) {response ->
                runOnUiThread {
                    txtAnswer.text = response
                }
            }
        }
    }

    fun encodeDrawableToBase64(context: Context, drawableId: Int): String {
        // Decodificar la imagen desde los resources (drawable)
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)

        // Crear un ByteArrayOutputStream para contener los bytes de la imagen
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Comprimir la imagen en formato JPEG y escribirla en el ByteArrayOutputStream
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)

        // Obtener el array de bytes de la imagen
        val byteArray = byteArrayOutputStream.toByteArray()

        // Codificar el array de bytes a base64
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }



    fun getResponse(context: Context, imagePath: String, callback: (String) -> Unit) {
        val URL = GPTConfig.BASE_URL
        val API_KEY = GPTConfig.CHAT_GPT_API_KEY

        val requestBody = """ {
            "model": "gpt-4o-mini",
            "messages": [
              {
                "role": "user",
                "content": [
                  {
                    "type": "text",
                    "text": "Describe en 5 palabras la imagen"
                  },
                  {
                    "type": "image_url",
                    "image_url": {
                      "url": "data:image/jpeg;base64,$imagePath"
                    }
                  }
                ]
              }
            ],
            "max_tokens": 300
        } """.trimIndent()

        println(requestBody)

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
                    try {
                        // Parsear la respuesta JSON
                        val jsonObject = JSONObject(body)
                        val choicesArray = jsonObject.getJSONArray("choices")
                        val contentResult = choicesArray.getJSONObject(0).getJSONObject("message").getString("content")
                        callback(contentResult)
                    } catch (e: JSONException) {
                        Log.e("Error", "Failed to parse JSON", e)
                        Toast.makeText(context, "Failed to parse JSON", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.v("data", "empty")
                }
            }
        })
    }


}