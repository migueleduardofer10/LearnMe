package com.example.learnme.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.ImageProxy
import com.example.learnme.databinding.ActivityModelTestingBinding
import com.example.learnme.fragment.CameraHelper
import com.example.learnme.fragment.CameraPermissionsManager
import com.example.learnme.fragment.TransferLearning
import com.example.learnme.fragment.TransferLearningManager
import org.tensorflow.lite.support.label.Category

class ModelTestingActivity : ComponentActivity(), TransferLearning.ClassifierListener {

    private lateinit var binding: ActivityModelTestingBinding

    private lateinit var cameraHelper: CameraHelper
    private lateinit var bitmapBuffer: Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityModelTestingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar permisos de cámara
        val cameraPermissionsManager = CameraPermissionsManager(this) {
            // Solo se ejecuta si el permiso es otorgado
            // Inicializar CameraHelper con la vista de vista previa
            cameraHelper = CameraHelper(
                this,
                binding.previewView,
                onImageCaptured = { image -> classifyImage(image) }
            )
            cameraHelper.startCamera()
            cameraHelper.startInference()
        }

        // Configurar el listener para esta actividad
        TransferLearningManager.updateClassifierListener(this)

        cameraPermissionsManager.checkAndRequestPermission()
    }

    private fun classifyImage(image: ImageProxy) {
        // Creo que va a ser necesario que sea una variable global de la aplicacion
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

        val transferLearningHelper = TransferLearningManager.getTransferLearningHelper()
        if (transferLearningHelper == null) {
            Log.e("Inference", "TransferLearningHelper no inicializado")
            return
        }

        transferLearningHelper.classify(bitmapBuffer, image.imageInfo.rotationDegrees)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper.shutdown()
    }

    override fun onError(error: String) {
        TODO("Not yet implemented")
    }

    override fun onResults(results: List<Category>?, inferenceTime: Long) {
        runOnUiThread {
            // Mostrar resultados
            results?.let { list ->

                // Destacar la clase con la puntuación más alta.
                list.maxByOrNull { it.score }?.let { highestScoreCategory ->
                    // Log de la categoría con el puntaje más alto y el tiempo de inferencia
                    Log.d(
                        "InferenceResult",
                        "Categoría: ${highestScoreCategory.label}, Puntaje: ${highestScoreCategory.score}, Tiempo de inferencia: ${inferenceTime} ms"
                    )
                }
            }
        }
    }

    override fun onLossResults(lossNumber: Float) {
        TODO("Not yet implemented")
    }
}

