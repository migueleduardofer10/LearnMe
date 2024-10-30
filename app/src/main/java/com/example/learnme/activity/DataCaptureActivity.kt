package com.example.learnme.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityDataCaptureBinding
import com.example.learnme.fragments.CameraHelper
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem
import java.io.File


class DataCaptureActivity : ComponentActivity() {

    private lateinit var binding: ActivityDataCaptureBinding

    private lateinit var cameraHelper: CameraHelper
    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<ImageItem>()
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false
    private var classId: Int = -1  // Valor por defecto en caso de que no se reciba

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDataCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Inicializar CameraHelper con la vista de vista previa
        cameraHelper = CameraHelper(
            this,
            binding.previewView,
            onImageCaptured = { imageFile ->
                imageList.add(ImageItem(imageFile.path, classId))
                adapter.notifyItemInserted(imageList.size - 1)
                saveImagePath(imageFile.path, classId)
            }
        )
        cameraHelper.startCamera()

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
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(captureRunnable)
    }

    private fun loadCapturedImages() {
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val imagePaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf())

        val tempImageList = mutableListOf<ImageItem>()
        imagePaths?.forEach { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                val path = parts[0]
                val savedClassId = parts[1].toIntOrNull() ?: 0
                if (savedClassId == classId) {
                    tempImageList.add(ImageItem(path, savedClassId))
                }
            }
        }

        imageList.clear()
        imageList.addAll(
            tempImageList.sortedBy { imageItem ->
                File(imageItem.imagePath).nameWithoutExtension.toLongOrNull() ?: 0L
            }
        )
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("MutatingSharedPrefs")
    private fun saveImagePath(imagePath: String, classId: Int) {
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val existingPaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf()) ?: mutableSetOf()
        existingPaths.add("$imagePath|$classId")

        editor.putStringSet("imagePaths", existingPaths)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}