package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R
import com.example.learnme.fragment.ImageItem


class ImageGaleryActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_galery)

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

        backButton.setOnClickListener {
            val intent = Intent(this, CaptureResumeActivity::class.java)
            startActivity(intent)
        }
    }
}