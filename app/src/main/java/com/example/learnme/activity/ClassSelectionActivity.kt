package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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


class ClassSelectionActivity : ComponentActivity(), ItemAdapter.OnItemClickListener{

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
            val intent = Intent(this, Step2Activity::class.java)
            startActivity(intent)
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

    // Agregar nueva clase de forma asíncrona
    private fun handleAddNewClass() {
        CoroutineScope(Dispatchers.IO).launch {
            val newClass = classService.addNewClass(itemList)

            withContext(Dispatchers.Main) {
                itemList.add(newClass)
                adapter.notifyItemInserted(itemList.size - 1)
            }
        }
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
        intent.putExtra("classId", classId)
        startActivity(intent)
    }
}