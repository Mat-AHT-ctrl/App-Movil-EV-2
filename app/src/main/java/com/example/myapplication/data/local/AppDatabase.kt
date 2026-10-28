package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.model.ArchivoItem
import com.example.myapplication.model.Espacio
import com.example.myapplication.model.Reserva
import com.example.myapplication.model.Usuario

@Database(entities = [Usuario::class, Espacio::class, Reserva::class, ArchivoItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun espacioDao(): EspacioDao
    abstract fun reservaDao(): ReservaDao
    abstract fun archivoDao(): ArchivoDao
}