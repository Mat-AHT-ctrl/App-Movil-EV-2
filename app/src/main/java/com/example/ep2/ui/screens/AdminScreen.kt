package com.example.ep2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ep2.model.SolicitudReserva

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    solicitudes: List<SolicitudReserva>,
    onAprobar: (String) -> Unit,
    onRechazar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    var solicitudARechazar by remember { mutableStateOf<SolicitudReserva?>(null) }
    var justificacionText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Inspectoria (Admin)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {
            Text(
                text = "Gestion y aprobacion de Solicitudes: ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            if (solicitudes.isEmpty()) {
                Text(
                    text = "No hay solicitudes registradas en el sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(solicitudes) { sol ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Espacio: ${sol.espacioNombre}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(text = "Solicitante: ${sol.solicitante} (${sol.perfil})")
                                Text(text = "Actividad: ${sol.actividad} | Grupo: ${sol.cursoGrupo}")
                                Text(text = "Fecha: ${sol.fechaReserva} (${sol.horaInicio} - ${sol.horaTermino})")
                                Text(
                                    text = "Estado: ${sol.estadoSolicitud.uppercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sol.estadoSolicitud == "pendiente") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )

                                if (sol.estadoSolicitud == "pendiente") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onAprobar(sol.id) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Aprobar")
                                        }
                                        OutlinedButton(
                                            onClick = { solicitudARechazar = sol },
                                            modifier = Modifier.weight(1f)
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

            Button(
                onClick = onVolver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Catálogo")
            }
        }
    }

    // Diálogo de justificación de rechazo obligatorio según las reglas del caso
    if (solicitudARechazar != null) {
        AlertDialog(
            onDismissRequest = { solicitudARechazar = null },
            title = { Text("Justificar Rechazo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingrese la justificación para rechazar la reserva de ${solicitudARechazar?.espacioNombre}:")
                    OutlinedTextField(
                        value = justificacionText,
                        onValueChange = { justificacionText = it },
                        label = { Text("Motivo de rechazo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (justificacionText.isNotBlank()) {
                            onRechazar(solicitudARechazar!!.id, justificacionText)
                            solicitudARechazar = null
                            justificacionText = ""
                        }
                    }
                ) {
                    Text("Confirmar Rechazo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        solicitudARechazar = null
                        justificacionText = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}