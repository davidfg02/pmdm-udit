package com.example.filtro400

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filtro400.ui.theme.DegradadoFin
import com.example.filtro400.ui.theme.DegradadoInicio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaEstadisticas(
    viewModel: HistorialViewModel,
    onVolver: () -> Unit
) {
    val llamadas by viewModel.llamadas.collectAsState()
    val estadisticas = remember(llamadas) { CalculadoraEstadisticas.calcular(llamadas) }

    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    var exportando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var mensajeEsError by remember { mutableStateOf(false) }

    val exportador = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            alcance.launch {
                exportando = true
                val correcto = runCatching {
                    withContext(Dispatchers.IO) {
                        val csv = ExportadorCsv.generar(llamadas, estadisticas)
                        context.contentResolver.openOutputStream(uri)?.use { salida ->
                            salida.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("No se pudo abrir el archivo")
                    }
                }.isSuccess

                exportando = false
                mensajeEsError = !correcto
                mensaje = if (correcto) {
                    "Estadísticas exportadas correctamente."
                } else {
                    "No se pudo guardar el archivo. Inténtalo de nuevo."
                }
            }
        }
    }

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
                titulo = "Estadísticas",
                subtitulo = if (estadisticas.total == 0) {
                    "Aún no hay datos que mostrar"
                } else {
                    "${estadisticas.total} llamadas bloqueadas en total"
                },
                onVolver = onVolver
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                TarjetaResumen(estadisticas)

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaDato(
                        titulo = "HORA PUNTA",
                        valor = estadisticas.horaPunta?.let { formatearHora(it * 60) } ?: "—",
                        detalle = if (estadisticas.horaPunta == null) "sin datos" else "más bloqueos",
                        emoji = "🕐",
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaDato(
                        titulo = "NÚMERO TOP",
                        valor = estadisticas.numeroMasRepetido?.let {
                            formatearNumero(it.numero)
                        } ?: "—",
                        detalle = estadisticas.numeroMasRepetido?.let {
                            "${it.cantidad} ${if (it.cantidad == 1) "vez" else "veces"}"
                        } ?: "sin datos",
                        emoji = "🔁",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TarjetaSemana(estadisticas)

                if (estadisticas.numerosTop.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TarjetaNumeros(estadisticas)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TarjetaExportar(
                    hayDatos = estadisticas.total > 0,
                    exportando = exportando,
                    mensaje = mensaje,
                    mensajeEsError = mensajeEsError,
                    onExportar = {
                        mensaje = null
                        exportador.launch(ExportadorCsv.nombreSugerido())
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TarjetaResumen(estadisticas: EstadisticasLlamadas) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = DegradadoInicio.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = (-52).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LLAMADAS BLOQUEADAS",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.9.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = estadisticas.total.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${estadisticas.hoy} hoy · ${estadisticas.totalSemana} en los últimos 7 días",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚫", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun TarjetaDato(
    titulo: String,
    valor: String,
    detalle: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(text = emoji, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TarjetaSemana(estadisticas: EstadisticasLlamadas) {
    val maximo = (estadisticas.dias.maxOfOrNull { it.cantidad } ?: 0).coerceAtLeast(1)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Últimos 7 días",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${estadisticas.totalSemana} bloqueos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                estadisticas.dias.forEach { dia ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (dia.cantidad > 0) dia.cantidad.toString() else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val altura = if (dia.cantidad == 0) {
                                4.dp
                            } else {
                                (86f * dia.cantidad / maximo).dp
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(altura)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 7.dp,
                                            topEnd = 7.dp
                                        )
                                    )
                                    .background(
                                        if (dia.esHoy) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (dia.esHoy) "Hoy" else dia.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dia.esHoy) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (dia.esHoy) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (estadisticas.totalSemana == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Todavía no hay llamadas bloqueadas esta semana.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TarjetaNumeros(estadisticas: EstadisticasLlamadas) {
    val maximo = (estadisticas.numerosTop.firstOrNull()?.cantidad ?: 1).coerceAtLeast(1)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Números más bloqueados",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            estadisticas.numerosTop.forEachIndexed { indice, numero ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${indice + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatearNumero(numero.numero),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { numero.cantidad.toFloat() / maximo },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${numero.cantidad}×",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaExportar(
    hayDatos: Boolean,
    exportando: Boolean,
    mensaje: String?,
    mensajeEsError: Boolean,
    onExportar: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📊", fontSize = 19.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Exportar estadísticas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Resumen, números top e historial completo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Se guardará un archivo CSV compatible con Excel y LibreOffice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onExportar,
                enabled = hayDatos && !exportando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (exportando) "Guardando…" else "Exportar CSV",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (!hayDatos) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aún no hay llamadas bloqueadas para exportar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mensaje != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mensajeEsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatearNumero(numero: String): String =
    numero.chunked(3).joinToString(" ")
