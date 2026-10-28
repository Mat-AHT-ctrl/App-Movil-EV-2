package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.model.Usuario

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insert(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios")
    suspend fun getAll(): List<Usuario>

    @Query("SELECT * FROM usuarios WHERE email = :email")
    suspend fun getByEmail(email: String): Usuario?

    @Update
    suspend fun update(usuario: Usuario): Int
}