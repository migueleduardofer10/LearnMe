package com.example.learnme

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes")
    fun getAllClasses(): List<ClassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClass(classEntity: ClassEntity): Long

    @Query("DELETE FROM classes WHERE classId = :classId")
    fun deleteClass(classId: Int)
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE classId = :classId")
    fun getImagesForClass(classId: Int): List<ImageEntity>

    @Insert
    fun insertImage(image: ImageEntity): Long

    @Delete
    fun deleteImage(image: ImageEntity)
}