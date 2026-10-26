package com.example.filtro400

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FranjaHorariaTest {

    @Test
    fun franjaNormalContieneHorasIntermedias() {
        val franja = FranjaHoraria(inicioMinuto = 9 * 60, finMinuto = 14 * 60)

        assertTrue(franja.contiene(9 * 60))
        assertTrue(franja.contiene(13 * 60 + 59))
        assertFalse(franja.contiene(8 * 60 + 59))
        assertFalse(franja.contiene(14 * 60))
    }

    @Test
    fun franjaNocturnaCruzaMedianoche() {
        val franja = FranjaHoraria(inicioMinuto = 22 * 60, finMinuto = 8 * 60)

        assertTrue(franja.contiene(23 * 60))
        assertTrue(franja.contiene(0))
        assertTrue(franja.contiene(7 * 60 + 59))
        assertFalse(franja.contiene(8 * 60))
        assertFalse(franja.contiene(12 * 60))
        assertTrue(franja.cruzaMedianoche)
    }

    @Test
    fun franjaDesactivadaNuncaContiene() {
        val franja = FranjaHoraria(
            inicioMinuto = 0,
            finMinuto = 23 * 60 + 59,
            activa = false
        )

        assertFalse(franja.contiene(12 * 60))
    }

    @Test
    fun inicioYFinIgualesSignificanTodoElDia() {
        val franja = FranjaHoraria(inicioMinuto = 10 * 60, finMinuto = 10 * 60)

        assertTrue(franja.contiene(0))
        assertTrue(franja.contiene(12 * 60))
        assertEquals(24 * 60, franja.duracionMinutos())
    }

    @Test
    fun duracionTieneEnCuentaLaMedianoche() {
        assertEquals(5 * 60, FranjaHoraria(0, 9 * 60, 14 * 60).duracionMinutos())
        assertEquals(10 * 60, FranjaHoraria(0, 22 * 60, 8 * 60).duracionMinutos())
    }

    @Test
    fun formateaHorasEnDosDigitos() {
        assertEquals("00:00", formatearHora(0))
        assertEquals("09:05", formatearHora(9 * 60 + 5))
        assertEquals("23:59", formatearHora(23 * 60 + 59))
    }

    @Test
    fun sinFranjasActivasSiempreSePuedeBloquear() {
        val configuracion = ConfiguracionFranjas(usarFranjas = false)

        assertTrue(ReglaHoraria.permiteBloquear(configuracion, 3 * 60))
    }

    @Test
    fun conFranjasSoloSeBloqueaDentro() {
        val configuracion = ConfiguracionFranjas(
            usarFranjas = true,
            franjas = listOf(FranjaHoraria(0, 22 * 60, 8 * 60))
        )

        assertTrue(ReglaHoraria.permiteBloquear(configuracion, 23 * 60))
        assertFalse(ReglaHoraria.permiteBloquear(configuracion, 12 * 60))
    }

    @Test
    fun franjaDesactivadaNoBloqueaAunqueElModoEsteActivo() {
        val configuracion = ConfiguracionFranjas(
            usarFranjas = true,
            franjas = listOf(FranjaHoraria(0, 0, 23 * 60 + 59, activa = false))
        )

        assertFalse(ReglaHoraria.permiteBloquear(configuracion, 12 * 60))
    }

    @Test
    fun modoFranjasSinFranjasNoBloqueaNada() {
        val configuracion = ConfiguracionFranjas(usarFranjas = true)

        assertFalse(ReglaHoraria.permiteBloquear(configuracion, 12 * 60))
    }
}
