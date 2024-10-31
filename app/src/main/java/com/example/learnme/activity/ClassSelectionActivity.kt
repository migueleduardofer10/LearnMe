package com.example.learnme.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnme.fragments.ItemAdapter
import com.example.learnme.fragments.ItemClass
import com.example.learnme.databinding.ActivityClassSelectionBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class ClassSelectionActivity : ComponentActivity(), ItemAdapter.OnItemClickListener{

    private lateinit var binding: ActivityClassSelectionBinding

    private lateinit var itemList: MutableList<ItemClass>
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClassSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar lista de clases desde SharedPreferences o establecer las clases por defecto
        itemList = loadClassesFromPreferences()

        // Configurar el RecyclerView
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ItemAdapter(itemList, this)
        binding.recyclerViewItems.adapter = adapter

        binding.nextButton.setOnClickListener {
            val intent = Intent(this, Step2Activity::class.java)
            startActivity(intent)
        }

        binding.newClassButton.setOnClickListener {
            addNewClass()
        }
    }

    // Función para agregar una nueva clase
    private fun addNewClass() {
        // Crear un nuevo ItemClass con una descripción dinámica
        val newClass = ItemClass("Clase ${itemList.size + 1}")
        // Agregar la nueva clase a la lista
        itemList.add(newClass)

        // Notificar al adaptador que la lista ha cambiado y guardar en SharedPreferences
        adapter.notifyItemInserted(itemList.size - 1)
        saveClassesToPreferences()
    }

    // Función para guardar la lista de clases en SharedPreferences
    private fun saveClassesToPreferences() {
        val sharedPreferences = getSharedPreferences("ClassPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(itemList)
        editor.putString("classList", json)
        editor.apply()
    }

    // Función para cargar la lista de clases desde SharedPreferences
    private fun loadClassesFromPreferences(): MutableList<ItemClass> {
        val sharedPreferences = getSharedPreferences("ClassPreferences", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("classList", null)
        val type = object : TypeToken<MutableList<ItemClass>>() {}.type

        // Si hay clases guardadas, cargar; si no, inicializar con clases por defecto
        return if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf(ItemClass("Clase 1"), ItemClass("Clase 2"))
        }
    }

    override fun onCameraClicked(position: Int) {
        val selectedClass = itemList[position]
        val intent = Intent(this, DataCaptureActivity::class.java)
        intent.putExtra("class", selectedClass.title)
        intent.putExtra("classId", position)
        startActivity(intent)
    }
    override fun onUploadClicked(position: Int) {
        val selectedClass = itemList[position]
        val intent = Intent(this, ImageGalleryActivity::class.java)
        intent.putExtra("class", selectedClass.title)
        intent.putExtra("classId", position)
        startActivity(intent)
    }

    override fun onEditClicked(position: Int) {
        val intent = Intent(this, CaptureResumeActivity::class.java)
        intent.putExtra("classId", position)
        startActivity(intent)
    }
}