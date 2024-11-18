package com.example.learnme.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.example.learnme.data.AppDatabase
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem
import com.example.learnme.helper.AudioHelper
import com.example.learnme.helper.GPT4Helper
import com.example.learnme.service.ClassService
import com.example.learnme.service.ImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class CaptureResumeActivity : ComponentActivity() {

    private lateinit var binding: ActivityCaptureResumeBinding
    private lateinit var adapter: ImageAdapter
    private var classId: Int = -1
    private var imageList: MutableList<ImageItem> = mutableListOf()
    private var isSelectionMode = false

    private lateinit var classService: ClassService
    private lateinit var imageService: ImageService

    private lateinit var audioHelper: AudioHelper
    private lateinit var gptHelper: GPT4Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos y el DAO
        val database = AppDatabase.getInstance(this)
        classService = ClassService(database)
        imageService = ImageService(database)


        // Obtener el nombre de la clase desde el Intent
        classId = intent.getIntExtra("classId", -1)

        gptHelper = GPT4Helper(OkHttpClient())
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
            val audioPath = classService.getEntityClass(classId).audioPath
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
            val className = classService.getClassName(classId)

            withContext(Dispatchers.Main) {
                binding.nameEditText.text = className
            }
        }
    }

    // Cargar imágenes asociadas al classId desde la base de datos
    private fun loadImagesForClass(onImagesLoaded: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val tempImageList = imageService.getImagesForClass(classId)

            withContext(Dispatchers.Main) {
                imageList.clear()
                imageList.addAll(tempImageList)
                adapter.notifyDataSetChanged()
                onImagesLoaded()
            }
        }
    }

    // Genera una etiqueta automática para la clase usando la primera imagen si no tiene un nombre asignado
    private fun generateLabelIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val classEntity = classService.getEntityClass(classId)

            // Verificar si ya se ha generado una etiqueta
            if (!classEntity.isLabelGenerated && imageList.isNotEmpty()) {
                val firstImage = gptHelper.encodeImageToBase64(imageList.first().imagePath)
                gptHelper.sendImageToGPT4(firstImage) { response ->
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
            binding.nameEditText.text = trimmedName
        }
        CoroutineScope(Dispatchers.IO).launch {
            classService.updateClassName(classId, trimmedName)
        }
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
            val imagePaths = selectedImages.map { it.imagePath }

            // Usar el servicio para eliminar las imágenes
            imageService.deleteImagesByPaths(imagePaths)
            withContext(Dispatchers.Main) {
                imageList.removeAll(selectedImages)
                adapter.notifyDataSetChanged()
                exitSelectionMode()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForSelectionMode() {
        binding.fileCountTextView.text = "Seleccionar"
        binding.hamburgerButton.visibility = View.GONE
        binding.deleteButton.visibility = View.VISIBLE
        binding.cancelButton.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForNormalMode() {
        binding.fileCountTextView.text = "Imágenes capturadas"
        binding.hamburgerButton.visibility = View.VISIBLE
        binding.deleteButton.visibility = View.GONE
        binding.cancelButton.visibility = View.GONE
    }
}