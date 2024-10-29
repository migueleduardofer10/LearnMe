package com.example.learnme.fragments

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

class GalleryHelper(private val context: Context) {

    fun loadImagesFromGallery(): MutableList<ImageItem> {
        val imageList = mutableListOf<ImageItem>()
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val imagePath = it.getString(columnIndex)
                imageList.add(ImageItem(imagePath))
            }
        }
        return imageList
    }
}