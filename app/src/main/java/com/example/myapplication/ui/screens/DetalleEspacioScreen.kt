package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.data.remote.BackendRepository
import com.example.myapplication.model.Espacio
import com.example.myapplication.ui.components.BadgeEstadoEspacio
import com.example.myapplication.viewmodel.AppViewModel
import com.example.myapplication.utils.DateTimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleEspacioScreen(
    espacio: Espacio,
    viewModel: AppViewModel,
    onSolicitar: (fecha: String, horaInicio: String, horaTermino: String) -> Unit,
    onVolver: () -> Unit
) {
    val darkTheme = viewModel.darkTheme

    var fechaSeleccionada by remember {
        mutableStateOf(DateTimeUtils.fechaMinimaHabil(viewModel.feriadosChile))
    }
    var indicesSeleccionados by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorBloques by remember { mutableStateOf<String?>(null) }
    var reservasExistentes by remember { mutableStateOf(emptyList<String>()) }

    val partesHorario = espacio.horarioFuncionamiento.split(" - ")
    val horaApertura = partesHorario.getOrNull(0)?.trim()?.takeIf { it.isNotBlank() } ?: "08:00"
    val horaCierre = partesHorario.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: "18:00"

    val horariosDisponibles = remember(horaApertura, horaCierre) {
        DateTimeUtils.generarHorariosBloques(horaApertura, horaCierre)
    }

    val proximosDias = remember {
        (0..13).map { LocalDate.now().plusDays(it.toLong()) }
    }

    val fechaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val fechaDisplayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val bloques = remember(horariosDisponibles, fechaSeleccionada, reservasExistentes.size) {
        horariosDisponibles.mapIndexed { index, rango ->
            val partes = rango.split(" - ")
            val inicio = LocalTime.parse(partes[0])
            val termino = LocalTime.parse(partes[1])
            val ocupado = reservasExistentes.any { reserva ->
                val resInicio = LocalTime.parse(reserva.split(" - ")[0])
                val resTermino = LocalTime.parse(reserva.split(" - ")[1])
                !(termino.isBefore(resInicio) || inicio.isAfter(resTermino))
            }
            Triple(index, rango, ocupado)
        }
    }

    LaunchedEffect(fechaSeleccionada) {
        val fechaDb = fechaSeleccionada.format(fechaDbFormatter)
        val rangos = mutableListOf<String>()

        // Reservas locales del solicitante.
        viewModel.getReservasByEspacioYFecha(espacio.id, fechaDb).forEach { r ->
            rangos += "${r.horaInicio} - ${r.horaTermino}"
        }

        // Bloqueos de Inspectoría.
        when (val resultado = viewModel.listarBloqueos()) {
            is ApiResult.Ok -> resultado.data
                .filter { it.espacioId == espacio.id && it.fecha == fechaDb }
                .forEach { b -> rangos += "${b.horaInicio} - ${b.horaTermino}" }
            is ApiResult.Error -> Unit
        }

        // Reservas remotas de otros docentes (excepto rechazadas/canceladas).
        when (val resultado = BackendRepository().listarReservas()) {
            is ApiResult.Ok -> resultado.data
                .filter {
                    it.espacioId == espacio.id &&
                            it.fecha == fechaDb &&
                            it.estado.uppercase() !in setOf("RECHAZADA", "CANCELADA")
                }
                .forEach { r -> rangos += "${r.horaInicio} - ${r.horaTermino}" }
            is ApiResult.Error -> Unit
        }

        reservasExistentes = rangos.distinct()
    }

    val puedeReservar = espacio.estado.uppercase() == "DISPONIBLE"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Detalle: ${espacio.nombre}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${espacio.tipo} · ${espacio.ubicacion}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    BadgeEstadoEspacio(estado = espacio.estado, darkTheme = darkTheme)
                }
                Text(
                    text = espacio.descripcion,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Capacidad máxima: ${espacio.capacidad} personas · Horario: ${espacio.horarioFuncionamiento}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!puedeReservar) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "Este espacio está '${espacio.estado}'. No admite nuevas solicitudes en este momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Selecciona fecha (mín. 1 día hábil de anticipación)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                proximosDias.forEach { dia ->
                    val esHabil = DateTimeUtils.esDiaHabil(dia, viewModel.feriadosChile) &&
                        !dia.isBefore(DateTimeUtils.fechaMinimaHabil(viewModel.feriadosChile))
                    val seleccionado = dia == fechaSeleccionada
                    val nombreDia = dia.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "CL"))
                        .replaceFirstChar { it.uppercase() }

                    Surface(
                        modifier = Modifier
                            .width(72.dp)
                            .clickable(enabled = esHabil) {
                                fechaSeleccionada = dia
                                indicesSeleccionados = emptyList()
                                errorBloques = null
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            seleccionado -> MaterialTheme.colorScheme.primary
                            esHabil -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (seleccionado) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = nombreDia,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    seleccionado -> MaterialTheme.colorScheme.onPrimary
                                    esHabil -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                            Text(
                                text = "${dia.dayOfMonth}/${dia.monthValue}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    seleccionado -> MaterialTheme.colorScheme.onPrimary
                                    esHabil -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                            if (!esHabil) {
                                Text(
                                    text = "No hábil",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Bloques horarios (45 min) · ${fechaSeleccionada.format(fechaDisplayFormatter)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        val errorMsg = errorBloques
        if (errorMsg != null) {
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(bloques, key = { it.first }) { (index, rango, ocupado) ->
                val estaSeleccionado = indicesSeleccionados.contains(index)
                val habilitado = puedeReservar && !ocupado

                val bgColor = when {
                    estaSeleccionado -> MaterialTheme.colorScheme.primaryContainer
                    ocupado -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    estaSeleccionado -> MaterialTheme.colorScheme.primary
                    ocupado -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = habilitado) {
                            errorBloques = null
                            if (estaSeleccionado) {
                                if (index == indicesSeleccionados.firstOrNull() || index == indicesSeleccionados.lastOrNull()) {
                                    indicesSeleccionados = indicesSeleccionados - index
                                } else {
                                    errorBloques = "Para mantener bloques contiguos, deselecciona desde el inicio o el final."
                                }
                            } else {
                                if (indicesSeleccionados.isEmpty()) {
                                    indicesSeleccionados = listOf(index)
                                } else {
                                    val min = indicesSeleccionados.minOrNull() ?: 0
                                    val max = indicesSeleccionados.maxOrNull() ?: 0
                                    if (index == min - 1 || index == max + 1) {
                                        indicesSeleccionados = (indicesSeleccionados + index).sorted()
                                    } else {
                                        errorBloques = "Los bloques seleccionados deben ser consecutivos."
                                    }
                                }
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rango,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (ocupado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (ocupado) {
                                Text(
                                    text = "Ocupado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = if (estaSeleccionado) "Seleccionado" else "Disponible",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        when {
                            estaSeleccionado -> Icon(
                                Icons.Default.Check,
                                contentDescription = "Seleccionado",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            ocupado -> Icon(
                                Icons.Default.Cancel,
                                contentDescription = "No disponible",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (puedeReservar && indicesSeleccionados.isNotEmpty()) {
            val primero = horariosDisponibles[indicesSeleccionados.first()]
            val ultimo = horariosDisponibles[indicesSeleccionados.last()]
            val horaInicio = primero.split(" - ")[0]
            val horaTermino = DateTimeUtils.calcularHoraTermino(ultimo)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${indicesSeleccionados.size} bloque${if (indicesSeleccionados.size > 1) "s" else ""} · $horaInicio a $horaTermino",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = fechaSeleccionada.format(fechaDisplayFormatter),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Button(
                        onClick = {
                            onSolicitar(
                                fechaSeleccionada.format(fechaDisplayFormatter),
                                horaInicio,
                                horaTermino
                            )
                        },
                        enabled = true,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Solicitar")
                    }
                }
            }
        } else if (puedeReservar && indicesSeleccionados.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selecciona 1 o más bloques contiguos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

}
