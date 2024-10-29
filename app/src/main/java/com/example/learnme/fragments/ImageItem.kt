package com.example.learnme.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.learnme.R

// Data class to represent each image item
data class ImageItem(
    val imagePath: String,
    var isSelected: Boolean = false
) // Cambiado a String para manejar rutas de archivos

class ImageAdapter(
    private val imageList: List<ImageItem>,  // Lista de imágenes a mostrar
    private val onItemClick: (ImageItem) -> Unit  // Evento para manejar clics en la imagen
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    // ViewHolder que contiene la vista de cada imagen individual
    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView =
            view.findViewById(R.id.imageView)  // ID de ImageView en el layout de ítem
    }

    // Crear nuevos ViewHolders
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)  // Infla el layout de cada ítem
        return ImageViewHolder(view)
    }

    // Vincular el ViewHolder con un ImageItem
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = imageList[position]

        // Usar Glide o similar para cargar la imagen desde el path
        Glide.with(holder.imageView.context)
            .load(imageItem.imagePath)  // Cargar la imagen de la ruta
            .into(holder.imageView)

        holder.imageView.alpha = if (imageItem.isSelected) 0.5f else 1.0f

        // Configurar un clic para manejar la selección de imágenes
        holder.imageView.setOnClickListener {
            onItemClick(imageItem)
            notifyItemChanged(position)
        }
    }

    // Devolver el tamaño de la lista
    override fun getItemCount(): Int = imageList.size
}