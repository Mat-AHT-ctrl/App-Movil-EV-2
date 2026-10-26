package com.example.myapplication.model

import android.net.Uri
import java.util.UUID

enum class TipoArchivo {
    PDF,
    WORD,
    EXCEL,
    POWERPOINT,
    IMAGEN,
    OTRO
}

data class ArchivoItem(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val uri: Uri? = null,
    val tipo: TipoArchivo = determinarTipo(nombre)
) {
    companion object {
        fun determinarTipo(nombreArchivo: String): TipoArchivo {
            val extension = nombreArchivo.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "pdf" -> TipoArchivo.PDF
                "doc", "docx" -> TipoArchivo.WORD
                "xls", "xlsx" -> TipoArchivo.EXCEL
                "ppt", "pptx" -> TipoArchivo.POWERPOINT
                "png", "jpg", "jpeg", "gif", "webp" -> TipoArchivo.IMAGEN
                else -> TipoArchivo.OTRO
            }
        }
    }
}
