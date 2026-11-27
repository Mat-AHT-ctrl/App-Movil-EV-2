package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.screens.Admin
import com.example.myapplication.ui.screens.Catalogo
import com.example.myapplication.ui.screens.DetalleEspacioScreen
import com.example.myapplication.ui.screens.File
import com.example.myapplication.ui.screens.Formulario
import com.example.myapplication.ui.screens.Login
import com.example.myapplication.ui.screens.MisReservas
import com.example.myapplication.ui.screens.Profile
import com.example.myapplication.ui.screens.Register
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(
                darkTheme = viewModel.darkTheme
            ) {
                MainAppNavigator(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigator(viewModel: AppViewModel) {
    val mostrarBarras = viewModel.currentScreen != "login" && viewModel.currentScreen != "register"
    var mostrarNotificaciones by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel.currentScreen) {
        viewModel.cargarNotificaciones()
    }

    BackHandler(enabled = viewModel.currentScreen !in listOf("login", "catalogo")) {
        when (viewModel.currentScreen) {
            "formulario" -> viewModel.currentScreen = "detalleEspacio"
            "detalleEspacio" -> {
                viewModel.espacioSeleccionado = null
                viewModel.currentScreen = "catalogo"
            }
            "register" -> viewModel.currentScreen = "login"
            "admin", "misReservas", "files", "perfil" -> viewModel.currentScreen = "catalogo"
            else -> viewModel.currentScreen = "catalogo"
        }
    }

    Scaffold(
        topBar = {
            if (mostrarBarras) {
                TopAppBar(
                    title = { Text("Escuela Marcela Paz - Reservas") },
                    actions = {
                        IconButton(onClick = { mostrarNotificaciones = true }) {
                            BadgedBox(
                                badge = {
                                    if (viewModel.conteoNoLeidas > 0L) {
                                        Badge { Text(viewModel.conteoNoLeidas.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notificaciones"
                                )
                            }
                        }
                    },
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
                        icon = { Text("👤") },
                        label = { Text("Perfil") },
                        selected = viewModel.currentScreen == "perfil",
                        onClick = { viewModel.currentScreen = "perfil" }
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
                            viewModel.currentScreen = "detalleEspacio"
                        },
                        onNavigateToAdmin = { viewModel.currentScreen = "admin" },
                        onNavigateToMisReservas = { viewModel.currentScreen = "misReservas" }
                    )
                    "detalleEspacio" -> if (viewModel.espacioSeleccionado != null) {
                        DetalleEspacioScreen(
                            espacio = viewModel.espacioSeleccionado!!,
                            viewModel = viewModel,
                            onSolicitar = { fecha, horaInicio, horaTermino ->
                                viewModel.reservaFecha = fecha
                                viewModel.reservaHoraInicio = horaInicio
                                viewModel.reservaHoraTermino = horaTermino
                                viewModel.currentScreen = "formulario"
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
                                viewModel.currentScreen = "detalleEspacio"
                            },
                            onNavigateToAdmin = { viewModel.currentScreen = "admin" },
                            onNavigateToMisReservas = { viewModel.currentScreen = "misReservas" }
                        )
                    }
                    "formulario" -> if (viewModel.espacioSeleccionado != null) {
                        Formulario(
                            espacio = viewModel.espacioSeleccionado!!,
                            viewModel = viewModel,
                            onEnviarSolicitud = {
                                viewModel.currentScreen = "catalogo"
                                viewModel.espacioSeleccionado = null
                            },
                            onVolver = {
                                viewModel.currentScreen = "detalleEspacio"
                            }
                        )
                    } else {
                        Catalogo(
                            viewModel = viewModel,
                            onEspacioSelected = { espacio ->
                                viewModel.espacioSeleccionado = espacio
                                viewModel.currentScreen = "detalleEspacio"
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
                    "perfil" -> Profile(
                        viewModel = viewModel,
                        onLogout = { viewModel.logout() }
                    )
                }
            }
        }
    }

    if (mostrarNotificaciones) {
        AlertDialog(
            onDismissRequest = { mostrarNotificaciones = false },
            title = { Text("Notificaciones") },
            text = {
                if (viewModel.notificaciones.isEmpty()) {
                    Text("No tienes notificaciones.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(viewModel.notificaciones, key = { it.id }) { notif ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!notif.leida) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        if (!notif.leida) viewModel.marcarNotificacionLeida(notif.id)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = notif.titulo,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = notif.mensaje,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!notif.fechaEnvio.isNullOrBlank()) {
                                        Text(
                                            text = notif.fechaEnvio,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!notif.leida) {
                                        Text(
                                            text = "No leída",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarNotificaciones = false }) {
                    Text("Cerrar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    coroutineScope.launch { viewModel.cargarNotificaciones() }
                }) {
                    Text("Actualizar")
                }
            }
        )
    }
}
