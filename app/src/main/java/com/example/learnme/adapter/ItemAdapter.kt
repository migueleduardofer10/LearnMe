package com.example.learnme.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R

// Modelo de datos
data class ItemClass(val className: String, val classId: Int, var sampleCount: Int = 0  )

class ItemAdapter(
    private val itemList: List<ItemClass>,
    private val itemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    // Interfaz para manejar los clics en los botones
    interface OnItemClickListener {
        fun onCameraClicked(classId: Int)
        fun onUploadClicked(classId: Int)
        fun onEditClicked(classId: Int)
        fun onAudioClicked(classId: Int)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val className: TextView = itemView.findViewById(R.id.classNameText)
        val imgCount: TextView = itemView.findViewById(R.id.imgCountText)
        val cameraButton: ImageButton = itemView.findViewById(R.id.cameraButton)
        val uploadButton: ImageButton = itemView.findViewById(R.id.uploadButton)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val audioButton: ImageButton = itemView.findViewById(R.id.audioButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = itemList[position]

        // Asignar los datos a las vistas
        holder.className.text = currentItem.className
        holder.imgCount.text = "Muestras: ${currentItem.sampleCount}"

        // Asignar listeners a los botones
        holder.cameraButton.setOnClickListener {
            itemClickListener.onCameraClicked(currentItem.classId)
        }
        holder.uploadButton.setOnClickListener {
            itemClickListener.onUploadClicked(currentItem.classId)
        }
        holder.editButton.setOnClickListener {
            itemClickListener.onEditClicked(currentItem.classId)
        }
        holder.audioButton.setOnClickListener {
            itemClickListener.onAudioClicked(currentItem.classId)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}