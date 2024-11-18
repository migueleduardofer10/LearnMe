package com.example.learnme.helper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TransferLearningManager {
    @SuppressLint("StaticFieldLeak")
    private var transferLearningHelper: TransferLearningHelper? = null

    // Inicializar el TransferLearning solo si no ha sido inicializado
    suspend fun initialize(context: Context, classes: List<ClassEntity>, listener: TransferLearningHelper.ClassifierListener) {
        if (transferLearningHelper == null) {
            transferLearningHelper = TransferLearningHelper(
                context = context.applicationContext,  // Acceso seguro al contexto de la aplicación
                classifierListener = listener,
                classes = classes
            )
            loadTrainingSamplesFromDatabase(context)
        }
    }

    fun getTransferLearningHelper(): TransferLearningHelper? {
        return transferLearningHelper
    }

    fun updateClassifierListener(listener: TransferLearningHelper.ClassifierListener) {
        transferLearningHelper?.updateListener(listener)
    }

    // Cargar muestras desde la base de datos
    private suspend fun loadTrainingSamplesFromDatabase(context: Context) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context.applicationContext)
            val images = database.imageDao().getAllImages()

            images.forEach { imageEntity ->
                val imagePath = imageEntity.imagePath
                val classId = imageEntity.classId
                val file = File(imagePath)

                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    transferLearningHelper?.addSample(bitmap, classId, 0)
                    Log.d("TransferLearningManager", "Imagen cargada desde la ruta: $imagePath")
                } else {
                    Log.e("TransferLearningManager", "No se pudo cargar la imagen en la ruta: $imagePath")
                }
            }
        }
    }

    fun startTraining() {
        transferLearningHelper?.startTraining()
    }

    fun pauseTraining() {
        transferLearningHelper?.pauseTraining()
    }
}

