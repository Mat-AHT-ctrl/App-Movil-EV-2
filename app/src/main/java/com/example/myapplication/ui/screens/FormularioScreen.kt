package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var fecha by remember { mutableStateOf("2026-11-15") }
    var horaInicio by remember { mutableStateOf("10:00") }
    var horaTermino by remember { mutableStateOf("11:30") }
    var errorMensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Reservar: $espacioNombre",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = actividad,
            onValueChange = { actividad = it; errorMensaje = "" },
            label = { Text("Actividad o Motivo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = cursoGrupo,
            onValueChange = { cursoGrupo = it; errorMensaje = "" },
            label = { Text("Curso o Grupo (ej: 3° Medio A)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = asistentes,
            onValueChange = { asistentes = it; errorMensaje = "" },
            label = { Text("Cantidad de Asistentes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = fecha,
            onValueChange = { fecha = it },
            label = { Text("Fecha (YYYY-MM-DD)") },
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
                label = { Text("Hora Inicio") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = horaTermino,
                onValueChange = { horaTermino = it },
                label = { Text("Hora Término") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        if (errorMensaje.isNotEmpty()) {
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (actividad.isBlank() || cursoGrupo.isBlank() || asistentes.isBlank()) {
                    errorMensaje = "Por favor, completa los campos obligatorios"
                } else if (asistentes.toIntOrNull() == null || asistentes.toInt() <= 0) {
                    errorMensaje = "Ingresa un número válido de asistentes"
                } else {
                    onEnviarSolicitud(actividad, cursoGrupo, asistentes, fecha, horaInicio, horaTermino)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar Solicitud de Reserva")
        }

        TextButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar y Volver")
        }
    }
}