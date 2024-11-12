package com.example.learnme.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnme.data.AppDatabase
import com.example.learnme.data.ClassDao
import com.example.learnme.data.ClassEntity
import com.example.learnme.adapter.ItemAdapter
import com.example.learnme.adapter.ItemClass
import com.example.learnme.databinding.ActivityClassSelectionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ClassSelectionActivity : ComponentActivity(), ItemAdapter.OnItemClickListener{

    private lateinit var binding: ActivityClassSelectionBinding

    private lateinit var itemList: MutableList<ItemClass>
    private lateinit var adapter: ItemAdapter

    private lateinit var database: AppDatabase
    private lateinit var classDao: ClassDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getInstance(this)
        classDao = database.classDao()

        binding = ActivityClassSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar lista de clases en un hilo en segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            val classes = classDao.getAllClasses().map { classEntity ->
                val sampleCount = database.imageDao().getImageCountForClass(classEntity.classId)
                ItemClass(classEntity.className, classEntity.classId, sampleCount)
            }
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
            addNewClass()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ItemAdapter(itemList, this)
        binding.recyclerViewItems.adapter = adapter
    }

    // Agregar nueva clase de forma asíncrona
    private fun addNewClass() {
        CoroutineScope(Dispatchers.IO).launch {
            val newClass = ClassEntity(className = "Clase ${itemList.size + 1}")
            val classId = classDao.insertClass(newClass).toInt()

            withContext(Dispatchers.Main) {
                itemList.add(ItemClass("Clase $classId", classId, 0))
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