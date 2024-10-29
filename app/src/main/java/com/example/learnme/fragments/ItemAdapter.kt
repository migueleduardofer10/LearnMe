package com.example.learnme.fragments


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R

// Modelo de datos
data class ItemClass(val title: String)

class ItemAdapter(
    private val itemList: List<ItemClass>,
    private val itemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    // Interfaz para manejar los clics en los botones
    interface OnItemClickListener {
        fun onBackClicked(position: Int)
        fun onUploadClicked(position: Int)
        fun onEditClicked(position: Int)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.classNameText)
        val cameraButton: ImageButton = itemView.findViewById(R.id.cameraButton)
        val uploadButton: ImageButton = itemView.findViewById(R.id.uploadButton)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = itemList[position]

        // Asignar los datos a las vistas
        holder.titleTextView.text = currentItem.title

        // Asignar listeners a los botones
        holder.cameraButton.setOnClickListener {
            itemClickListener.onBackClicked(position)
        }
        holder.uploadButton.setOnClickListener {
            itemClickListener.onUploadClicked(position)
        }
        holder.editButton.setOnClickListener {
            itemClickListener.onEditClicked(position)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}