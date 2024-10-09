package com.example.learnme.fragment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.R

//Creo que se debe definir como Clase recursiva

class ImageGridActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_capture)

        // Find RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewImages)

        // Define the grid layout with 3 columns
        val spanCount = 5
        recyclerView.layoutManager = GridLayoutManager(this, spanCount)

        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing) // Define grid_spacing in your dimens.xml
        recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing, true))

        // Replace with actual image loading
        val imageList = listOf(
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
            ImageItem(R.drawable.ic_launcher_background),
        )

        // Set up the adapter
        val adapter = ImageGridAdapter(imageList)
        recyclerView.adapter = adapter
    }
}