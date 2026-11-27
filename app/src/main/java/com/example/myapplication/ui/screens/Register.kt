package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(
    viewModel: AppViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("DOCENTE") }
    var expandedCargo by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val cargos = listOf("DOCENTE" to "Docente", "UTP" to "UTP", "PIE" to "PIE", "DIRECCION" to "Dirección", "INSPECTORIA" to "Inspectoría")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Registro de personal del colegio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it; errorMensaje = "" },
            label = { Text("Nombre completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMensaje = "" },
            label = { Text("Correo Electrónico") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMensaje = "" },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Cargo (perfil) - SIEMPRE queda como SOLICITANTE, nunca ADMIN
        ExposedDropdownMenuBox(
            expanded = expandedCargo,
            onExpandedChange = { expandedCargo = it }
        ) {
            OutlinedTextField(
                value = cargos.firstOrNull { it.first == cargo }?.second ?: "Docente",
                onValueChange = {},
                readOnly = true,
                label = { Text("Cargo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCargo) },
                modifier = Modifier.fillMaxWidth().menuAnchor(
                    MenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
            )
            ExposedDropdownMenu(
                expanded = expandedCargo,
                onDismissRequest = { expandedCargo = false }
            ) {
                cargos.forEach { (codigo, etiqueta) ->
                    DropdownMenuItem(
                        text = { Text(etiqueta) },
                        onClick = { cargo = codigo; expandedCargo = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "El acceso de administrador no se crea desde aquí. Solo el superusuario del sistema puede ser administrador.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (errorMensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
                    errorMensaje = "Todos los campos son obligatorios"
                } else if (password.length < 4) {
                    errorMensaje = "La contraseña debe tener al menos 4 caracteres"
                } else {
                    loading = true
                    errorMensaje = ""
                    coroutineScope.launch {
                        val resultado = viewModel.register(nombre, email, password, cargo)
                        loading = false
                        when (resultado) {
                            is ApiResult.Ok -> {
                                onRegisterSuccess()
                            }
                            is ApiResult.Error -> {
                                errorMensaje = resultado.mensaje
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Registrarse")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}