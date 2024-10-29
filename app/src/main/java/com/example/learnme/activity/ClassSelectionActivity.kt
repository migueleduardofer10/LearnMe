package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnme.fragments.ItemAdapter
import com.example.learnme.fragments.ItemClass
import com.example.learnme.databinding.ActivityClassSelectionBinding


class ClassSelectionActivity : ComponentActivity(), ItemAdapter.OnItemClickListener{

    private lateinit var binding: ActivityClassSelectionBinding

    private lateinit var itemList: MutableList<ItemClass>
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClassSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lista de datos de ejemplo
        itemList = mutableListOf(
            ItemClass("Clase 1"),
            ItemClass("Clase 2")
        )

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

        // Notificar al adaptador que el dataset ha cambiado
        adapter.notifyItemInserted(itemList.size - 1)
    }

    override fun onBackClicked(position: Int) {
        // Implementación temporal para mostrar un mensaje
        val className = itemList[position].title
        Toast.makeText(this, "Botón de volver para $className", Toast.LENGTH_SHORT).show()
    }

    override fun onUploadClicked(position: Int) {
        // Implementación temporal para mostrar un mensaje
        val className = itemList[position].title
        Toast.makeText(this, "Botón de cargar para $className", Toast.LENGTH_SHORT).show()
    }

    override fun onEditClicked(position: Int) {
        val selectedClass = itemList[position]
        val intent = Intent(this, DataCaptureActivity::class.java)
        intent.putExtra("classId", position) // Envía el índice como classId
        startActivity(intent)
    }

}