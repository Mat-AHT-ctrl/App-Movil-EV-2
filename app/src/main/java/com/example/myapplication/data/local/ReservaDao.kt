package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.model.Reserva

@Dao
interface ReservaDao {
    @Insert
    suspend fun insert(reserva: Reserva): Long

    @Query("SELECT * FROM reservas WHERE usuarioId = :usuarioId ORDER BY fecha DESC, horaInicio ASC")
    suspend fun getByUsuario(usuarioId: Int): List<Reserva>

    @Query("SELECT * FROM reservas WHERE espacioId = :espacioId AND fecha = :fecha AND estado != 'Rechazada' AND estado != 'Cancelada' ORDER BY horaInicio ASC")
    suspend fun getByEspacioYFecha(espacioId: String, fecha: String): List<Reserva>

    @Query("SELECT * FROM reservas ORDER BY fecha DESC, horaInicio ASC")
    suspend fun getAll(): List<Reserva>

    @Query("SELECT * FROM reservas WHERE estado = 'Pendiente' ORDER BY fecha ASC, horaInicio ASC")
    suspend fun getPendientes(): List<Reserva>

    @Query("SELECT * FROM reservas WHERE id = :id")
    suspend fun getById(id: Int): Reserva?

    @Update
    suspend fun update(reserva: Reserva): Int

    @Query("DELETE FROM reservas WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE reservas SET estado = 'Cancelada' WHERE id = :id")
    suspend fun cancelar(id: Int)

    @Query("UPDATE reservas SET estado = :estado, justificacionRechazo = :justificacion WHERE id = :id")
    suspend fun actualizarEstado(id: Int, estado: String, justificacion: String? = null)
}