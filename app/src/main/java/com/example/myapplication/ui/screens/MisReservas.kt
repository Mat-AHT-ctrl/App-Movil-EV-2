package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.viewmodel.AppViewModel
import com.example.myapplication.viewmodel.ReservaVista
import kotlinx.coroutines.launch

private fun colorEstado(estado: String): Color = when (estado.lowercase()) {
    "aprobada" -> Color(0xFF43A047)
    "rechazada" -> Color(0xFFE53935)
    "cancelada" -> Color(0xFF9E9E9E)
    "finalizada" -> Color(0xFF5E35B1)
    else -> Color(0xFFFB8C00)
}

@Composable
fun MisReservas(
    viewModel: AppViewModel,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var reservas by remember { mutableStateOf(emptyList<ReservaVista>()) }
    var loading by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf("Todas") }
    var reservaDetalle by remember { mutableStateOf<ReservaVista?>(null) }
    var reservaACancelar by remember { mutableStateOf<ReservaVista?>(null) }
    var cancelandoId by remember { mutableStateOf<String?>(null) }

    fun recargar() {
        coroutineScope.launch {
            loading = true
            mensajeError = ""
            reservas = viewModel.obtenerHistorialReservas()
            loading = false
        }
    }

    LaunchedEffect(Unit) { recargar() }

    val esAdmin = viewModel.esAdmin
    val idUsuarioActual = viewModel.usuarioRemoto?.id ?: viewModel.currentUser?.id?.toString()

    fun esMia(reserva: ReservaVista): Boolean =
        !esAdmin || reserva.usuarioId == idUsuarioActual

    fun clave(reserva: ReservaVista): String =
        reserva.idRemota ?: "local-${reserva.idLocal}"

    fun cancelar(reserva: ReservaVista) {
        cancelandoId = clave(reserva)
        coroutineScope.launch {
            val idRemota = reserva.idRemota
            if (idRemota != null) {
                when (val resultado = viewModel.cancelarReservaRemota(idRemota)) {
                    is ApiResult.Ok -> reservas = reservas.map {
                        if (it == reserva) it.copy(estado = "Cancelada") else it
                    }
                    is ApiResult.Error -> mensajeError = resultado.mensaje
                }
            } else {
                viewModel.cancelarReserva(reserva.idLocal)
                reservas = reservas.map {
                    if (it == reserva) it.copy(estado = "Cancelada") else it
                }
            }
            cancelandoId = null
            reservaACancelar = null
        }
    }

    val estados = listOf("Todas", "Pendiente", "Aprobada", "Rechazada", "Cancelada")
    val filtradas = if (filtro == "Todas") {
        reservas
    } else {
        reservas.filter { it.estado.equals(filtro, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Cabecera compacta (el TopAppBar global ya está en MainActivity).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = if (esAdmin) "Historial Global de Reservas" else "Mi Historial de Reservas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { recargar() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(estados) { estado ->
                val conteo = if (estado == "Todas") {
                    reservas.size
                } else {
                    reservas.count { it.estado.equals(estado, ignoreCase = true) }
                }
                val texto = "$estado ($conteo)"
                FilterChip(
                    selected = filtro == estado,
                    onClick = { filtro = estado },
                    label = {
                        Text(text = texto, maxLines = 1, softWrap = false)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.modoOffline) {
            Text(
                text = "Modo offline: mostrando las reservas guardadas en el dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        if (mensajeError.isNotEmpty()) {
            Text(text = mensajeError, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filtradas.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay reservas en este filtro",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
            ) {
                items(filtradas, key = { it.idRemota ?: "local-${it.idLocal}" }) { reserva ->
                    val color = colorEstado(reserva.estado)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reservaDetalle = reserva },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = reserva.espacioNombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = color.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = reserva.estado.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(text = "Fecha: ${reserva.fecha} | ${reserva.horaInicio} - ${reserva.horaTermino}")
                            Text(text = "Actividad: ${reserva.actividad.ifBlank { "—" }}")
                            if (esAdmin) {
                                Text(
                                    text = "Solicitante: ${reserva.solicitanteNombre} (${reserva.perfilSolicitante})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (reserva.estado.equals("Pendiente", ignoreCase = true) && esMia(reserva)) {
                                Button(
                                    onClick = { reservaACancelar = reserva },
                                    enabled = cancelandoId == null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF44336)
                                    )
                                ) {
                                    if (cancelandoId == clave(reserva)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Cancelar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reservaDetalle?.let { reserva ->
        val color = colorEstado(reserva.estado)
        AlertDialog(
            onDismissRequest = { reservaDetalle = null },
            title = {
                Column {
                    Text(reserva.espacioNombre)
                    Text(
                        text = reserva.estado.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                }
            },
            text = {
                Column {
                    Text("Solicitante: ${reserva.solicitanteNombre.ifBlank { "—" }}")
                    Text("Perfil/Cargo: ${reserva.perfilSolicitante.ifBlank { "—" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fecha: ${reserva.fecha}")
                    Text("Horario: ${reserva.horaInicio} - ${reserva.horaTermino}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Actividad: ${reserva.actividad.ifBlank { "—" }}")
                    Text("Curso/Grupo: ${reserva.cursoGrupo.ifBlank { "—" }}")
                    Text("Asistentes: ${reserva.asistentes}")
                    if (!reserva.justificacion.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Motivo: ${reserva.justificacion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reservaDetalle = null }) {
                    Text("Cerrar")
                }
            },
            dismissButton = {
                if (reserva.estado.equals("Pendiente", ignoreCase = true) && esMia(reserva)) {
                    TextButton(
                        onClick = {
                            reservaDetalle = null
                            reservaACancelar = reserva
                        }
                    ) {
                        Text("Cancelar solicitud")
                    }
                }
            }
        )
    }

    reservaACancelar?.let { reserva ->
        AlertDialog(
            onDismissRequest = {
                if (cancelandoId == null) reservaACancelar = null
            },
            title = { Text("Cancelar reserva") },
            text = { Text("¿Cancelar la reserva de ${reserva.espacioNombre}?") },
            confirmButton = {
                Button(
                    enabled = cancelandoId == null,
                    onClick = { cancelar(reserva) }
                ) {
                    if (cancelandoId == clave(reserva)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Cancelar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = cancelandoId == null,
                    onClick = { reservaACancelar = null }
                ) {
                    Text("Volver")
                }
            }
        )
    }
}
