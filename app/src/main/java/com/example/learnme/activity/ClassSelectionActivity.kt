package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import android.app.AlertDialog
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
    private var itemList: MutableList<ItemClass> = emptyList<ItemClass>().toMutableList()
    private var adapter: ItemAdapter = ItemAdapter(itemList, this)

    private lateinit var classService: ClassService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(this)
        classService = ClassService(database)

        binding = ActivityClassSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar clases por defecto y cargar lista de clases
        CoroutineScope(Dispatchers.IO).launch {
            classService.initializeDefaultClasses() // Esto se ejecuta en un hilo secundario

            // Obtener la lista de clases después de inicializar
            val classes = classService.getAllClasses()
            withContext(Dispatchers.Main) {
                itemList = classes.toMutableList()
                setupRecyclerView() // Configurar RecyclerView en el hilo principal
            }
        }
        binding.nextButton.setOnClickListener {
            validateClassCountBeforeProceeding()
        }

        binding.newClassButton.setOnClickListener {
            handleAddNewClass()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshClasses()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ItemAdapter(itemList, this)
        binding.recyclerViewItems.adapter = adapter
        refreshClasses()
    }

    //Carga la lista de clases actualizada
    private fun refreshClasses() {
        CoroutineScope(Dispatchers.IO).launch {
            val classes = classService.getAllClasses()
            withContext(Dispatchers.Main) {
                itemList.clear()
                itemList.addAll(classes)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleAddNewClass() {
        CoroutineScope(Dispatchers.IO).launch {
            if (itemList.size >= 4) {
                withContext(Dispatchers.Main) {
                    //Validación de clases
                    AlertDialog.Builder(this@ClassSelectionActivity)
                        .setTitle("Límite alcanzado")
                        .setMessage("Solo puedes tener un máximo de 4 clases.")
                        .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
                return@launch
            } else {
                try {
                    classService.addNewClass()
                    refreshClasses()
                } catch (e: IllegalStateException) {
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@ClassSelectionActivity)
                            .setTitle("Error")
                            .setMessage(e.message)
                            .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
                            .create()
                            .show()
                    }
                }
            }
        }
    }

    private fun validateClassCountBeforeProceeding() {
        CoroutineScope(Dispatchers.IO).launch {
            if (itemList.size != 4) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@ClassSelectionActivity)
                        .setTitle("Clases incompletas")
                        .setMessage("Debes tener exactamente 4 clases para continuar.")
                        .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
                return@launch
            }
            //Validar que las clases tengan imágenes
            val incompleteClasses = classService.getClassesWithoutImages()
            withContext(Dispatchers.Main) {
                if (incompleteClasses.isNotEmpty()) {
                    showIncompleteClassesAlert(incompleteClasses)
                } else {
                    val intent = Intent(this@ClassSelectionActivity, Step2Activity::class.java)
                    startActivity(intent)
                }
            }
        }
    }


    private fun showIncompleteClassesAlert(incompleteClasses: List<ItemClass>) {
        val classNames = incompleteClasses.joinToString("\n") { it.className }
        val message = "Las siguientes clases no cuentan con una imagen:\n\n$classNames"

        val alertDialog = AlertDialog.Builder(this)
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
            CoroutineScope(Dispatchers.IO).launch {
                classService.deleteImagesByClassId(classId)
                classService.deleteClass(classId)

                withContext(Dispatchers.Main) {
                    refreshClasses()
                }
            }
        }
    }


}