package com.example.filtro400

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EstadisticasTest {

    private val zona = ZoneOffset.UTC
    private val hoy = LocalDate.of(2026, 10, 26)

    private fun llamadaA(
        numero: String,
        diasAtras: Long,
        hora: Int,
        minuto: Int = 0,
        leida: Boolean = false
    ): LlamadaBloqueada {
        val fecha = hoy.minusDays(diasAtras)
            .atTime(hora, minuto)
            .atZone(zona)
        return LlamadaBloqueada(
            numero = numero,
            fechaMillis = fecha.toInstant().toEpochMilli(),
            leida = leida
        )
    }

    private fun llamadasDePrueba() = listOf(
        llamadaA("400111111", 0, 10),
        llamadaA("400111111", 0, 18, 30),
        llamadaA("400222222", 1, 9),
        llamadaA("400111111", 3, 12),
        // Fuera de la ventana de 7 días: suma al total, no a la semana.
        llamadaA("400999999", 8, 10)
    )

    @Test
    fun cuentaTotalesYVentanaDeSieteDias() {
        val estadisticas = CalculadoraEstadisticas.calcular(llamadasDePrueba(), hoy, zona)

        assertEquals(5, estadisticas.total)
        assertEquals(2, estadisticas.hoy)
        assertEquals(4, estadisticas.totalSemana)
        assertEquals(7, estadisticas.dias.size)
    }

    @Test
    fun losDiasTerminanEnHoy() {
        val estadisticas = CalculadoraEstadisticas.calcular(llamadasDePrueba(), hoy, zona)

        assertTrue(estadisticas.dias.last().esHoy)
        assertEquals(hoy, estadisticas.dias.last().fecha)
        assertEquals(hoy.minusDays(6), estadisticas.dias.first().fecha)
        assertEquals(2, estadisticas.dias.last().cantidad)
    }

    @Test
    fun calculaHoraPuntaYDistribucion() {
        val estadisticas = CalculadoraEstadisticas.calcular(llamadasDePrueba(), hoy, zona)

        assertEquals(10, estadisticas.horaPunta)
        assertEquals(2, estadisticas.porHora[10])
        assertEquals(1, estadisticas.porHora[18])
        assertEquals(1, estadisticas.porHora[9])
        assertEquals(1, estadisticas.porHora[12])
    }

    @Test
    fun ordenaNumerosPorCantidad() {
        val estadisticas = CalculadoraEstadisticas.calcular(llamadasDePrueba(), hoy, zona)

        assertEquals(3, estadisticas.numerosTop.size)
        assertEquals("400111111", estadisticas.numerosTop[0].numero)
        assertEquals(3, estadisticas.numerosTop[0].cantidad)
        assertEquals("400222222", estadisticas.numerosTop[1].numero)
    }

    @Test
    fun sinLlamadasDevuelveCeros() {
        val estadisticas = CalculadoraEstadisticas.calcular(emptyList(), hoy, zona)

        assertEquals(0, estadisticas.total)
        assertEquals(0, estadisticas.hoy)
        assertEquals(0, estadisticas.totalSemana)
        assertEquals(null, estadisticas.horaPunta)
        assertEquals(null, estadisticas.numeroMasRepetido)
    }

    @Test
    fun csvContieneResumenYHistorial() {
        val llamadas = llamadasDePrueba()
        val estadisticas = CalculadoraEstadisticas.calcular(llamadas, hoy, zona)
        val generado = hoy.atTime(12, 0).atZone(zona)

        val csv = ExportadorCsv.generar(llamadas, estadisticas, zona, generado)

        assertTrue(csv.startsWith("\uFEFFsep=;"))
        assertTrue(csv.contains("Total bloqueadas;5"))
        assertTrue(csv.contains("Bloqueadas hoy;2"))
        assertTrue(csv.contains("Hora con más bloqueos;10:00"))
        assertTrue(csv.contains("Número más bloqueado;400111111 (3)"))
        assertTrue(csv.contains("Fecha;Hora;Número;Leída"))
        assertTrue(csv.contains("26/10/2026;10:00;400111111;No"))
    }

    @Test
    fun nombreDeArchivoLlevaLaFecha() {
        assertEquals(
            "filtro400_estadisticas_20261026.csv",
            ExportadorCsv.nombreSugerido(hoy)
        )
    }
}
