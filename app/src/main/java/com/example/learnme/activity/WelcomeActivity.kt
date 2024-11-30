package com.example.learnme.activity

import ActivityTimeTracker
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.learnme.R

class WelcomeActivity : ComponentActivity() {
    private val activityTimeTracker = ActivityTimeTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)  // Aquí vinculamos activity_start.xml a StartActivity

        val startButton = findViewById<Button>(R.id.startButton)

        activityTimeTracker.startActivity()

        startButton.setOnClickListener {
            val intent = Intent(this, Step1Activity::class.java)
            startActivity(intent)
        }
    }
}