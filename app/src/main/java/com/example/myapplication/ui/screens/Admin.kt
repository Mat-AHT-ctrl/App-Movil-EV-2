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
import com.example.myapplication.model.Reserva
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun Admin(
    viewModel: AppViewModel,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var solicitudes by remember { mutableStateOf(emptyList<Reserva>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            solicitudes = viewModel.getReservasPendientes()
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Panel de Inspectoría", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (solicitudes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No hay solicitudes pendientes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(solicitudes, key = { it.id }) { solicitud ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = solicitud.espacioNombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Solicitante ID: ${solicitud.usuarioId}")
                            Text(text = "Espacio: ${solicitud.espacioNombre}")
                            Text(text = "Fecha: ${solicitud.fecha} | ${solicitud.horaInicio} - ${solicitud.horaTermino}")
                            Text(
                                text = "Estado: ${solicitud.estado.uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = when (solicitud.estado) {
                                    "Aprobada" -> Color(0xFF43A047)
                                    "Rechazada" -> Color(0xFFE53935)
                                    else -> Color(0xFFFB8C00)
                                }
                            )

                            if (solicitud.estado == "Pendiente") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.aprobarReserva(solicitud.id)
                                                solicitudes = solicitudes.filter { it.id != solicitud.id }
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
                                                viewModel.rechazarReserva(solicitud.id, "Rechazado por inspector")
                                                solicitudes = solicitudes.filter { it.id != solicitud.id }
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
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al Catálogo")
        }
    }
}