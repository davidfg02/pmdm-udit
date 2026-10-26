package com.example.filtro400

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EdicionCategoria(
    val id: Long,
    val nombre: String
)

@Composable
fun PantallaCategorias(
    viewModel: BloqueosViewModel,
    onVolver: () -> Unit
) {
    BackHandler(onBack = onVolver)

    val categorias by viewModel.categoriasConConteo.collectAsState()

    var categoriaEnEdicion by remember { mutableStateOf<EdicionCategoria?>(null) }
    var categoriaAEliminar by remember { mutableStateOf<CategoriaConConteo?>(null) }

    val subtitulo = when (categorias.size) {
        1 -> "1 categoría"
        else -> "${categorias.size} categorías"
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
                titulo = "Categorías",
                subtitulo = subtitulo,
                onVolver = onVolver
            )

            Text(
                text = "Agrupa tus números bloqueados. Si eliminas una categoría, " +
                    "sus números pasan a otra: nunca se borran.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (categorias.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay categorías. Crea la primera para poder " +
                            "organizar tus números.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categorias, key = { it.id }) { categoria ->
                        val (fondoAvatar, colorAvatar) = coloresCategoria(categoria.nombre)
                        ElevatedCard(
                            onClick = {
                                categoriaEnEdicion = EdicionCategoria(
                                    id = categoria.id,
                                    nombre = categoria.nombre
                                )
                            },
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
                                        .background(fondoAvatar),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = categoria.nombre
                                            .trim()
                                            .firstOrNull()
                                            ?.uppercaseChar()
                                            ?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorAvatar
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoria.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (categoria.total) {
                                            0 -> "Sin números"
                                            1 -> "1 número"
                                            else -> "${categoria.total} números"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = { categoriaAEliminar = categoria },
                                    enabled = categorias.size > 1,
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "Eliminar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (categorias.size > 1) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                Text(
                                    text = "✏️",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    categoriaEnEdicion = EdicionCategoria(id = 0, nombre = "")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "+ Nueva categoría",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    categoriaEnEdicion?.let { edicion ->
        DialogoCategoria(
            edicion = edicion,
            categorias = categorias,
            onDismiss = { categoriaEnEdicion = null },
            onGuardar = { nombre ->
                if (edicion.id == 0L) {
                    viewModel.crearCategoria(nombre)
                } else {
                    viewModel.renombrarCategoria(edicion.id, nombre)
                }
                categoriaEnEdicion = null
            }
        )
    }

    categoriaAEliminar?.let { categoria ->
        val destino = categorias.firstOrNull {
            it.id != categoria.id && it.nombre.equals("Otro", ignoreCase = true)
        } ?: categorias.firstOrNull { it.id != categoria.id }

        AlertDialog(
            onDismissRequest = { categoriaAEliminar = null },
            title = { Text("Eliminar categoría") },
            text = {
                val destinoNombre = destino?.nombre ?: "Otro"
                val texto = buildString {
                    append("«${categoria.nombre}» se eliminará. ")
                    when (categoria.total) {
                        0 -> append("No tiene números asociados. ")
                        1 -> append("Su único número pasará a «$destinoNombre». ")
                        else -> append(
                            "Sus ${categoria.total} números pasarán a «$destinoNombre». "
                        )
                    }
                    append("No se borrará ningún número.")
                }
                Text(texto)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarCategoria(categoria.id)
                        categoriaAEliminar = null
                    }
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { categoriaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DialogoCategoria(
    edicion: EdicionCategoria,
    categorias: List<CategoriaConConteo>,
    onDismiss: () -> Unit,
    onGuardar: (String) -> Unit
) {
    var nombre by remember(edicion.id) { mutableStateOf(edicion.nombre) }
    var error by remember(edicion.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (edicion.id == 0L) "Nueva categoría" else "Renombrar categoría")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej. Encuestas") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    shape = RoundedCornerShape(16.dp)
                )
                if (edicion.id != 0L) {
                    Text(
                        text = "El cambio se aplica a todos los números de la categoría.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limpio = nombre.trim()
                    when {
                        limpio.isBlank() -> {
                            error = "Escribe un nombre."
                        }

                        categorias.any {
                            it.id != edicion.id &&
                                it.nombre.equals(limpio, ignoreCase = true)
                        } -> {
                            error = "Ya existe una categoría con ese nombre."
                        }

                        else -> onGuardar(limpio)
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
