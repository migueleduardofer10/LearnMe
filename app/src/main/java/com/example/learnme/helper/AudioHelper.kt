package com.example.learnme.helper

import android.content.ContentResolver
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.io.IOException

class AudioHelper(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val audioStatusView: TextView,
    private val playAudioButton: Button,
    private val seekBar: SeekBar,
) {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying = false
    private var audioUri: Uri? = null
    private val handler = Handler(Looper.getMainLooper())

    fun setAudioUri(uri: Uri) {
        audioUri = uri
        audioStatusView.text = if (isUriAccessible(uri)) "Audio cargado" else "Audio no accesible"
        if (!isUriAccessible(uri)) {
            Toast.makeText(context, "Error al cargar el audio", Toast.LENGTH_SHORT).show()
        }
    }

    fun playAudio() {
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
                playAudioButton.text = "Pausa"
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(context, "No se ha cargado un audio", Toast.LENGTH_SHORT).show()
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                playAudioButton.text = "Reproducir"
            }
        }
    }

    private fun setupSeekBar() {
        mediaPlayer?.let { player ->
            seekBar.max = player.duration
            handler.postDelayed(object : Runnable {
                override fun run() {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            seekBar.progress = mp.currentPosition
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
            }, 1000)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) mediaPlayer?.seekTo(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            player.setOnCompletionListener {
                isPlaying = false
                playAudioButton.text = "Reproducir"
                seekBar.progress = 0
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

    fun release() {
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
