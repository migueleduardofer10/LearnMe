package com.example.learnme.service

import com.example.learnme.adapter.ItemClass
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassDao
import com.example.learnme.data.ClassEntity
import com.example.learnme.data.ImageDao

class ClassService(private val database: AppDatabase) {

    private val classDao: ClassDao = database.classDao()
    private val imageDao: ImageDao = database.imageDao()

    fun initializeDefaultClasses() {
        val existingClasses = classDao.getAllClasses()
        if (existingClasses.isEmpty()) {
            val defaultClasses = listOf(
                "Clase 1",
                "Clase 2",
                "Clase 3",
                "Clase 4"
            )

            defaultClasses.forEach { className ->
                val newClass = ClassEntity(className = className)
                classDao.insertClass(newClass)
            }
        }
    }

    fun addNewClass(): ItemClass {
        val currentClassCount = getClassCount()

        //Validación de clases
        if (currentClassCount >= 4) {
            throw IllegalStateException("No se pueden agregar más de 4 clases.")
        }

        val newClassName = "Clase ${currentClassCount + 1}"
        val newClass = ClassEntity(className = newClassName)

        val classId = classDao.insertClass(newClass).toInt()

        return ItemClass(newClassName, classId, 0)
    }


    fun getClassCount(): Int {
        return classDao.getAllClasses().size
    }

    fun getAllClasses(): List<ItemClass> {
        val classes = classDao.getAllClasses()
        return classes.sortedBy { it.classId }.map { classEntity ->
            val sampleCount = imageDao.getImageCountForClass(classEntity.classId)
            ItemClass(classEntity.className, classEntity.classId, sampleCount)
        }
    }

    fun getClassName(classId: Int): String {
        val classEntity = database.classDao().getClassById(classId)
        return classEntity?.className ?: "Clase no encontrada"
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

    fun getClassesWithoutImages(): List<ItemClass> {
        val classes = classDao.getAllClasses()
        return classes.filter { classEntity ->
            imageDao.getImageCountForClass(classEntity.classId) == 0
        }.map { classEntity ->
            ItemClass(classEntity.className, classEntity.classId, 0)
        }
    }
}