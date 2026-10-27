package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioScreen(
    espacioNombre: String,
    onEnviarSolicitud: (String, String, String, String, String, String) -> Unit,
    onVolver: () -> Unit
) {
    var actividad by remember { mutableStateOf("") }
    var cursoGrupo by remember { mutableStateOf("") }
    var asistentes by remember { mutableStateOf("") }
    var fechaReserva by remember { mutableStateOf("") }
    var horaInicio by remember { mutableStateOf("") }
    var horaTermino by remember { mutableStateOf("") }

    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitar: $espacioNombre") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { Text(
            text = "Complete los datos (Anticipación mínima: 1 día hábil):",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

            OutlinedTextField(
                value = actividad,
                onValueChange = { actividad = it },
                label = { Text("Actividad a realizar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = cursoGrupo,
                onValueChange = { cursoGrupo = it },
                label = { Text("Curso o grupo participante (Ej: 5° Básico)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = asistentes,
                onValueChange = { asistentes = it },
                label = { Text("Cantidad aproximada de asistentes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = fechaReserva,
                onValueChange = { fechaReserva = it },
                label = { Text("Fecha de reserva (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = horaInicio,
                    onValueChange = { horaInicio = it },
                    label = { Text("Hora Inicio (HH:MM)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = horaTermino,
                    onValueChange = { horaTermino = it },
                    label = { Text("Hora Término (HH:MM)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            if (errorMensaje != null) {
                Text(
                    text = errorMensaje!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Validación de campos vacíos obligatorios
                    if (actividad.isBlank() || cursoGrupo.isBlank() || asistentes.isBlank() || fechaReserva.isBlank() || horaInicio.isBlank() || horaTermino.isBlank()) {
                        errorMensaje = "Por favor, complete todos los campos obligatorios."
                    } else {
                        errorMensaje = null
                        mostrarDialogoConfirmacion = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar Solicitud")
            }

            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Catálogo")
            }
        }
    }

    // Diálogo de confirmación requerido por las condiciones de uso de la app
    if (mostrarDialogoConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmacion = false },
            title = { Text("Confirmar Envío") },
            text = { Text("¿Desea enviar la solicitud para el espacio $espacioNombre?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoConfirmacion = false
                        onEnviarSolicitud(actividad, cursoGrupo, asistentes, fechaReserva, horaInicio, horaTermino)
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmacion = false }) {
                    Text("Cancelar")
                }

            }
        )
    }
}