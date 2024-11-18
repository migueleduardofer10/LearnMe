package com.example.learnme.service

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class GalleryServiceTest {

    private lateinit var galleryService: GalleryService
    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockCursor: Cursor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Crear mocks de Context y ContentResolver
        mockContext = mock(Context::class.java)
        mockContentResolver = mock(ContentResolver::class.java)
        mockCursor = mock(Cursor::class.java)

        // Configurar ContentResolver para retornar un cursor simulado
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(),
            isNull(),
            isNull(),
            anyString()
        )).thenReturn(mockCursor)

        // Instanciar el GalleryHelper con el contexto simulado
        galleryService = GalleryService(mockContext)
    }

    @Test
    //Cuando "cursor" es nulo, la función debe devolver una lista vacía.
    fun `loadImagesFromGallery returns empty list when cursor is null`() {
        `when`(mockContentResolver.query(
            eq(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            any(),
            isNull(),
            isNull(),
            anyString()
        )).thenReturn(null)

        val images = galleryService.loadImagesFromGallery()

        assertEquals(0, images.size)
    }

    @Test
    //Cuando "cursor" no contiene datos, la función debe devolver una lista vacía.
    fun `loadImagesFromGallery returns empty list when cursor has no data`() {
        `when`(mockCursor.moveToNext()).thenReturn(false)

        val images = galleryService.loadImagesFromGallery()

        assertEquals(0, images.size)
    }

    @Test
    //Cuando "cursor" contiene datos, la función debe cargar las imágenes correctamente en la lista.
    fun `loadImagesFromGallery returns list of images when cursor has data`() {
        // Simular una lista de rutas de imágenes
        val imagePath = "/path/to/image.jpg"
        `when`(mockCursor.moveToNext()).thenReturn(true).thenReturn(false)
        `when`(mockCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)).thenReturn(0)
        `when`(mockCursor.getString(0)).thenReturn(imagePath)

        val images = galleryService.loadImagesFromGallery()

        // Verificar que se ha cargado la imagen correctamente en la lista
        assertEquals(1, images.size)
        assertEquals(imagePath, images[0].imagePath)
    }

    @Test
    //Después de usar el cursor, se debe cerrar para liberar recursos.
    fun `loadImagesFromGallery closes cursor after use`() {
        galleryService.loadImagesFromGallery()

        // Verificar que el cursor se cierra después de su uso
        verify(mockCursor).close()
    }
}