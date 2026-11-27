package com.example.myapplication.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
    var texto by remember { mutableStateOf("") }
    var paginas by remember { mutableStateOf(emptyList<Bitmap>()) }
    var resetZoom by remember { mutableIntStateOf(0) }

    LaunchedEffect(uri, archivo.id) {
        if (uri == null) return@LaunchedEffect
        when (archivo.tipo) {
            "PDF" -> {
                val lista = mutableListOf<Bitmap>()
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        val renderer = PdfRenderer(pfd)
                        try {
                            for (i in 0 until renderer.pageCount) {
                                val page = renderer.openPage(i)
                                try {
                                    val escala = (1080f / page.width.coerceAtLeast(1)).coerceAtMost(2f)
                                    val ancho = (page.width * escala).toInt().coerceAtLeast(1)
                                    val alto = (page.height * escala).toInt().coerceAtLeast(1)
                                    val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    lista.add(bmp)
                                } finally {
                                    page.close()
                                }
                            }
                        } finally {
                            renderer.close()
                        }
                    }
                } catch (_: Exception) {
                    // Si falla, se muestra el aviso dentro del dialogo.
                }
                paginas = lista
            }
            else -> if (esTexto(archivo.nombre)) {
                try {
                    texto = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    texto = ""
                }
            }
        }
    }

    val bitmapImagen = remember(archivo.id, uri) {
        if (archivo.tipo == "IMAGEN" && uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
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
                    if (archivo.tipo == "IMAGEN") {
                        TextButton(onClick = { resetZoom++ }) { Text("Ajustar") }
                    }
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
                            if (bitmapImagen != null) {
                                ZoomableImage(bitmapImagen, archivo.nombre, resetZoom)
                            } else {
                                Text("No se pudo cargar la imagen.")
                            }
                        }
                        archivo.tipo == "PDF" -> {
                            if (paginas.isEmpty()) {
                                Text("No se pudo renderizar este PDF en la app.", textAlign = TextAlign.Center)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    items(paginas.size) { i ->
                                        Image(
                                            bitmap = paginas[i].asImageBitmap(),
                                            contentDescription = "Página ${i + 1}",
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.FillWidth
                                        )
                                        Text(
                                            "Página ${i + 1} de ${paginas.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                        )
                                    }
                                }
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
            }
        }
    }
}

/** Imagen con zoom por pellizco y botón "Ajustar" que vuelve a 1:1. */
@Composable
private fun ZoomableImage(bitmap: Bitmap, description: String, resetKey: Int = 0) {
    var escala by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(bitmap, resetKey) {
        escala = 1f
        offset = Offset.Zero
    }
    val estadoZoom = rememberTransformableState { factorZoom, desplazamiento, _ ->
        escala = (escala * factorZoom).coerceIn(1f, 6f)
        offset = offset + desplazamiento
        if (escala == 1f) offset = Offset.Zero
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = description,
        modifier = Modifier
            .fillMaxSize()
            .transformable(estadoZoom)
            .graphicsLayer(
                scaleX = escala,
                scaleY = escala,
                translationX = offset.x,
                translationY = offset.y
            ),
        contentScale = ContentScale.Fit
    )
}

private fun esTexto(nombre: String): Boolean =
    nombre.substringAfterLast('.', "").lowercase() in setOf("txt", "csv", "md", "json", "xml", "log")
