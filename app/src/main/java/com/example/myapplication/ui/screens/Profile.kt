package com.example.myapplication.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.myapplication.viewmodel.AppViewModel

@Composable
fun Profile(
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    onEditUser: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val usuario = viewModel.currentUser
    val context = LocalContext.current

    var darkTheme by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Leer preferencia de modo oscuro al iniciar
    LaunchedEffect(Unit) {
        val prefs: SharedPreferences = context.getSharedPreferences("app_colegio_prefs", Context.MODE_PRIVATE)
        darkTheme = prefs.getBoolean("dark_theme", false)
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Acerca de") },
            text = {
                Column {
                    Text(text = "App Colegio")
                    Text(text = "Versión 1.0.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Gestión escolar - Colegio Marcela Paz", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = usuario?.nombre ?: "Usuario del Colegio",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = usuario?.email ?: "estudiante@colegio.edu",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingRow(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                subtitle = "Recibir alertas de tareas",
                onClick = {
                    Toast.makeText(context, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                }
            )

            Divider()

            SettingRow(
                icon = Icons.Default.DarkMode,
                title = "Modo oscuro",
                subtitle = "Cambiar apariencia de la app",
                isToggle = true,
                isChecked = darkTheme,
                onClick = {},
                onCheckedChange = { checked ->
                    darkTheme = checked
                    onThemeChanged(checked)
                    val prefs: SharedPreferences = context.getSharedPreferences("app_colegio_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("dark_theme", checked).apply()
                }
            )

            Divider()

            SettingRow(
                icon = Icons.Default.Info,
                title = "Acerca de",
                subtitle = "Versión 1.0.0",
                onClick = { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEditUser,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("Editar perfil")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isToggle: Boolean = false,
    isChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isToggle) {
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        } else {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
