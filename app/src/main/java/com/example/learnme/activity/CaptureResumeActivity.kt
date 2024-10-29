package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnme.R
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.fragments.GridSpacingItemDecoration
import com.example.learnme.fragments.ImageAdapter
import com.example.learnme.fragments.ImageItem

class CaptureResumeActivity : ComponentActivity() {

    private lateinit var binding: ActivityCaptureResumeBinding
    private lateinit var imageAdapter: ImageAdapter
    private var classPosition: String = "Clase"
    private var classId: Int = -1 // Inicializa classId con un valor por defecto
    private var imageList: MutableList<ImageItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el nombre de la clase y el classId desde el Intent
        classPosition = intent.getStringExtra("class") ?: "Clase"
        classId = intent.getIntExtra("classId", -1)

        // Configurar el título de la actividad
        binding.nameEditText.text = classPosition

        // Configurar el RecyclerView como una cuadrícula
        imageAdapter = ImageAdapter(imageList) { imageItem ->
            // Implementar lógica de clic en la imagen, si es necesario
        }

        val spanCount = 3 // Número de columnas en la cuadrícula
        binding.recyclerViewImages.layoutManager = GridLayoutManager(this, spanCount)

        // Agregar espaciado entre los elementos de la cuadrícula
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        binding.recyclerViewImages.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing))

        binding.recyclerViewImages.adapter = imageAdapter

        // Cargar imágenes asociadas al classId
        loadImagesForClass()

        // Configurar botones
        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, DataCaptureActivity::class.java)
            intent.putExtra("classId", classId) // Pasar classId a DataCaptureActivity
            startActivity(intent)
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    // Método para cargar las imágenes de SharedPreferences que coincidan con el classId
    private fun loadImagesForClass() {
        val sharedPreferences = getSharedPreferences("CapturedImages", MODE_PRIVATE)
        val imagePaths = sharedPreferences.getStringSet("imagePaths", mutableSetOf())

        imagePaths?.forEach { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                val path = parts[0]
                val savedClassId = parts[1].toIntOrNull() ?: -1
                if (savedClassId == classId) { // Filtrar por classId
                    imageList.add(ImageItem(path, savedClassId))
                }
            }
        }

        // Notificar al adaptador que los datos han cambiado
        imageAdapter.notifyDataSetChanged()
    }
}
