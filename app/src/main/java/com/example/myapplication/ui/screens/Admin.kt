package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.data.remote.BloqueoDto
import com.example.myapplication.data.remote.BloqueoRequestDto
import com.example.myapplication.model.Espacio
import com.example.myapplication.viewmodel.AppViewModel
import com.example.myapplication.viewmodel.ReservaVista
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Admin(
    viewModel: AppViewModel,
    onVolver: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }

    var solicitudes by remember { mutableStateOf(emptyList<ReservaVista>()) }
    var bloqueos by remember { mutableStateOf(emptyList<BloqueoDto>()) }
    var espacios by remember { mutableStateOf(emptyList<com.example.myapplication.model.Espacio>()) }
    var loading by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var recarga by remember { mutableStateOf(0) }

    var aprobarSolicitud by remember { mutableStateOf<ReservaVista?>(null) }
    var rechazarSolicitud by remember { mutableStateOf<ReservaVista?>(null) }
    var justificacion by remember { mutableStateOf("") }
    var bloqueoAEliminar by remember { mutableStateOf<BloqueoDto?>(null) }
    var espacioAEditar by remember { mutableStateOf<Espacio?>(null) }
    var mostrarNuevoBloqueo by remember { mutableStateOf(false) }
    var expandirEspacios by remember { mutableStateOf(false) }
    var espacioBloqueo by remember { mutableStateOf("") }
    var fechaBloqueo by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var inicioBloqueo by remember { mutableStateOf("08:15") }
    var finBloqueo by remember { mutableStateOf("09:00") }
    var motivoBloqueo by remember { mutableStateOf("") }

    LaunchedEffect(recarga) {
        loading = true
        when (val resultado = viewModel.listarPendientesParaAdmin()) {
            is ApiResult.Ok -> solicitudes = resultado.data
            is ApiResult.Error -> mensajeError = resultado.mensaje
        }
        when (val resultado = viewModel.listarBloqueos()) {
            is ApiResult.Ok -> bloqueos = resultado.data
            is ApiResult.Error -> Unit
        }
        when (val resultado = viewModel.listarEspaciosBackend()) {
            is ApiResult.Ok -> {
                espacios = resultado.data
                if (espacioBloqueo.isBlank()) espacioBloqueo = resultado.data.firstOrNull()?.id ?: ""
            }
            is ApiResult.Error -> Unit
        }
        loading = false
    }

    suspend fun aprobar(solicitud: ReservaVista) {
        val idRemota = solicitud.idRemota
        val resultado = if (idRemota != null) {
            viewModel.aprobarReservaRemota(idRemota)
        } else {
            viewModel.aprobarReserva(solicitud.idLocal)
            ApiResult.Ok(solicitud)
        }
        when (resultado) {
            is ApiResult.Ok -> {
                solicitudes = solicitudes.filter { it != solicitud }
                mensajeExito = "Reserva aprobada correctamente"
                mensajeError = ""
            }
            is ApiResult.Error -> mensajeError = resultado.mensaje
        }
    }

    suspend fun rechazar(solicitud: ReservaVista, motivo: String) {
        val idRemota = solicitud.idRemota
        val resultado = if (idRemota != null) {
            viewModel.rechazarReservaRemota(idRemota, motivo)
        } else {
            viewModel.rechazarReserva(solicitud.idLocal, motivo)
            ApiResult.Ok(solicitud)
        }
        when (resultado) {
            is ApiResult.Ok -> {
                solicitudes = solicitudes.filter { it != solicitud }
                mensajeExito = "Reserva rechazada"
                mensajeError = ""
            }
            is ApiResult.Error -> mensajeError = resultado.mensaje
        }
    }

    fun nombreEspacio(id: String): String =
        espacios.firstOrNull { it.id == id }?.nombre ?: id.take(8)

    // ---------- Diálogos ----------

    aprobarSolicitud?.let { solicitud ->
        AlertDialog(
            onDismissRequest = { aprobarSolicitud = null },
            title = { Text("¿Aprobar esta reserva?") },
            text = {
                Text("${solicitud.espacioNombre}\n${solicitud.fecha} · ${solicitud.horaInicio} - ${solicitud.horaTermino}\nSolicitante: ${solicitud.solicitanteNombre}")
            },
            confirmButton = {
                TextButton(onClick = {
                    aprobarSolicitud = null
                    coroutineScope.launch { aprobar(solicitud) }
                }) { Text("Aprobar") }
            },
            dismissButton = { TextButton(onClick = { aprobarSolicitud = null }) { Text("Cancelar") } }
        )
    }

    rechazarSolicitud?.let { solicitud ->
        val motivoValido = justificacion.trim().length >= 10
        AlertDialog(
            onDismissRequest = { rechazarSolicitud = null; justificacion = "" },
            title = { Text("Rechazar reserva") },
            text = {
                Column {
                    Text("Indica el motivo (mínimo 10 caracteres):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = justificacion,
                        onValueChange = { justificacion = it },
                        label = { Text("Motivo del rechazo") },
                        minLines = 2,
                        isError = justificacion.isNotBlank() && !motivoValido,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!motivoValido && justificacion.isNotBlank()) {
                        Text(
                            "Faltan ${10 - justificacion.trim().length} caracteres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = motivoValido,
                    onClick = {
                        val motivo = justificacion.trim()
                        rechazarSolicitud = null
                        justificacion = ""
                        coroutineScope.launch { rechazar(solicitud, motivo) }
                    }
                ) { Text("Rechazar") }
            },
            dismissButton = {
                TextButton(onClick = { rechazarSolicitud = null; justificacion = "" }) { Text("Cancelar") }
            }
        )
    }

    bloqueoAEliminar?.let { bloqueo ->
        AlertDialog(
            onDismissRequest = { bloqueoAEliminar = null },
            title = { Text("¿Eliminar bloqueo?") },
            text = { Text("Se liberará ${nombreEspacio(bloqueo.espacioId)} el ${bloqueo.fecha} de ${bloqueo.horaInicio} a ${bloqueo.horaTermino}.") },
            confirmButton = {
                TextButton(onClick = {
                    bloqueoAEliminar = null
                    coroutineScope.launch {
                        when (val r = viewModel.eliminarBloqueo(bloqueo.id)) {
                            is ApiResult.Ok -> {
                                bloqueos = bloqueos.filter { it.id != bloqueo.id }
                                mensajeExito = "Bloqueo eliminado"
                            }
                            is ApiResult.Error -> mensajeError = r.mensaje
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { bloqueoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    espacioAEditar?.let { espacio ->
        AlertDialog(
            onDismissRequest = { espacioAEditar = null },
            title = { Text("Estado de ${espacio.nombre}") },
            text = { Text("Elige el nuevo estado. Los espacios en mantenimiento o fuera de servicio no admiten reservas.") },
            confirmButton = {
                TextButton(onClick = {
                    espacioAEditar = null
                    coroutineScope.launch {
                        when (val r = viewModel.cambiarEstadoEspacio(espacio, "DISPONIBLE")) {
                            is ApiResult.Ok -> {
                                espacios = espacios.map { if (it.id == espacio.id) it.copy(estado = "Disponible") else it }
                                mensajeExito = "Espacio disponible"
                            }
                            is ApiResult.Error -> mensajeError = r.mensaje
                        }
                    }
                }) { Text("Disponible") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        espacioAEditar = null
                        coroutineScope.launch {
                            when (val r = viewModel.cambiarEstadoEspacio(espacio, "EN_MANTENIMIENTO")) {
                                is ApiResult.Ok -> {
                                    espacios = espacios.map { if (it.id == espacio.id) it.copy(estado = "Mantenimiento") else it }
                                    mensajeExito = "Espacio en mantenimiento"
                                }
                                is ApiResult.Error -> mensajeError = r.mensaje
                            }
                        }
                    }) { Text("En mantenimiento") }
                    TextButton(onClick = {
                        espacioAEditar = null
                        coroutineScope.launch {
                            when (val r = viewModel.cambiarEstadoEspacio(espacio, "FUERA_DE_SERVICIO")) {
                                is ApiResult.Ok -> {
                                    espacios = espacios.map { if (it.id == espacio.id) it.copy(estado = "Fuera de servicio") else it }
                                    mensajeExito = "Espacio fuera de servicio"
                                }
                                is ApiResult.Error -> mensajeError = r.mensaje
                            }
                        }
                    }) { Text("Fuera de servicio") }
                    TextButton(onClick = { espacioAEditar = null }) { Text("Cancelar") }
                }
            }
        )
    }

    if (mostrarNuevoBloqueo) {
        AlertDialog(
            onDismissRequest = { mostrarNuevoBloqueo = false },
            title = { Text("Bloquear horario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandirEspacios,
                        onExpandedChange = { expandirEspacios = it }
                    ) {
                        OutlinedTextField(
                            value = espacios.firstOrNull { it.id == espacioBloqueo }?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Espacio") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirEspacios) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandirEspacios,
                            onDismissRequest = { expandirEspacios = false }
                        ) {
                            espacios.forEach { espacio ->
                                DropdownMenuItem(
                                    text = { Text(espacio.nombre) },
                                    onClick = {
                                        espacioBloqueo = espacio.id
                                        expandirEspacios = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = fechaBloqueo,
                        onValueChange = { fechaBloqueo = it },
                        label = { Text("Fecha (aaaa-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inicioBloqueo,
                            onValueChange = { inicioBloqueo = it },
                            label = { Text("Desde") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = finBloqueo,
                            onValueChange = { finBloqueo = it },
                            label = { Text("Hasta") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = motivoBloqueo,
                        onValueChange = { motivoBloqueo = it },
                        label = { Text("Motivo") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = espacioBloqueo.isNotBlank() && motivoBloqueo.isNotBlank(),
                    onClick = {
                        mostrarNuevoBloqueo = false
                        coroutineScope.launch {
                            val request = BloqueoRequestDto(
                                espacioId = espacioBloqueo,
                                fecha = fechaBloqueo,
                                horaInicio = inicioBloqueo,
                                horaTermino = finBloqueo,
                                motivo = motivoBloqueo.trim(),
                                creadoPor = viewModel.usuarioRemoto?.id ?: "ADMIN"
                            )
                            when (val r = viewModel.crearBloqueo(request)) {
                                is ApiResult.Ok -> {
                                    bloqueos = bloqueos + r.data
                                    mensajeExito = "Horario bloqueado"
                                    motivoBloqueo = ""
                                }
                                is ApiResult.Error -> mensajeError = r.mensaje
                            }
                        }
                    }
                ) { Text("Bloquear") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarNuevoBloqueo = false }) { Text("Cancelar") }
            }
        )
    }

    // ---------- Contenido ----------

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Panel de Inspectoría",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { recarga++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                }
            }
            Text(
                "${solicitudes.size} solicitud(es) pendiente(s) · ${bloqueos.size} bloqueo(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (mensajeError.isNotEmpty()) Text(mensajeError, color = MaterialTheme.colorScheme.error)
            if (mensajeExito.isNotEmpty()) Text(mensajeExito, color = Color(0xFF43A047))

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pendientes (${solicitudes.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Bloqueos (${bloqueos.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Espacios (${espacios.size})") })
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (tab == 0) {
                if (solicitudes.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No hay solicitudes pendientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(solicitudes, key = { it.idRemota ?: "local-${it.idLocal}" }) { solicitud ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(solicitud.espacioNombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text("Solicitante: ${solicitud.solicitanteNombre.ifBlank { "—" }} · ${solicitud.perfilSolicitante}")
                                    Text("Actividad: ${solicitud.actividad.ifBlank { "—" }}")
                                    Text("Curso/Grupo: ${solicitud.cursoGrupo.ifBlank { "—" }}")
                                    Text("Fecha: ${solicitud.fecha} | ${solicitud.horaInicio} - ${solicitud.horaTermino}")
                                    Text("Asistentes: ${solicitud.asistentes}")
                                    Text("Estado: ${solicitud.estado.uppercase()}", color = Color(0xFFFB8C00))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { justificacion = ""; rechazarSolicitud = solicitud },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                                        ) { Text("Rechazar") }
                                        Button(
                                            onClick = { aprobarSolicitud = solicitud },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                                        ) { Text("Aprobar") }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                if (bloqueos.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No hay horarios bloqueados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bloqueos, key = { it.id }) { bloqueo ->
                            val autor = if (bloqueo.creadoPor == viewModel.usuarioRemoto?.id) {
                                "Inspectoría (tú)"
                            } else {
                                bloqueo.creadoPor.take(8)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(nombreEspacio(bloqueo.espacioId), style = MaterialTheme.typography.titleMedium)
                                        Text("${bloqueo.fecha} · ${bloqueo.horaInicio} - ${bloqueo.horaTermino}")
                                        Text("Motivo: ${bloqueo.motivo}")
                                        Text("Registrado por: $autor", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { bloqueoAEliminar = bloqueo }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar bloqueo", tint = Color(0xFFE53935))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(espacios, key = { it.id }) { espacio ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(espacio.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("${espacio.tipo} · ${espacio.ubicacion}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Estado: ${espacio.estado}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when (espacio.estado) {
                                            "Disponible" -> Color(0xFF43A047)
                                            "Mantenimiento" -> Color(0xFFFB8C00)
                                            else -> Color(0xFFE53935)
                                        }
                                    )
                                }
                                TextButton(onClick = { espacioAEditar = espacio }) { Text("Cambiar") }
                            }
                        }
                    }
                }
            }
        }

        if (tab == 1 && !loading) {
            ExtendedFloatingActionButton(
                onClick = { mostrarNuevoBloqueo = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Bloquear horario") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}
