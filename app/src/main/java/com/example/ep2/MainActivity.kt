package com.example.ep2

import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ep2.model.SolicitudReserva
import com.example.ep2.ui.screens.*
import com.example.ep2.ui.theme.EP2Theme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EP2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigator() {

    var currentScreen by remember { mutableStateOf("login") }
    var espacioSeleccionadoParaFormulario by remember { mutableStateOf("") }

    val listaSolicitudes = remember {
        mutableStateListOf(
            SolicitudReserva(
                id = "1",
                espacioId = "1",
                espacioNombre = "Laboratorio de Computación",
                solicitante = "Juan Pérez",
                perfil = "Docente",
                actividad = "Clase práctica de programación",
                cursoGrupo = "3° Medio A",
                asistentes = 25,
                fechaReserva = "2026-11-10",
                horaInicio = "10:00",
                horaTermino = "11:30",
                estadoSolicitud = "pendiente"
            )
        )
    }

    val mostrarBarras = currentScreen != "login" && currentScreen != "register"

    Scaffold(
        topBar = {
            if (mostrarBarras) {
                TopAppBar(
                    title = { Text("Escuela Marcela Paz - Reservas") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (mostrarBarras) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("🏠") },
                        label = { Text("Catálogo") },
                        selected = currentScreen == "catalogo",
                        onClick = { currentScreen = "catalogo" }
                    )
                    NavigationBarItem(
                        icon = { Text("📋") },
                        label = { Text("Admin") },
                        selected = currentScreen == "admin",
                        onClick = { currentScreen = "admin" }
                    )
                    NavigationBarItem(
                        icon = { Text("🚪") },
                        label = { Text("Salir") },
                        selected = false,
                        onClick = { currentScreen = "login" }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                "login" -> LoginScreen(
                    onLoginSuccess = { _ ->
                        currentScreen = "catalogo"
                    },
                    onNavigateToRegister = {
                        currentScreen = "register"
                    }
                )

                "register" -> RegisterScreen(
                    onRegisterSuccess = {
                        currentScreen = "login"
                    },
                    onBackToLogin = {
                        currentScreen = "login"
                    }
                )

                "catalogo" -> CatalogoScreen(
                    onEspacioSelected = { espacio ->
                        espacioSeleccionadoParaFormulario = espacio.nombre
                        currentScreen = "formulario"
                    },
                    onNavigateToAdmin = {
                        currentScreen = "admin"
                    },
                    onNavigateToMisReservas = {}
                )

                "formulario" -> FormularioScreen(
                    espacioNombre = espacioSeleccionadoParaFormulario,
                    onEnviarSolicitud = { actividad, cursoGrupo, asistentes, fecha, hInicio, hTermino ->
                        val nuevaSolicitud = SolicitudReserva(
                            id = System.currentTimeMillis().toString(),
                            espacioId = "0",
                            espacioNombre = espacioSeleccionadoParaFormulario,
                            solicitante = "Usuario Actual",
                            perfil = "Docente",
                            actividad = actividad,
                            cursoGrupo = cursoGrupo,
                            asistentes = asistentes.toIntOrNull() ?: 1,
                            fechaReserva = fecha,
                            horaInicio = hInicio,
                            horaTermino = hTermino,
                            estadoSolicitud = "pendiente"
                        )
                        listaSolicitudes.add(nuevaSolicitud)
                        currentScreen = "catalogo"
                    },
                    onVolver = {
                        currentScreen = "catalogo"
                    }
                )

                "admin" -> AdminScreen(
                    solicitudes = listaSolicitudes,
                    onAprobar = { id ->
                        val index = listaSolicitudes.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val item = listaSolicitudes[index]
                            listaSolicitudes[index] = item.copy(estadoSolicitud = "aceptada")
                        }
                    },
                    onRechazar = { id, justificacion ->
                        val index = listaSolicitudes.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val item = listaSolicitudes[index]
                            listaSolicitudes[index] = item.copy(
                                estadoSolicitud = "rechazada",
                                justificacionRechazo = justificacion
                            )
                        }
                    },
                    onVolver = {
                        currentScreen = "catalogo"
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onNavigateToRegister: () -> Unit) {
    TODO("Not yet implemented")
}