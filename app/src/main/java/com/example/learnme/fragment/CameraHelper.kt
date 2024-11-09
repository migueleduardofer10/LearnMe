package com.example.learnme.fragment

import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(
    private val activity: ComponentActivity,
    private val previewView: PreviewView,
    private val onImageCaptured: (File) -> Unit
) {

    private lateinit var imageCapture: ImageCapture
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Iniciar la cámara y configurar la vista previa
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Manejar error de la cámara
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    // Tomar una foto y guardar el archivo
    fun takePhoto() {
        val imageFile = File(activity.externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Manejar error de captura
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onImageCaptured(imageFile) // Llamada de retorno cuando se guarda la imagen
                }
            }
        )
    }

    // Liberar el ejecutor de la cámara
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
