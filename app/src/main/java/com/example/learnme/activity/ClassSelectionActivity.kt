package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.fragments.ItemAdapter
import com.example.learnme.fragments.ItemClass
import com.example.learnme.R


class ClassSelectionActivity : ComponentActivity(), ItemAdapter.OnItemClickListener{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_selection)

        // Lista de datos de ejemplo
        val itemList = listOf(
            ItemClass("Clase 1", "Descripción de clase 1"),
            ItemClass("Clase 2", "Descripción de clase 2"),
            ItemClass("Clase 1", "Descripción de clase 1"),
            ItemClass("Clase 2", "Descripción de clase 2"),
            ItemClass("Clase 1", "Descripción de clase 1"),
            ItemClass("Clase 2", "Descripción de clase 2"),
            ItemClass("Clase 3", "Descripción de clase 3")

        )

        // Configurar el RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ItemAdapter(itemList, this)

        val nextButton = findViewById<Button>(R.id.nextButton)

        nextButton.setOnClickListener {
            val intent = Intent(this, Step2Activity::class.java)
            startActivity(intent)
        }

        // Botón para agregar una nueva clase
        // val newClassButton = findViewById<Button>(R.id.newClassButton)

    }

    // Manejar clics en los botones
    override fun onBackClicked(position: Int) {
        // Acción para el botón "Volver"
    }

    override fun onUploadClicked(position: Int) {
        // Acción para el botón "Subir"
    }

    override fun onEditClicked(position: Int) {
        // Acción para el botón "Editar"
    }
}