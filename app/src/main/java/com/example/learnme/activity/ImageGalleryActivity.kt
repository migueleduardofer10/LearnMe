package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityImageGaleryBinding
import com.example.learnme.fragments.GalleryPermissionsManager
import com.example.learnme.fragments.GalleryHelper
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem


class ImageGalleryActivity : ComponentActivity() {

    private lateinit var binding: ActivityImageGaleryBinding

    private lateinit var galleryHelper: GalleryHelper
    private lateinit var imageList: MutableList<ImageItem>
    private lateinit var adapter: ImageAdapter
    private var classId: Int = -1  // Valor por defecto en caso de que no se reciba

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageGaleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtén el classId del Intent
        classId = intent.getIntExtra("classId", -1)  // -1 es un valor por defecto

        galleryHelper = GalleryHelper(this)

        val permissionsManager = GalleryPermissionsManager(this) {
            imageList = galleryHelper.loadImagesFromGallery()
            val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
            adapter = GridConfig.setupGridWithAdapter(
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
            val intent = Intent(this, CaptureResumeActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }

        binding.checkButton.setOnClickListener {
            saveSelectedImages()
            Toast.makeText(this, "Imágenes seleccionadas guardadas", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, CaptureResumeActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }


    }

    // Lógica para seleccionar imágenes
    private fun handleImageSelection(imageItem: ImageItem) {
        imageItem.isSelected = !imageItem.isSelected  // Cambiar el estado de selección
        adapter.notifyDataSetChanged()
    }

    // Guardar imágenes seleccionadas en SharedPreferences
    private fun saveSelectedImages() {
        val selectedImages = imageList.filter { it.isSelected }
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val existingPaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf()) ?: mutableSetOf()

        // Agregar las imágenes seleccionadas con el formato "ruta|classId"
        selectedImages.forEach { imageItem ->
            existingPaths.add("${imageItem.imagePath}|$classId")
        }

        editor.putStringSet("imagePaths", existingPaths)
        editor.apply()
    }
}
