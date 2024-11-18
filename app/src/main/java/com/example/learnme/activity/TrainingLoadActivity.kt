package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassEntity
import com.example.learnme.databinding.ActivityTrainingLoadBinding
import com.example.learnme.helper.TransferLearningHelper
import com.example.learnme.helper.TransferLearningManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.label.Category
import java.util.Locale

class TrainingLoadActivity : ComponentActivity(), TransferLearningHelper.ClassifierListener {

    private lateinit var binding: ActivityTrainingLoadBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrainingLoadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos
        database = AppDatabase.getInstance(this)

        // Iniciar configuración y entrenamiento de forma asincrónica
        CoroutineScope(Dispatchers.Main).launch {
            val numberOfClasses = getNumberOfClasses()
            Log.d("TrainingLoadActivity", "Número de clases: ${numberOfClasses.size}")

            // Inicializar TransferLearning en el Singleton
            TransferLearningManager.initialize(
                this@TrainingLoadActivity,
                numberOfClasses,
                this@TrainingLoadActivity
            )

            // Iniciar entrenamiento
            TransferLearningManager.startTraining()
        }

        binding.stopButton.setOnClickListener {
            TransferLearningManager.pauseTraining()
            startActivity(Intent(this, Step3Activity::class.java))
        }
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