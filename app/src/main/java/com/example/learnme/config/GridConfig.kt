package com.example.learnme.config

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnme.fragment.GridSpacingItemDecoration
import com.example.learnme.adapter.ImageAdapter
import com.example.learnme.adapter.ImageItem

object GridConfig {

    /**
     * Configura el RecyclerView con un GridLayoutManager, un ItemDecoration para espaciado, y un adaptador.
     * @param recyclerView RecyclerView a configurar
     * @param context Contexto de la actividad o fragmento
     * @param spanCount Número de columnas en el grid
     * @param spacing Espaciado entre los elementos del grid en píxeles
     * @param imageList Lista de elementos que se van a mostrar en el grid
     * @param onItemClick Función que se ejecutará cuando se haga clic en un elemento del grid
     */
    fun setupGridWithAdapter(
        recyclerView: RecyclerView,
        context: Context,
        spanCount: Int,
        spacing: Int,
        imageList: MutableList<ImageItem>,
        onItemClick: (ImageItem) -> Unit
    ): ImageAdapter {
        // Configura el LayoutManager con el número de columnas
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)

        // Añade la decoración de espaciado entre ítems
        recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing))

        // Configura el adaptador y lo asigna al RecyclerView
        val adapter = ImageAdapter(imageList, onItemClick)
        recyclerView.adapter = adapter

        return adapter
    }
}
