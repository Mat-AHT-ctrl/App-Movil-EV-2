package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "archivos")
data class ArchivoItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val uri: String? = null,
    val tipo: String = "OTRO"
) {
    companion object {
        fun determinarTipo(nombreArchivo: String): String {
            val extension = nombreArchivo.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "pdf" -> "PDF"
                "doc", "docx" -> "WORD"
                "xls", "xlsx" -> "EXCEL"
                "ppt", "pptx" -> "POWERPOINT"
                "png", "jpg", "jpeg", "gif", "webp" -> "IMAGEN"
                else -> "OTRO"
            }
        }
    }
}