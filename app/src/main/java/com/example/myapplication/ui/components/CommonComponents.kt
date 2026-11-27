package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

/**
 * Badge con forma de píldora para estados de espacio.
 */
@Composable
fun BadgeEstadoEspacio(estado: String, darkTheme: Boolean = false) {
    val (bg, fg) = when (estado.uppercase()) {
        "DISPONIBLE" -> if (darkTheme) StatusGreenBgDark to StatusGreenFgDark else StatusGreenBgLight to StatusGreenFgLight
        "EN_MANTENIMIENTO" -> if (darkTheme) StatusAmberBgDark to StatusAmberFgDark else StatusAmberBgLight to StatusAmberFgLight
        "FUERA_DE_SERVICIO" -> if (darkTheme) StatusGrayBgDark to StatusGrayFgDark else StatusGrayBgLight to StatusGrayFgLight
        else -> if (darkTheme) StatusBlueBgDark to StatusBlueFgDark else StatusBlueBgLight to StatusBlueFgLight
    }
    PillBadge(texto = estado.replaceFirstChar { it.uppercase() }, bg = bg, fg = fg, darkTheme = darkTheme)
}

/**
 * Badge con forma de píldora para estados de reserva.
 */
@Composable
fun BadgeEstadoReserva(estado: String, darkTheme: Boolean = false) {
    val (bg, fg) = when (estado.uppercase()) {
        "PENDIENTE" -> if (darkTheme) StatusAmberBgDark to StatusAmberFgDark else StatusAmberBgLight to StatusAmberFgLight
        "APROBADA" -> if (darkTheme) StatusGreenBgDark to StatusGreenFgDark else StatusGreenBgLight to StatusGreenFgLight
        "RECHAZADA" -> if (darkTheme) StatusRedBgDark to StatusRedFgDark else StatusRedBgLight to StatusRedFgLight
        "CANCELADA" -> if (darkTheme) StatusGrayBgDark to StatusGrayFgDark else StatusGrayBgLight to StatusGrayFgLight
        "FINALIZADA" -> if (darkTheme) StatusBlueBgDark to StatusBlueFgDark else StatusBlueBgLight to StatusBlueFgLight
        else -> if (darkTheme) StatusGrayBgDark to StatusGrayFgDark else StatusGrayBgLight to StatusGrayFgLight
    }
    PillBadge(texto = estado.replaceFirstChar { it.uppercase() }, bg = bg, fg = fg, darkTheme = darkTheme)
}

/**
 * Badge genérico con forma de píldora.
 */
@Composable
fun PillBadge(texto: String, bg: Color, fg: Color, darkTheme: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

/**
 * Diálogo de confirmación genérico con dos botones.
 */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String = "Confirmar",
    esDestructivo: Boolean = false,
    darkTheme: Boolean = false,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = if (esDestructivo) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Volver")
            }
        }
    )
}

/**
 * Diálogo para ingresar un motivo con validación de longitud mínima.
 */
@Composable
fun DialogoMotivoAccion(
    titulo: String,
    etiqueta: String,
    placeholder: String,
    minCaracteres: Int,
    textoBoton: String,
    esDestructivo: Boolean = false,
    darkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    val valido = motivo.trim().length >= minCaracteres

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text(etiqueta) },
                    placeholder = { Text(placeholder) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${motivo.trim().length} / $minCaracteres caracteres mínimos",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (valido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(motivo) },
                enabled = valido,
                colors = if (esDestructivo) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(textoBoton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Volver")
            }
        }
    )
}

/**
 * Tarjeta de espacio con estilo del prototipo.
 */
@Composable
fun TarjetaEspacio(
    nombre: String,
    tipo: String,
    descripcion: String,
    ubicacion: String,
    capacidad: Int,
    estado: String,
    darkTheme: Boolean = false,
    onClick: () -> Unit
) {
    val disponible = estado.uppercase() == "DISPONIBLE"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (disponible) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tipo.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                BadgeEstadoEspacio(estado = estado, darkTheme = darkTheme)
            }

            Text(
                text = nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ubicacion · Máx. $capacidad pers.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
