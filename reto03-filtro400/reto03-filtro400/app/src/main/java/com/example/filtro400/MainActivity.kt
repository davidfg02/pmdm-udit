package com.example.filtro400

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.filtro400.ui.theme.DegradadoFin
import com.example.filtro400.ui.theme.DegradadoInicio
import com.example.filtro400.ui.theme.EstadoActivo
import com.example.filtro400.ui.theme.EstadoInactivo
import com.example.filtro400.ui.theme.Filtro400Theme
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Una sola instancia para la actividad: sobrevive a las rotaciones.
        val historialViewModel =
            ViewModelProvider(this)[HistorialViewModel::class.java]
        val franjasViewModel =
            ViewModelProvider(this)[FranjasViewModel::class.java]
        val bloqueosViewModel =
            ViewModelProvider(this)[BloqueosViewModel::class.java]

        setContent {
            Filtro400Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppFiltro400(historialViewModel, franjasViewModel, bloqueosViewModel)
                }
            }
        }
    }
}

private enum class PantallaApp {
    PRINCIPAL,
    HISTORIAL,
    FRANJAS,
    ESTADISTICAS,
    NUMEROS,
    PERMITIDOS,
    CATEGORIAS
}

private enum class EstadoPrueba {
    BLOQUEADA,
    FUERA_DE_FRANJA,
    PASA
}

private data class ResultadoPrueba(
    val estado: EstadoPrueba,
    val detalle: String
)

// Navegación mínima: un estado decide qué pantalla se ve.
@Composable
private fun AppFiltro400(
    historialViewModel: HistorialViewModel,
    franjasViewModel: FranjasViewModel,
    bloqueosViewModel: BloqueosViewModel
) {
    var pantalla by rememberSaveable { mutableStateOf(PantallaApp.PRINCIPAL) }
    var origenCategorias by rememberSaveable {
        mutableStateOf(PantallaApp.PRINCIPAL)
    }
    val noLeidas by historialViewModel.noLeidas.collectAsState()
    val configuracionFranjas by franjasViewModel.configuracion.collectAsState()
    val numerosBloqueados by bloqueosViewModel.numeros.collectAsState()
    val numerosPermitidos by bloqueosViewModel.permitidos.collectAsState()
    val categorias by bloqueosViewModel.categorias.collectAsState()

    Crossfade(targetState = pantalla, label = "navegacion") { pantallaActual ->
        when (pantallaActual) {
            PantallaApp.PRINCIPAL -> PantallaFiltro(
                noLeidas = noLeidas,
                configuracionFranjas = configuracionFranjas,
                numerosBloqueados = numerosBloqueados,
                numerosPermitidos = numerosPermitidos,
                totalCategorias = categorias.size,
                onAbrirHistorial = { pantalla = PantallaApp.HISTORIAL },
                onAbrirFranjas = { pantalla = PantallaApp.FRANJAS },
                onAbrirEstadisticas = { pantalla = PantallaApp.ESTADISTICAS },
                onAbrirNumeros = { pantalla = PantallaApp.NUMEROS },
                onAbrirPermitidos = { pantalla = PantallaApp.PERMITIDOS },
                onAbrirCategorias = {
                    origenCategorias = PantallaApp.PRINCIPAL
                    pantalla = PantallaApp.CATEGORIAS
                }
            )

            PantallaApp.HISTORIAL -> PantallaHistorial(
                viewModel = historialViewModel,
                onVolver = { pantalla = PantallaApp.PRINCIPAL }
            )

            PantallaApp.FRANJAS -> PantallaFranjas(
                viewModel = franjasViewModel,
                onVolver = { pantalla = PantallaApp.PRINCIPAL }
            )

            PantallaApp.ESTADISTICAS -> PantallaEstadisticas(
                viewModel = historialViewModel,
                onVolver = { pantalla = PantallaApp.PRINCIPAL }
            )

            PantallaApp.NUMEROS -> PantallaNumerosBloqueados(
                viewModel = bloqueosViewModel,
                onVolver = { pantalla = PantallaApp.PRINCIPAL },
                onGestionarCategorias = {
                    origenCategorias = PantallaApp.NUMEROS
                    pantalla = PantallaApp.CATEGORIAS
                }
            )

            PantallaApp.PERMITIDOS -> PantallaNumerosPermitidos(
                viewModel = bloqueosViewModel,
                onVolver = { pantalla = PantallaApp.PRINCIPAL }
            )

            PantallaApp.CATEGORIAS -> PantallaCategorias(
                viewModel = bloqueosViewModel,
                onVolver = { pantalla = origenCategorias }
            )
        }
    }
}

@Composable
fun PantallaFiltro(
    noLeidas: Int,
    configuracionFranjas: ConfiguracionFranjas,
    numerosBloqueados: List<NumeroBloqueado>,
    numerosPermitidos: List<NumeroPermitido>,
    totalCategorias: Int,
    onAbrirHistorial: () -> Unit,
    onAbrirFranjas: () -> Unit,
    onAbrirEstadisticas: () -> Unit,
    onAbrirNumeros: () -> Unit,
    onAbrirPermitidos: () -> Unit,
    onAbrirCategorias: () -> Unit
) {
    val usarFranjas = configuracionFranjas.usarFranjas
    val context = LocalContext.current

    // RoleManager: el servicio de Android que gestiona "roles" como
    // "app de filtrado de llamadas". Sin el rol, nuestro servicio no se activa.
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }

    // Estado que la pantalla "vigila": si el filtro está activo.
    var filtroActivo by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }
    var numeroPrueba by rememberSaveable { mutableStateOf("") }
    // null = todavía no se ha probado nada.
    var resultadoPrueba by remember { mutableStateOf<ResultadoPrueba?>(null) }

    // Cuando el usuario responde al diálogo del sistema, volvemos aquí
    // y volvemos a comprobar si ahora tenemos el rol.
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        filtroActivo = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            BarraSuperiorFiltro(
                noLeidas = noLeidas,
                onAbrirHistorial = onAbrirHistorial
            )

            Spacer(modifier = Modifier.height(24.dp))

            TarjetaEstado(
                activo = filtroActivo,
                usarFranjas = usarFranjas,
                onActivar = {
                    val intent =
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    lanzador.launch(intent)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            TarjetaProbador(
                numero = numeroPrueba,
                onNumeroChange = {
                    numeroPrueba = it
                    resultadoPrueba = null
                },
                resultado = resultadoPrueba,
                onProbar = {
                    val ahora = LocalTime.now()
                    val minutoActual = ahora.hour * 60 + ahora.minute
                    val numeroNormalizado = ReglaBloqueo.normalizar(numeroPrueba)
                    val ahoraMillis = System.currentTimeMillis()
                    val enListaBlanca = numerosPermitidos.any {
                        it.numero == numeroNormalizado
                    }
                    val enLista = numerosBloqueados.any {
                        it.numero == numeroNormalizado && it.estaActivo(ahoraMillis)
                    }
                    val bloqueoExpirado = numerosBloqueados.any {
                        it.numero == numeroNormalizado && it.estaExpirado(ahoraMillis)
                    }
                    val porPrefijo = ReglaBloqueo.debeBloquearse(numeroPrueba)
                    val coincide = enLista || porPrefijo
                    val dentroDeFranja = ReglaHoraria.permiteBloquear(
                        configuracionFranjas,
                        minutoActual
                    )

                    resultadoPrueba = when {
                        enListaBlanca -> ResultadoPrueba(
                            estado = EstadoPrueba.PASA,
                            detalle = "El número está en tu lista blanca: nunca " +
                                "se bloquea."
                        )

                        !coincide && bloqueoExpirado -> ResultadoPrueba(
                            estado = EstadoPrueba.PASA,
                            detalle = "El bloqueo manual de este número ha caducado."
                        )

                        !coincide -> ResultadoPrueba(
                            estado = EstadoPrueba.PASA,
                            detalle = "El número no está en tu lista ni empieza por 400."
                        )

                        !dentroDeFranja -> ResultadoPrueba(
                            estado = EstadoPrueba.FUERA_DE_FRANJA,
                            detalle = "El número se bloquearía, pero ahora estás " +
                                "fuera de tus franjas horarias."
                        )

                        enLista && porPrefijo -> ResultadoPrueba(
                            estado = EstadoPrueba.BLOQUEADA,
                            detalle = "Está en tu lista de bloqueados y además " +
                                "empieza por 400."
                        )

                        enLista -> ResultadoPrueba(
                            estado = EstadoPrueba.BLOQUEADA,
                            detalle = "El número está en tu lista de bloqueados."
                        )

                        else -> ResultadoPrueba(
                            estado = EstadoPrueba.BLOQUEADA,
                            detalle = "El número empieza por 400 y estás dentro " +
                                "de una franja activa."
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            FilaAccesos(
                usarFranjas = usarFranjas,
                totalNumeros = numerosBloqueados.size,
                totalPermitidos = numerosPermitidos.size,
                totalCategorias = totalCategorias,
                onAbrirFranjas = onAbrirFranjas,
                onAbrirEstadisticas = onAbrirEstadisticas,
                onAbrirNumeros = onAbrirNumeros,
                onAbrirPermitidos = onAbrirPermitidos,
                onAbrirCategorias = onAbrirCategorias
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Las llamadas rechazadas se guardan en el historial y alimentan las estadísticas.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilaAccesos(
    usarFranjas: Boolean,
    totalNumeros: Int,
    totalPermitidos: Int,
    totalCategorias: Int,
    onAbrirFranjas: () -> Unit,
    onAbrirEstadisticas: () -> Unit,
    onAbrirNumeros: () -> Unit,
    onAbrirPermitidos: () -> Unit,
    onAbrirCategorias: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaAcceso(
                emoji = "🚫",
                titulo = "Bloqueados",
                detalle = when (totalNumeros) {
                    0 -> "Añade el primero"
                    1 -> "1 número en tu lista"
                    else -> "$totalNumeros números en tu lista"
                },
                onClick = onAbrirNumeros,
                modifier = Modifier.weight(1f)
            )
            TarjetaAcceso(
                emoji = "✅",
                titulo = "Permitidos",
                detalle = when (totalPermitidos) {
                    0 -> "Siempre pasan"
                    1 -> "1 número permitido"
                    else -> "$totalPermitidos números permitidos"
                },
                onClick = onAbrirPermitidos,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaAcceso(
                emoji = "🕐",
                titulo = "Franjas",
                detalle = if (usarFranjas) "Solo en tus horarios" else "A cualquier hora",
                onClick = onAbrirFranjas,
                modifier = Modifier.weight(1f)
            )
            TarjetaAcceso(
                emoji = "📊",
                titulo = "Estadísticas",
                detalle = "Resumen y CSV",
                onClick = onAbrirEstadisticas,
                modifier = Modifier.weight(1f)
            )
        }
        TarjetaAcceso(
            emoji = "🏷️",
            titulo = "Categorías",
            detalle = when (totalCategorias) {
                1 -> "1 categoría"
                else -> "$totalCategorias categorías"
            },
            onClick = onAbrirCategorias,
            modifier = Modifier.fillMaxWidth(),
            ancha = true
        )
    }
}

@Composable
private fun TarjetaAcceso(
    emoji: String,
    titulo: String,
    detalle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ancha: Boolean = false
) {
    ElevatedCard(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        if (ancha) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BarraSuperiorFiltro(
    noLeidas: Int,
    onAbrirHistorial: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Logotipo: las tres cifras del filtro sobre el degradado de marca.
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "400",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Filtro 400",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Protección de llamadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BadgedBox(
            badge = {
                if (noLeidas > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(noLeidas.toString())
                    }
                }
            }
        ) {
            FilledTonalButton(
                onClick = onAbrirHistorial,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("Historial", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TarjetaEstado(
    activo: Boolean,
    usarFranjas: Boolean,
    onActivar: () -> Unit
) {
    // Latido suave del punto de estado: solo cuando el filtro está activo.
    val transicion = rememberInfiniteTransition(label = "latido")
    val opacidadPunto by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacidadPunto"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = DegradadoInicio.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin))
            )
    ) {
        // Círculo decorativo que asoma por la esquina.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-64).dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 32.dp, y = 48.dp)
                .size(110.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (activo) EstadoActivo else EstadoInactivo)
                                        .copy(alpha = if (activo) opacidadPunto else 1f)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activo) "PROTECCIÓN ACTIVA" else "SIN PROTECCIÓN",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (activo) "🛡️" else "⚠️",
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (activo) "Filtro activo" else "Filtro inactivo",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (activo) {
                    if (usarFranjas) {
                        "Se rechazan las llamadas 400 y los números de tu lista " +
                            "dentro de tus franjas horarias."
                    } else {
                        "Se rechazan las llamadas 400 y los números de tu lista " +
                            "de bloqueados."
                    }
                } else {
                    "Activa el filtro para rechazar las llamadas 400 y los " +
                        "números de tu lista."
                },
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (activo) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓",
                        color = EstadoActivo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Todo listo, no tienes que hacer nada más",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Button(
                    onClick = onActivar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DegradadoInicio
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Activar filtro de llamadas",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaProbador(
    numero: String,
    onNumeroChange: (String) -> Unit,
    resultado: ResultadoPrueba?,
    onProbar: () -> Unit
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
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☎",
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Probador de números",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Comprueba la regla sin recibir una llamada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = numero,
                onValueChange = onNumeroChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Número de teléfono") },
                placeholder = { Text("Ej. +34 400 123 456") },
                leadingIcon = {
                    Text(text = "📱", fontSize = 18.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onProbar,
                enabled = numero.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Comprobar número",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            AnimatedVisibility(
                visible = resultado != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val estado = resultado?.estado
                val bloqueada = estado == EstadoPrueba.BLOQUEADA
                val fueraDeFranja = estado == EstadoPrueba.FUERA_DE_FRANJA
                val detalle = resultado?.detalle.orEmpty()

                val fondo = when {
                    bloqueada -> MaterialTheme.colorScheme.errorContainer
                    fueraDeFranja -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val contenido = when {
                    bloqueada -> MaterialTheme.colorScheme.onErrorContainer
                    fueraDeFranja -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(fondo)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            bloqueada -> "🚫"
                            fueraDeFranja -> "🕐"
                            else -> "✅"
                        },
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                bloqueada -> "Se bloquearía"
                                fueraDeFranja -> "Pasaría por el horario"
                                else -> "Pasaría la llamada"
                            },
                            color = contenido,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = detalle,
                            color = contenido.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
