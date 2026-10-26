package com.example.ep2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ep2.model.Espacio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    onEspacioSelected: (Espacio) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToMisReservas: () -> Unit
) {
    val listaEspacios = listOf(
        Espacio("1", "Laboratorio de Computacion", "Laboratorio", "Equipado con PCs para estudiantes", "Edificio A", 30, "08:00 - 18:00", "disponible"),
        Espacio("2", "Biblioteca", "Biblioteca", "Espacio de lectura y estudio", "Edificio Central", 40, "08:30 - 17:00", "disponible"),
        Espacio("3", "Sala Sensorial", "Apoyo", "Sala de integracion y apoyo PIE", "Edificio B", 15, "09:00 - 17:30", "disponible"),
        Espacio("4", "Centro de Innovacion CIEMP", "Tecnologico", "Espacio de innovacion y proyectos", "Edificio C", 25, "08:00 - 17:30", "disponible"),
        Espacio("5", "Sala de Reuniones o Capacitacion", "Reuniones", "Sala para reuniones de docentes y UTP", "Administracion", 20, "08:00 - 18:00", "disponible"),
        Espacio("6", "Huerto Escolar", "Recreativo", "Espacio al aire libre para actividades cientificas", "Patio Trasterior", 35, "09:00 - 15:00", "disponible")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catalogo de Espacios - Marcela Paz")},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Button(onClick = onNavigateToMisReservas) {
                        Text("Mis Reservas")
                    }
                    OutlinedButton(onClick = onNavigateToAdmin) {
                        Text("Vista Inspectoria (Admin)")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Seleccione un espacio para solicitar su reserva:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(listaEspacios) { espacio ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = espacio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tipo: ${espacio.tipo} | Capacidad: ${espacio.capacidad} personas",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = espacio.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Button(
                            onClick = { onEspacioSelected(espacio) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Solicitar Reserva")
                        }
                    }

                }
            }
        }
    }
}