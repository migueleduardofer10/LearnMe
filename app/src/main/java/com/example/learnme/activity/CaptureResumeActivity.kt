package com.example.learnme.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ImageDao
import com.example.learnme.R
import com.example.learnme.config.GPTConfig
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem
import com.example.learnme.fragment.AudioHelper
import com.example.learnme.fragment.GPT4Helper
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

    private lateinit var audioHelper: AudioHelper
    private lateinit var gptHelper: GPT4Helper

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var audioUri: Uri? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos y el DAO
        database = AppDatabase.getInstance(this)
        imageDao = database.imageDao()

        // Obtener el nombre de la clase desde el Intent
        classId = intent.getIntExtra("classId", -1)

        gptHelper = GPT4Helper(client)
        audioHelper = AudioHelper(
            context = this,
            contentResolver = contentResolver,
            audioStatusView = binding.audioStatus,
            playAudioButton = binding.playAudioButton,
            seekBar = binding.seekBar
        )

        // Obtener el nombre de la clase desde la base de datos y mostrarlo en la UI
        loadClassName()
        loadSavedAudio()

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

        binding.playAudioButton.setOnClickListener {
            binding.playAudioButton.setOnClickListener {
                if (audioHelper.isPlaying) {
                    audioHelper.pauseAudio()
                } else {
                    audioHelper.playAudio()
                }
            }
        }
    }

    private fun loadSavedAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            val audioPath = database.classDao().getClassById(classId)?.audioPath
            audioPath?.let {
                audioHelper.setAudioUri(Uri.parse(it))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHelper.release()
    }


    private fun loadClassName() {
        CoroutineScope(Dispatchers.IO).launch {
            val classEntity = database.classDao().getClassById(classId)

            val className = classEntity?.className ?: "Clase desconocida"

            withContext(Dispatchers.Main) {
                binding.nameEditText.setText(className)
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

                gptHelper.sendImageToGPT4(base64Image) { response ->
                    // Actualizar el nombre de la clase y marcar isLabelGenerated como true
                    updateClassName(response)
                }
            }
        }
    }

    // Actualizar el nombre de la clase en la base de datos y la UI
    private fun updateClassName(newName: String) {
        val trimmedName = newName.trim().replace("\"", "")

        CoroutineScope(Dispatchers.Main).launch {
            binding.nameEditText.setText(trimmedName)  // Actualiza el nombre en la UI
        }

        CoroutineScope(Dispatchers.IO).launch {
            database.classDao().updateClassName(classId, trimmedName)
        }
    }

    // Codifica la imagen a Base64
    private fun encodeImageToBase64(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
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