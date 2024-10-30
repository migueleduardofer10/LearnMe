package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem
import java.io.File

class CaptureResumeActivity : ComponentActivity(){

    private lateinit var binding: ActivityCaptureResumeBinding
    private lateinit var adapter: ImageAdapter
    private lateinit var classPosition: String
    private var classId: Int = -1
    private var imageList: MutableList<ImageItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el nombre de la clase desde el Intent
        classPosition = intent.getStringExtra("class") ?: "Clase"
        classId = intent.getIntExtra("classId", -1)

        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        adapter = GridConfig.setupGridWithAdapter(
            recyclerView = binding.recyclerViewImages,
            context = this,
            spanCount = 5,
            spacing = spacing,
            imageList = imageList,
            onItemClick = { imageItem ->
                // Implementar lógica de eliminación o cualquier otra acción necesaria
            }
        )

        // Configurar el título de la actividad
        binding.nameEditText.text = classPosition

        // Cargar imágenes asociadas al classId
        loadImagesForClass()


        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, DataCaptureActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }

        /*
        binding.uploadButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
        }
        */

        binding.backButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            intent.putExtra("classId", classId)
            startActivity(intent)
        }
    }

    // Método para cargar las imágenes de SharedPreferences que coincidan con el classId
    private fun loadImagesForClass() {
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val imagePaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf())

        val tempImageList = mutableListOf<ImageItem>()

        imagePaths?.forEach { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                val path = parts[0]
                val savedClassId = parts[1].toIntOrNull() ?: -1
                if (savedClassId == classId) { // Filtrar por classId
                    tempImageList.add(ImageItem(path, savedClassId))
                }
            }
        }

        // Ordenar la lista de imágenes por el timestamp en el nombre del archivo
        imageList.clear()
        imageList.addAll(
            tempImageList.sortedBy { imageItem ->
                File(imageItem.imagePath).nameWithoutExtension.toLongOrNull() ?: 0L
            }
        )

        // Notificar al adaptador que los datos han cambiado
        adapter.notifyDataSetChanged()
    }

}