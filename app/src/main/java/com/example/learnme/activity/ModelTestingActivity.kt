package com.example.learnme.activity

import android.annotation.SuppressLint
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelTestingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar y verificar permisos antes de cargar los URIs de audio o cualquier operación adicional
        val cameraPermissionsManager = CameraPermissionsManager(this) {
            // Una vez otorgado el permiso, inicializar la cámara
            cameraHelper = CameraHelper(
                this,
                binding.previewView,
                onImageCaptured = { image -> classifyImage(image) }
            )
            cameraHelper.startCamera()
            cameraHelper.startInference()
        }

        cameraPermissionsManager.checkAndRequestPermission()

        // Configurar el MediaPlayer
        initializeMediaPlayer()

        // Cargar los URIs de audio después de la verificación de permisos
        loadAudioUrisFromDatabase {
            TransferLearningManager.updateClassifierListener(this)
            // Aquí se pueden realizar configuraciones adicionales si es necesario
        }
    }


    // Modificación en loadAudioUrisFromDatabase para recibir una función 'onComplete'
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

                // Rellenar el mapa con classId y className
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

            // Ejecutar la acción después de cargar los URIs
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
                    it.reset() // Resetea para que pueda ser reutilizado más tarde
                }
            }
        }
    }

    private fun playClassAudio(className: String) {
        Log.d("playClassAudio", "Intentando reproducir audio para la clase: '$className'")

        if (className != currentPlayingClass) {
            // Si hay un audio en reproducción, detenerlo y resetear
            mediaPlayer?.apply {
                if (isPlaying) {
                    Log.d("playClassAudio", "Deteniendo el audio actual y reseteando.")
                    stop()
                    reset()
                }
            }

            // Verifica si el MediaPlayer es null y lo inicializa si es necesario
            if (mediaPlayer == null) {
                initializeMediaPlayer()
            }

            // Busca el URI correspondiente al className
            val audioUri = audioUriMap.entries.find { it.value.first == className }?.value?.second
            if (audioUri != null) {
                try {
                    // Configura la fuente de datos del MediaPlayer
                    mediaPlayer?.apply {
                        // El MediaPlayer debe estar reseteado antes de setear una nueva fuente
                        reset()

                        setDataSource(applicationContext, audioUri)

                        setOnPreparedListener {
                            Log.d("playClassAudio", "Nuevo audio preparado, iniciando reproducción.")
                            it.start()
                        }

                        setOnCompletionListener {
                            Log.d("playClassAudio", "Reproducción completada para la clase $className.")
                            it.reset() // Resetear para poder reutilizarlo
                        }

                        // Prepara el MediaPlayer de forma asíncrona para evitar bloquear el hilo principal
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
            image.close()  // Asegúrate de cerrar la imagen para evitar fugas de memoria
            return
        }

        // Procesar la imagen solo si TransferLearningHelper está listo
        if (!::bitmapBuffer.isInitialized) {
            bitmapBuffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }

        Log.d("Inference", "Imagen capturada")

        // Copia los píxeles de la imagen en el buffer de bitmap
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        transferLearningHelper.classify(bitmapBuffer, image.imageInfo.rotationDegrees)
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
        runOnUiThread {
            results?.let { list ->
                list.maxByOrNull { it.score }?.let { highestScoreCategory ->
                    val confidenceScore = highestScoreCategory.score
                    val label = highestScoreCategory.label
                    Log.d("InferenceResult", "Categoría: $label, Puntaje: $confidenceScore, Tiempo de inferencia: $inferenceTime ms")

                    if (audioUrisLoaded && confidenceScore >= 0.999) {
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
        TODO("Not yet implemented")
    }

}

