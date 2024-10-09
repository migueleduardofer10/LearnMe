package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.example.learnme.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_1)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val acceptButton = findViewById<Button>(R.id.acceptButton)

        // Acción del botón para navegar a StartActivity
        acceptButton.setOnClickListener {
            val name = nameEditText.text.toString()

            // Crear un Intent para lanzar StartActivity
            val intent = Intent(this, WelcomeActivity::class.java)

            // Pasar el nombre a la StartActivity
            intent.putExtra("USER_NAME", name)

            // Iniciar StartActivity
            startActivity(intent)
        }
    }
}