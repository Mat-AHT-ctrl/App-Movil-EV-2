package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.remote.ApiResult
import com.example.myapplication.viewmodel.AppViewModel
import com.example.myapplication.viewmodel.LoginResult
import kotlinx.coroutines.launch

@Composable
fun Login(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf("") }
    var exitoMensaje by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var mostrarReset by remember { mutableStateOf(false) }
    var pasoReset by remember { mutableStateOf(1) }
    var resetEmail by remember { mutableStateOf("") }
    var resetCodigo by remember { mutableStateOf("") }
    var resetNueva by remember { mutableStateOf("") }
    var resetConfirmar by remember { mutableStateOf("") }
    var codigoDemo by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }

    var mostrarCambio by remember { mutableStateOf(false) }
    var cambioEmail by remember { mutableStateOf("") }
    var cambioActual by remember { mutableStateOf("") }
    var cambioNueva by remember { mutableStateOf("") }
    var cambioConfirmar by remember { mutableStateOf("") }
    var cambioError by remember { mutableStateOf("") }
    var cambioLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Escuela Marcela Paz",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Portal de Reservas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (errorMensaje.isNotEmpty()) {
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (exitoMensaje.isNotEmpty()) {
            Text(
                text = exitoMensaje,
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it; errorMensaje = ""; exitoMensaje = "" },
            label = { Text("Correo o Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMensaje = ""; exitoMensaje = "" },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (usuario.isBlank() || password.isBlank()) {
                    errorMensaje = "Por favor, completa todos los campos"
                } else {
                    loading = true
                    coroutineScope.launch {
                        val resultado = viewModel.login(usuario, password)
                        loading = false
                        when (resultado) {
                            is LoginResult.Ok -> onLoginSuccess()
                            is LoginResult.CredencialesInvalidas ->
                                errorMensaje = "Correo o contraseña incorrectos"
                            is LoginResult.Error -> errorMensaje = resultado.mensaje
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
                Text("Ingresar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                resetEmail = usuario
                resetCodigo = ""
                resetNueva = ""
                resetConfirmar = ""
                codigoDemo = ""
                resetError = ""
                pasoReset = 1
                mostrarReset = true
            }) {
                Text("¿Olvidaste tu contraseña?")
            }
            TextButton(onClick = {
                cambioEmail = usuario
                cambioActual = ""
                cambioNueva = ""
                cambioConfirmar = ""
                cambioError = ""
                mostrarCambio = true
            }) {
                Text("Cambiar contraseña")
            }
        }
    }

    // ---------- Recuperación en 2 pasos ----------
    if (mostrarReset) {
        AlertDialog(
            onDismissRequest = { if (!resetLoading) mostrarReset = false },
            title = { Text("Recuperar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pasoReset == 1) {
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it; resetError = "" },
                            label = { Text("Correo electrónico") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (codigoDemo.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Código de verificación (demo): $codigoDemo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (resetError.isNotEmpty()) {
                            Text(text = resetError, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                if (resetEmail.isBlank()) {
                                    resetError = "Ingresa tu correo electrónico."
                                } else {
                                    resetLoading = true
                                    coroutineScope.launch {
                                        when (val r = viewModel.solicitarResetPassword(resetEmail)) {
                                            is ApiResult.Ok -> {
                                                codigoDemo = r.data.codigoDemo ?: ""
                                                pasoReset = 2
                                            }
                                            is ApiResult.Error -> resetError = r.mensaje
                                        }
                                        resetLoading = false
                                    }
                                }
                            },
                            enabled = !resetLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (resetLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Solicitar código")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = resetCodigo,
                            onValueChange = { resetCodigo = it; resetError = "" },
                            label = { Text("Código de 6 dígitos") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = resetNueva,
                            onValueChange = { resetNueva = it; resetError = "" },
                            label = { Text("Nueva contraseña (mín. 6)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = resetConfirmar,
                            onValueChange = { resetConfirmar = it; resetError = "" },
                            label = { Text("Confirmar contraseña") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (resetError.isNotEmpty()) {
                            Text(text = resetError, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                when {
                                    resetCodigo.isBlank() -> resetError = "Ingresa el código recibido."
                                    resetNueva.length < 6 -> resetError = "La contraseña debe tener al menos 6 caracteres."
                                    resetNueva != resetConfirmar -> resetError = "Las contraseñas no coinciden."
                                    else -> {
                                        resetLoading = true
                                        coroutineScope.launch {
                                            when (val r = viewModel.confirmarResetPassword(
                                                resetEmail, resetCodigo, resetNueva
                                            )) {
                                                is ApiResult.Ok -> {
                                                    mostrarReset = false
                                                    usuario = resetEmail
                                                    password = ""
                                                    exitoMensaje = "Contraseña restablecida. Ya puedes iniciar sesión."
                                                }
                                                is ApiResult.Error -> resetError = r.mensaje
                                            }
                                            resetLoading = false
                                        }
                                    }
                                }
                            },
                            enabled = !resetLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restablecer contraseña")
                        }
                        TextButton(onClick = { pasoReset = 1 }) {
                            Text("Volver")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarReset = false }) { Text("Cerrar") }
            }
        )
    }

    // ---------- Cambio directo ----------
    if (mostrarCambio) {
        AlertDialog(
            onDismissRequest = { if (!cambioLoading) mostrarCambio = false },
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cambioEmail,
                        onValueChange = { cambioEmail = it; cambioError = "" },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cambioActual,
                        onValueChange = { cambioActual = it; cambioError = "" },
                        label = { Text("Contraseña actual") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cambioNueva,
                        onValueChange = { cambioNueva = it; cambioError = "" },
                        label = { Text("Nueva contraseña (mín. 6)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cambioConfirmar,
                        onValueChange = { cambioConfirmar = it; cambioError = "" },
                        label = { Text("Confirmar nueva contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (cambioError.isNotEmpty()) {
                        Text(text = cambioError, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            when {
                                cambioEmail.isBlank() || cambioActual.isBlank() ->
                                    cambioError = "Completa el correo y la contraseña actual."
                                cambioNueva.length < 6 ->
                                    cambioError = "La nueva contraseña debe tener al menos 6 caracteres."
                                cambioNueva != cambioConfirmar ->
                                    cambioError = "Las contraseñas no coinciden."
                                else -> {
                                    cambioLoading = true
                                    coroutineScope.launch {
                                        when (val r = viewModel.cambiarPassword(
                                            cambioEmail, cambioActual, cambioNueva
                                        )) {
                                            is ApiResult.Ok -> {
                                                mostrarCambio = false
                                                usuario = cambioEmail
                                                password = ""
                                                exitoMensaje = "Contraseña actualizada. Ya puedes iniciar sesión."
                                            }
                                            is ApiResult.Error -> cambioError = r.mensaje
                                        }
                                        cambioLoading = false
                                    }
                                }
                            }
                        },
                        enabled = !cambioLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (cambioLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Actualizar contraseña")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarCambio = false }) { Text("Cerrar") }
            }
        )
    }
}
