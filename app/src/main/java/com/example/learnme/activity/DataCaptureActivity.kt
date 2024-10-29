package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.fragments.GridSpacingItemDecoration
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DataCaptureActivity : ComponentActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<ImageItem>()
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false
    private var classId: Int = -1  // Valor por defecto en caso de que no se reciba

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_capture)

        // Obtén el classId del Intent
        classId = intent.getIntExtra("classId", -1)  // -1 es un valor por defecto


        // Configurar RecyclerView directamente
        recyclerView = findViewById(R.id.recyclerViewImages)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerView.addItemDecoration(GridSpacingItemDecoration(3, spacing))

        // Inicializar adaptador
        adapter = ImageAdapter(imageList) { imagesToDelete ->
            // Implementar lógica de eliminación si es necesario
        }
        recyclerView.adapter = adapter

        // Cargar imágenes desde SharedPreferences
        loadCapturedImages()

        // Configurar cámara
        startCamera()

        // Ejecutar cámara en segundo plano
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Botón de captura
        val captureButton = findViewById<Button>(R.id.cameraButton)
        captureButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isCapturing = true
                    startContinuousCapture()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isCapturing = false
                    handler.removeCallbacksAndMessages(null)  // Detener el ciclo de captura
                }
            }
            true
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, CaptureResumeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Configurar vista previa
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }

            // Configurar capturas de imagen
            imageCapture = ImageCapture.Builder().build()

            // Selección de cámara (trasera)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind antes de bind (en caso de recargar)
                cameraProvider.unbindAll()

                // Bind de cámara con vista previa y captura
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                // Manejar error de cámara
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startContinuousCapture() {
        val captureRunnable = object : Runnable {
            override fun run() {
                if (isCapturing) {
                    takePhoto()
                    handler.postDelayed(this, 1000)  // Capturar cada segundo (ajustable)
                }
            }
        }
        handler.post(captureRunnable)  // Iniciar la primera captura
    }

    private fun takePhoto() {
        val imageFile = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Manejar error de captura
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Imagen capturada correctamente, agregar a la lista de imágenes
                    imageList.add(ImageItem(imageFile.path, classId))
                    adapter.notifyItemInserted(imageList.size - 1)  // Actualizar en tiempo real

                    // Guardar la ruta de la imagen en SharedPreferences
                    saveImagePath(imageFile.path, classId)
                }
            }
        )
    }

    // Método para cargar las rutas de imágenes guardadas en SharedPreferences
    private fun loadCapturedImages() {
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val imagePaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf())

        val tempImageList = mutableListOf<ImageItem>()

        imagePaths?.forEach { entry ->
            // Separar la entrada en "ruta" y "classId" usando el delimitador "|"
            val parts = entry.split("|")
            if (parts.size == 2) {
                val path = parts[0]
                val savedClassId = parts[1].toIntOrNull() ?: 0
                if (savedClassId == classId) {
                    tempImageList.add(ImageItem(path, savedClassId))
                }
            }
        }

        // Ordenar la lista de imágenes por el timestamp en el nombre del archivo
        imageList.clear()
        imageList.addAll(
            tempImageList.sortedBy { imageItem ->
                File(imageItem.imagePath).nameWithoutExtension.toLongOrNull() ?: 0L
            }
        )

        // Notificar al adaptador que los datos han cambiado
        adapter.notifyDataSetChanged()
    }


    private fun saveImagePath(imagePath: String, classId: Int) {
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Recuperar las rutas anteriores y agregar la nueva en el formato "ruta|classId"
        val existingPaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf()) ?: mutableSetOf()
        existingPaths.add("$imagePath|$classId")

        // Guardar las rutas actualizadas
        editor.putStringSet("imagePaths", existingPaths)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handler.removeCallbacksAndMessages(null)  // Asegurarse de limpiar el Handler al destruir la actividad
    }
}