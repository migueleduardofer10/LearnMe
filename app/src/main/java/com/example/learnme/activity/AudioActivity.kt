package com.example.learnme.activity

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.learnme.data.AppDatabase
import com.example.learnme.databinding.ActivityAudioBinding
import com.example.learnme.fragment.AudioHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class AudioActivity : ComponentActivity() {

    private lateinit var audioHelper: AudioHelper

    private lateinit var binding: ActivityAudioBinding
    private var audioUri: Uri? = null
    private var classId: Int = -1
    private val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val audioFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/audio_${System.currentTimeMillis()}.m4a"
    }

    private val audioPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
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

        audioHelper = AudioHelper(
            context = this,
            contentResolver = contentResolver,
            audioStatusView = binding.audioStatus,
            playAudioButton = binding.playPauseButton,
            seekBar = binding.seekBar
        )

        binding.previousButton.setOnClickListener {
            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.uploadAudioButton.setOnClickListener {
            audioPickerLauncher.launch(arrayOf("audio/*"))
        }

        binding.recordAudioButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                if (checkPermissions()) {
                    startRecording()
                }
            }
        }

        binding.previousButton.setOnClickListener {
            finish()
        }

        binding.playPauseButton.setOnClickListener {
            if (audioHelper.isPlaying) {
                audioHelper.pauseAudio()
            } else {
                audioHelper.playAudio()
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

    private fun loadSavedAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            val audioPath = database.classDao().getClassById(classId)?.audioPath
            audioPath?.let {
                audioUri = Uri.parse(it)
                withContext(Dispatchers.Main) {
                    audioHelper.setAudioUri(audioUri!!)
                }
            }
        }
    }

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecording = true
                binding.recordAudioButton.text = "Detener Grabación"
                Toast.makeText(this@AudioActivity, "Grabación iniciada", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@AudioActivity, "Error al iniciar la grabación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        binding.recordAudioButton.text = "Grabar Audio"
        Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show()

        // Usar FileProvider para obtener el URI correcto
        val audioFile = File(audioFilePath)
        audioUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            audioFile
        )

        saveAudioUriToDatabase()
        binding.audioStatus.text = "Audio grabado y guardado"
    }

    private fun checkPermissions(): Boolean {
        val recordPermission = android.Manifest.permission.RECORD_AUDIO
        val hasRecordPermission = ContextCompat.checkSelfPermission(this, recordPermission) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordPermission) {
            ActivityCompat.requestPermissions(this, arrayOf(recordPermission), 200)
        }
        return hasRecordPermission
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHelper.release()
    }
}
