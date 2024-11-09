package com.example.learnme.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.example.learnme.AppDatabase
import com.example.learnme.ImageDao
import com.example.learnme.ImageEntity
import com.example.learnme.R
import com.example.learnme.config.GPTConfig
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.io.File
import java.io.IOException

class CaptureResumeActivity : ComponentActivity() {

    private val client = OkHttpClient()

    private lateinit var binding: ActivityCaptureResumeBinding
    private lateinit var adapter: ImageAdapter
    private var classId: Int = -1
    private var imageList: MutableList<ImageItem> = mutableListOf()
    private var isSelectionMode = false

    private lateinit var database: AppDatabase
    private lateinit var imageDao: ImageDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos y el DAO
        database = AppDatabase.getInstance(this)
        imageDao = database.imageDao()

        // Obtener el nombre de la clase desde el Intent
        classId = intent.getIntExtra("classId", -1)

        // Obtener el nombre de la clase desde la base de datos y mostrarlo en la UI
        loadClassName()

        // Configurar el RecyclerView
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        adapter = GridConfig.setupGridWithAdapter(
            recyclerView = binding.recyclerViewImages,
            context = this,
            spanCount = 5,
            spacing = spacing,
            imageList = imageList,
            onItemClick = { imageItem ->
                toggleSelection(imageItem)
            }
        )

        // Cargar imágenes desde la base de datos
        loadImagesForClass { generateLabelIfNeeded() }

        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, DataCaptureActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }

        binding.uploadButton.setOnClickListener {
            val intent = Intent(this, ImageGalleryActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }

        // Botón para activar el modo de selección
        binding.hamburgerButton.setOnClickListener {
            enterSelectionMode()
        }

        // Botón para eliminar imágenes seleccionadas
        binding.deleteButton.setOnClickListener {
            deleteSelectedImages()
        }

        // Botón para cancelar el modo de selección
        binding.cancelButton.setOnClickListener {
            exitSelectionMode()
        }

        // Botón para regresar a la actividad anterior
        binding.backButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }
    }

    // Cargar el nombre de la clase desde la base de datos
    private fun loadClassName() {
        CoroutineScope(Dispatchers.IO).launch {
            val className = database.classDao().getClassNameById(classId) ?: "Clase $classId"

            withContext(Dispatchers.Main) {
                binding.nameEditText.text = className
            }
        }
    }

    // Cargar imágenes asociadas al classId desde la base de datos
    private fun loadImagesForClass(onImagesLoaded: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val images = imageDao.getImagesForClass(classId)
            val tempImageList = images.map { ImageItem(it.imagePath, it.classId) }

            withContext(Dispatchers.Main) {
                imageList.clear()
                imageList.addAll(tempImageList.sortedBy {
                    File(it.imagePath).nameWithoutExtension.toLongOrNull() ?: 0L
                })
                adapter.notifyDataSetChanged()
                onImagesLoaded()  // Llama a la función una vez que las imágenes se hayan cargado
            }
        }
    }

    // Genera una etiqueta automática para la clase usando la primera imagen si no tiene un nombre asignado
    private fun generateLabelIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val classEntity = database.classDao().getClassById(classId)

            // Verificar si ya se ha generado una etiqueta
            if (classEntity != null && !classEntity.isLabelGenerated && imageList.isNotEmpty()) {
                val firstImagePath = imageList.first().imagePath
                val base64Image = encodeImageToBase64(firstImagePath)

                sendImageToGPT4(base64Image) { response ->
                    // Actualizar el nombre de la clase y marcar isLabelGenerated como true
                    updateClassName(response)
                }
            }
        }
    }

    // Actualizar el nombre de la clase en la base de datos
    private fun updateClassName(newName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            database.classDao().updateClassName(classId, newName)
            withContext(Dispatchers.Main) {
                binding.nameEditText.text = newName  // Actualizar en la interfaz
            }
        }
    }

    // Codifica la imagen a Base64
    private fun encodeImageToBase64(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Envía la imagen a la API de GPT-4 para obtener una etiqueta
    private fun sendImageToGPT4(base64Image: String, callback: (String) -> Unit) {
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
                        val jsonObject = JSONObject(responseBody)
                        val choicesArray = jsonObject.getJSONArray("choices")
                        val generatedLabel = choicesArray
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        callback(generatedLabel)
                    } catch (e: JSONException) {
                        Log.e("Error", "Failed to parse JSON", e)
                    }
                }
            }
        })
    }

    private fun toggleSelection(imageItem: ImageItem) {
        if (isSelectionMode) {
            imageItem.isSelected = !imageItem.isSelected
            adapter.notifyDataSetChanged()
        }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.isSelectionMode = true
        updateUIForSelectionMode()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter.isSelectionMode = false
        adapter.clearSelection()
        updateUIForNormalMode()
    }

    private fun deleteSelectedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val selectedImages = imageList.filter { it.isSelected }
            selectedImages.forEach { imageItem ->
                imageDao.deleteImageByPath(imageItem.imagePath)
            }

            withContext(Dispatchers.Main) {
                imageList.removeAll(selectedImages)
                adapter.notifyDataSetChanged()
                exitSelectionMode()
            }
        }
    }

    private fun updateUIForSelectionMode() {
        binding.fileCountTextView.text = "Seleccionar"
        binding.hamburgerButton.visibility = View.GONE
        binding.deleteButton.visibility = View.VISIBLE
        binding.cancelButton.visibility = View.VISIBLE
    }

    private fun updateUIForNormalMode() {
        binding.fileCountTextView.text = "Imágenes capturadas"
        binding.hamburgerButton.visibility = View.VISIBLE
        binding.deleteButton.visibility = View.GONE
        binding.cancelButton.visibility = View.GONE
    }
}