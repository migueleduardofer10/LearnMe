package com.example.learnme.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R

// Data class to represent each image item
data class ImageItem(val imageResource: Int) // You can modify it to use file URIs or URLs

class ImageGridAdapter(private val imageList: List<ImageItem>) :
    RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder>() {

    // ViewHolder that holds the image view
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = imageList[position]
        holder.imageView.setImageResource(imageItem.imageResource) // Replace with actual image loading
    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}