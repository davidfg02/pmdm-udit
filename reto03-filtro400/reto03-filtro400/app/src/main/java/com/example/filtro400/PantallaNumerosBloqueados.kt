package com.example.filtro400

import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private data class MensajeBloqueos(val texto: String, val esError: Boolean)

private val FORMATO_FECHA_LISTA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun PantallaNumerosBloqueados(
    viewModel: BloqueosViewModel,
    onVolver: () -> Unit,
    onGestionarCategorias: () -> Unit
) {
    BackHandler(onBack = onVolver)

    val numeros by viewModel.numeros.collectAsState()
    val listado by viewModel.listado.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val filtro by viewModel.filtro.collectAsState()

    val context = LocalContext.current
    val alcance = rememberCoroutineScope()

    var numeroEnEdicion by remember { mutableStateOf<NumeroBloqueado?>(null) }
    var numeroAEliminar by remember { mutableStateOf<NumeroBloqueado?>(null) }
    var mensaje by remember { mutableStateOf<MensajeBloqueos?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    var menuCategoriaAbierto by remember { mutableStateOf(false) }
    var menuOrdenAbierto by remember { mutableStateOf(false) }

    val nombresCategoria = remember(categorias) {
        categorias.associate { it.id to it.nombre }
    }

    fun nombreDeCategoria(id: Long): String = nombresCategoria[id] ?: "Otro"

    fun categoriaPorDefecto(): Long =
        categorias.firstOrNull { it.nombre.equals("Otro", ignoreCase = true) }?.id
            ?: categorias.firstOrNull()?.id
            ?: 0L

    val categoriaSeleccionada = categorias.firstOrNull { it.id == filtro.categoriaId }
    val subtitulo = if (filtro.hayFiltros) {
        "${listado.size} de ${cuandoHayNumeros(numeros.size, categorias.size)}"
    } else {
        cuandoHayNumeros(numeros.size, categorias.size)
    }

    val importador = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            alcance.launch {
                trabajando = true
                mensaje = null
                val resultado = runCatching {
                    withContext(Dispatchers.IO) {
                        val texto = context.contentResolver.openInputStream(uri)
                            ?.use { entrada ->
                                entrada.bufferedReader(Charsets.UTF_8).readText()
                            }
                            ?: error("No se pudo abrir el archivo")
                        viewModel.importarCsv(texto)
                    }
                }
                trabajando = false

                resultado.onSuccess { resumen ->
                    mensaje = if (resumen.total == 0) {
                        MensajeBloqueos(
                            if (resumen.errores > 0) {
                                "No se pudo importar ninguna fila. Revisa el formato del CSV."
                            } else {
                                "El archivo no contenía números que importar."
                            },
                            esError = true
                        )
                    } else {
                        val texto = buildString {
                            append(
                                "Importados ${resumen.total}: " +
                                    "${resumen.nuevos} nuevos y " +
                                    "${resumen.actualizados} actualizados."
                            )
                            if (resumen.categoriasCreadas > 0) {
                                append(
                                    " ${resumen.categoriasCreadas} " +
                                        if (resumen.categoriasCreadas == 1) {
                                            "categoría nueva."
                                        } else {
                                            "categorías nuevas."
                                        }
                                )
                            }
                            if (resumen.errores > 0) {
                                append(" ${resumen.errores} fila(s) con errores.")
                            }
                        }
                        MensajeBloqueos(texto, esError = resumen.errores > 0)
                    }
                }.onFailure {
                    mensaje = MensajeBloqueos(
                        "No se pudo leer el CSV. Comprueba el archivo e inténtalo de nuevo.",
                        esError = true
                    )
                }
            }
        }
    }

    val exportador = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            alcance.launch {
                trabajando = true
                mensaje = null
                val resultado = runCatching {
                    withContext(Dispatchers.IO) {
                        val csv = viewModel.exportarCsv()
                        context.contentResolver.openOutputStream(uri)?.use { salida ->
                            salida.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("No se pudo abrir el archivo")
                    }
                }
                trabajando = false

                mensaje = if (resultado.isSuccess) {
                    MensajeBloqueos(
                        "Lista exportada correctamente con numero,motivo,categoria.",
                        esError = false
                    )
                } else {
                    MensajeBloqueos(
                        "No se pudo guardar el archivo. Inténtalo de nuevo.",
                        esError = true
                    )
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
                titulo = "Números bloqueados",
                subtitulo = subtitulo,
                onVolver = onVolver
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // ---------- Búsqueda y filtros ----------

                OutlinedTextField(
                    value = filtro.texto,
                    onValueChange = viewModel::actualizarTexto,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar") },
                    placeholder = { Text("Número o motivo") },
                    leadingIcon = { Text("🔍", fontSize = 17.sp) },
                    trailingIcon = {
                        if (filtro.texto.isNotEmpty()) {
                            IconButton(onClick = { viewModel.actualizarTexto("") }) {
                                Text("✕", fontSize = 16.sp)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { menuCategoriaAbierto = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = categoriaSeleccionada?.nombre
                                    ?: "Todas las categorías",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                            Text("▾")
                        }

                        DropdownMenu(
                            expanded = menuCategoriaAbierto,
                            onDismissRequest = { menuCategoriaAbierto = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas las categorías") },
                                onClick = {
                                    viewModel.seleccionarCategoria(null)
                                    menuCategoriaAbierto = false
                                },
                                trailingIcon = {
                                    if (filtro.categoriaId == null) Text("✓")
                                }
                            )
                            categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria.nombre) },
                                    onClick = {
                                        viewModel.seleccionarCategoria(categoria.id)
                                        menuCategoriaAbierto = false
                                    },
                                    trailingIcon = {
                                        if (filtro.categoriaId == categoria.id) {
                                            Text("✓")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { menuOrdenAbierto = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "${filtro.orden.etiquetaCorta} " +
                                    if (filtro.ascendente) "↑" else "↓",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                            Text("▾")
                        }

                        DropdownMenu(
                            expanded = menuOrdenAbierto,
                            onDismissRequest = { menuOrdenAbierto = false }
                        ) {
                            OrdenNumeros.entries.forEach { orden ->
                                listOf(true, false).forEach { ascendente ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${orden.etiqueta} " +
                                                    if (ascendente) "↑ ascendente"
                                                    else "↓ descendente"
                                            )
                                        },
                                        onClick = {
                                            viewModel.ordenar(orden, ascendente)
                                            menuOrdenAbierto = false
                                        },
                                        trailingIcon = {
                                            if (filtro.orden == orden &&
                                                filtro.ascendente == ascendente
                                            ) {
                                                Text("✓")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EstadoNumeros.entries.forEach { estado ->
                        FilterChip(
                            selected = filtro.estado == estado,
                            onClick = { viewModel.seleccionarEstado(estado) },
                            label = {
                                Text(
                                    estado.etiqueta,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                if (filtro.hayFiltros) {
                    TextButton(
                        onClick = viewModel::limpiarFiltros,
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            "Limpiar filtros",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // ---------- Acciones de mantenimiento ----------

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onGestionarCategorias,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Categorías", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { importador.launch(arrayOf("*/*")) },
                        enabled = !trabajando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Importar", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { exportador.launch(nombreCsvSugerido()) },
                        enabled = numeros.isNotEmpty() && !trabajando,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Exportar", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "CSV: numero,motivo,categoria. El formato antiguo de dos " +
                        "columnas se importa como «Otro».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trabajando) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            mensaje?.let { aviso ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (aviso.esError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = aviso.texto,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (aviso.esError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            when {
                numeros.isEmpty() -> {
                    EstadoVacioBloqueados(
                        titulo = "Tu lista está vacía",
                        detalle = "Añade números concretos para bloquearlos además " +
                            "de la regla del 400."
                    )
                }

                listado.isEmpty() -> {
                    EstadoVacioBloqueados(
                        titulo = "Sin resultados",
                        detalle = "Ningún número coincide con la búsqueda y los " +
                            "filtros seleccionados.",
                        accion = {
                            TextButton(onClick = viewModel::limpiarFiltros) {
                                Text("Limpiar filtros", fontWeight = FontWeight.Bold)
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
                            top = 4.dp,
                            bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(listado, key = { it.id }) { numero ->
                            TarjetaNumeroBloqueado(
                                numero = numero,
                                onClick = {
                                    numeroEnEdicion = numero.comoEntidad()
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    numeroEnEdicion = NumeroBloqueado(
                        numero = "",
                        motivo = "",
                        categoriaId = categoriaPorDefecto(),
                        fechaAlta = System.currentTimeMillis(),
                        tipo = TipoBloqueo.PERMANENTE,
                        expiraEn = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "+ Añadir número",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    numeroEnEdicion?.let { numero ->
        DialogoNumeroBloqueado(
            numeroInicial = numero.copy(
                categoriaId = numero.categoriaId.takeIf { id ->
                    categorias.any { it.id == id }
                } ?: categoriaPorDefecto()
            ),
            categorias = categorias,
            numerosExistentes = numeros,
            onDismiss = { numeroEnEdicion = null },
            onGuardar = {
                viewModel.guardarNumero(it)
                numeroEnEdicion = null
            },
            onEliminar = if (numero.id != 0L) {
                {
                    numeroAEliminar = numero
                    numeroEnEdicion = null
                }
            } else {
                null
            }
        )
    }

    numeroAEliminar?.let { numero ->
        AlertDialog(
            onDismissRequest = { numeroAEliminar = null },
            title = { Text("Quitar número") },
            text = {
                Text(
                    "¿Quieres quitar ${numero.numero} de la lista? " +
                        "La regla automática del 400 no cambia."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarNumero(numero.id)
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
private fun ColumnScope.EstadoVacioBloqueados(
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🚫", fontSize = 30.sp)
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

private fun cuandoHayNumeros(numeros: Int, categorias: Int): String {
    val textoNumeros = if (numeros == 1) "1 número" else "$numeros números"
    val textoCategorias = if (categorias == 1) "1 categoría" else "$categorias categorías"
    return "$textoNumeros · $textoCategorias"
}

private fun nombreCsvSugerido(): String {
    val fecha = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    return "filtro400_numeros_$fecha.csv"
}

@Composable
private fun TarjetaNumeroBloqueado(
    numero: NumeroBloqueadoListado,
    onClick: () -> Unit
) {
    val ahora = System.currentTimeMillis()
    val expirado = numero.tipo == TipoBloqueo.TEMPORAL &&
        numero.expiraEn != null &&
        numero.expiraEn <= ahora

    val estadoTexto = when {
        expirado -> "Expirado el ${formatearDia(numero.expiraEn!!)}"
        numero.tipo == TipoBloqueo.TEMPORAL && numero.expiraEn != null ->
            "Temporal · caduca el ${formatearDia(numero.expiraEn)}"
        numero.tipo == TipoBloqueo.TEMPORAL -> "Temporal · sin caducidad"
        else -> "Permanente"
    }
    val estadoColor = when {
        expirado -> MaterialTheme.colorScheme.error
        numero.tipo == TipoBloqueo.TEMPORAL -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                    .background(
                        if (expirado) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (expirado) "⌛" else "☎",
                    fontSize = 18.sp,
                    color = if (expirado) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = numero.numero,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (numero.motivo.isNotBlank()) {
                    Text(
                        text = numero.motivo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipCategoria(nombre = numero.categoria)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = estadoTexto,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = estadoColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alta ${formatearDia(numero.fechaAlta)} · " +
                        cuandoHayIntentos(numero.intentos),
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

private fun cuandoHayIntentos(intentos: Int): String =
    if (intentos == 1) "1 intento" else "$intentos intentos"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNumeroBloqueado(
    numeroInicial: NumeroBloqueado,
    categorias: List<CategoriaBloqueo>,
    numerosExistentes: List<NumeroBloqueado>,
    onDismiss: () -> Unit,
    onGuardar: (NumeroBloqueado) -> Unit,
    onEliminar: (() -> Unit)?
) {
    var numero by remember(numeroInicial.id) { mutableStateOf(numeroInicial.numero) }
    var motivo by remember(numeroInicial.id) { mutableStateOf(numeroInicial.motivo) }
    var categoriaId by remember(numeroInicial.id) {
        mutableStateOf(numeroInicial.categoriaId)
    }
    var tipo by remember(numeroInicial.id) { mutableStateOf(numeroInicial.tipo) }
    var expiraEn by remember(numeroInicial.id) { mutableStateOf(numeroInicial.expiraEn) }
    var errorNumero by remember(numeroInicial.id) { mutableStateOf<String?>(null) }
    var errorCategoria by remember(numeroInicial.id) { mutableStateOf<String?>(null) }
    var errorFecha by remember(numeroInicial.id) { mutableStateOf<String?>(null) }
    var menuCategoriasAbierto by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val categoriaSeleccionada = categorias.firstOrNull { it.id == categoriaId }
    val fechaYaPasada = tipo == TipoBloqueo.TEMPORAL &&
        expiraEn != null &&
        expiraEn!! <= System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (numeroInicial.id == 0L) "Añadir número" else "Editar número")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                    placeholder = { Text("Ej. Llamadas repetidas") },
                    maxLines = 2,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box {
                    OutlinedButton(
                        onClick = { menuCategoriasAbierto = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Categoría",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = categoriaSeleccionada?.nombre ?: "Sin categoría",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (categoriaSeleccionada == null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        Text("▾")
                    }

                    DropdownMenu(
                        expanded = menuCategoriasAbierto,
                        onDismissRequest = { menuCategoriasAbierto = false }
                    ) {
                        if (categorias.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay categorías") },
                                onClick = { menuCategoriasAbierto = false },
                                enabled = false
                            )
                        } else {
                            categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria.nombre) },
                                    onClick = {
                                        categoriaId = categoria.id
                                        errorCategoria = null
                                        menuCategoriasAbierto = false
                                    },
                                    trailingIcon = {
                                        if (categoria.id == categoriaId) {
                                            Text("✓")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (categorias.isEmpty()) {
                    Text(
                        text = "Crea una categoría desde la pantalla Categorías.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    errorCategoria?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Duración del bloqueo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipo == TipoBloqueo.PERMANENTE,
                        onClick = {
                            tipo = TipoBloqueo.PERMANENTE
                            expiraEn = null
                            errorFecha = null
                        },
                        label = { Text("Permanente") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    FilterChip(
                        selected = tipo == TipoBloqueo.TEMPORAL,
                        onClick = {
                            tipo = TipoBloqueo.TEMPORAL
                            if (expiraEn == null) {
                                expiraEn = finDelDia(LocalDate.now().plusDays(7))
                            }
                            errorFecha = null
                        },
                        label = { Text("Temporal") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                if (tipo == TipoBloqueo.TEMPORAL) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { mostrarCalendario = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = expiraEn?.let {
                                "Caduca el ${formatearDia(it)}"
                            } ?: "Elegir fecha de caducidad",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Text("📅")
                    }
                    errorFecha?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (fechaYaPasada) {
                        Text(
                            text = "La fecha ya pasó: el número aparecerá como " +
                                "expirado y no bloqueará.",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Text(
                            text = "Al caducar dejará de bloquear, pero seguirá en " +
                                "la lista hasta que lo elimines.",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Permanece bloqueado hasta que lo elimines.",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onEliminar != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onEliminar,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar de la lista")
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

                        numerosExistentes.any {
                            it.numero == normalizado && it.id != numeroInicial.id
                        } -> {
                            errorNumero = "Ese número ya está en la lista."
                        }

                        categoriaId <= 0L -> {
                            errorCategoria = "Elige una categoría."
                        }

                        tipo == TipoBloqueo.TEMPORAL && expiraEn == null -> {
                            errorFecha = "Elige una fecha de caducidad."
                        }

                        else -> {
                            onGuardar(
                                numeroInicial.copy(
                                    numero = normalizado,
                                    motivo = motivo.trim(),
                                    categoriaId = categoriaId,
                                    tipo = tipo,
                                    expiraEn = if (tipo == TipoBloqueo.TEMPORAL) {
                                        expiraEn
                                    } else {
                                        null
                                    }
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

    if (mostrarCalendario) {
        // La app está en español, pero Material3 usa el idioma del sistema para
        // el calendario. Forzamos es-ES para que el selector no aparezca en inglés.
        val contexto = LocalContext.current
        val configuracionCalendario = remember(contexto) {
            Configuration(contexto.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags("es-ES"))
            }
        }
        val contextoCalendario = remember(configuracionCalendario) {
            contexto.createConfigurationContext(configuracionCalendario)
        }

        CompositionLocalProvider(
            LocalContext provides contextoCalendario,
            LocalConfiguration provides configuracionCalendario
        ) {
            val selector = rememberDatePickerState(
                initialSelectedDateMillis = expiraEn?.let { inicioDelDiaUtc(it) }
                    ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { mostrarCalendario = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selector.selectedDateMillis?.let {
                                expiraEn = fechaDesdeSelector(it)
                            }
                            errorFecha = null
                            mostrarCalendario = false
                        }
                    ) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarCalendario = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                // El diálogo crea su propia composición con el contexto del
                // sistema; volvemos a aplicar el idioma dentro de su contenido.
                CompositionLocalProvider(
                    LocalContext provides contextoCalendario,
                    LocalConfiguration provides configuracionCalendario
                ) {
                    DatePicker(state = selector)
                }
            }
        }
    }
}

private fun formatearDia(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(FORMATO_FECHA_LISTA)

private fun finDelDia(fecha: LocalDate): Long =
    fecha.atTime(23, 59, 59, 999_000_000)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun inicioDelDiaUtc(millis: Long): Long =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

private fun fechaDesdeSelector(millisUtc: Long): Long =
    Instant.ofEpochMilli(millisUtc)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .let(::finDelDia)
