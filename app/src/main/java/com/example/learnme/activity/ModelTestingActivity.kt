package com.example.learnme.activity

import ActivityTimeTracker
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageProxy
import androidx.lifecycle.lifecycleScope
import com.example.learnme.data.AppDatabase
import com.example.learnme.databinding.ActivityModelTestingBinding
import com.example.learnme.helper.CameraHelper
import com.example.learnme.helper.CameraPermissionsManager
import com.example.learnme.helper.TransferLearningHelper
import com.example.learnme.helper.TransferLearningManager
import com.tuempresa.helpers.BatteryUsageMonitor
import com.tuempresa.helpers.ResourceUsageMonitor
import com.tuempresa.helpers.logMemoryUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import java.io.IOException

class ModelTestingActivity : ComponentActivity(), TransferLearningHelper.ClassifierListener {

    private lateinit var binding: ActivityModelTestingBinding
    private lateinit var cameraHelper: CameraHelper
    private lateinit var bitmapBuffer: Bitmap
    private var mediaPlayer: MediaPlayer? = null
    private var audioUriMap: MutableMap<Int, Pair<String, Uri>> = mutableMapOf() // Mapa que asocia classId a (className, Uri)
    private var currentPlayingClass: String? = null
    private var audioUrisLoaded = false

    private var totalImagesTested = 0
    private var correctClassifications = 0
    private var totalInferenceTime = 0L

    private lateinit var resourceUsageMonitor: ResourceUsageMonitor
    private lateinit var batteryUsageMonitor: BatteryUsageMonitor

    private val activityTimeTracker = ActivityTimeTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelTestingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resourceUsageMonitor = ResourceUsageMonitor(this)
        batteryUsageMonitor = BatteryUsageMonitor(this)

        resourceUsageMonitor.startCpuMonitoring()
        batteryUsageMonitor.startMonitoring()

        val cameraPermissionsManager = CameraPermissionsManager(this) {
            cameraHelper = CameraHelper(
                this,
                binding.previewView,
                onImageCaptured = { image -> classifyImage(image) }
            )
            cameraHelper.startCamera()
            cameraHelper.startInference()
        }

        cameraPermissionsManager.checkAndRequestPermission()

        initializeMediaPlayer()

        loadAudioUrisFromDatabase {
            TransferLearningManager.updateClassifierListener(this)
        }

        binding.recaptureButton.setOnClickListener {
            startActivity(Intent(this, ClassSelectionActivity::class.java))
        }
    }


    private var classIdToNameMap: MutableMap<Int, String> = mutableMapOf() // Mapa que asocia classId a className

    private fun loadAudioUrisFromDatabase(onComplete: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val classDao = AppDatabase.getInstance(applicationContext).classDao()
            val classes = classDao.getAllClasses()

            Log.d("AudioDebug", "Iniciando la carga de URIs de audio para todas las clases. Total clases: ${classes.size}")

            classes.forEach { classEntity ->
                val className = classEntity.className
                val classId = classEntity.classId
                val audioPath = classEntity.audioPath

                classIdToNameMap[classId] = className

                if (audioPath != null) {
                    audioUriMap[classId] = Pair(className, Uri.parse(audioPath))
                    Log.d("AudioDebug", "Cargado audio para clase '$className' (ID: $classId): URI=${Uri.parse(audioPath)}")
                } else {
                    Log.w("AudioDebug", "Clase '$className' (ID: $classId) no tiene un audioPath asignado.")
                }
            }

            audioUrisLoaded = true
            Log.d("AudioDebug", "Carga de URIs de audio completa. Total URIs cargados: ${audioUriMap.size}")

            launch(Dispatchers.Main) { onComplete() }
        }
    }




    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener {
                    Log.d("MediaPlayer", "MediaPlayer preparado, iniciando reproducción")
                    it.start()
                }
                setOnCompletionListener {
                    Log.d("MediaPlayer", "Reproducción completada")
                    it.reset()
                }
            }
        }
    }

    private fun playClassAudio(className: String) {
        Log.d("playClassAudio", "Intentando reproducir audio para la clase: '$className'")

        if (className != currentPlayingClass) {
            mediaPlayer?.apply {
                if (isPlaying) {
                    Log.d("playClassAudio", "Deteniendo el audio actual y reseteando.")
                    stop()
                    reset()
                }
            }

            if (mediaPlayer == null) {
                initializeMediaPlayer()
            }

            val audioUri = audioUriMap.entries.find { it.value.first == className }?.value?.second
            if (audioUri != null) {
                try {
                    mediaPlayer?.apply {
                        reset()

                        setDataSource(applicationContext, audioUri)

                        setOnPreparedListener {
                            Log.d("playClassAudio", "Nuevo audio preparado, iniciando reproducción.")
                            it.start()
                        }

                        setOnCompletionListener {
                            Log.d("playClassAudio", "Reproducción completada para la clase $className.")
                            it.reset()
                        }

                        prepareAsync()
                    }

                    currentPlayingClass = className

                } catch (e: IOException) {
                    Log.e("ModelTestingActivity", "Error al preparar el audio: ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e("ModelTestingActivity", "Error de estado ilegal en el MediaPlayer: ${e.message}")
                }
            } else {
                Log.e("ModelTestingActivity", "No se encontró URI para la clase '$className'")
            }
        } else {
            Log.d("playClassAudio", "La clase $className ya está en reproducción, manteniendo el audio actual.")
        }
    }


    private fun classifyImage(image: ImageProxy) {
        val transferLearningHelper = TransferLearningManager.getTransferLearningHelper()
        if (transferLearningHelper == null) {
            Log.e("Inference", "TransferLearningHelper no inicializado")
            image.close()
            return
        }

        if (!::bitmapBuffer.isInitialized) {
            bitmapBuffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }

        Log.d("Inference", "Imagen capturada")

        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        transferLearningHelper.classify(bitmapBuffer, image.imageInfo.rotationDegrees)
    }

    override fun onStop() {
        super.onStop()

        resourceUsageMonitor.stopCpuMonitoring()
        batteryUsageMonitor.stopMonitoring()

        val accuracy = if (totalImagesTested > 0) {
            (correctClassifications.toFloat() / totalImagesTested) * 100
        } else {
            0f
        }

        val avgInferenceTime = if (totalImagesTested > 0) {
            totalInferenceTime / totalImagesTested
        } else {
            0L
        }

        Log.d("ModelTestingActivity", "Total de imágenes procesadas (Inferencia): $totalImagesTested")
        Log.d("ModelTestingActivity", "Número de clasificaciones correctas: $correctClassifications")
        Log.d("ModelTestingActivity", "Precisión: %.2f%%".format(accuracy))
        Log.d("ModelTestingActivity", "Tiempo total de clasificación: $totalInferenceTime ms")
        Log.d("ModelTestingActivity", "Tiempo promedio por imagen: %.2f ms".format(avgInferenceTime.toFloat()))

        logMemoryUsage(this)

        activityTimeTracker.endActivity()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        cameraHelper.shutdown()
    }

    override fun onError(error: String) {
        TODO("Not yet implemented")
    }

    @SuppressLint("SetTextI18n")
    override fun onResults(results: List<Category>?, inferenceTime: Long) {
        Log.d("InferenceResult", "Tiempo de inferencia para la imagen: $inferenceTime ms")
        runOnUiThread {
            results?.let { list ->
                list.maxByOrNull { it.score }?.let { highestScoreCategory ->
                    val confidenceScore = highestScoreCategory.score
                    val label = highestScoreCategory.label
                    Log.d("InferenceResult", "Categoría: $label, Puntaje: $confidenceScore, Tiempo de inferencia: $inferenceTime ms")

                    totalImagesTested++
                    if (confidenceScore == 1.0f) {
                        correctClassifications++
                    }

                    // Acumular el tiempo de inferencia
                    totalInferenceTime += inferenceTime

                    if (audioUrisLoaded && confidenceScore == 1.0f) {
                        val classId = label.toIntOrNull() ?: -1
                        if (classId != -1) {
                            val className = classIdToNameMap[classId]
                            if (className != null) {
                                binding.nameClass.text = className
                                binding.precisionText.text = "Precisión: ${"%.2f".format(confidenceScore * 100)}%"

                                playClassAudio(className)
                            } else {
                                Log.e("ModelTestingActivity", "No se encontró un nombre para la clase con ID: $classId")
                            }
                        }
                    } else {
                        binding.nameClass.text = "Clase desconocida"
                        binding.precisionText.text = "Precisión: ¿?"
                    }
                }
            }
        }
    }

    override fun onLossResults(lossNumber: Float) {
        Log.d("ModelTestingActivity", "Loss recibido: $lossNumber")
    }

}

