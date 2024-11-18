package com.example.learnme.service

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.example.learnme.adapter.ImageItem
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ImageEntity
import java.io.File
import java.io.FileOutputStream

class ImageService(private val database: AppDatabase) {

    // Guardar imágenes seleccionadas en la base de datos
    fun saveImages(selectedImages: List<ImageItem>, classId: Int) {
        val imageDao = database.imageDao()
        selectedImages.forEach { imageItem ->
            val imageEntity = ImageEntity(
                imagePath = imageItem.imagePath,
                classId = classId
            )
            imageDao.insertImage(imageEntity)
        }
    }

    fun processAndSaveImage(imageProxy: ImageProxy, classId: Int, externalMediaDir: File): String {
        val imageFile = File(externalMediaDir, "${System.currentTimeMillis()}.jpg")
        val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)

        // Aplicar rotación
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }, true)

        // Guardar imagen en almacenamiento
        FileOutputStream(imageFile).use { outputStream ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        // Guardar la ruta en la base de datos
        saveImage(imageFile.path, classId)

        // Cerrar el proxy
        imageProxy.close()

        return imageFile.path
    }

    private fun saveImage(imagePath: String, classId: Int) {
        val imageDao = database.imageDao()
        imageDao.insertImage(ImageEntity(imagePath = imagePath, classId = classId))
    }

    fun getImagesForClass(classId: Int): List<ImageItem> {
        val images = database.imageDao().getImagesForClass(classId)
        return images.map { ImageItem(it.imagePath) }
            .sortedBy {
                File(it.imagePath).nameWithoutExtension.toLongOrNull() ?: 0L
            }
    }

    fun deleteImagesByPaths(imagePaths: List<String>) {
        val imageDao = database.imageDao()
        imagePaths.forEach { imagePath ->
            imageDao.deleteImageByPath(imagePath)
        }
    }
}
