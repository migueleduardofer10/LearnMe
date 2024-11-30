package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassEntity
import com.example.learnme.databinding.ActivityTrainingLoadBinding
import com.example.learnme.helper.TransferLearningHelper
import com.example.learnme.helper.TransferLearningManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.label.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingLoadActivity : ComponentActivity(), TransferLearningHelper.ClassifierListener {

    private lateinit var binding: ActivityTrainingLoadBinding
    private lateinit var database: AppDatabase
    private var trainingStartTime: Long = 0
    private var trainingEndTime: Long = 0
    private var totalImagesProcessedTraining = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrainingLoadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos
        database = AppDatabase.getInstance(this)

        // Iniciar configuración y entrenamiento de forma asincrónica
        CoroutineScope(Dispatchers.Main).launch {
            val numberOfClasses = getNumberOfClasses()
            Log.d("TrainingLoadActivity", "Número de clases evaluadas: ${numberOfClasses.size}")

            // Inicializar TransferLearning en el Singleton
            TransferLearningManager.initialize(
                this@TrainingLoadActivity,
                numberOfClasses,
                this@TrainingLoadActivity
            )

            // Registrar tiempo de inicio
            trainingStartTime = System.currentTimeMillis()
            val startFormattedTime = formatTime(trainingStartTime)
            Log.d("TiempoEntrenamiento", "Inicio del entrenamiento: $startFormattedTime")

            // Iniciar entrenamiento
            TransferLearningManager.startTraining()

            processImageForTraining()

            delay(5000)
            binding.stopButton.visibility = View.VISIBLE
        }

        binding.stopButton.setOnClickListener {
            // Registrar tiempo de fin cuando se pause el entrenamiento
            trainingEndTime = System.currentTimeMillis()
            val endFormattedTime = formatTime(trainingEndTime)
            Log.d("TiempoEntrenamiento", "Fin del entrenamiento: $endFormattedTime")

            // Calcular la duración
            val duration = trainingEndTime - trainingStartTime
            val formattedDuration = formatDuration(duration)
            Log.d("TiempoEntrenamiento", "Duración total del entrenamiento: $formattedDuration")

            // Log del número de imágenes procesadas durante el entrenamiento
            Log.d("TrainingLoadActivity", "Total de imágenes procesadas (Entrenamiento): $totalImagesProcessedTraining")

            TransferLearningManager.pauseTraining()
            startActivity(Intent(this, Step3Activity::class.java))
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timeInMillis))
    }

    private fun formatDuration(durationInMillis: Long): String {
        val minutes = (durationInMillis / 1000) / 60
        val seconds = (durationInMillis / 1000) % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun processImageForTraining() {
        totalImagesProcessedTraining++
        Log.d("TrainingLoadActivity", "Imagen procesada en entrenamiento: $totalImagesProcessedTraining")
    }

    private suspend fun getNumberOfClasses(): List<ClassEntity> = withContext(Dispatchers.IO) {
        // Obtener el número total de clases en la base de datos
        database.classDao().getAllClasses().toList()
    }

    override fun onError(error: String) {
        //Para el entrenamiento solo se muestra un error
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(results: List<Category>?, inferenceTime: Long) {
        TODO("Not yet implemented")
    }

    override fun onLossResults(lossNumber: Float) {
        String.format(
            Locale.US,
            "Loss: %.3f", lossNumber
        ).let {
            binding.tvLossConsumer.text = it
        }
    }
}