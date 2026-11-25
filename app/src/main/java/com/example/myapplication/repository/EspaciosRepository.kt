package com.example.myapplication.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class EspaciosRepository {

    suspend fun obtenerEspaciosJson(endpointUrl: String): Result<String> = withContext(Dispatchers.IO) {
        var conexion: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            val codigoRespuesta = conexion.responseCode
            if (codigoRespuesta in 200..299) {
                val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
                Result.success(cuerpo)
            } else {
                Result.failure(Exception("Error HTTP: $codigoRespuesta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conexion?.disconnect()
        }
    }
}