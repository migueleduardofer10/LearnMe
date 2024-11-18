package com.example.learnme.service

import com.example.learnme.adapter.ItemClass
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassDao
import com.example.learnme.data.ClassEntity
import com.example.learnme.data.ImageDao

class ClassService(private val database: AppDatabase) {

    private val classDao: ClassDao = database.classDao()
    private val imageDao: ImageDao = database.imageDao()

    // Agregar una nueva clase y devolver su representación
    fun addNewClass(existingClasses: List<ItemClass>): ItemClass {
        val newClass = ClassEntity(className = "Clase ${existingClasses.size + 1}")
        val classId = classDao.insertClass(newClass).toInt()

        return ItemClass("Clase $classId", classId, 0)
    }

    // Obtener todas las clases con el conteo de muestras asociado
    fun getAllClasses(): List<ItemClass> {
        val classes = classDao.getAllClasses()
        return classes.map { classEntity ->
            val sampleCount = imageDao.getImageCountForClass(classEntity.classId)
            ItemClass(classEntity.className, classEntity.classId, sampleCount)
        }
    }

    fun getClassName(classId: Int): String {
        val classEntity = database.classDao().getClassById(classId)
        return classEntity?.className ?: "Clase desconocida"
    }

    fun getEntityClass(classId: Int): ClassEntity {
        val classEntity = database.classDao().getClassById(classId)
        return classEntity ?: throw IllegalArgumentException("Clase no encontrada")
    }

    fun updateClassName(classId: Int, newName: String) {
        database.classDao().updateClassName(classId, newName)
    }
}