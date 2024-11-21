package com.example.learnme.service

import com.example.learnme.adapter.ItemClass
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassDao
import com.example.learnme.data.ClassEntity
import com.example.learnme.data.ImageDao
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ClassServiceTest {

    @Mock
    private lateinit var mockDatabase: AppDatabase

    @Mock
    private lateinit var mockClassDao: ClassDao

    @Mock
    private lateinit var mockImageDao: ImageDao

    private lateinit var classService: ClassService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockDatabase.classDao()).thenReturn(mockClassDao)
        `when`(mockDatabase.imageDao()).thenReturn(mockImageDao)

        classService = ClassService(mockDatabase)
    }

//    @Test
//    //Cuando se agrega una nueva clase, se debe crear y devolver una nueva entidad
//    fun `addNewClass should create and return a new ItemClass`() {
//        // Configurar mocks
//        val existingClasses = listOf(ItemClass("Clase 1", 1, 0))
//        val newClass = ClassEntity(className = "Clase 2")
//        val newClassId = 2L
//        `when`(mockClassDao.insertClass(newClass)).thenReturn(newClassId)
//
//        // Ejecutar
//        val result = classService.addNewClass()
//
//        // Verificar
//        assertEquals("Clase 2", result.className)
//        assertEquals(2, result.classId)
//        assertEquals(0, result.sampleCount)
//
//        verify(mockClassDao).insertClass(newClass)
//    }

    @Test
    //Cuando se obtienen todas las clases, se debe devolver una lista de ItemClass con el conteo de muestras asociado
    fun `getAllClasses should return a list of ItemClass with correct sample counts`() {
        // Configurar mocks
        val classes = listOf(
            ClassEntity(1, "Clase 1"),
            ClassEntity(2, "Clase 2")
        )
        `when`(mockClassDao.getAllClasses()).thenReturn(classes)
        `when`(mockImageDao.getImageCountForClass(1)).thenReturn(3)
        `when`(mockImageDao.getImageCountForClass(2)).thenReturn(5)

        // Ejecutar
        val result = classService.getAllClasses()

        // Verificar
        assertEquals(2, result.size)
        assertEquals(ItemClass("Clase 1", 1, 3), result[0])
        assertEquals(ItemClass("Clase 2", 2, 5), result[1])
    }

    @Test
    //Cuando no hay clases en la base de datos, getAllClasses debe devolver una lista vacía
    fun `getAllClasses should return an empty list when no classes exist`() {
        // Configurar mocks
        `when`(mockClassDao.getAllClasses()).thenReturn(emptyList())

        // Ejecutar
        val result = classService.getAllClasses()

        // Verificar
        assertTrue("The result list should be empty", result.isEmpty())
        verify(mockClassDao).getAllClasses() // Verifica que se llamó al DAO
        verifyNoInteractions(mockImageDao)
    }

    @Test
    //Cuando el nombre de la clase existe en la base de datos, getClassName debe devolver el nombre de la clase
    fun `getClassName should return the name of the class if it exists`() {
        // Configurar mocks
        val classEntity = ClassEntity(1, "Clase 1")
        `when`(mockClassDao.getClassById(1)).thenReturn(classEntity)

        // Ejecutar
        val result = classService.getClassName(1)

        // Verificar
        assertEquals("Clase 1", result)
    }

    @Test
    //Cuando la clase no existe en la base de datos, getClassName debe devolver "Clase desconocida"
    fun `getClassName should return 'Clase desconocida' if class does not exist`() {
        // Configurar mocks
        `when`(mockClassDao.getClassById(1)).thenReturn(null)

        // Ejecutar
        val result = classService.getClassName(1)

        // Verificar
        assertEquals("Clase desconocida", result)
    }

    @Test
    //Cuando la clase existe en la base de datos, getEntityClass debe devolver la entidad de clase
    fun `getEntityClass should return the ClassEntity if it exists`() {
        // Configurar mocks
        val classEntity = ClassEntity(1, "Clase 1")
        `when`(mockClassDao.getClassById(1)).thenReturn(classEntity)

        // Ejecutar
        val result = classService.getEntityClass(1)

        // Verificar
        assertEquals(classEntity, result)
    }

    @Test(expected = IllegalArgumentException::class)
    //Cuando la clase no existe en la base de datos, getEntityClass debe lanzar una excepción
    fun `getEntityClass should throw IllegalArgumentException if class does not exist`() {
        // Configurar mocks
        `when`(mockClassDao.getClassById(1)).thenReturn(null)

        // Ejecutar
        classService.getEntityClass(1)
    }

    @Test
    //Cuando se actualiza el nombre de la clase, se debe actualizar el nombre en la base de datos
    fun `updateClassName should update the class name in the database`() {
        // Ejecutar
        classService.updateClassName(1, "Nuevo Nombre")

        // Verificar
        verify(mockClassDao).updateClassName(1, "Nuevo Nombre")
    }
}