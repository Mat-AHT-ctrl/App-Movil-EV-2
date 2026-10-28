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
fun MisReservas(
    viewModel: AppViewModel,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var reservas by remember { mutableStateOf(emptyList<Reserva>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val usuarioId = viewModel.currentUser?.id
            if (usuarioId != null) {
                reservas = viewModel.getReservasByUsuario(usuarioId)
            }
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Mis Reservas", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onVolver) {
                Text("Volver")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (reservas.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No tienes reservas registradas", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(reservas, key = { it.id }) { reserva ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = reserva.espacioNombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Fecha: ${reserva.fecha} | ${reserva.horaInicio} - ${reserva.horaTermino}")
                            Text(
                                text = "Estado: ${reserva.estado.uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = when (reserva.estado) {
                                    "Aprobada" -> Color(0xFF43A047)
                                    "Rechazada" -> Color(0xFFE53935)
                                    "Cancelada" -> Color(0xFF9E9E9E)
                                    else -> Color(0xFFFB8C00)
                                }
                            )
                            if (reserva.estado == "Pendiente") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.cancelarReserva(reserva.id)
                                            reservas = reservas.filter { it.id != reserva.id }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                                ) {
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