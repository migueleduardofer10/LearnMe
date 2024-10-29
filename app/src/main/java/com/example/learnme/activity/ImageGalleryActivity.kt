package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityImageGaleryBinding
import com.example.learnme.fragments.GalleryPermissionsManager
import com.example.learnme.fragments.GalleryHelper
import com.example.learnme.fragments.ImageItem


class ImageGalleryActivity : ComponentActivity() {

    private lateinit var binding: ActivityImageGaleryBinding

    private lateinit var galleryHelper: GalleryHelper
    private lateinit var imageList: MutableList<ImageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageGaleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        galleryHelper = GalleryHelper(this)

        val permissionsManager = GalleryPermissionsManager(this) {
            imageList = galleryHelper.loadImagesFromGallery()
            val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
            GridConfig.setupGridWithAdapter(
                recyclerView = binding.recyclerViewImages,
                context = this,
                spanCount = 5,  // Número de columnas
                spacing = spacing,
                imageList = imageList,
                onItemClick = { imageItem ->
                    handleImageSelection(imageItem) // Lógica para seleccionar imágenes
                }
            )
        }

        permissionsManager.checkAndRequestPermission()

        binding.backButton.setOnClickListener {
            finish()
            val intent = Intent(this, CaptureResumeActivity::class.java)
            startActivity(intent)
        }


    }

    // Lógica para seleccionar imágenes
    private fun handleImageSelection(imageItem: ImageItem) {
        imageItem.isSelected = !imageItem.isSelected  // Cambiar el estado de selección
    }
}
