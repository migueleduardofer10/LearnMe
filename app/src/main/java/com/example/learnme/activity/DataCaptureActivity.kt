package com.example.learnme.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageProxy
import com.example.learnme.data.AppDatabase
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityDataCaptureBinding
import com.example.learnme.helper.CameraHelper
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem
import com.example.learnme.helper.CameraPermissionsManager
import com.example.learnme.service.ImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DataCaptureActivity : ComponentActivity() {

    private lateinit var binding: ActivityDataCaptureBinding
    private lateinit var cameraHelper: CameraHelper
    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<ImageItem>()
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false
    private var classId: Int = -1

    private lateinit var imageService: ImageService

    private var isSelectionMode = false
    private val selectedImages = mutableListOf<ImageItem>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDataCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Configurar la base de datos y el DAO
        val database = AppDatabase.getInstance(this)
        imageService = ImageService(database)


        // Obtén el classId del Intent
        classId = intent.getIntExtra("classId", -1)  // -1 es un valor por defecto

        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        adapter = GridConfig.setupGridWithAdapter(
            recyclerView = binding.recyclerViewImages,
            context = this,
            spanCount = 5,
            spacing = spacing,
            imageList = imageList,
            onItemClick = { imageItem ->
                if (isSelectionMode) {
                    toggleSelection(imageItem)
                } else {
                    // Lógica adicional para clics normales si es necesario
                }
            }
        )

        // Cargar imágenes desde SharedPreferences
        loadCapturedImages()

        // Inicializar permisos de cámara
        val cameraPermissionsManager = CameraPermissionsManager(this) {
            // Solo se ejecuta si el permiso es otorgado
            // Inicializar CameraHelper con la vista de vista previa
            cameraHelper = CameraHelper(
                this,
                binding.previewView,
                onImageCaptured = { imageProxy ->
                    processAndSaveImage(imageProxy)
                }
            )
            cameraHelper.startCamera()
        }

        cameraPermissionsManager.checkAndRequestPermission()

        binding.cameraButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isCapturing = true
                    startContinuousCapture()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isCapturing = false
                    handler.removeCallbacksAndMessages(null)
                }
            }
            true
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, CaptureResumeActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }

        // Configurar botones para selección y eliminación
        binding.hamburgerButton.setOnClickListener { enterSelectionMode() }
        binding.deleteButton.setOnClickListener { deleteSelectedImages() }
        binding.cancelButton.setOnClickListener { exitSelectionMode() }

    }

    private fun startContinuousCapture() {
        val captureRunnable = object : Runnable {
            override fun run() {
                if (isCapturing) {
                    cameraHelper.takePhoto()
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(captureRunnable)
    }

    // Cargar imágenes desde la base de datos
    private fun loadCapturedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val tempImageList = imageService.getImagesForClass(classId)

            withContext(Dispatchers.Main) {
                imageList.clear()
                imageList.addAll(tempImageList)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // Procesar y guardar ImageProxy en un archivo
    private fun processAndSaveImage(imageProxy: ImageProxy) {
        val externalMediaDir = externalMediaDirs.first()
        CoroutineScope(Dispatchers.IO).launch {
            val imagePath = imageService.processAndSaveImage(imageProxy, classId, externalMediaDir)

            withContext(Dispatchers.Main) {
                imageList.add(ImageItem(imagePath))
                adapter.notifyItemInserted(imageList.size - 1)
            }
        }
    }

    private fun toggleSelection(imageItem: ImageItem) {
        if (selectedImages.contains(imageItem)) {
            selectedImages.remove(imageItem)
        } else {
            selectedImages.add(imageItem)
        }
        adapter.notifyDataSetChanged()
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.isSelectionMode = true
        updateUIForSelectionMode()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedImages.clear()
        adapter.isSelectionMode = false
        adapter.notifyDataSetChanged()
        updateUIForNormalMode()
    }

    private fun deleteSelectedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val imagePaths = selectedImages.map { it.imagePath }
            imageService.deleteImagesByPaths(imagePaths)

            withContext(Dispatchers.Main) {
                imageList.removeAll(selectedImages)
                adapter.notifyDataSetChanged()
                exitSelectionMode()
            }
        }
    }

    private fun updateUIForSelectionMode() {
        binding.fileCountTextView.text = "Seleccionar imágenes"
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

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}