package com.example.filtro400.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = PrimarioClaro,
    onPrimary = SobrePrimarioClaro,
    primaryContainer = ContenedorPrimarioClaro,
    onPrimaryContainer = SobreContenedorPrimarioClaro,

    secondary = SecundarioClaro,
    onSecondary = SobreSecundarioClaro,
    secondaryContainer = ContenedorSecundarioClaro,
    onSecondaryContainer = SobreContenedorSecundarioClaro,

    tertiary = TerciarioClaro,
    onTertiary = SobreTerciarioClaro,
    tertiaryContainer = ContenedorTerciarioClaro,
    onTertiaryContainer = SobreContenedorTerciarioClaro,

    background = FondoClaro,
    onBackground = SobreFondoClaro,
    surface = SuperficieClaro,
    onSurface = SobreSuperficieClaro,
    surfaceVariant = SuperficieVarianteClaro,
    onSurfaceVariant = SobreSuperficieVarianteClaro,
    outline = ContornoClaro,
    outlineVariant = ContornoVarianteClaro,

    error = ErrorClaro,
    onError = SobreErrorClaro,
    errorContainer = ContenedorErrorClaro,
    onErrorContainer = SobreContenedorErrorClaro
)

private val EsquemaOscuro = darkColorScheme(
    primary = PrimarioOscuro,
    onPrimary = SobrePrimarioOscuro,
    primaryContainer = ContenedorPrimarioOscuro,
    onPrimaryContainer = SobreContenedorPrimarioOscuro,

    secondary = SecundarioOscuro,
    onSecondary = SobreSecundarioOscuro,
    secondaryContainer = ContenedorSecundarioOscuro,
    onSecondaryContainer = SobreContenedorSecundarioOscuro,

    tertiary = TerciarioOscuro,
    onTertiary = SobreTerciarioOscuro,
    tertiaryContainer = ContenedorTerciarioOscuro,
    onTertiaryContainer = SobreContenedorTerciarioOscuro,

    background = FondoOscuro,
    onBackground = SobreFondoOscuro,
    surface = SuperficieOscuro,
    onSurface = SobreSuperficieOscuro,
    surfaceVariant = SuperficieVarianteOscuro,
    onSurfaceVariant = SobreSuperficieVarianteOscuro,
    outline = ContornoOscuro,
    outlineVariant = ContornoVarianteOscuro,

    error = ErrorOscuro,
    onError = SobreErrorOscuro,
    errorContainer = ContenedorErrorOscuro,
    onErrorContainer = SobreContenedorErrorOscuro
)

// Esquinas generosas: es lo que le da el aire "moderno" a las tarjetas.
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun Filtro400Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivado a propósito: la app tiene su propia identidad de color y
    // no queremos que el fondo de pantalla del móvil la cambie.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
