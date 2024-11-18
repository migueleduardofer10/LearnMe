package com.example.learnme.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
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
                // Implementar lógica de eliminación o cualquier otra acción necesaria
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

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}