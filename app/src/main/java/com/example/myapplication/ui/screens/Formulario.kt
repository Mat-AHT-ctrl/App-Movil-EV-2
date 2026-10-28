package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.Espacio
import com.example.myapplication.model.Reserva
import com.example.myapplication.utils.DateTimeUtils
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Formulario(
    espacio: Espacio,
    viewModel: AppViewModel,
    onEnviarSolicitud: () -> Unit,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var actividad by remember { mutableStateOf("") }
    var cursoGrupo by remember { mutableStateOf("") }
    var asistentes by remember { mutableStateOf("") }
    var fechaSeleccionada by remember { mutableStateOf(DateTimeUtils.fechaFormatter.format(LocalDate.now())) }
    var horariosSeleccionados = remember { mutableStateListOf<String>() }
    val horariosOrdenados = horariosSeleccionados.sortedBy { it.split(" - ")[0] }
    var errorMensaje by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var reservasExistentes by remember { mutableStateOf(emptyList<Reserva>()) }

    val horariosDisponibles = remember { DateTimeUtils.generarHorariosBloques() }
    val fechaDb = DateTimeUtils.formatearFechaParaDb(fechaSeleccionada)

    LaunchedEffect(fechaSeleccionada) {
        coroutineScope.launch {
            reservasExistentes = viewModel.getReservasByEspacioYFecha(espacio.id, fechaDb)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Reservar: ${espacio.nombre}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = actividad,
            onValueChange = { actividad = it; errorMensaje = "" },
            label = { Text("Actividad o Motivo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cursoGrupo,
            onValueChange = { cursoGrupo = it; errorMensaje = "" },
            label = { Text("Curso o Grupo (ej: 3° Medio A)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = asistentes,
            onValueChange = { asistentes = it; errorMensaje = "" },
            label = { Text("Cantidad de Asistentes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fechaSeleccionada,
            onValueChange = { fechaSeleccionada = it; errorMensaje = "" },
            label = { Text("Fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Horarios disponibles (bloques de 45 min)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            horariosDisponibles.forEach { horario ->
                val bloqueInicio = horario.split(" - ")[0]
                val bloqueTermino = horario.split(" - ")[1]
                val estaOcupado = reservasExistentes.any { reserva ->
                    reserva.horaInicio < bloqueTermino && reserva.horaTermino > bloqueInicio
                }
                val estaSeleccionado = horario in horariosSeleccionados

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !estaOcupado,
                            onClick = {
                                if (!estaOcupado) {
                                    if (estaSeleccionado) {
                                        horariosSeleccionados.remove(horario)
                                    } else {
                                        horariosSeleccionados.add(horario)
                                    }
                                }
                            }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            estaOcupado -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            estaSeleccionado -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (estaOcupado) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Ocupado",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$horario (Ocupado)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            if (estaSeleccionado) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonChecked,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Disponible",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = horario,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (estaOcupado) {
                            Text(
                                text = "Ocupado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resumen de la reserva
        if (horariosSeleccionados.isNotEmpty()) {
            val horaInicio = horariosOrdenados.first().split(" - ")[0]
            val ultimoHorario = horariosOrdenados.last()
            val horaTermino = DateTimeUtils.calcularHoraTermino(ultimoHorario)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Resumen de tu reserva (${horariosSeleccionados.size} bloque${if (horariosSeleccionados.size > 1) "s" else ""})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sala: ${espacio.nombre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${DateTimeUtils.formatearHora(horaInicio)} - ${DateTimeUtils.formatearHora(horaTermino)} ${DateTimeUtils.formatearFecha(fechaDb)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (errorMensaje.isNotEmpty()) {
            Text(
                text = errorMensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (actividad.isBlank() || cursoGrupo.isBlank() || asistentes.isBlank()) {
                    errorMensaje = "Por favor, completa todos los campos obligatorios"
                } else if (asistentes.toIntOrNull() == null || asistentes.toInt() <= 0) {
                    errorMensaje = "Ingresa un número válido de asistentes"
                } else if (fechaSeleccionada.isBlank()) {
                    errorMensaje = "Selecciona una fecha"
                } else if (horariosSeleccionados.isEmpty()) {
                    errorMensaje = "Selecciona al menos un horario disponible"
                } else {
                    // Validar que los bloques sean consecutivos
                    val indices = horariosSeleccionados.map { horariosDisponibles.indexOf(it) }.sorted()
                    val esContiguo = indices.zipWithNext().all { (a, b) -> b == a + 1 }
                    if (!esContiguo && horariosSeleccionados.size > 1) {
                        errorMensaje = "Los bloques horarios deben ser consecutivos (sin saltos)"
                    } else {
                        enviando = true
                        errorMensaje = ""
                        coroutineScope.launch {
                            val fechaDb = DateTimeUtils.formatearFechaParaDb(fechaSeleccionada)
                            val reservasOcupadas = viewModel.getReservasByEspacioYFecha(espacio.id, fechaDb)
                            val conflicto = reservasOcupadas.any { reserva ->
                                val reservaInicio = reserva.horaInicio
                                val reservaTermino = reserva.horaTermino
                                horariosSeleccionados.any { bloque ->
                                    val bloqueInicio = bloque.split(" - ")[0]
                                    val bloqueTermino = bloque.split(" - ")[1]
                                    (bloqueInicio >= reservaInicio && bloqueInicio < reservaTermino) ||
                                            (bloqueTermino > reservaInicio && bloqueTermino <= reservaTermino)
                                }
                            }
                            if (conflicto) {
                                enviando = false
                                errorMensaje = "Uno o más bloques seleccionados ya están ocupados"
                            } else {
                                val nuevaReserva = Reserva(
                                    espacioId = espacio.id,
                                    espacioNombre = espacio.nombre,
                                    usuarioId = viewModel.currentUser?.id ?: 0,
                                    fecha = fechaDb,
                                    horaInicio = horariosOrdenados.first().split(" - ")[0],
                                    horaTermino = DateTimeUtils.calcularHoraTermino(horariosOrdenados.last()),
                                    estado = "Pendiente"
                                )
                                val id = viewModel.insertReserva(nuevaReserva)
                                enviando = false
                                if (id > 0) {
                                    onEnviarSolicitud()
                                } else {
                                    errorMensaje = "No se pudo crear la reserva"
                                }
                            }
                        }
                    }
                }
            },
            enabled = !enviando,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7E88EB),
                contentColor = Color.White
            )
        ) {
            if (enviando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Enviar Solicitud de Reserva")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF7E88EB)
            )
        ) {
            Text("Cancelar y Volver")
        }
    }
}
