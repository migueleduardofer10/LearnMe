package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.learnme.R

class Step1Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_step_1)

        val nextButton = findViewById<Button>(R.id.nextButton)

        nextButton.setOnClickListener {

            val intent = Intent(this, Step2Activity::class.java)
            startActivity(intent)
        }
    }
}