package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reservas",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["usuarioId", "fecha"])]
)
data class Reserva(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val espacioId: String,
    val espacioNombre: String,
    val usuarioId: Int,
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val estado: String = "Pendiente",
    val justificacionRechazo: String? = null
)