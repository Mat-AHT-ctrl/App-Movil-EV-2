package com.example.myapplication.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Xml
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.model.ArchivoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream
import androidx.core.net.toUri

private sealed interface ContenidoVisor {
    data object Cargando : ContenidoVisor
    data object SinUri : ContenidoVisor
    data class Error(val mensaje: String) : ContenidoVisor
    data class Imagen(val bitmap: Bitmap) : ContenidoVisor
    data class Pdf(val paginas: List<Bitmap>) : ContenidoVisor
    data class Texto(val contenido: String) : ContenidoVisor
    data object NoSoportado : ContenidoVisor
}

@Composable
private fun VisorTopBar(
    escala: Float,
    soportaZoom: Boolean,
    onReset: () -> Unit,
    archivoNombre: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archivoNombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (soportaZoom) {
                    Text(
                        text = if (escala > 1.02f) {
                            "Zoom ${(escala * 100).roundToInt()}% · Doble tap para restaurar"
                        } else {
                            "Usa dos dedos para zoom · Doble tap para acercar"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = soportaZoom && escala > 1.05f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Ajustar a pantalla",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar visor")
            }
        }
    }
}

@Composable
fun VisorArchivo(
    archivo: ArchivoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(archivo.id, archivo.uri) { archivo.uri?.let { it.toUri() } }
    var estadoContenido by remember(archivo.id) { mutableStateOf<ContenidoVisor>(ContenidoVisor.Cargando) }
    var escalaActual by remember(archivo.id) { mutableFloatStateOf(1f) }
    var triggerReset by remember(archivo.id) { mutableIntStateOf(0) }

    LaunchedEffect(archivo.id, uri) {
        if (uri == null) {
            estadoContenido = ContenidoVisor.SinUri
            return@LaunchedEffect
        }

        estadoContenido = ContenidoVisor.Cargando
        estadoContenido = withContext(Dispatchers.IO) {
            when {
                archivo.tipo == "PDF" -> {
                    try {
                        val lista = mutableListOf<Bitmap>()
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                for (i in 0 until renderer.pageCount) {
                                    renderer.openPage(i).use { page ->
                                        val escalaRender = (1600f / page.width.coerceAtLeast(1)).coerceIn(1f, 2.5f)
                                        val ancho = (page.width * escalaRender).toInt().coerceAtLeast(1)
                                        val alto = (page.height * escalaRender).toInt().coerceAtLeast(1)
                                        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                                        bmp.eraseColor(android.graphics.Color.WHITE)
                                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        lista.add(bmp)
                                    }
                                }
                            }
                        }
                        if (lista.isEmpty()) ContenidoVisor.Error("El PDF no contiene páginas legibles.")
                        else ContenidoVisor.Pdf(lista)
                    } catch (e: Exception) {
                        ContenidoVisor.Error("No se pudo renderizar este PDF en la app.")
                    }
                }

                esImagen(archivo) -> {
                    try {
                        val bmp = decodificarImagenOptimizada(context, uri)
                        if (bmp != null) ContenidoVisor.Imagen(bmp)
                        else ContenidoVisor.Error("No se pudo decodificar la imagen.")
                    } catch (e: Exception) {
                        ContenidoVisor.Error("No se pudo cargar la imagen.")
                    }
                }

                esWordDocx(archivo.nombre) -> {
                    try {
                        val resultado = extraerTextoDocx(context, uri)
                        if (resultado.isBlank()) {
                            ContenidoVisor.Error("El documento Word no contiene texto legible.")
                        } else {
                            ContenidoVisor.Texto(resultado)
                        }
                    } catch (e: Exception) {
                        ContenidoVisor.Error("No se pudo leer el documento Word.")
                    }
                }

                esExcelXlsx(archivo.nombre) -> {
                    try {
                        val resultado = extraerTextoXlsx(context, uri)
                        if (resultado.isBlank()) {
                            ContenidoVisor.Error("El libro de Excel no contiene datos legibles.")
                        } else {
                            ContenidoVisor.Texto(resultado)
                        }
                    } catch (e: Exception) {
                        ContenidoVisor.Error("No se pudo leer el libro de Excel.")
                    }
                }

                esPowerPointPptx(archivo.nombre) -> {
                    try {
                        val resultado = extraerTextoPptx(context, uri)
                        if (resultado.isBlank()) {
                            ContenidoVisor.Error("La presentación no contiene texto legible.")
                        } else {
                            ContenidoVisor.Texto(resultado)
                        }
                    } catch (e: Exception) {
                        ContenidoVisor.Error("No se pudo leer la presentación PowerPoint.")
                    }
                }

                esTexto(archivo.nombre) -> {
                    try {
                        val texto = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                            it.readText()
                        } ?: ""
                        ContenidoVisor.Texto(texto)
                    } catch (e: Exception) {
                        ContenidoVisor.Error("No se pudo leer el archivo de texto.")
                    }
                }

                else -> ContenidoVisor.NoSoportado
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val soportaZoom = estadoContenido is ContenidoVisor.Pdf ||
                        estadoContenido is ContenidoVisor.Imagen ||
                        estadoContenido is ContenidoVisor.Texto

                VisorTopBar(
                    escala = escalaActual,
                    soportaZoom = soportaZoom,
                    onReset = { triggerReset++ },
                    archivoNombre = archivo.nombre,
                    onDismiss = onDismiss
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (val contenido = estadoContenido) {
                        is ContenidoVisor.Cargando -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Preparando vista previa...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is ContenidoVisor.SinUri -> {
                            Text(
                                text = "Este archivo no tiene una copia local.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        is ContenidoVisor.Error -> {
                            Text(
                                text = contenido.mensaje,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        is ContenidoVisor.Imagen -> {
                            ContenedorZoomFluido(
                                minScale = 1f,
                                maxScale = 6f,
                                doubleTapScale = 2.5f,
                                resetKey = triggerReset,
                                onScaleChanged = { escalaActual = it }
                            ) {
                                Image(
                                    bitmap = contenido.bitmap.asImageBitmap(),
                                    contentDescription = archivo.nombre,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    filterQuality = FilterQuality.High
                                )
                            }
                        }

                        is ContenidoVisor.Pdf -> {
                            VisorPdfFluido(
                                paginas = contenido.paginas,
                                resetKey = triggerReset,
                                onScaleChanged = { escalaActual = it }
                            )
                        }

                        is ContenidoVisor.Texto -> {
                            VisorTextoZoomable(
                                contenido = contenido.contenido,
                                resetKey = triggerReset,
                                onScaleChanged = { escalaActual = it }
                            )
                        }

                        is ContenidoVisor.NoSoportado -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Vista previa no disponible para este formato.\nEl archivo se mantiene guardado dentro de la app.",
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
}

@Composable
private fun ContenedorZoomFluido(
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapScale: Float = 2.5f,
    resetKey: Int = 0,
    onScaleChanged: (Float) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    fun actualizarEscala(nuevaEscala: Float) {
        if (nuevaEscala.isFinite()) {
            scale = nuevaEscala
            onScaleChanged(nuevaEscala)
        }
    }

    fun clampOffset(target: Offset, currentScale: Float): Offset {
        if (currentScale <= 1.01f || containerSize.width == 0 || containerSize.height == 0) {
            return Offset.Zero
        }
        val maxX = (containerSize.width * (currentScale - 1f)) / 2f
        val maxY = (containerSize.height * (currentScale - 1f)) / 2f
        return Offset(
            x = target.x.coerceIn(-maxX, maxX),
            y = target.y.coerceIn(-maxY, maxY)
        )
    }

    LaunchedEffect(resetKey) {
        if (resetKey > 0) {
            val animScale = Animatable(scale)
            val animX = Animatable(offset.x)
            val animY = Animatable(offset.y)
            launch { animScale.animateTo(1f, tween(220)) { actualizarEscala(value) } }
            launch { animX.animateTo(0f, tween(220)) { offset = offset.copy(x = value) } }
            launch { animY.animateTo(0f, tween(220)) { offset = offset.copy(y = value) } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val targetScale = if (scale > 1.2f) 1f else doubleTapScale
                        val targetOffset = if (targetScale == 1f) {
                            Offset.Zero
                        } else {
                            val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            clampOffset((center - tapOffset) * (targetScale - 1f), targetScale)
                        }

                        scope.launch {
                            val animScale = Animatable(scale)
                            val animX = Animatable(offset.x)
                            val animY = Animatable(offset.y)
                            launch { animScale.animateTo(targetScale, tween(240)) { actualizarEscala(value) } }
                            launch { animX.animateTo(targetOffset.x, tween(240)) { offset = offset.copy(x = value) } }
                            launch { animY.animateTo(targetOffset.y, tween(240)) { offset = offset.copy(y = value) } }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.isConsumed }) {
                            val pointerCount = event.changes.count { it.pressed }
                            if (pointerCount >= 2 || scale > 1.01f) {
                                val zoomChange = if (pointerCount >= 2) event.calculateZoom() else 1f
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)

                                if (zoomChange.isFinite() && zoomChange > 0f &&
                                    panChange.x.isFinite() && panChange.y.isFinite() &&
                                    centroid.x.isFinite() && centroid.y.isFinite() &&
                                    containerSize.width > 0 && containerSize.height > 0
                                ) {
                                    val oldScale = scale
                                    val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
                                    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                    val zoomFactor = newScale / oldScale

                                    val rawOffset = offset + panChange - (centroid - center) * (zoomFactor - 1f)
                                    if (rawOffset.x.isFinite() && rawOffset.y.isFinite()) {
                                        actualizarEscala(newScale)
                                        offset = clampOffset(rawOffset, newScale)
                                    }
                                }

                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun VisorPdfFluido(
    paginas: List<Bitmap>,
    resetKey: Int,
    onScaleChanged: (Float) -> Unit
) {
    val listState = rememberLazyListState()
    val paginaActual by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceIn(1, paginas.size) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ContenedorZoomFluido(
            minScale = 1f,
            maxScale = 5f,
            doubleTapScale = 2.2f,
            resetKey = resetKey,
            onScaleChanged = onScaleChanged
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
            ) {
                items(
                    count = paginas.size,
                    key = { index -> index }
                ) { i ->
                    val bitmapPagina = paginas[i]
                    Surface(
                        shadowElevation = 2.dp,
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = bitmapPagina.asImageBitmap(),
                            contentDescription = "Página ${i + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(bitmapPagina.width.toFloat() / bitmapPagina.height.toFloat()),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.82f)
        ) {
            Text(
                text = "Pág. $paginaActual / ${paginas.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun VisorTextoZoomable(
    contenido: String,
    resetKey: Int,
    onScaleChanged: (Float) -> Unit
) {
    var escalaTexto by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(resetKey) {
        if (resetKey > 0) {
            Animatable(escalaTexto).animateTo(1f, tween(200)) {
                escalaTexto = value
                onScaleChanged(value)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val target = if (escalaTexto > 1.2f) 1f else 1.6f
                        scope.launch {
                            Animatable(escalaTexto).animateTo(target, tween(200)) {
                                escalaTexto = value
                                onScaleChanged(value)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var acumuladorZoom = escalaTexto
                    do {
                        val event = awaitPointerEvent()
                        val dedos = event.changes.count { it.pressed }
                        if (dedos >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom.isFinite() && zoom > 0f) {
                                acumuladorZoom = (acumuladorZoom * zoom).coerceIn(0.8f, 3f)
                                if (abs(acumuladorZoom - escalaTexto) > 0.03f) {
                                    escalaTexto = acumuladorZoom
                                    onScaleChanged(escalaTexto)
                                }
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SelectionContainer {
                Text(
                    text = contenido.ifBlank { "El archivo está vacío." },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (14f * escalaTexto).sp,
                        lineHeight = (20f * escalaTexto).sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun extensionArchivo(nombre: String): String =
    nombre.substringAfterLast('.', "").lowercase()

private fun esWordDocx(nombre: String): Boolean = extensionArchivo(nombre) == "docx"

private fun esExcelXlsx(nombre: String): Boolean = extensionArchivo(nombre) == "xlsx"

private fun esPowerPointPptx(nombre: String): Boolean = extensionArchivo(nombre) == "pptx"

private fun esTexto(nombre: String): Boolean =
    extensionArchivo(nombre) in setOf("txt", "csv", "md", "json", "xml", "log")

private fun esImagen(archivo: ArchivoItem): Boolean =
    archivo.tipo.equals("IMAGEN", ignoreCase = true) ||
            extensionArchivo(archivo.nombre) in setOf(
        "jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif"
    )

/** Decodifica fotos de cámara limitando memoria y respetando la orientación EXIF. */
private fun decodificarImagenOptimizada(
    context: Context,
    uri: Uri,
    maxDimension: Int = 2048
): Bitmap? = try {
    // API 28+: ImageDecoder aplica EXIF y color sRGB automáticamente.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Necesario para que asImageBitmap() pueda subir el bitmap a la GPU.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ancho = info.size.width
            val alto = info.size.height
            val ladoMayor = maxOf(ancho, alto)
            if (ladoMayor > maxDimension) {
                val factor = maxDimension.toFloat() / ladoMayor.toFloat()
                val anchoDestino = (ancho * factor).roundToInt().coerceAtLeast(1)
                val altoDestino = (alto * factor).roundToInt().coerceAtLeast(1)
                decoder.setTargetSize(anchoDestino, altoDestino)
            }
        }
    } else {
        // API < 28: BitmapFactory + corrección manual de EXIF.
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            BitmapFactory.decodeStream(entrada, null, limites)
        }

        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            null
        } else {
            val ladoMayor = maxOf(limites.outWidth, limites.outHeight)
            var muestra = 1
            if (ladoMayor > maxDimension) {
                while ((ladoMayor / (muestra * 2)) >= maxDimension) {
                    muestra *= 2
                }
            }

            var bitmap = context.contentResolver.openInputStream(uri)?.use { entrada ->
                BitmapFactory.decodeStream(
                    entrada,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = muestra
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                )
            }

            if (bitmap == null) {
                null
            } else {
                try {
                    context.contentResolver.openInputStream(uri)?.use { entrada ->
                        val exif = ExifInterface(entrada)
                        val grados = when (
                            exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                        ) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                        if (grados != 0f) {
                            val matriz = Matrix().apply { postRotate(grados) }
                            val enderezado = Bitmap.createBitmap(
                                bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true
                            )
                            bitmap.recycle()
                            bitmap = enderezado
                        }
                    }
                } catch (_: Exception) {
                    // Formato sin EXIF: se conserva la orientación original.
                }
                bitmap
            }
        }
    }
} catch (_: Throwable) {
    null
}

/** Abre el ZIP y ejecuta [bloque] sobre la entrada indicada, si existe. */
private fun <T> conEntradaZip(
    context: Context,
    uri: Uri,
    nombreEntrada: String,
    bloque: (InputStream) -> T
): T? {
    var resultado: T? = null
    context.contentResolver.openInputStream(uri)?.use { crudo ->
        ZipInputStream(crudo).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null && resultado == null) {
                if (entrada.name == nombreEntrada) resultado = bloque(zip)
                entrada = zip.nextEntry
            }
        }
    }
    return resultado
}

/** Word: conserva párrafos/saltos (`p`, `br`) y tabuladores de celda (`tc`). */
private fun extraerTextoDocx(context: Context, uri: Uri): String {
    val salida = StringBuilder()
    conEntradaZip(context, uri, "word/document.xml") { input ->
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        var evento = parser.eventType
        while (evento != XmlPullParser.END_DOCUMENT) {
            when (evento) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "br" -> salida.append('\n')
                    "t" -> salida.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "p" -> salida.append('\n')
                    "tc" -> salida.append('\t')
                }
            }
            evento = parser.next()
        }
    }
    return salida.toString().replace(Regex("\n{3,}"), "\n\n").trim()
}

/** Excel: lee cadenas compartidas y luego cada hoja ordenada por número. */
private fun extraerTextoXlsx(context: Context, uri: Uri): String {
    // 1ª pasada: sharedStrings.xml
    val compartidas = mutableListOf<String>()
    conEntradaZip(context, uri, "xl/sharedStrings.xml") { input ->
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        var actual = StringBuilder()
        var dentroSi = false
        var evento = parser.eventType
        while (evento != XmlPullParser.END_DOCUMENT) {
            when (evento) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> {
                        dentroSi = true
                        actual = StringBuilder()
                    }
                    "t" -> if (dentroSi) actual.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (parser.name == "si" && dentroSi) {
                    compartidas.add(actual.toString())
                    dentroSi = false
                }
            }
            evento = parser.next()
        }
    }

    // 2ª pasada: hojas ordenadas por número
    val hojas = mutableMapOf<Int, String>()
    val regexHoja = Regex("xl/worksheets/sheet(\\d+)\\.xml")
    context.contentResolver.openInputStream(uri)?.use { crudo ->
        ZipInputStream(crudo).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                val numero = regexHoja.matchEntire(entrada.name)?.groupValues?.get(1)?.toIntOrNull()
                if (numero != null) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, null)
                    val bloqueHoja = StringBuilder()
                    var celdas = mutableListOf<String>()
                    var evento = parser.eventType
                    while (evento != XmlPullParser.END_DOCUMENT) {
                        when (evento) {
                            XmlPullParser.START_TAG -> when (parser.name) {
                                "row" -> celdas = mutableListOf()
                                "c" -> {
                                    val tipo = parser.getAttributeValue(null, "t")
                                    var valor = ""
                                    var interno = parser.next()
                                    while (interno != XmlPullParser.END_DOCUMENT) {
                                        if (interno == XmlPullParser.END_TAG && parser.name == "c") break
                                        if (interno == XmlPullParser.START_TAG && parser.name == "v") {
                                            valor = parser.nextText()
                                        }
                                        interno = parser.next()
                                    }
                                    val texto = if (tipo == "s") {
                                        valor.toIntOrNull()?.let { compartidas.getOrNull(it) } ?: valor
                                    } else {
                                        valor
                                    }
                                    celdas.add(texto)
                                }
                            }
                            XmlPullParser.END_TAG -> if (parser.name == "row") {
                                bloqueHoja.append(celdas.joinToString(" | ")).append('\n')
                                celdas = mutableListOf()
                            }
                        }
                        evento = parser.next()
                    }
                    hojas[numero] = bloqueHoja.toString()
                }
                entrada = zip.nextEntry
            }
        }
    }

    val salida = StringBuilder()
    hojas.keys.sorted().forEach { numero ->
        salida.append("=== Hoja $numero ===\n")
        salida.append(hojas[numero])
    }
    return salida.toString().trim()
}

/** PowerPoint: diapositivas ordenadas numéricamente con texto de etiquetas `t`. */
private fun extraerTextoPptx(context: Context, uri: Uri): String {
    val diapositivas = mutableMapOf<Int, String>()
    val regexDiapositiva = Regex("ppt/slides/slide(\\d+)\\.xml")
    context.contentResolver.openInputStream(uri)?.use { crudo ->
        ZipInputStream(crudo).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                val numero = regexDiapositiva.matchEntire(entrada.name)?.groupValues?.get(1)?.toIntOrNull()
                if (numero != null) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, null)
                    val bloque = StringBuilder()
                    var evento = parser.eventType
                    while (evento != XmlPullParser.END_DOCUMENT) {
                        when (evento) {
                            XmlPullParser.START_TAG -> when (parser.name) {
                                "br" -> bloque.append('\n')
                                "t" -> bloque.append(parser.nextText())
                            }
                            XmlPullParser.END_TAG -> if (parser.name == "p") bloque.append('\n')
                        }
                        evento = parser.next()
                    }
                    diapositivas[numero] = bloque.toString().trim()
                }
                entrada = zip.nextEntry
            }
        }
    }

    val salida = StringBuilder()
    diapositivas.keys.sorted().forEach { numero ->
        salida.append("--- Diapositiva $numero ---\n")
        salida.append(diapositivas[numero]).append('\n')
    }
    return salida.toString().trim()
}