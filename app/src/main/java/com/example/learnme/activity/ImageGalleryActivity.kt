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
import com.example.learnme.fragment.GalleryPermissionsManager
import com.example.learnme.fragment.GalleryHelper
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ImageGalleryActivity : ComponentActivity() {

    private lateinit var binding: ActivityImageGaleryBinding
    private lateinit var galleryHelper: GalleryHelper
    private lateinit var imageList: MutableList<ImageItem>
    private lateinit var adapter: ImageAdapter
    private var classId: Int = -1  // Valor por defecto en caso de que no se reciba
    private lateinit var database: AppDatabase // Base de datos para almacenar las imágenes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageGaleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtén el classId del Intent
        classId = intent.getIntExtra("classId", -1)  // -1 es un valor por defecto

        galleryHelper = GalleryHelper(this)
        database = AppDatabase.getInstance(this) // Inicializa la base de datos

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

    // Guardar imágenes seleccionadas en la base de datos
    private fun saveSelectedImages() {
        val selectedImages = imageList.filter { it.isSelected }

        // Ejecutar la inserción en un hilo de fondo
        CoroutineScope(Dispatchers.IO).launch {
            selectedImages.forEach { imageItem ->
                val imageEntity = ImageEntity(
                    imagePath = imageItem.imagePath,
                    classId = classId
                )
                database.imageDao().insertImage(imageEntity)  // Inserta la imagen en la base de datos
            }
        }
    }
}
