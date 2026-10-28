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
import com.example.myapplication.model.SolicitudReserva

@Composable
fun AdminScreen(
    solicitudes: List<SolicitudReserva>,
    onAprobar: (String) -> Unit,
    onRechazar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Panel de Inspectoría",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (solicitudes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay solicitudes registradas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(solicitudes) { solicitud ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Espacio: ${solicitud.espacioNombre}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "Solicitante: ${solicitud.solicitante} (${solicitud.perfil})")
                            Text(text = "Actividad: ${solicitud.actividad} | Grupo: ${solicitud.cursoGrupo}")
                            Text(text = "Fecha: ${solicitud.fechaReserva} (${solicitud.horaInicio} - ${solicitud.horaTermino})")
                            Text(
                                text = "Estado: ${solicitud.estadoSolicitud.uppercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (solicitud.estadoSolicitud == "pendiente")
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.secondary
                            )

                            if (solicitud.estadoSolicitud == "pendiente") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onAprobar(solicitud.id) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                                    ) {
                                        Text("Aprobar")
                                    }
                                    Button(
                                        onClick = { onRechazar(solicitud.id, "No disponible") },
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
        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al Catálogo")
        }
    }
}