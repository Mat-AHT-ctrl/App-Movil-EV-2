package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.Espacio

@Composable
fun CatalogoScreen(
    onEspacioSelected: (Espacio) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToMisReservas: () -> Unit
) {
    val espacios = listOf(
        Espacio("1", "Laboratorio de Computación", "Laboratorio", "Equipado con PCs para estudiantes", "Edificio A", 30, "08:00 - 18:00", "disponible"),
        Espacio("2", "Biblioteca", "Biblioteca", "Espacio de lectura y estudio", "Edificio Central", 40, "08:30 - 17:00", "disponible"),
        Espacio("3", "Sala Sensorial", "Apoyo", "Sala de integracion y apoyo PIE", "Edificio B", 15, "09:00 - 17:30", "disponible"),
        Espacio("4", "Centro de Innovacion CIEMP", "Tecnologico", "Espacio de innovacion y proyectos", "Edificio C", 25, "08:00 - 17:30", "disponible"),
        Espacio("5", "Sala de Reuniones o Capacitacion", "Reuniones", "Sala para reuniones de docentes y UTP", "Administracion", 20, "08:00 - 18:00", "disponible"),
        Espacio("6", "Huerto Escolar", "Recreativo", "Espacio al aire libre para actividades cientificas", "Patio Trasterior", 35, "09:00 - 15:00", "disponible")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Espacios Disponibles",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(espacios) { espacio ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEspacioSelected(espacio) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = espacio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ubicación: ${espacio.ubicacion} | Capacidad: ${espacio.capacidad} pers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = espacio.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}