package com.example.myapplication.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.model.ArchivoItem

/**
 * Visor 100% interno: imágenes y PDF se renderizan en la app (sin ACTION_VIEW);
 * archivos de texto se leen como texto y los demás muestran solo sus datos.
 */
@Composable
fun VisorArchivo(
    archivo: ArchivoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(archivo) { archivo.uri?.let { Uri.parse(it) } }
    var pagina by remember { mutableIntStateOf(0) }
    var totalPaginas by remember { mutableIntStateOf(0) }
    var texto by remember { mutableStateOf("") }

    LaunchedEffect(uri, archivo.id) {
        if (uri == null) return@LaunchedEffect
        if (archivo.tipo == "PDF") {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    try {
                        totalPaginas = renderer.pageCount
                    } finally {
                        renderer.close()
                    }
                }
            } catch (_: Exception) {
                totalPaginas = 0
            }
        } else if (esTexto(archivo.nombre)) {
            try {
                texto = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (_: Exception) {
                texto = ""
            }
        }
    }

    val bitmap = remember(archivo.id, pagina, uri) {
        cargarBitmap(context, uri, archivo, pagina)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        archivo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar visor")
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uri == null -> Text("Este archivo no tiene una copia local.", textAlign = TextAlign.Center)
                        archivo.tipo == "IMAGEN" -> {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = archivo.nombre,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("No se pudo cargar la imagen.")
                            }
                        }
                        archivo.tipo == "PDF" -> {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Página ${pagina + 1}",
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("No se pudo renderizar este PDF en la app.", textAlign = TextAlign.Center)
                            }
                        }
                        esTexto(archivo.nombre) -> Text(
                            texto.ifBlank { "El archivo está vacío." },
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                        )
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Vista previa no disponible para este formato.\n" +
                                    "El archivo se mantiene guardado dentro de la app.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (archivo.tipo == "PDF" && totalPaginas > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { pagina = (pagina - 1).coerceAtLeast(0) },
                            enabled = pagina > 0
                        ) { Text("Anterior") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${pagina + 1} / $totalPaginas")
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { pagina = (pagina + 1).coerceAtMost(totalPaginas - 1) },
                            enabled = pagina < totalPaginas - 1
                        ) { Text("Siguiente") }
                    }
                }
            }
        }
    }
}

private fun esTexto(nombre: String): Boolean =
    nombre.substringAfterLast('.', "").lowercase() in setOf("txt", "csv", "md", "json", "xml", "log")

private fun cargarBitmap(context: Context, uri: Uri?, archivo: ArchivoItem, pagina: Int): Bitmap? {
    uri ?: return null
    return try {
        when (archivo.tipo) {
            "IMAGEN" -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            "PDF" -> context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                try {
                    if (renderer.pageCount == 0) {
                        null
                    } else {
                        val page = renderer.openPage(pagina.coerceIn(0, renderer.pageCount - 1))
                        try {
                            val escala = (1080f / page.width.coerceAtLeast(1)).coerceAtMost(2f)
                            val ancho = (page.width * escala).toInt().coerceAtLeast(1)
                            val alto = (page.height * escala).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        } finally {
                            page.close()
                        }
                    }
                } finally {
                    renderer.close()
                }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
