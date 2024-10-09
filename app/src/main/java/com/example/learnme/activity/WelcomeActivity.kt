package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.learnme.R

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)  // Aquí vinculamos activity_start.xml a StartActivity


        //Debemos borrar este intent
        val name = intent.getStringExtra("USER_NAME")

        val welcomeTextView = findViewById<TextView>(R.id.welcomeTextView)

        if (!name.isNullOrEmpty()) {
            welcomeTextView.text = "Hola, $name"
        } else {
            welcomeTextView.text = "Hola!"
        }

        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {
            val intent = Intent(this, Step1Activity::class.java)
            startActivity(intent)
        }
    }
}