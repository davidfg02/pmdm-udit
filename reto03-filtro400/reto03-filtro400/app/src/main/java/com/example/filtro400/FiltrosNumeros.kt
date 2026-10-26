package com.example.filtro400

/** Un bloqueo manual puede ser indefinido o caducar en una fecha. */
enum class TipoBloqueo {
    PERMANENTE,
    TEMPORAL
}

/**
 * Estados que se pueden combinar con la búsqueda y la categoría.
 *
 * - ACTIVOS: bloqueos manuales que todavía están vigentes (permanentes o
 *   temporales sin caducar).
 * - TEMPORALES: temporales que todavía no han caducado.
 * - PERMANENTES: bloqueos sin fecha de caducidad.
 * - EXPIRADOS: temporales cuya fecha de caducidad ya pasó.
 */
enum class EstadoNumeros(val etiqueta: String) {
    TODOS("Todos"),
    ACTIVOS("Activos"),
    TEMPORALES("Temporales"),
    PERMANENTES("Permanentes"),
    EXPIRADOS("Expirados")
}

enum class OrdenNumeros(val etiqueta: String, val etiquetaCorta: String) {
    FECHA("Fecha de alta", "Fecha"),
    NUMERO("Número", "Número"),
    INTENTOS("Intentos", "Intentos"),
    CATEGORIA("Categoría", "Categoría")
}
