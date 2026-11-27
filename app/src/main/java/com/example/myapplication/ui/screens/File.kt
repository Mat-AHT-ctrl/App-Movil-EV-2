package com.example.myapplication.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.myapplication.model.ArchivoItem
import com.example.myapplication.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.io.File as JavaFile

@Composable
fun File(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val archivos by viewModel.archivosFlow.collectAsState(initial = emptyList())
    var selectedArchivo by remember { mutableStateOf<ArchivoItem?>(null) }
    var visorArchivo by remember { mutableStateOf<ArchivoItem?>(null) }
    var fotoPendiente by remember { mutableStateOf<JavaFile?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val fileName = getFileName(context, uri) ?: uri.lastPathSegment ?: "Archivo"
            val archivo = ArchivoItem(
                nombre = fileName,
                uri = uri.toString(),
                tipo = ArchivoItem.determinarTipo(fileName)
            )
            viewModel.insertArchivo(archivo)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        val archivoFoto = fotoPendiente
        if (exito && archivoFoto != null && archivoFoto.exists() && archivoFoto.length() > 0) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    archivoFoto
                )
                val archivo = ArchivoItem(
                    nombre = archivoFoto.name,
                    uri = uri.toString(),
                    tipo = "IMAGEN"
                )
                viewModel.insertArchivo(archivo)
                Toast.makeText(context, "Foto guardada exitosamente", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                archivoFoto.delete()
                Toast.makeText(context, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
            }
        } else {
            archivoFoto?.delete()
            Toast.makeText(context, "No se tomó la foto", Toast.LENGTH_SHORT).show()
        }
        fotoPendiente = null
    }

    Scaffold(
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        var archivoFoto: JavaFile? = null
                        try {
                            archivoFoto = JavaFile(context.filesDir, "foto_${System.currentTimeMillis()}.jpg")
                            if (!archivoFoto.exists()) archivoFoto.createNewFile()
                            fotoPendiente = archivoFoto
                            val uriFoto = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                archivoFoto
                            )
                            takePictureLauncher.launch(uriFoto)
                        } catch (_: ActivityNotFoundException) {
                            archivoFoto?.delete()
                            fotoPendiente = null
                            Toast.makeText(
                                context,
                                "No hay una cámara disponible en este dispositivo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (_: Exception) {
                            archivoFoto?.delete()
                            fotoPendiente = null
                            Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Tomar Foto")
                }
                FloatingActionButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Subir Archivo")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Archivos Escolares",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onBack) {
                        Text("Volver")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (archivos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay archivos subidos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(archivos, key = { it.id }) { archivo ->
                            val fileIcon = when (archivo.tipo) {
                                "PDF" -> Icons.Default.PictureAsPdf
                                "IMAGEN" -> Icons.Default.Image
                                "WORD", "EXCEL", "POWERPOINT" -> Icons.Default.Description
                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (archivo.uri != null) {
                                            selectedArchivo = archivo
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = fileIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = archivo.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tipo: ${archivo.tipo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.deleteArchivo(archivo.id)
                                                val uri = archivo.uri?.let { Uri.parse(it) }
                                                if (uri != null && uri.authority == "${context.packageName}.fileprovider") {
                                                    val nombre = uri.lastPathSegment
                                                    if (nombre != null) {
                                                        val fisico = JavaFile(context.filesDir, nombre)
                                                        if (fisico.exists()) fisico.delete()
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedArchivo != null) {
                ArchivoPreview(
                    archivo = selectedArchivo!!,
                    onDismiss = { selectedArchivo = null },
                    onAbrirVisor = { visorArchivo = selectedArchivo }
                )
            }
        }
    }

    visorArchivo?.let { archivo ->
        VisorArchivo(archivo = archivo, onDismiss = { visorArchivo = null })
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
