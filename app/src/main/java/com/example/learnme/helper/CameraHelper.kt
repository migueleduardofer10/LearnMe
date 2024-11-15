package com.example.learnme.helper

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(
    private val activity: ComponentActivity,
    private val previewView: PreviewView,
    private val onImageCaptured: (ImageProxy) -> Unit,
) {

    private lateinit var imageAnalyzer: ImageAnalysis
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isInferenceMode: Boolean = false  // Estado de inferencia


    // Iniciar la cámara y configurar la vista previa
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Verifica que el display no sea null
            val rotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0

            val preview = Preview.Builder()
                .setTargetRotation(rotation)  // Usa rotación con verificación de null
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(rotation)  // Usa rotación con verificación de null
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also {
                    if (isInferenceMode) {
                        it.setAnalyzer(cameraExecutor) { image ->
                            onImageCaptured(image)  // Envía el ImageProxy a la actividad
                            image.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    activity,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                // Manejar error de la cámara
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    // Función para tomar una foto en formato ImageProxy
    fun takePhoto() {
        var captureMode = true
        imageAnalyzer.setAnalyzer(cameraExecutor) { image ->
            if (captureMode) {
                onImageCaptured(image)  // Envía el ImageProxy a la actividad
                captureMode = false
                imageAnalyzer.clearAnalyzer()  // Detener el análisis después de capturar la imagen
            } else {
                image.close()
            }
        }
    }

    fun startInference() {
        isInferenceMode = true
        Log.d("InferenceResult", "Inferencia iniciada")
    }


    // Liberar el ejecutor de la cámara
    fun shutdown() {
        cameraExecutor.shutdown()
        isInferenceMode = false
        imageAnalyzer.clearAnalyzer()
    }
}
