package com.example.learnme.activity

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.learnme.data.AppDatabase
import com.example.learnme.databinding.ActivityAudioBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AudioActivity : ComponentActivity() {

    private lateinit var binding: ActivityAudioBinding
    private var mediaPlayer: MediaPlayer? = null // Cambiado a null para manejar la liberación
    private var isPlaying = false
    private var audioUri: Uri? = null
    private var classId: Int = -1
    private val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())

    // Agrega estas variables para manejar la grabación de audio
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val audioFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/audio_${System.currentTimeMillis()}.m4a"
    }

    private val audioPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            // Guardar permiso persistente
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            audioUri = it
            saveAudioUriToDatabase()
            binding.audioStatus.text = "Audio cargado: ${it.lastPathSegment}"
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        classId = intent.getIntExtra("classId", -1)

        binding.uploadAudioButton.setOnClickListener {
            audioPickerLauncher.launch(arrayOf("audio/*"))
        }

        binding.recordAudioButton.setOnClickListener {

        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.playPauseButton.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playAudio()
            }
        }

        loadSavedAudio()
    }

    private fun saveAudioUriToDatabase() {
        audioUri?.let { uri ->
            val audioPath = uri.toString()
            CoroutineScope(Dispatchers.IO).launch {
                database.classDao().updateAudioPath(classId, audioPath)
            }
        }
    }

    private fun playAudio() {
        audioUri?.let { uri ->
            try {
                mediaPlayer?.release()

                mediaPlayer = MediaPlayer().apply {
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        setDataSource(pfd.fileDescriptor)
                    }
                    prepare()
                    start()
                }

                setupSeekBar()
                isPlaying = true
                binding.playPauseButton.text = "Pausa"
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "No se ha cargado un audio", Toast.LENGTH_SHORT).show()
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                binding.playPauseButton.text = "Reproducir"
            }
        }
    }

    private fun setupSeekBar() {
        mediaPlayer?.let { player ->
            binding.seekBar.max = player.duration
            handler.postDelayed(object : Runnable {
                override fun run() {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            binding.seekBar.progress = mp.currentPosition
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
            }, 1000)

            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            player.setOnCompletionListener {
                isPlaying = false
                binding.playPauseButton.text = "Reproducir"
                binding.seekBar.progress = 0
            }
        }
    }

    private fun loadSavedAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            val audioPath = database.classDao().getClassById(classId)?.audioPath
            audioPath?.let {
                audioUri = Uri.parse(it)
                if (isUriAccessible(audioUri!!)) {
                    withContext(Dispatchers.Main) {
                        binding.audioStatus.text = "Audio cargado"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AudioActivity, "El audio guardado no es accesible", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}