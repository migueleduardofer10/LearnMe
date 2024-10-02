package com.example.learnme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class DataCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_data_capture)

        val nextButton = findViewById<Button>(R.id.nextButton)

        nextButton.setOnClickListener {

            val intent = Intent(this, com.example.learnme.MyCameraCaptureActivity.CameraCaptureActivity::class.java)
            startActivity(intent)
        }
    }
}
