package com.example.learnme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// No trabaja con suspend fun

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes")
    fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE classId = :classId")
    fun getClassById(classId: Int): ClassEntity?

    @Query("SELECT class_name FROM classes WHERE classId = :classId")
    fun getClassNameById(classId: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClass(classEntity: ClassEntity): Long

//    @Query("DELETE FROM classes WHERE classId = :classId")
//    fun deleteClass(classId: Int)

    @Query("UPDATE classes SET class_name = :newName, isLabelGenerated = 1 WHERE classId = :classId")
    fun updateClassName(classId: Int, newName: String)
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM images")
    fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM images WHERE classId = :classId")
    fun getImagesForClass(classId: Int): List<ImageEntity>

    @Insert
    fun insertImage(image: ImageEntity): Long

    @Delete
    fun deleteImage(image: ImageEntity)

    // Alternativa: agregar un método de borrado basado en el path o el ID
    @Query("DELETE FROM images WHERE image_path = :imagePath")
    fun deleteImageByPath(imagePath: String)
}