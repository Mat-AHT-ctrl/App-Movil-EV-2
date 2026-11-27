package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.viewmodel.AppViewModel

@Composable
fun Profile(
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usuario = viewModel.currentUser
    var pendientes by remember { mutableStateOf(0) }
    var aprobadas by remember { mutableStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val reservas = viewModel.sincronizarMisReservas()
        pendientes = reservas.count { it.estado.equals("Pendiente", ignoreCase = true) }
        aprobadas = reservas.count { it.estado.equals("Aprobada", ignoreCase = true) }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Acerca de") },
            text = {
                Column {
                    Text("Escuela Escritora Marcela Paz · Portal de Reservas")
                    Text("Versión 1.0 · DSY1105", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Aceptar") } }
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que iniciar sesión nuevamente para reservar.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onLogout() }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Perfil",
                    modifier = Modifier.padding(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = usuario?.nombre ?: "Usuario del Colegio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = usuario?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = viewModel.usuarioRemoto?.perfil ?: usuario?.rol?.uppercase() ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                valor = pendientes.toString(),
                etiqueta = "Pendientes",
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                valor = aprobadas.toString(),
                etiqueta = "Aprobadas",
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingRow(
            icon = Icons.Default.DarkMode,
            title = "Modo oscuro",
            subtitle = "Cambiar apariencia de la app",
            isToggle = true,
            checked = viewModel.darkTheme,
            onCheckedChange = { viewModel.cambiarTema(it) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingRow(
            icon = Icons.Default.Info,
            title = "Acerca de",
            subtitle = "Versión 1.0",
            onClick = { showAbout = true }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { confirmarSalida = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun StatCard(
    valor: String,
    etiqueta: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    isToggle: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isToggle) {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        } else if (onClick != null) {
            TextButton(onClick = onClick) { Text("Ver") }
        }
    }
}
