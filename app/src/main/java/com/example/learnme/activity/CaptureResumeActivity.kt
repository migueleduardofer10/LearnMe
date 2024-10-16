package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R
import com.example.learnme.fragments.ImageItem

//Creo que se debe definir como Clase recursiva

class CaptureResumeActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_resume)

        // Find RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewImages)

        // Replace with actual image loading
        val imageList = listOf(
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
        )

        // Configurar el grid con 3 columnas, espaciado y pasar la lista de imágenes
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        GridConfig.setupGridWithAdapter(recyclerView, this, spanCount = 5, spacing = spacing, imageList = imageList)


        val backButton = findViewById<Button>(R.id.backButton)
        // val uploadButton = findViewById<Button>(R.id.uploadButton)
        val cameraButton = findViewById<Button>(R.id.cameraButton)

        cameraButton.setOnClickListener {
            val intent = Intent(this, DataCaptureActivity::class.java)
            startActivity(intent)
        }

        /*
        uploadButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
        }
        */

        backButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
        }
    }
}