package com.example.learnme.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDataCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos y el DAO
        val database = AppDatabase.getInstance(this)
        imageService = ImageService(database)

        // Obtener el classId del Intent
        classId = intent.getIntExtra("classId", -1)

        // Configurar el RecyclerView
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
                }
            }
        )

        // Cargar imágenes desde la base de datos
        loadCapturedImages()

        // Inicializar permisos de cámara
        val cameraPermissionsManager = CameraPermissionsManager(this) {
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

        // Configurar botones
        binding.cameraButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!hasReachedMaxSamples()) {
                        isCapturing = true
                        startContinuousCapture()
                    } else {
                        showMaxSamplesReachedAlert()
                    }
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
                if (isCapturing && !hasReachedMaxSamples()) {
                    cameraHelper.takePhoto()
                    handler.postDelayed(this, 500)
                } else if (hasReachedMaxSamples()) {
                    isCapturing = false
                    showMaxSamplesReachedAlert()
                }
            }
        }
        handler.post(captureRunnable)
    }

    private fun hasReachedMaxSamples(): Boolean {
        return imageList.size >= 5
    }

    private fun showMaxSamplesReachedAlert() {
        AlertDialog.Builder(this)
            .setTitle("Límite alcanzado")
            .setMessage("Solo puedes capturar un máximo de 5 imágenes para esta clase.")
            .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

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

    private fun processAndSaveImage(imageProxy: ImageProxy) {
        if (!hasReachedMaxSamples()) {
            val externalMediaDir = externalMediaDirs.first()
            CoroutineScope(Dispatchers.IO).launch {
                val imagePath =
                    imageService.processAndSaveImage(imageProxy, classId, externalMediaDir)

                withContext(Dispatchers.Main) {
                    imageList.add(ImageItem(imagePath))
                    adapter.notifyItemInserted(imageList.size - 1)
                }
            }
        }
    }

    private fun toggleSelection(imageItem: ImageItem) {
        imageItem.isSelected = !imageItem.isSelected
        adapter.notifyDataSetChanged()
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.isSelectionMode = true
        updateUIForSelectionMode()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter.isSelectionMode = false
        imageList.forEach { it.isSelected = false } // Limpiar selección
        adapter.notifyDataSetChanged()
        updateUIForNormalMode()
    }

    private fun deleteSelectedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val selectedImages = imageList.filter { it.isSelected }
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
