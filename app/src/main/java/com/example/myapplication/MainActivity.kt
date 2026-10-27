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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ep2.LoginScreen
import com.example.ep2.RegisterScreen
import com.example.ep2.model.SolicitudReserva
import com.example.ep2.ui.screens.AdminScreen
import com.example.ep2.ui.screens.CatalogoScreen
import com.example.ep2.ui.screens.FormularioScreen
import com.example.myapplication.model.ArchivoItem
import com.example.myapplication.model.TipoArchivo
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

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
                val selectedTab = viewModel.selectedTab.intValue

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("App Colegio") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "Archivos") },
                                label = { Text("Archivos") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        if (selectedTab == 0) {
                            HomeScreen(
                                modifier = Modifier.weight(1f),
                                onGoToFiles = { viewModel.selectTab(1) }
                            )
                        } else {
                            ArchivosScreen(
                                archivos = viewModel.archivos,
                                onAddFile = { openDocument.launch(arrayOf("*/*")) },
                                onDeleteFile = { archivo -> viewModel.eliminarArchivo(archivo) },
                                onOpenFile = { archivo -> abrirArchivo(archivo) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
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

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onGoToFiles: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bienvenido a App Colegio",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gestión de documentos y archivos escolares",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGoToFiles) {
            Text("Ver Archivos")
        }
    }
}

@Composable
private fun ArchivosScreen(
    archivos: List<ArchivoItem>,
    onAddFile: () -> Unit,
    onDeleteFile: (ArchivoItem) -> Unit,
    onOpenFile: (ArchivoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Archivos",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAddFile,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cargar archivo (PDF/Word/etc.)")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "${archivos.size} archivo(s)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (archivos.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archivos, key = { it.id }) { archivo ->
                    FileCard(
                        archivo = archivo,
                        onDelete = { onDeleteFile(archivo) },
                        onClick = { onOpenFile(archivo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No hay archivos",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Carga tu primer documento PDF, Word, Imagen u otro",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileCard(
    archivo: ArchivoItem,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val (icon, iconColor, tipoNombre) = when (archivo.tipo) {
        TipoArchivo.PDF -> Triple(Icons.Default.PictureAsPdf, Color(0xFFE53935), "PDF")
        TipoArchivo.WORD -> Triple(Icons.Default.Description, Color(0xFF1E88E5), "Word")
        TipoArchivo.EXCEL -> Triple(Icons.Default.TableChart, Color(0xFF43A047), "Excel")
        TipoArchivo.POWERPOINT -> Triple(Icons.Default.Slideshow, Color(0xFFFB8C00), "PowerPoint")
        TipoArchivo.IMAGEN -> Triple(Icons.Default.Image, Color(0xFF8E24AA), "Imagen")
        TipoArchivo.OTRO -> Triple(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF757575), "Documento")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tipoNombre,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archivo.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = tipoNombre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

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
}
