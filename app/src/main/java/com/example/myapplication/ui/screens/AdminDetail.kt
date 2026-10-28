package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
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
fun AdminDetail(
    reserva: Reserva,
    viewModel: AppViewModel,
    onAprobada: () -> Unit,
    onRechazada: () -> Unit,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var justificacion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header con flecha atrás
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Detalle de Reserva",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Card de la reserva
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Ícono
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.School,
                    contentDescription = "Espacio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre del espacio
                Text(
                    text = reserva.espacioNombre,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Profesor
                Text(
                    text = "Profesor: Usuario ID ${reserva.usuarioId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha
                Text(
                    text = "Fecha: ${reserva.fecha}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Horario
                Text(
                    text = "Horario: ${reserva.horaInicio} - ${reserva.horaTermino}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estado
                Text(
                    text = "Estado: ${reserva.estado.capitalize()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = when (reserva.estado) {
                        "Pendiente" -> Color(0xFFFB8C00)
                        "Aprobada" -> Color(0xFF43A047)
                        "Rechazada" -> Color(0xFFE53935)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(
                            color = when (reserva.estado) {
                                "Pendiente" -> Color(0xFFFB8C00).copy(alpha = 0.15f)
                                "Aprobada" -> Color(0xFF43A047).copy(alpha = 0.15f)
                                "Rechazada" -> Color(0xFFE53935).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Justificación de rechazo (opcional)
        Text(
            text = "Justificación de rechazo (opcional)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = justificacion,
            onValueChange = { justificacion = it },
            label = { Text("Escribe el motivo...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botones Aprobar / Rechazar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.rechazarReserva(reserva.id, justificacion)
                        onRechazada()
                    }
                },
                enabled = reserva.estado == "Pendiente",
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Rechazar",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rechazar")
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.aprobarReserva(reserva.id)
                        onAprobada()
                    }
                },
                enabled = reserva.estado == "Pendiente",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Aprobar")
            }
        }
    }
}
