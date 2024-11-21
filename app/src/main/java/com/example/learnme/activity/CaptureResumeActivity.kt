package com.example.learnme.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import com.example.learnme.data.AppDatabase
import com.example.learnme.R
import com.example.learnme.config.GridConfig
import com.example.learnme.databinding.ActivityCaptureResumeBinding
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem
import com.example.learnme.helper.AudioHelper
import com.example.learnme.helper.GPT4Helper
import com.example.learnme.service.ClassService
import com.example.learnme.service.ImageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class CaptureResumeActivity : ComponentActivity() {

    private lateinit var binding: ActivityCaptureResumeBinding
    private lateinit var adapter: ImageAdapter
    private var classId: Int = -1
    private var imageList: MutableList<ImageItem> = mutableListOf()
    private var isSelectionMode = false
    private var isAutoLabeling = false

    private lateinit var classService: ClassService
    private lateinit var imageService: ImageService

    private lateinit var audioHelper: AudioHelper
    private lateinit var gptHelper: GPT4Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureResumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la base de datos y el DAO
        val database = AppDatabase.getInstance(this)
        classService = ClassService(database)
        imageService = ImageService(database)

        // Obtener el nombre de la clase desde el Intent
        classId = intent.getIntExtra("classId", -1)

        gptHelper = GPT4Helper(OkHttpClient())
        audioHelper = AudioHelper(
            context = this,
            contentResolver = contentResolver,
            audioStatusView = binding.audioStatus,
            playAudioButton = binding.playAudioButton,
            seekBar = binding.seekBar
        )

        // Obtener el nombre de la clase desde la base de datos y mostrarlo en la UI
        loadClassName()
        loadSavedAudio()

        // Configurar el RecyclerView
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        adapter = GridConfig.setupGridWithAdapter(
            recyclerView = binding.recyclerViewImages,
            context = this,
            spanCount = 5,
            spacing = spacing,
            imageList = imageList,
            onItemClick = { imageItem ->
                toggleSelection(imageItem)
                clearEditTextFocus() // Limpiar el foco al interactuar con el RecyclerView
            }
        )

        // Añadir un OnTouchListener al RecyclerView para quitar el foco del EditText
        binding.recyclerViewImages.setOnTouchListener { _, _ ->
            clearEditTextFocus()
            false // Devolvemos false para que el evento de toque sea manejado por el RecyclerView también
        }

        // Cargar imágenes desde la base de datos
        loadImagesForClass { generateLabelIfNeeded() }

        // Añadir listeners a los botones para limpiar el foco del EditText cuando se haga clic en ellos
        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        // Crear un método de orden superior para reducir la duplicación
        val clearFocusAndExecute: (View.OnClickListener) -> View.OnClickListener = { action ->
            View.OnClickListener {
                clearEditTextFocus()
                action.onClick(it)
            }
        }

        binding.cameraButton.setOnClickListener(
            clearFocusAndExecute {
                val intent = Intent(this, DataCaptureActivity::class.java)
                intent.putExtra("classId", classId)
                startActivity(intent)
            }
        )

        binding.uploadButton.setOnClickListener(
            clearFocusAndExecute {
                val intent = Intent(this, ImageGalleryActivity::class.java)
                intent.putExtra("classId", classId)
                startActivity(intent)
            }
        )

        binding.backButton.setOnClickListener(
            clearFocusAndExecute {
                val intent = Intent(this, ClassSelectionActivity::class.java)
                intent.putExtra("classId", classId)
                startActivity(intent)
            }
        )

        binding.hamburgerButton.setOnClickListener(
            clearFocusAndExecute {
                enterSelectionMode()
            }
        )

        binding.deleteButton.setOnClickListener(
            clearFocusAndExecute {
                deleteSelectedImages()
            }
        )

        binding.cancelButton.setOnClickListener(
            clearFocusAndExecute {
                exitSelectionMode()
            }
        )

        binding.playAudioButton.setOnClickListener(
            clearFocusAndExecute {
                if (audioHelper.isPlaying) {
                    audioHelper.pauseAudio()
                } else {
                    audioHelper.playAudio()
                }
            }
        )

        // Botón para habilitar la edición del nombre de la clase
        binding.editButton.setOnClickListener {
            if (!isAutoLabeling) {
                enableClassNameEditing()
            }
        }

        // Listener para saber cuándo el EditText pierde el foco
        binding.nameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newClassName = binding.nameEditText.text.toString().trim()
                if (newClassName.isNotEmpty()) {
                    saveClassName(newClassName)
                }
            }
        }

        // Listener global para toda la vista raíz (cualquier clic fuera del EditText)
        binding.root.setOnClickListener {
            clearEditTextFocus()
        }
    }

    // Método para limpiar el foco del EditText y guardar el nombre de la clase
    private fun clearEditTextFocus() {
        if (binding.nameEditText.hasFocus()) {
            binding.nameEditText.clearFocus()

            // Ocultar el teclado
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.nameEditText.windowToken, 0)

            // Guardar el nombre si no está vacío
            val newClassName = binding.nameEditText.text.toString().trim()
            if (newClassName.isNotEmpty()) {
                saveClassName(newClassName)
            }
        }
    }

    private fun loadSavedAudio() {
        CoroutineScope(Dispatchers.IO).launch {
            val audioPath = classService.getEntityClass(classId).audioPath
            audioPath?.let {
                audioHelper.setAudioUri(Uri.parse(it))
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        audioHelper.release()
    }

    private fun loadClassName() {
        CoroutineScope(Dispatchers.IO).launch {
            val className = classService.getClassName(classId)

            withContext(Dispatchers.Main) {
                binding.nameEditText.setText(className)
                binding.nameEditText.isEnabled = false // Deshabilitar edición inicialmente
            }
        }
    }

    private fun enableClassNameEditing() {
        if (isAutoLabeling) return

        binding.nameEditText.isEnabled = true
        binding.nameEditText.requestFocus()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.nameEditText, InputMethodManager.SHOW_IMPLICIT)

        binding.nameEditText.post {
            binding.nameEditText.selectAll()
        }
    }

    private fun saveClassName(newClassName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                classService.updateClassName(classId, newClassName)
                withContext(Dispatchers.Main) {
                    binding.nameEditText.isEnabled = false
                }
            } catch (e: IllegalStateException) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@CaptureResumeActivity)
                        .setTitle("Error")
                        .setMessage(e.message)
                        .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
            }
        }
    }

    private fun loadImagesForClass(onImagesLoaded: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val tempImageList = imageService.getImagesForClass(classId)

            withContext(Dispatchers.Main) {
                imageList.clear()
                imageList.addAll(tempImageList)
                adapter.notifyDataSetChanged()
                onImagesLoaded()
            }
        }
    }

    private fun generateLabelIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val classEntity = classService.getEntityClass(classId)

            if (!classEntity.isLabelGenerated && imageList.isNotEmpty()) {
                isAutoLabeling = true // Activar bandera de etiquetado automático

                val firstImage = gptHelper.encodeImageToBase64(imageList.first().imagePath)
                gptHelper.sendImageToGPT4(firstImage) { response ->
                    updateClassName(response)
                    isAutoLabeling = false // Desactivar bandera al finalizar
                }
            }
        }
    }

    private fun updateClassName(newName: String) {
        val trimmedName = newName.trim().replace("\"", "")
        CoroutineScope(Dispatchers.Main).launch {
            binding.nameEditText.setText(trimmedName)
            binding.nameEditText.isEnabled = false
        }
        CoroutineScope(Dispatchers.IO).launch {
            classService.updateClassName(classId, trimmedName)
        }
    }

    private fun toggleSelection(imageItem: ImageItem) {
        if (isSelectionMode) {
            imageItem.isSelected = !imageItem.isSelected
            adapter.notifyDataSetChanged()
        }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.isSelectionMode = true
        updateUIForSelectionMode()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter.isSelectionMode = false
        adapter.clearSelection()
        updateUIForNormalMode()
    }

    private fun deleteSelectedImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val selectedImages = imageList.filter { it.isSelected }
            val imagePaths = selectedImages.map { it.imagePath }

            imageService.deleteImagesByPaths(imagePaths)
            withContext(Dispatchers.Main) {
                imageList.removeAll(selectedImages)
                adapter.notifyDataSetChanged()
                exitSelectionMode()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForSelectionMode() {
        binding.fileCountTextView.text = "Seleccionar"
        binding.hamburgerButton.visibility = View.GONE
        binding.deleteButton.visibility = View.VISIBLE
        binding.cancelButton.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForNormalMode() {
        binding.fileCountTextView.text = "Imágenes capturadas"
        binding.hamburgerButton.visibility = View.VISIBLE
        binding.deleteButton.visibility = View.GONE
        binding.cancelButton.visibility = View.GONE
    }
}