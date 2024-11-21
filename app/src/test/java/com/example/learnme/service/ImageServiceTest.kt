package com.example.learnme.service

import com.example.learnme.adapter.ImageItem
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ImageDao
import com.example.learnme.data.ImageEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ImageServiceTest {

    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockImageDao: ImageDao
    private lateinit var imageService: ImageService

    @Before
    fun setUp() {
        mockDatabase = mock(AppDatabase::class.java)
        mockImageDao = mock(ImageDao::class.java)
        `when`(mockDatabase.imageDao()).thenReturn(mockImageDao)
        imageService = ImageService(mockDatabase)
    }

    @Test
    //Cuando se ejecuta saveImages, se deben guardar las imágenes seleccionadas en la base de datos
    fun `saveImages should save images to the database`() {
        // Datos de prueba
        val classId = 1
        val selectedImages = listOf(
            ImageItem(imagePath = "path1.jpg"),
            ImageItem(imagePath = "path2.jpg")
        )

        // Ejecutar el método
        imageService.saveImages(selectedImages, classId)

        // Verificar interacciones
        verify(mockImageDao).insertImage(ImageEntity(imagePath = "path1.jpg", classId = classId))
        verify(mockImageDao).insertImage(ImageEntity(imagePath = "path2.jpg", classId = classId))
    }

    @Test
    //Cuando se obtienen imágenes para una clase, se deben devolver en orden cronológico
    fun `getImagesForClass should return a list of images sorted by timestamp`() {
        // Datos simulados
        val classId = 1
        val dbImages = listOf(
            ImageEntity(imagePath = "/media/1697030000000.jpg", classId = classId),
            ImageEntity(imagePath = "/media/1697035000000.jpg", classId = classId),
            ImageEntity(imagePath = "/media/1697020000000.jpg", classId = classId)
        )
        `when`(mockImageDao.getImagesForClass(classId)).thenReturn(dbImages)

        // Ejecutar el método
        val result = imageService.getImagesForClass(classId)

        // Verificar resultados
        val expectedPaths = listOf(
            "/media/1697020000000.jpg",
            "/media/1697030000000.jpg",
            "/media/1697035000000.jpg"
        )
        assertEquals(expectedPaths, result.map { it.imagePath })
    }

    @Test
    //Cuando se ejecuta deleteImagesByPaths, se deben eliminar las imágenes de la base de datos
    fun `deleteImagesByPaths should delete images from the database`() {
        // Datos de prueba
        val imagePaths = listOf("path1.jpg", "path2.jpg")

        // Ejecutar el método
        imageService.deleteImagesByPaths(imagePaths)

        // Verificar interacciones
        verify(mockImageDao).deleteImageByPath("path1.jpg")
        verify(mockImageDao).deleteImageByPath("path2.jpg")
    }

    @Test
    //Cuando se ejecuta saveImage, se debe guardar la imagen en la base de datos
    fun `saveImage should save a single image to the database`() {
        // Datos de prueba
        val imagePath = "path1.jpg"
        val classId = 1

        // Ejecutar el método
        val saveImageMethod = ImageService::class.java.getDeclaredMethod("saveImage", String::class.java, Int::class.java)
        saveImageMethod.isAccessible = true
        saveImageMethod.invoke(imageService, imagePath, classId)

        // Verificar interacciones
        verify(mockImageDao).insertImage(ImageEntity(imagePath = imagePath, classId = classId))
    }
}
