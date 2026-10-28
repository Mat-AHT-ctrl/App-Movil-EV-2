package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.model.Espacio

@Dao
interface EspacioDao {
    @Insert
    suspend fun insert(espacio: Espacio): Long

    @Query("SELECT * FROM espacios ORDER BY nombre ASC")
    suspend fun getAll(): List<Espacio>

    @Update
    suspend fun update(espacio: Espacio): Int

    @Query("DELETE FROM espacios WHERE id = :id")
    suspend fun delete(id: String)
}