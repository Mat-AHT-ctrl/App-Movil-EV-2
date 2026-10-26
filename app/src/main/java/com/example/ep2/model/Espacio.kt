package com.example.ep2.model

data class Espacio(
    val id: String,
    val nombre: String,
    val tipo: String,
    val descripcion: String,
    val ubicacion: String,
    val capacidad: Int,
    val horarioFuncionamiento: String,
    val estado: String
)