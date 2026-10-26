package com.example.filtro400

/** Una fila del CSV de números bloqueados, ya normalizada. */
data class FilaCsvBloqueados(
    val numero: String,
    val motivo: String,
    val categoria: String
)

data class CsvBloqueadosParseado(
    val filas: List<FilaCsvBloqueados>,
    val errores: Int
)

/**
 * Lector/escritor del CSV de la lista de bloqueados.
 *
 * Formato nuevo:  numero,motivo,categoria
 * Formato antiguo: numero,motivo  -> la categoría pasa a ser "Otro".
 *
 * Es Kotlin puro (sin Android) para poder probarlo con tests de JVM.
 */
object CsvBloqueados {

    const val CABECERA = "numero,motivo,categoria"
    private const val CATEGORIA_POR_DEFECTO = "Otro"

    fun parsear(texto: String): CsvBloqueadosParseado {
        // Algunos editores guardan el BOM de UTF-8 al principio del archivo.
        val limpio = texto.removePrefix("\uFEFF")
        val registros = leerRegistros(limpio)

        val filas = mutableListOf<FilaCsvBloqueados>()
        var errores = 0
        var primeraFila = true

        registros.forEach { campos ->
            if (campos.all { it.isBlank() }) return@forEach

            if (primeraFila) {
                primeraFila = false
                val encabezado = campos.firstOrNull()?.trim()?.lowercase()
                if (encabezado == "numero" || encabezado == "número") {
                    return@forEach
                }
            }

            val numero = ReglaBloqueo.normalizar(campos.getOrElse(0) { "" })
            if (numero.isBlank()) {
                errores++
                return@forEach
            }

            val motivo = campos.getOrElse(1) { "" }.trim()
            val categoria = campos.getOrElse(2) { "" }.trim()
                .ifBlank { CATEGORIA_POR_DEFECTO }

            filas += FilaCsvBloqueados(
                numero = numero,
                motivo = motivo,
                categoria = categoria
            )
        }

        return CsvBloqueadosParseado(filas = filas, errores = errores)
    }

    fun generar(filas: List<FilaCsvBloqueados>): String {
        val salida = StringBuilder()
        // BOM: así Excel reconoce bien los acentos.
        salida.append('\uFEFF')
        salida.append(CABECERA).append("\r\n")

        filas.sortedBy { it.numero }.forEach { fila ->
            salida.append(escapar(fila.numero)).append(',')
                .append(escapar(fila.motivo)).append(',')
                .append(escapar(fila.categoria)).append("\r\n")
        }

        return salida.toString()
    }

    private fun escapar(campo: String): String {
        val necesitaComillas =
            campo.contains(',') || campo.contains('"') ||
                campo.contains('\n') || campo.contains('\r')
        return if (necesitaComillas) {
            "\"" + campo.replace("\"", "\"\"") + "\""
        } else {
            campo
        }
    }

    /**
     * Divide el texto en filas y campos respetando las comillas.
     * Soporta comas y saltos de línea dentro de un campo entrecomillado.
     */
    private fun leerRegistros(texto: String): List<List<String>> {
        val registros = mutableListOf<List<String>>()
        var campos = mutableListOf<String>()
        val campo = StringBuilder()
        var entreComillas = false
        var indice = 0

        while (indice < texto.length) {
            val caracter = texto[indice]
            when {
                entreComillas && caracter == '"' &&
                    indice + 1 < texto.length && texto[indice + 1] == '"' -> {
                    campo.append('"')
                    indice++
                }

                caracter == '"' -> entreComillas = !entreComillas

                caracter == ',' && !entreComillas -> {
                    campos += campo.toString()
                    campo.clear()
                }

                (caracter == '\n' || caracter == '\r') && !entreComillas -> {
                    if (caracter == '\r' && indice + 1 < texto.length &&
                        texto[indice + 1] == '\n'
                    ) {
                        indice++
                    }
                    campos += campo.toString()
                    campo.clear()
                    registros += campos
                    campos = mutableListOf()
                }

                else -> campo.append(caracter)
            }
            indice++
        }

        if (campo.isNotEmpty() || campos.isNotEmpty()) {
            campos += campo.toString()
            registros += campos
        }

        return registros
    }
}
