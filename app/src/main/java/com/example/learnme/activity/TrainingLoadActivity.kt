package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.learnme.R

class TrainingLoadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_training_load)

        val stopButton = findViewById<Button>(R.id.stopButton)

        stopButton.setOnClickListener {
            val intent = Intent(this, Step3Activity::class.java)
            startActivity(intent)
        }

    }
}