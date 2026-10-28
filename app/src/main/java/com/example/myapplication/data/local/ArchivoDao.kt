package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.model.ArchivoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivoDao {
    @Insert
    suspend fun insert(archivo: ArchivoItem): Long

    @Query("SELECT * FROM archivos")
    suspend fun getAll(): List<ArchivoItem>

    @Query("SELECT * FROM archivos")
    fun getAllFlow(): Flow<List<ArchivoItem>>

    @Query("DELETE FROM archivos WHERE id = :id")
    suspend fun delete(id: String)
}
