package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "espacios")
data class Espacio(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipo: String,
    val descripcion: String,
    val ubicacion: String,
    val capacidad: Int,
    val horarioFuncionamiento: String,
    val estado: String = "Disponible"
)