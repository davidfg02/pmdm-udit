package com.example.filtro400

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaFranjas(
    viewModel: FranjasViewModel,
    onVolver: () -> Unit
) {
    val configuracion by viewModel.configuracion.collectAsState()
    var borrador by remember { mutableStateOf<FranjaHoraria?>(null) }

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
                titulo = "Franjas horarias",
                subtitulo = if (configuracion.usarFranjas) {
                    "El filtro actúa solo dentro de tus franjas"
                } else {
                    "El filtro actúa a cualquier hora"
                },
                onVolver = onVolver
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                TarjetaModoFranjas(
                    usarFranjas = configuracion.usarFranjas,
                    onCambiar = viewModel::activarFranjas
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (configuracion.usarFranjas && configuracion.activas.isEmpty()) {
                    AvisoSinFranjasActivas()
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tus franjas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${configuracion.franjas.size} configurada" +
                            if (configuracion.franjas.size == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (configuracion.franjas.isEmpty()) {
                    EstadoVacioFranjas()
                } else {
                    configuracion.franjas.forEach { franja ->
                        TarjetaFranja(
                            franja = franja,
                            onEditar = { borrador = franja },
                            onCambiarActiva = { activa ->
                                viewModel.cambiarActiva(franja.id, activa)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        borrador = FranjaHoraria(inicioMinuto = 22 * 60, finMinuto = 8 * 60)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "+  Añadir franja",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Fuera de estas franjas, las llamadas que empiezan por 400 pasan con normalidad. " +
                        "Si una franja empieza y termina a la misma hora, se aplica todo el día.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val franjaEnEdicion = borrador
    if (franjaEnEdicion != null) {
        DialogoFranja(
            franja = franjaEnEdicion,
            onDismiss = { borrador = null },
            onGuardar = {
                viewModel.guardarFranja(it)
                borrador = null
            },
            onEliminar = {
                viewModel.eliminarFranja(it)
                borrador = null
            }
        )
    }
}

@Composable
private fun TarjetaModoFranjas(
    usarFranjas: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🕐", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Usar franjas horarias",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (usarFranjas) {
                        "Solo se bloquea dentro de tus franjas"
                    } else {
                        "Se bloquea a cualquier hora del día"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(checked = usarFranjas, onCheckedChange = onCambiar)
        }
    }
}

@Composable
private fun AvisoSinFranjasActivas() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "⚠️", fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "No hay ninguna franja activa",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Con el modo por franjas activado y todas desactivadas, no se bloqueará ninguna llamada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun EstadoVacioFranjas() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⏰", fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Todavía no hay franjas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Añade un tramo para decidir a qué horas debe actuar el filtro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaFranja(
    franja: FranjaHoraria,
    onEditar: () -> Unit,
    onCambiarActiva: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEditar),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = franja.rangoTexto(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (franja.activa) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(franja.duracionTexto())
                        if (franja.cruzaMedianoche) append(" · termina al día siguiente")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = franja.activa,
                onCheckedChange = onCambiarActiva
            )
        }
    }
}

@Composable
private fun DialogoFranja(
    franja: FranjaHoraria,
    onDismiss: () -> Unit,
    onGuardar: (FranjaHoraria) -> Unit,
    onEliminar: (Long) -> Unit
) {
    var inicio by remember(franja.id) { mutableIntStateOf(franja.inicioMinuto) }
    var fin by remember(franja.id) { mutableIntStateOf(franja.finMinuto) }
    val context = LocalContext.current

    fun abrirReloj(minutoActual: Int, alElegir: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hora, minuto -> alElegir(hora * 60 + minuto) },
            minutoActual / 60,
            minutoActual % 60,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (franja.id == 0L) "Nueva franja" else "Editar franja",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "Elige el tramo del día en el que quieres que actúe el filtro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorHora(
                        etiqueta = "Inicio",
                        minuto = inicio,
                        modifier = Modifier.weight(1f),
                        onClick = { abrirReloj(inicio) { inicio = it } }
                    )
                    SelectorHora(
                        etiqueta = "Fin",
                        minuto = fin,
                        modifier = Modifier.weight(1f),
                        onClick = { abrirReloj(fin) { fin = it } }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val vistaPrevia = FranjaHoraria(inicioMinuto = inicio, finMinuto = fin)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏱", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append(vistaPrevia.duracionTexto())
                            append(" · ")
                            append(vistaPrevia.rangoTexto())
                            if (vistaPrevia.cruzaMedianoche) append(" (día siguiente)")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (franja.id != 0L) {
                    TextButton(
                        onClick = { onEliminar(franja.id) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar franja")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGuardar(
                        franja.copy(
                            inicioMinuto = inicio,
                            finMinuto = fin
                        )
                    )
                }
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun SelectorHora(
    etiqueta: String,
    minuto: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = etiqueta.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatearHora(minuto),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
