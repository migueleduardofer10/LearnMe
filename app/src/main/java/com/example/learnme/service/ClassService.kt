package com.example.learnme.service

import com.example.learnme.adapter.ItemClass
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassDao
import com.example.learnme.data.ClassEntity
import com.example.learnme.data.ImageDao

class ClassService(private val database: AppDatabase) {

    private val classDao: ClassDao = database.classDao()
    private val imageDao: ImageDao = database.imageDao()

    fun addNewClass(): ItemClass {
        val currentClassCount = getClassCount()
        val newClassName = "Clase ${currentClassCount + 1}"

        val newClass = ClassEntity(className = newClassName)
        val classId = classDao.insertClass(newClass).toInt()

        return ItemClass(newClassName, classId, 0)
    }

    fun getClassCount(): Int {
        return classDao.getAllClasses().size
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

    fun deleteImagesByClassId(classId: Int) {
        imageDao.deleteImagesByClassId(classId)
    }
    fun deleteClass(classId: Int) {
        classDao.deleteClass(classId)
    }
}