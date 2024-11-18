package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnme.data.AppDatabase
import com.example.learnme.adapter.ItemAdapter
import com.example.learnme.adapter.ItemClass
import com.example.learnme.databinding.ActivityClassSelectionBinding
import com.example.learnme.service.ClassService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ClassSelectionActivity : ComponentActivity(), ItemAdapter.OnItemClickListener {

    private lateinit var binding: ActivityClassSelectionBinding
    private lateinit var itemList: MutableList<ItemClass>
    private lateinit var adapter: ItemAdapter

    private lateinit var classService: ClassService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(this)
        classService = ClassService(database)

        binding = ActivityClassSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar lista de clases en un hilo en segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            val classes = classService.getAllClasses()
            withContext(Dispatchers.Main) {
                itemList = classes.toMutableList()
                setupRecyclerView()
            }
        }

        binding.nextButton.setOnClickListener {
            validateClassesBeforeProceeding()
        }

        binding.newClassButton.setOnClickListener {
            handleAddNewClass()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ItemAdapter(itemList, this)
        binding.recyclerViewItems.adapter = adapter
    }

    private fun handleAddNewClass() {
        CoroutineScope(Dispatchers.IO).launch {
            val newClass = classService.addNewClass()

            withContext(Dispatchers.Main) {
                itemList.add(newClass)
                adapter.notifyItemInserted(itemList.size - 1)
            }
        }
    }

    private fun validateClassesBeforeProceeding() {
        CoroutineScope(Dispatchers.IO).launch {
            // Obtiene las clases que no tienen imágenes
            val incompleteClasses = classService.getClassesWithoutImages()

            withContext(Dispatchers.Main) {
                if (incompleteClasses.isNotEmpty()) {
                    showIncompleteClassesAlert(incompleteClasses)
                } else {
                    // Si todas las clases tienen al menos una foto, procede
                    val intent = Intent(this@ClassSelectionActivity, Step2Activity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun showIncompleteClassesAlert(incompleteClasses: List<ItemClass>) {
        val classNames = incompleteClasses.joinToString("\n") { it.className }
        val message = "Las siguientes clases no cuentan con una imagen:\n\n$classNames"

        val alertDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Clases incompletas")
            .setMessage(message)
            .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
            .create()

        alertDialog.show()
    }

    override fun onCameraClicked(classId: Int) {
        val intent = Intent(this, DataCaptureActivity::class.java)
        intent.putExtra("classId", classId)
        startActivity(intent)
    }

    override fun onUploadClicked(classId: Int) {
        val intent = Intent(this, ImageGalleryActivity::class.java)
        intent.putExtra("classId", classId)
        startActivity(intent)
    }

    override fun onEditClicked(classId: Int) {
        val intent = Intent(this, CaptureResumeActivity::class.java)
        intent.putExtra("classId", classId)
        startActivity(intent)
    }

    override fun onAudioClicked(classId: Int) {
        val intent = Intent(this, AudioActivity::class.java)
        Log.d("AudioActivity", "Valor antes de pasar a  AudioActivity: $classId")
        intent.putExtra("classId", classId)
        startActivity(intent)
    }

    override fun onDeleteClicked(classId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            classService.deleteImagesByClassId(classId)

            classService.deleteClass(classId)

            withContext(Dispatchers.Main) {
                val removedIndex = itemList.indexOfFirst { it.classId == classId }
                if (removedIndex != -1) {
                    itemList.removeAt(removedIndex)
                    adapter.notifyItemRemoved(removedIndex)
                }
            }
        }
    }
}