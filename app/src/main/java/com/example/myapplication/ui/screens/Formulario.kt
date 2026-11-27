package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.logic.ValidadorReserva
import com.example.myapplication.model.Espacio
import com.example.myapplication.utils.DateTimeUtils
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    // Viene del paso anterior (detalle del espacio); no se puede editar acá.
    val fechaSeleccionada by remember {
        mutableStateOf(
            viewModel.reservaFecha.ifBlank { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
        )
    }
    var errorMensaje by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    val fechaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Reservar: ${espacio.nombre}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${espacio.tipo} · ${espacio.ubicacion}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${fechaSeleccionada} · ${viewModel.reservaHoraInicio} - ${viewModel.reservaHoraTermino}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Capacidad: ${espacio.capacidad} personas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = actividad,
            onValueChange = { actividad = it; errorMensaje = "" },
            label = { Text("Actividad o Motivo *") },
            placeholder = { Text("Ej. Taller de lectura comprensiva") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cursoGrupo,
            onValueChange = { cursoGrupo = it; errorMensaje = "" },
            label = { Text("Curso o Grupo (ej: 3° Medio A) *") },
            placeholder = { Text("Ej. 5° Básico A") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = asistentes,
            onValueChange = { asistentes = it; errorMensaje = "" },
            label = { Text("Cantidad de Asistentes *") },
            placeholder = { Text("Ej. 25") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (errorMensaje.isNotEmpty()) {
            Text(
                text = errorMensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val error = ValidadorReserva.validarFormulario(
                    actividad, cursoGrupo, asistentes, espacio.capacidad
                )

                if (error != null) {
                    errorMensaje = error
                } else {
                    enviando = true
                    errorMensaje = ""
                    coroutineScope.launch {
                        val fechaDbFinal = DateTimeUtils.formatearFechaParaDb(fechaSeleccionada)
                        val usuarioId = viewModel.usuarioRemoto?.id ?: viewModel.currentUser?.id.toString() ?: ""
                        val solicitanteNombre = viewModel.usuarioRemoto?.nombre ?: viewModel.currentUser?.nombre ?: ""
                        val perfilSolicitante = viewModel.usuarioRemoto?.perfil ?: viewModel.currentUser?.rol ?: "DOCENTE"

                        val horaInicioFinal = viewModel.reservaHoraInicio
                        val horaTerminoFinal = viewModel.reservaHoraTermino

                        val resultado = viewModel.crearReservaBackend(
                            espacioId = espacio.id,
                            espacioNombre = espacio.nombre,
                            usuarioId = usuarioId,
                            solicitanteNombre = solicitanteNombre,
                            perfilSolicitante = perfilSolicitante,
                            fecha = fechaDbFinal,
                            horaInicio = horaInicioFinal,
                            horaTermino = horaTerminoFinal,
                            actividad = actividad,
                            cursoGrupo = cursoGrupo,
                            asistentes = asistentes.toInt(),
                            observaciones = null
                        )
                        enviando = false
                        when (resultado) {
                            is ApiResult.Ok<*> -> {
                                onEnviarSolicitud()
                            }
                            is ApiResult.Error -> {
                                errorMensaje = resultado.mensaje
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
            Text("Volver al detalle")
        }
    }
}
