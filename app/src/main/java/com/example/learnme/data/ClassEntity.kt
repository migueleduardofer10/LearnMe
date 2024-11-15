package com.example.learnme.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val classId: Int = 0,
    @ColumnInfo(name = "class_name") val className: String,
//    @ColumnInfo(name = "description") val description: String
    @ColumnInfo(name = "isLabelGenerated")var isLabelGenerated: Boolean = false

)

@Entity(
    tableName = "images",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class,
        parentColumns = ["classId"],
        childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("classId")]
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val imageId: Int = 0,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "classId") val classId: Int,
//    @ColumnInfo(name = "rotation") val rotation: Int
)
