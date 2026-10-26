package com.example.filtro400

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun PantallaHistorial(
    viewModel: HistorialViewModel,
    onVolver: () -> Unit
) {
    val llamadas by viewModel.llamadas.collectAsState()
    val noLeidas by viewModel.noLeidas.collectAsState()
    var confirmarBorrado by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            CabeceraPantalla(
                titulo = "Historial",
                subtitulo = when {
                    llamadas.isEmpty() -> "Todavía no hay llamadas bloqueadas"
                    noLeidas == 0 -> "${llamadas.size} llamadas bloqueadas"
                    else -> "${llamadas.size} llamadas · $noLeidas sin leer"
                },
                onVolver = onVolver,
                accion = {
                    AnimatedVisibility(
                        visible = noLeidas > 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$noLeidas nueva${if (noLeidas == 1) "" else "s"}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )

            if (llamadas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EstadoVacio()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(llamadas, key = { it.id }) { llamada ->
                        FilaLlamada(llamada)
                    }
                }
            }

            AccionesHistorial(
                hayLlamadas = llamadas.isNotEmpty(),
                noLeidas = noLeidas,
                onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
                onBorrarHistorial = { confirmarBorrado = true }
            )
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Borrar historial",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "¿Seguro que quieres borrar todas las llamadas bloqueadas? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.borrarTodas()
                        confirmarBorrado = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Borrar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EstadoVacio() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🛡️", fontSize = 38.sp)
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Todo tranquilo por aquí",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Cuando el filtro rechace una llamada, aparecerá en esta lista.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun FilaLlamada(llamada: LlamadaBloqueada) {
    val noLeida = !llamada.leida
    val forma = RoundedCornerShape(18.dp)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (noLeida) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = forma
                    )
                } else {
                    Modifier
                }
            ),
        shape = forma,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura con el prefijo del número: refuerza la idea del filtro.
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (noLeida) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = llamada.numero.take(3),
                    color = if (noLeida) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatearNumero(llamada.numero),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (noLeida) FontWeight.Bold else FontWeight.Medium,
                    color = if (noLeida) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatearFecha(llamada.fechaMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (noLeida) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Nueva",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AccionesHistorial(
    hayLlamadas: Boolean,
    noLeidas: Int,
    onMarcarTodasLeidas: () -> Unit,
    onBorrarHistorial: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            onClick = onMarcarTodasLeidas,
            enabled = noLeidas > 0,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("Marcar leídas", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }

        OutlinedButton(
            onClick = onBorrarHistorial,
            enabled = hayLlamadas,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Borrar historial", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

// "400123456" -> "400 123 456"
private fun formatearNumero(numero: String): String =
    numero.chunked(3).joinToString(" ")

// "hoy 20:42", "ayer 18:10" o "12/10 09:30".
private fun formatearFecha(fechaMillis: Long): String {
    val fecha = Instant.ofEpochMilli(fechaMillis).atZone(ZoneId.systemDefault())
    val hoy = LocalDate.now()
    return when (fecha.toLocalDate()) {
        hoy -> "hoy " + fecha.format(FORMATO_HORA)
        hoy.minusDays(1) -> "ayer " + fecha.format(FORMATO_HORA)
        else -> fecha.format(FORMATO_DIA_MES) + " " + fecha.format(FORMATO_HORA)
    }
}
