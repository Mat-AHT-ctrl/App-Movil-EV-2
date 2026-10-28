package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.ArchivoItem
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.screens.AdminScreen
import com.example.myapplication.ui.screens.CatalogoScreen
import com.example.myapplication.ui.screens.FormularioScreen
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.screens.RegisterScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

// Modelos necesarios
data class SolicitudReserva(
    val id: String,
    val espacioId: String,
    val espacioNombre: String,
    val solicitante: String,
    val perfil: String,
    val actividad: String,
    val cursoGrupo: String,
    val asistentes: Int,
    val fechaReserva: String,
    val horaInicio: String,
    val horaTermino: String,
    val estadoSolicitud: String,
    val justificacionRechazo: String = ""
)

data class Espacio(
    val id: String,
    val nombre: String,
    val descripcion: String
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                Toast.makeText(this, "No se puede acceder al archivo", Toast.LENGTH_SHORT).show()
            }
            val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "Archivo"
            viewModel.agregarArchivo(fileName, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppNavigator()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            val path = uri.path
            if (path != null) {
                val cut = path.lastIndexOf('/')
                name = if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }

    private fun abrirArchivo(archivo: ArchivoItem) {
        val uri = archivo.uri
        if (uri == null) {
            Toast.makeText(this, "No se puede abrir el archivo", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, "Abrir con"))
        } catch (_: Exception) {
            Toast.makeText(this, "No hay aplicaciones para abrir este archivo", Toast.LENGTH_SHORT).show()
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
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it / 4 }) togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -it / 4 })
                },
                label = "transicion_pantallas"
            ) { screen ->
                when (screen) {
                    "login" -> LoginScreen(
                        onLoginSuccess = { _ -> currentScreen = "catalogo" },
                        onNavigateToRegister = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        onRegisterSuccess = { currentScreen = "login" },
                        onBackToLogin = { currentScreen = "login" }
                    )
                    "catalogo" -> CatalogoScreen(
                        onEspacioSelected = { espacio ->
                            espacioSeleccionadoParaFormulario = espacio.nombre
                            currentScreen = "formulario"
                        },
                        onNavigateToAdmin = { currentScreen = "admin" },
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
                        onVolver = { currentScreen = "catalogo" }
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
                        onRechazar = { id, _ ->
                            val index = listaSolicitudes.indexOfFirst { it.id == id }
                            if (index != -1) {
                                val item = listaSolicitudes[index]
                                listaSolicitudes[index] = item.copy(
                                    estadoSolicitud = "rechazada",
                                    justificacionRechazo = "No disponible"
                                )
                            }
                        },
                        onVolver = { currentScreen = "catalogo" }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Escuela Marcela Paz", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Portal de Reservas", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it; errorMensaje = "" },
            label = { Text("Correo o Usuario") },
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

        if (errorMensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (usuario.isBlank() || password.isBlank()) {
                    errorMensaje = "Por favor, completa todos los campos"
                } else {
                    onLoginSuccess(usuario)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it; errorMensaje = "" },
            label = { Text("Nombre de Usuario") },
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

        if (errorMensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (usuario.isBlank() || password.isBlank()) {
                    errorMensaje = "Todos los campos son obligatorios"
                } else if (password.length < 4) {
                    errorMensaje = "La contraseña debe tener al menos 4 caracteres"
                } else {
                    onRegisterSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

@Composable
fun CatalogoScreen(
    onEspacioSelected: (Espacio) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToMisReservas: () -> Unit
) {
    val espacios = listOf(
        Espacio("1", "Laboratorio de Computación", "Equipado con 30 PCs, proyector y red de alta velocidad."),
        Espacio("2", "Gimnasio Techado", "Cancha multideporte para básquetbol, vóleibol y eventos escolares."),
        Espacio("3", "Sala Audiovisual", "Butacas cómodas, pantalla gigante y sistema de sonido surround."),
        Espacio("4", "Biblioteca Central", "Espacio silencioso para estudio y lectura grupal o individual.")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Espacios Disponibles", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(espacios) { espacio ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onEspacioSelected(espacio) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = espacio.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = espacio.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun FormularioScreen(
    espacioNombre: String,
    onEnviarSolicitud: (String, String, String, String, String, String) -> Unit,
    onVolver: () -> Unit
) {
    var actividad by remember { mutableStateOf("") }
    var cursoGrupo by remember { mutableStateOf("") }
    var asistentes by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("2026-11-15") }
    var horaInicio by remember { mutableStateOf("10:00") }
    var horaTermino by remember { mutableStateOf("11:30") }
    var errorMensaje by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Reservar: $espacioNombre", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)

        OutlinedTextField(value = actividad, onValueChange = { actividad = it; errorMensaje = "" }, label = { Text("Actividad o Motivo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = cursoGrupo, onValueChange = { cursoGrupo = it; errorMensaje = "" }, label = { Text("Curso o Grupo (ej: 3° Medio A)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = asistentes, onValueChange = { asistentes = it; errorMensaje = "" }, label = { Text("Cantidad de Asistentes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = horaInicio, onValueChange = { horaInicio = it }, label = { Text("Hora Inicio") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = horaTermino, onValueChange = { horaTermino = it }, label = { Text("Hora Término") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        if (errorMensaje.isNotEmpty()) {
            Text(text = errorMensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (actividad.isBlank() || cursoGrupo.isBlank() || asistentes.isBlank()) {
                    errorMensaje = "Por favor, completa los campos obligatorios"
                } else if (asistentes.toIntOrNull() == null || asistentes.toInt() <= 0) {
                    errorMensaje = "Ingresa un número válido de asistentes"
                } else {
                    onEnviarSolicitud(actividad, cursoGrupo, asistentes, fecha, horaInicio, horaTermino)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar Solicitud de Reserva")
        }

        TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar y Volver")
        }
    }
}

@Composable
fun AdminScreen(
    solicitudes: List<SolicitudReserva>,
    onAprobar: (String) -> Unit,
    onRechazar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Panel de Inspectoría", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        if (solicitudes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No hay solicitudes registradas", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(solicitudes) { solicitud ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = solicitud.espacioNombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Solicitante: ${solicitud.solicitante} (${solicitud.perfil})")
                            Text(text = "Actividad: ${solicitud.actividad} | Curso: ${solicitud.cursoGrupo}")
                            Text(text = "Fecha: ${solicitud.fechaReserva} | ${solicitud.horaInicio} - ${solicitud.horaTermino}")
                            Text(
                                text = "Estado: ${solicitud.estadoSolicitud.uppercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = when (solicitud.estadoSolicitud) {
                                    "aceptada" -> Color(0xFF43A047)
                                    "rechazada" -> Color(0xFFE53935)
                                    else -> Color(0xFFFB8C00)
                                }
                            )

                            if (solicitud.estadoSolicitud == "pendiente") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onAprobar(solicitud.id) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                                    ) {
                                        Text("Aprobar")
                                    }
                                    Button(
                                        onClick = { onRechazar(solicitud.id, "No disponible") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                                    ) {
                                        Text("Rechazar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al Catálogo")
        }
    }
}