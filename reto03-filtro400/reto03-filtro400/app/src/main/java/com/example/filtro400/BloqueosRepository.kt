package com.example.filtro400

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

data class ResumenImportacion(
    val nuevos: Int,
    val actualizados: Int,
    val categoriasCreadas: Int,
    val errores: Int
) {
    val total: Int get() = nuevos + actualizados
}

/**
 * Toda la lógica de la lista de bloqueados, sin depender de Compose.
 * Así la importación/exportación, las categorías y los filtros se pueden
 * probar aparte.
 */
class BloqueosRepository(private val dao: BloqueoDao) {

    fun observarNumeros(): Flow<List<NumeroBloqueado>> = dao.observarNumeros()

    fun observarCategorias(): Flow<List<CategoriaBloqueo>> = dao.observarCategorias()

    fun observarCategoriasConConteo(): Flow<List<CategoriaConConteo>> =
        dao.observarCategoriasConConteo()

    /**
     * Listado filtrado y ordenado directamente en SQLite. El texto se envía
     * como parámetro; nunca se construye una consulta con él.
     */
    fun observarListado(
        texto: String = "",
        categoriaId: Long? = null,
        estado: EstadoNumeros = EstadoNumeros.TODOS,
        orden: OrdenNumeros = OrdenNumeros.FECHA,
        ascendente: Boolean = false,
        ahora: Long = System.currentTimeMillis()
    ): Flow<List<NumeroBloqueadoListado>> {
        val (textoLimpio, textoNumero) = prepararBusqueda(texto)
        return dao.observarListado(
            texto = textoLimpio,
            textoNumero = textoNumero,
            categoriaId = categoriaId,
            estado = estado.name,
            orden = orden.name,
            ascendente = ascendente,
            ahora = ahora
        )
    }

    /** Devuelve false si el número está vacío o ya existe otro con ese número. */
    suspend fun guardarNumero(numero: NumeroBloqueado): Boolean {
        val ahora = System.currentTimeMillis()
        val limpio = numero.copy(
            numero = ReglaBloqueo.normalizar(numero.numero),
            motivo = numero.motivo.trim(),
            fechaAlta = if (numero.fechaAlta <= 0L) ahora else numero.fechaAlta,
            expiraEn = if (numero.tipo == TipoBloqueo.PERMANENTE) {
                null
            } else {
                numero.expiraEn
            }
        )
        if (limpio.numero.isBlank()) return false

        val existente = dao.buscarPorNumero(limpio.numero)
        if (existente != null && existente.id != limpio.id) return false

        if (limpio.id == 0L) {
            dao.insertarNumero(limpio)
        } else {
            dao.actualizarNumero(limpio)
        }
        return true
    }

    suspend fun eliminarNumero(id: Long) {
        dao.eliminarNumero(id)
    }

    // ---------- Lista blanca ----------

    fun observarTodosPermitidos(): Flow<List<NumeroPermitido>> =
        dao.observarTodosPermitidos()

    fun observarPermitidos(texto: String = ""): Flow<List<NumeroPermitido>> {
        val (textoLimpio, textoNumero) = prepararBusqueda(texto)
        return dao.observarPermitidos(
            texto = textoLimpio,
            textoNumero = textoNumero
        )
    }

    /** Devuelve false si el número está vacío o ya está en la lista blanca. */
    suspend fun guardarPermitido(permitido: NumeroPermitido): Boolean {
        val limpio = permitido.copy(
            numero = ReglaBloqueo.normalizar(permitido.numero),
            motivo = permitido.motivo.trim(),
            fechaAlta = if (permitido.fechaAlta <= 0L) {
                System.currentTimeMillis()
            } else {
                permitido.fechaAlta
            }
        )
        if (limpio.numero.isBlank()) return false

        val existente = dao.buscarPermitidoPorNumero(limpio.numero)
        if (existente != null && existente.id != limpio.id) return false

        if (limpio.id == 0L) {
            dao.insertarPermitido(limpio)
        } else {
            dao.actualizarPermitido(limpio)
        }
        return true
    }

    suspend fun eliminarPermitido(id: Long) {
        dao.eliminarPermitido(id)
    }

    // ---------- Categorías ----------

    /** Crea la categoría o devuelve el id de una ya existente con ese nombre. */
    suspend fun crearCategoria(nombre: String): Long {
        val limpio = nombre.trim()
        if (limpio.isBlank()) return -1

        dao.buscarCategoriaPorNombre(limpio)?.let { return it.id }

        val id = dao.insertarCategoria(CategoriaBloqueo(nombre = limpio))
        if (id > 0) return id

        return dao.buscarCategoriaPorNombre(limpio)?.id ?: -1
    }

    /** Devuelve false si el nombre está vacío o ya lo usa otra categoría. */
    suspend fun renombrarCategoria(id: Long, nombre: String): Boolean {
        val limpio = nombre.trim()
        if (limpio.isBlank()) return false

        val actual = dao.buscarCategoriaPorId(id) ?: return false
        val repetida = dao.buscarCategoriaPorNombre(limpio)
        if (repetida != null && repetida.id != id) return false

        dao.actualizarCategoria(actual.copy(nombre = limpio))
        return true
    }

    /**
     * Elimina la categoría y pasa sus números a "Otro" (o a la primera que quede).
     * Nunca borra los números. Devuelve false si es la última categoría.
     */
    suspend fun eliminarCategoria(id: Long): Boolean {
        val categorias = dao.obtenerCategorias()
        if (categorias.size <= 1) return false
        if (categorias.none { it.id == id }) return false

        val destino = categorias.firstOrNull {
            it.id != id && it.nombre.equals("Otro", ignoreCase = true)
        } ?: categorias.first { it.id != id }

        dao.reasignarCategoria(origen = id, destino = destino.id)
        dao.eliminarCategoria(id)
        return true
    }

    suspend fun importarCsv(texto: String): ResumenImportacion =
        withContext(Dispatchers.IO) {
            val parseado = CsvBloqueados.parsear(texto)
            val categoriasPorNombre = dao.obtenerCategorias()
                .associateByTo(mutableMapOf()) { it.nombre.lowercase(Locale.ROOT) }

            var nuevos = 0
            var actualizados = 0
            var categoriasCreadas = 0

            for (fila in parseado.filas) {
                val nombreCategoria = fila.categoria.trim().ifBlank { "Otro" }
                val clave = nombreCategoria.lowercase(Locale.ROOT)

                var categoria = categoriasPorNombre[clave]
                if (categoria == null) {
                    var id = dao.insertarCategoria(CategoriaBloqueo(nombre = nombreCategoria))
                    if (id <= 0L) {
                        // Otra fila o el usuario la creó mientras importábamos.
                        id = dao.buscarCategoriaPorNombre(nombreCategoria)?.id ?: -1
                    } else {
                        categoriasCreadas++
                    }
                    if (id <= 0L) continue

                    categoria = CategoriaBloqueo(id = id, nombre = nombreCategoria)
                    categoriasPorNombre[clave] = categoria
                }

                val existente = dao.buscarPorNumero(fila.numero)
                if (existente == null) {
                    dao.insertarNumero(
                        NumeroBloqueado(
                            numero = fila.numero,
                            motivo = fila.motivo,
                            categoriaId = categoria.id
                        )
                    )
                    nuevos++
                } else {
                    dao.actualizarNumero(
                        existente.copy(
                            motivo = fila.motivo,
                            categoriaId = categoria.id
                        )
                    )
                    actualizados++
                }
            }

            ResumenImportacion(
                nuevos = nuevos,
                actualizados = actualizados,
                categoriasCreadas = categoriasCreadas,
                errores = parseado.errores
            )
        }

    suspend fun exportarCsv(): String = withContext(Dispatchers.IO) {
        val nombresCategoria = dao.obtenerCategorias().associate { it.id to it.nombre }
        val filas = dao.obtenerNumeros().map { numero ->
            FilaCsvBloqueados(
                numero = numero.numero,
                motivo = numero.motivo,
                categoria = nombresCategoria[numero.categoriaId] ?: "Otro"
            )
        }
        CsvBloqueados.generar(filas)
    }

    /**
     * El texto se usa en la consulta como parámetro. Además de la versión
     * normalizada para el número, conservamos la original (en minúsculas)
     * para buscar en el motivo.
     */
    private fun prepararBusqueda(texto: String): Pair<String, String> {
        val limpio = texto.trim().lowercase(Locale.ROOT)
        val soloTelefono = limpio.all {
            it.isDigit() || it == '+' || it == ' ' || it == '-' ||
                it == '(' || it == ')'
        }
        val textoNumero = if (soloTelefono) {
            ReglaBloqueo.normalizar(limpio)
        } else {
            // Si la búsqueda tiene letras (por ejemplo, "no-existe-123"), no
            // convertimos los dígitos sueltos en una búsqueda de número.
            ""
        }
        return limpio to textoNumero
    }
}
