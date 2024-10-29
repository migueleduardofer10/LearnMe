package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem

class CaptureResumeActivity : ComponentActivity(){

    private lateinit var binding: ActivityCaptureResumeBinding
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var classPosition: String
    private var imageList: MutableList<ImageItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el nombre de la clase desde el Intent
        classPosition = intent.getStringExtra("class") ?: "Clase"

        // Configurar el título de la actividad
        binding.nameEditText.text = classPosition


        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, DataCaptureActivity::class.java)
            startActivity(intent)
        }

        /*
        binding.uploadButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
        }
        */

        binding.backButton.setOnClickListener {
            finish()
        }
    }

}