package com.example.ep2.model

data class SolicitudReserva(
    val id: String,
    val espacioId: String,
    val espacioNombre: String,
    val solicitante: String,
    val perfil: String,
    val actividad: String,
    val cursoGrupo: String,
    val asistentes: Int,
    val fechaReserva: String,
    val horaInicio: String,
    val horaTermino: String,
    val estadoSolicitud: String,
    val justificacionRechazo: String? = null
)