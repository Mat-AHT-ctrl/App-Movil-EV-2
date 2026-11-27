package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.model.Espacio
import com.example.myapplication.ui.components.BadgeEstadoEspacio
import com.example.myapplication.ui.components.TarjetaEspacio
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Catalogo(
    viewModel: AppViewModel,
    onEspacioSelected: (Espacio) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToMisReservas: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var espacios by remember { mutableStateOf(listOf<Espacio>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("Todos") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val resultado = viewModel.listarEspaciosBackend()
            when (resultado) {
                is ApiResult.Ok -> {
                    espacios = resultado.data
                    loading = false
                    errorMensaje = ""
                }
                is ApiResult.Error -> {
                    loading = false
                    errorMensaje = resultado.mensaje
                    // Fallback a Room local si el backend falla
                    espacios = viewModel.getAllEspacios()
                }
            }
        }
    }

    val tipos = remember(espacios.size) {
        listOf("Todos") + espacios.map { it.tipo }.distinct().sorted()
    }

    val espaciosFiltrados = espacios.filter { esp ->
        val coincideTexto = busqueda.isBlank() ||
            esp.nombre.contains(busqueda, ignoreCase = true) ||
            esp.ubicacion.contains(busqueda, ignoreCase = true) ||
            esp.tipo.contains(busqueda, ignoreCase = true)
        val coincideTipo = tipoSeleccionado == "Todos" || esp.tipo == tipoSeleccionado
        coincideTexto && coincideTipo
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Espacios Disponibles",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Buscador
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it; errorMensaje = "" },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar laboratorio, biblioteca, pabellón...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (busqueda.isNotEmpty()) {
                    IconButton(onClick = { busqueda = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Filtros tipo chip por tipo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tipos.forEach { tipo ->
                FilterChip(
                    selected = tipoSeleccionado == tipo,
                    onClick = { tipoSeleccionado = tipo },
                    label = { Text(tipo) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMensaje.isNotEmpty() && espacios.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Sin conexión al servidor", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Mostrando espacios guardados localmente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (errorMensaje.isNotEmpty()) {
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        } else if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (espaciosFiltrados.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No se encontraron espacios con ese criterio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(espaciosFiltrados, key = { it.id }) { espacio ->
                    TarjetaEspacio(
                        nombre = espacio.nombre,
                        tipo = espacio.tipo,
                        descripcion = espacio.descripcion,
                        ubicacion = espacio.ubicacion,
                        capacidad = espacio.capacidad,
                        estado = espacio.estado,
                        darkTheme = viewModel.darkTheme,
                        onClick = { onEspacioSelected(espacio) }
                    )
                }
            }
        }
    }
}
