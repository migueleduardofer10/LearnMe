package com.example.learnme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.learnme.R

// Data class to represent each image item
data class ImageItem(
    val imagePath: String, // Cambiado a String para manejar rutas de archivos
    var isSelected: Boolean = false // Indicador de selección para el modo de selección múltiple
)

class ImageAdapter(
    private var imageList: MutableList<ImageItem>,  // Lista de imágenes a mostrar
    private val onItemClick: (ImageItem) -> Unit  // Evento para manejar clics en la imagen
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    var isSelectionMode = false  // Controla si el adaptador está en modo de selección múltiple
    private val selectedImages = mutableSetOf<ImageItem>() // Almacena las imágenes seleccionadas


    // ViewHolder que contiene la vista de cada imagen individual
    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }


    // Crear nuevos ViewHolders
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    // Vincular el ViewHolder con un ImageItem
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = imageList[position]

        // Usar Glide o similar para cargar la imagen desde el path
        Glide.with(holder.imageView.context)
            .load(imageItem.imagePath)
            .into(holder.imageView)

        // Cambiar la opacidad de la imagen según el estado de selección
        holder.imageView.alpha = if (imageItem.isSelected) 0.5f else 1.0f

        // Configurar un clic para manejar la selección de imágenes
        holder.imageView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(imageItem)
            } else {
                onItemClick(imageItem)
            }
            notifyItemChanged(position)
        }
    }

    // Alternar la selección de una imagen
    private fun toggleSelection(imageItem: ImageItem) {
        imageItem.isSelected = !imageItem.isSelected
        if (imageItem.isSelected) {
            selectedImages.add(imageItem)
        } else {
            selectedImages.remove(imageItem)
        }
    }

    // Método para limpiar todas las selecciones
    fun clearSelection() {
        selectedImages.forEach { it.isSelected = false }
        selectedImages.clear()
        notifyDataSetChanged()
    }

    // Método para eliminar las imágenes seleccionadas
    fun deleteSelectedImages() {
        imageList.removeAll(selectedImages)
        selectedImages.clear()
        notifyDataSetChanged()
    }

    // Devolver el tamaño de la lista
    override fun getItemCount(): Int = imageList.size

}