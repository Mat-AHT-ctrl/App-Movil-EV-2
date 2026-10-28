package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.screens.Admin
import com.example.myapplication.ui.screens.Catalogo
import com.example.myapplication.ui.screens.File
import com.example.myapplication.ui.screens.Formulario
import com.example.myapplication.ui.screens.Login
import com.example.myapplication.ui.screens.MisReservas
import com.example.myapplication.ui.screens.Register
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppNavigator(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigator(viewModel: AppViewModel) {
    val mostrarBarras = viewModel.currentScreen != "login" && viewModel.currentScreen != "register"

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
                        selected = viewModel.currentScreen == "catalogo",
                        onClick = { viewModel.currentScreen = "catalogo" }
                    )
                    if (viewModel.esAdmin) {
                        NavigationBarItem(
                            icon = { Text("📋") },
                            label = { Text("Admin") },
                            selected = viewModel.currentScreen == "admin",
                            onClick = { viewModel.currentScreen = "admin" }
                        )
                    }
                    NavigationBarItem(
                        icon = { Text("📁") },
                        label = { Text("Mis Reservas") },
                        selected = viewModel.currentScreen == "misReservas",
                        onClick = { viewModel.currentScreen = "misReservas" }
                    )
                    NavigationBarItem(
                        icon = { Text("📄") },
                        label = { Text("Archivos") },
                        selected = viewModel.currentScreen == "files",
                        onClick = { viewModel.currentScreen = "files" }
                    )
                    NavigationBarItem(
                        icon = { Text("🚪") },
                        label = { Text("Salir") },
                        selected = false,
                        onClick = {
                            viewModel.logout()
                        }
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
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it / 4 }) togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it / 4 })
                },
                label = "transicion_pantallas"
            ) { screen ->
                when (screen) {
                    "login" -> Login(
                        viewModel = viewModel,
                        onLoginSuccess = { viewModel.currentScreen = "catalogo" },
                        onNavigateToRegister = { viewModel.currentScreen = "register" }
                    )
                    "register" -> Register(
                        viewModel = viewModel,
                        onRegisterSuccess = { viewModel.currentScreen = "login" },
                        onBackToLogin = { viewModel.currentScreen = "login" }
                    )
                    "catalogo" -> Catalogo(
                        viewModel = viewModel,
                        onEspacioSelected = { espacio ->
                            viewModel.espacioSeleccionado = espacio
                            viewModel.currentScreen = "formulario"
                        },
                        onNavigateToAdmin = { viewModel.currentScreen = "admin" },
                        onNavigateToMisReservas = { viewModel.currentScreen = "misReservas" }
                    )
                    "formulario" -> if (viewModel.espacioSeleccionado != null) {
                        Formulario(
                            espacio = viewModel.espacioSeleccionado!!,
                            viewModel = viewModel,
                            onEnviarSolicitud = {
                                viewModel.currentScreen = "catalogo"
                                viewModel.espacioSeleccionado = null
                            },
                            onVolver = {
                                viewModel.currentScreen = "catalogo"
                                viewModel.espacioSeleccionado = null
                            }
                        )
                    } else {
                        Catalogo(
                            viewModel = viewModel,
                            onEspacioSelected = { espacio ->
                                viewModel.espacioSeleccionado = espacio
                                viewModel.currentScreen = "formulario"
                            },
                            onNavigateToAdmin = { viewModel.currentScreen = "admin" },
                            onNavigateToMisReservas = { viewModel.currentScreen = "misReservas" }
                        )
                    }
                    "admin" -> Admin(
                        viewModel = viewModel,
                        onVolver = { viewModel.currentScreen = "catalogo" }
                    )
                    "misReservas" -> MisReservas(
                        viewModel = viewModel,
                        onVolver = { viewModel.currentScreen = "catalogo" }
                    )
                    "files" -> File(
                        viewModel = viewModel,
                        onBack = { viewModel.currentScreen = "catalogo" }
                    )
                }
            }
        }
    }
}
