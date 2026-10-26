package com.example.filtro400

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DiaConteo(
    val fecha: LocalDate,
    val etiqueta: String,
    val cantidad: Int,
    val esHoy: Boolean
)

data class NumeroConteo(
    val numero: String,
    val cantidad: Int
)

data class EstadisticasLlamadas(
    val total: Int,
    val hoy: Int,
    val dias: List<DiaConteo>,
    val porHora: List<Int>,
    val numerosTop: List<NumeroConteo>
) {
    val totalSemana: Int = dias.sumOf { it.cantidad }

    val horaPunta: Int? =
        porHora.indices.maxByOrNull { porHora[it] }?.takeIf { porHora[it] > 0 }

    val numeroMasRepetido: NumeroConteo? = numerosTop.firstOrNull()
}

/*
 * Cálculo de estadísticas a partir del historial. Es código Kotlin puro
 * (sin Android), así que se puede comprobar con tests de JUnit.
 */
object CalculadoraEstadisticas {

    private val ETIQUETAS_DIA = listOf("L", "M", "X", "J", "V", "S", "D")

    fun calcular(
        llamadas: List<LlamadaBloqueada>,
        hoy: LocalDate = LocalDate.now(),
        zona: ZoneId = ZoneId.systemDefault()
    ): EstadisticasLlamadas {
        val fechas = llamadas.map {
            Instant.ofEpochMilli(it.fechaMillis).atZone(zona).toLocalDate()
        }
        val porDia = fechas.groupingBy { it }.eachCount()

        val dias = (6 downTo 0).map { diasAtras ->
            val fecha = hoy.minusDays(diasAtras.toLong())
            DiaConteo(
                fecha = fecha,
                etiqueta = ETIQUETAS_DIA[fecha.dayOfWeek.value - 1],
                cantidad = porDia[fecha] ?: 0,
                esHoy = fecha == hoy
            )
        }

        val porHora = IntArray(24)
        llamadas.forEach { llamada ->
            val hora = Instant.ofEpochMilli(llamada.fechaMillis).atZone(zona).hour
            porHora[hora]++
        }

        val numerosTop = llamadas
            .groupingBy { it.numero }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key }
            )
            .take(5)
            .map { NumeroConteo(it.key, it.value) }

        return EstadisticasLlamadas(
            total = llamadas.size,
            hoy = porDia[hoy] ?: 0,
            dias = dias,
            porHora = porHora.toList(),
            numerosTop = numerosTop
        )
    }
}

/*
 * Genera un CSV pensado para abrirlo directamente en Excel/LibreOffice:
 * BOM UTF-8, separador ";" y "sep=;" como primera línea.
 */
object ExportadorCsv {

    private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
    private val FORMATO_GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun generar(
        llamadas: List<LlamadaBloqueada>,
        estadisticas: EstadisticasLlamadas,
        zona: ZoneId = ZoneId.systemDefault(),
        generado: ZonedDateTime = ZonedDateTime.now(zona)
    ): String {
        val texto = StringBuilder()
        texto.append('\uFEFF')
        texto.append("sep=;\r\n")

        texto.append("Filtro 400 - Estadísticas de llamadas bloqueadas\r\n")
        linea(texto, "Generado", generado.format(FORMATO_GENERADO))
        linea(texto, "Total bloqueadas", estadisticas.total.toString())
        linea(texto, "Bloqueadas hoy", estadisticas.hoy.toString())
        linea(texto, "Total últimos 7 días", estadisticas.totalSemana.toString())
        linea(
            texto,
            "Hora con más bloqueos",
            estadisticas.horaPunta?.let { String.format(Locale.getDefault(), "%02d:00", it) } ?: "Sin datos"
        )
        linea(
            texto,
            "Número más bloqueado",
            estadisticas.numeroMasRepetido?.let { "${it.numero} (${it.cantidad})" } ?: "Sin datos"
        )

        texto.append("\r\n")
        texto.append("Detalle por número\r\n")
        texto.append("Número;Bloqueos\r\n")
        estadisticas.numerosTop.forEach { numero ->
            linea(texto, numero.numero, numero.cantidad.toString())
        }

        texto.append("\r\n")
        texto.append("Historial de llamadas\r\n")
        texto.append("Fecha;Hora;Número;Leída\r\n")
        llamadas.sortedByDescending { it.fechaMillis }.forEach { llamada ->
            val fecha = Instant.ofEpochMilli(llamada.fechaMillis).atZone(zona)
            linea(
                texto,
                fecha.format(FORMATO_FECHA),
                fecha.format(FORMATO_HORA),
                llamada.numero,
                if (llamada.leida) "Sí" else "No"
            )
        }

        return texto.toString()
    }

    fun nombreSugerido(hoy: LocalDate = LocalDate.now()): String {
        val fecha = hoy.format(DateTimeFormatter.BASIC_ISO_DATE)
        return "filtro400_estadisticas_$fecha.csv"
    }

    private fun linea(texto: StringBuilder, vararg campos: String) {
        texto.append(campos.joinToString(";") { escapar(it) }).append("\r\n")
    }

    private fun escapar(campo: String): String {
        val necesitaComillas = campo.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        return if (necesitaComillas) {
            "\"" + campo.replace("\"", "\"\"") + "\""
        } else {
            campo
        }
    }
}
