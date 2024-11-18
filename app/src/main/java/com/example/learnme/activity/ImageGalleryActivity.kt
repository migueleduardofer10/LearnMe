package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ImageEntity
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityImageGaleryBinding
import com.example.learnme.helper.GalleryPermissionsManager
import com.example.learnme.service.GalleryService
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem
import com.example.learnme.service.ImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ImageGalleryActivity : ComponentActivity() {

    private lateinit var binding: ActivityImageGaleryBinding
    private lateinit var galleryService: GalleryService
    private lateinit var imageService: ImageService
    private lateinit var imageList: MutableList<ImageItem>
    private lateinit var adapter: ImageAdapter
    private var classId: Int = -1  // Valor por defecto en caso de que no se reciba

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageGaleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtén el classId del Intent
        classId = intent.getIntExtra("classId", -1)  // -1 es un valor por defecto

        val database = AppDatabase.getInstance(this) // Inicializa la base de datos
        galleryService = GalleryService(this)
        imageService = ImageService(database)

        val permissionsManager = GalleryPermissionsManager(this) {
            imageList = galleryService.loadImagesFromGallery()
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
            handleSaveSelectedImages()
        }


    }

    // Lógica para seleccionar imágenes
    private fun handleImageSelection(imageItem: ImageItem) {
        imageItem.isSelected = !imageItem.isSelected  // Cambiar el estado de selección
        adapter.notifyDataSetChanged()
    }

    // Guardar imágenes seleccionadas en la base de datos
    private fun handleSaveSelectedImages() {
        val selectedImages = imageList.filter { it.isSelected }

        CoroutineScope(Dispatchers.IO).launch {
            imageService.saveImages(selectedImages, classId)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ImageGalleryActivity, "Imágenes seleccionadas guardadas", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ImageGalleryActivity, CaptureResumeActivity::class.java)
                intent.putExtra("classId", classId)
                startActivity(intent)
            }
        }
    }
}
