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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.roundToInt

@Composable
fun ArchivoPreview(
    archivo: ArchivoItem,
    onDismiss: () -> Unit,
    onAbrirVisor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(archivo) { miniatura(context, archivo) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    archivo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = archivo.nombre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sin miniatura. Ábrelo para verlo dentro de la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAbrirVisor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver en pantalla completa")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

private fun miniatura(context: Context, archivo: ArchivoItem): Bitmap? {
    val uriString = archivo.uri ?: return null
    return try {
        val uri = Uri.parse(uriString)
        when (archivo.tipo) {
            "IMAGEN" -> miniaturaImagen(context, uri)
            "PDF" -> context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                try {
                    if (renderer.pageCount == 0) {
                        null
                    } else {
                        val page = renderer.openPage(0)
                        try {
                            val escala = 600f / page.width.coerceAtLeast(1)
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
    } catch (_: Throwable) {
        null
    }
}

private fun miniaturaImagen(context: Context, uri: Uri): Bitmap? {
    val maxLado = 640

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val ladoMayor = maxOf(info.size.width, info.size.height)
                if (ladoMayor > maxLado) {
                    val factor = maxLado.toFloat() / ladoMayor.toFloat()
                    val ancho = (info.size.width * factor).roundToInt().coerceAtLeast(1)
                    val alto = (info.size.height * factor).roundToInt().coerceAtLeast(1)
                    decoder.setTargetSize(ancho, alto)
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    // API < 28: BitmapFactory + corrección manual de EXIF.
    return try {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            BitmapFactory.decodeStream(entrada, null, limites)
        }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val ladoMayor = maxOf(limites.outWidth, limites.outHeight)
        var muestra = 1
        if (ladoMayor > maxLado) {
            while ((ladoMayor / (muestra * 2)) >= maxLado) muestra *= 2
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
        } ?: return null

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
    } catch (_: Throwable) {
        null
    }
}
