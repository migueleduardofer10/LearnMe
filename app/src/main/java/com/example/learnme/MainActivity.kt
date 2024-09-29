package com.example.learnme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.KeyEventDispatcher.Component
import com.example.learnme.ui.theme.LearnMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val acceptButton = findViewById<Button>(R.id.acceptButton)

        // Acción del botón para navegar a StartActivity
        acceptButton.setOnClickListener {
            val name = nameEditText.text.toString()

            // Crear un Intent para lanzar StartActivity
            val intent = Intent(this, StartActivity::class.java)

            // Pasar el nombre a la StartActivity
            intent.putExtra("USER_NAME", name)

            // Iniciar StartActivity
            startActivity(intent)
        }
    }
}