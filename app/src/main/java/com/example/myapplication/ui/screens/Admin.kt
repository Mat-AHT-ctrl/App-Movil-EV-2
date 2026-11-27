package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.viewmodel.AppViewModel
import com.example.myapplication.viewmodel.ReservaVista
import kotlinx.coroutines.launch

@Composable
fun Admin(
    viewModel: AppViewModel,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var solicitudes by remember { mutableStateOf(emptyList<ReservaVista>()) }
    var loading by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        when (val resultado = viewModel.listarPendientesParaAdmin()) {
            is ApiResult.Ok -> solicitudes = resultado.data
            is ApiResult.Error -> mensajeError = resultado.mensaje
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Panel de Inspectoría",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (viewModel.modoOffline) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Modo offline: mostrando solicitudes guardadas en el dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (mensajeError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensajeError, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (solicitudes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay solicitudes pendientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(solicitudes, key = { it.idRemota ?: "local-${it.idLocal}" }) { solicitud ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = solicitud.espacioNombre,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Solicitante: ${solicitud.solicitanteNombre.ifBlank { "—" }} (${solicitud.perfilSolicitante})")
                            Text(text = "Curso/Grupo: ${solicitud.cursoGrupo.ifBlank { "—" }}")
                            Text(text = "Fecha: ${solicitud.fecha} | ${solicitud.horaInicio} - ${solicitud.horaTermino}")
                            Text(text = "Asistentes: ${solicitud.asistentes}")
                            Text(
                                text = "Estado: ${solicitud.estado.uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFB8C00)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val idRemota = solicitud.idRemota
                                            if (idRemota != null) {
                                                when (val resultado = viewModel.aprobarReservaRemota(idRemota)) {
                                                    is ApiResult.Ok -> solicitudes = solicitudes.filter { it != solicitud }
                                                    is ApiResult.Error -> mensajeError = resultado.mensaje
                                                }
                                            } else {
                                                viewModel.aprobarReserva(solicitud.idLocal)
                                                solicitudes = solicitudes.filter { it != solicitud }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                                ) {
                                    Text("Aprobar")
                                }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val idRemota = solicitud.idRemota
                                            val justificacion = "Rechazado por inspectoría"
                                            if (idRemota != null) {
                                                when (val resultado = viewModel.rechazarReservaRemota(idRemota, justificacion)) {
                                                    is ApiResult.Ok -> solicitudes = solicitudes.filter { it != solicitud }
                                                    is ApiResult.Error -> mensajeError = resultado.mensaje
                                                }
                                            } else {
                                                viewModel.rechazarReserva(solicitud.idLocal, justificacion)
                                                solicitudes = solicitudes.filter { it != solicitud }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                                ) {
                                    Text("Rechazar")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al Catálogo")
        }
    }
}
