package com.example.myapplication.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

/** DTO de la API pública de feriados (date.nager.at). */
data class FeriadoDto(
    val date: String,       // yyyy-MM-dd
    val localName: String   // Ej: "Independencia Nacional"
)

/**
 * API externa: feriados legales de Chile. No requiere API key.
 * Se usa para que los días feriados no sean reservables.
 */
interface FeriadosApiService {
    @GET("api/v3/PublicHolidays/{anio}/CL")
    suspend fun obtenerFeriadosChile(@Path("anio") anio: Int): List<FeriadoDto>
}
