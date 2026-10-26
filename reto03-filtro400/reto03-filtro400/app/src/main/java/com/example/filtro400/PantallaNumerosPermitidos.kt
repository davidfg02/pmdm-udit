package com.example.filtro400

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA_PERMITIDOS = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun PantallaNumerosPermitidos(
    viewModel: BloqueosViewModel,
    onVolver: () -> Unit
) {
    BackHandler(onBack = onVolver)

    val permitidos by viewModel.permitidos.collectAsState()
    val listado by viewModel.permitidosListado.collectAsState()
    val busqueda by viewModel.busquedaPermitidos.collectAsState()

    var numeroEnEdicion by remember { mutableStateOf<NumeroPermitido?>(null) }
    var numeroAEliminar by remember { mutableStateOf<NumeroPermitido?>(null) }

    // Al entrar de nuevo en la pantalla no arrastramos la búsqueda anterior.
    LaunchedEffect(Unit) {
        viewModel.buscarPermitidos("")
    }

    val subtitulo = if (busqueda.isNotBlank()) {
        val total = permitidos.size
        "${listado.size} de $total " +
            (if (total == 1) "número permitido" else "números permitidos")
    } else {
        when (permitidos.size) {
            0 -> "Ningún número permitido"
            1 -> "1 número permitido"
            else -> "${permitidos.size} números permitidos"
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
                titulo = "Lista blanca",
                subtitulo = subtitulo,
                onVolver = onVolver
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Los números de esta lista nunca se bloquean: tienen " +
                            "prioridad sobre la lista de bloqueados y la regla del 400.",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = busqueda,
                    onValueChange = viewModel::buscarPermitidos,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar") },
                    placeholder = { Text("Número o motivo") },
                    leadingIcon = { Text("🔍", fontSize = 17.sp) },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { viewModel.buscarPermitidos("") }) {
                                Text("✕", fontSize = 16.sp)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            when {
                permitidos.isEmpty() -> {
                    EstadoVacioPermitidos(
                        titulo = "No hay números permitidos",
                        detalle = "Añade un número para que siempre pase, aunque " +
                            "empiece por 400 o esté en la lista de bloqueados."
                    )
                }

                listado.isEmpty() -> {
                    EstadoVacioPermitidos(
                        titulo = "Sin resultados",
                        detalle = "Ningún número permitido coincide con la búsqueda.",
                        accion = {
                            TextButton(onClick = { viewModel.buscarPermitidos("") }) {
                                Text("Limpiar búsqueda", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(listado, key = { it.id }) { permitido ->
                            TarjetaNumeroPermitido(
                                permitido = permitido,
                                onClick = { numeroEnEdicion = permitido }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    numeroEnEdicion = NumeroPermitido(
                        numero = "",
                        motivo = "",
                        fechaAlta = System.currentTimeMillis()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "+ Añadir número permitido",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    numeroEnEdicion?.let { permitido ->
        DialogoNumeroPermitido(
            permitidoInicial = permitido,
            permitidosExistentes = permitidos,
            onDismiss = { numeroEnEdicion = null },
            onGuardar = {
                viewModel.guardarPermitido(it)
                numeroEnEdicion = null
            },
            onEliminar = if (permitido.id != 0L) {
                {
                    numeroAEliminar = permitido
                    numeroEnEdicion = null
                }
            } else {
                null
            }
        )
    }

    numeroAEliminar?.let { permitido ->
        AlertDialog(
            onDismissRequest = { numeroAEliminar = null },
            title = { Text("Quitar de la lista blanca") },
            text = {
                Text(
                    "¿Quieres quitar ${permitido.numero} de la lista blanca? " +
                        "Volverá a aplicarse la regla del 400 y la lista de bloqueados."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarPermitido(permitido.id)
                        numeroAEliminar = null
                    }
                ) {
                    Text(
                        "Quitar",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { numeroAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ColumnScope.EstadoVacioPermitidos(
    titulo: String,
    detalle: String,
    accion: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("✅", fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            accion?.let {
                Spacer(modifier = Modifier.height(4.dp))
                it()
            }
        }
    }
}

@Composable
private fun TarjetaNumeroPermitido(
    permitido: NumeroPermitido,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✅",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permitido.numero,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (permitido.motivo.isNotBlank()) {
                    Text(
                        text = permitido.motivo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alta ${formatearDiaPermitido(permitido.fechaAlta)} · " +
                        "Siempre permitido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "✏️",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DialogoNumeroPermitido(
    permitidoInicial: NumeroPermitido,
    permitidosExistentes: List<NumeroPermitido>,
    onDismiss: () -> Unit,
    onGuardar: (NumeroPermitido) -> Unit,
    onEliminar: (() -> Unit)?
) {
    var numero by remember(permitidoInicial.id) {
        mutableStateOf(permitidoInicial.numero)
    }
    var motivo by remember(permitidoInicial.id) {
        mutableStateOf(permitidoInicial.motivo)
    }
    var errorNumero by remember(permitidoInicial.id) {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (permitidoInicial.id == 0L) {
                    "Añadir a la lista blanca"
                } else {
                    "Editar número permitido"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        numero = it
                        errorNumero = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Número") },
                    placeholder = { Text("Ej. 913 456 789") },
                    singleLine = true,
                    isError = errorNumero != null,
                    supportingText = {
                        errorNumero?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo (opcional)") },
                    placeholder = { Text("Ej. Proveedor de confianza") },
                    maxLines = 2,
                    shape = RoundedCornerShape(16.dp)
                )

                Text(
                    text = "Tiene prioridad sobre la lista de bloqueados y el prefijo 400.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (onEliminar != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onEliminar,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar de la lista blanca")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizado = ReglaBloqueo.normalizar(numero)
                    when {
                        normalizado.isBlank() -> {
                            errorNumero = "Escribe un número válido."
                        }

                        permitidosExistentes.any {
                            it.numero == normalizado &&
                                it.id != permitidoInicial.id
                        } -> {
                            errorNumero = "Ese número ya está en la lista blanca."
                        }

                        else -> {
                            onGuardar(
                                permitidoInicial.copy(
                                    numero = normalizado,
                                    motivo = motivo.trim()
                                )
                            )
                        }
                    }
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

private fun formatearDiaPermitido(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(FORMATO_FECHA_PERMITIDOS)
