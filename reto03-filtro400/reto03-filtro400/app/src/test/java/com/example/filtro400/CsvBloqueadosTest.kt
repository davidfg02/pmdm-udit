package com.example.filtro400

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvBloqueadosTest {

    @Test
    fun exportarYVolverAImportarConservaNumeroMotivoYCategoria() {
        val original = listOf(
            FilaCsvBloqueados("400111111", "Llamadas repetidas", "Spam"),
            FilaCsvBloqueados("913456789", "", "Otro"),
            FilaCsvBloqueados(
                "600000000",
                "Dice \"hola\", y algo más",
                "Fraude"
            )
        )

        val csv = CsvBloqueados.generar(original)

        assertTrue(csv.startsWith("\uFEFF${CsvBloqueados.CABECERA}\r\n"))

        val parseado = CsvBloqueados.parsear(csv)
        assertEquals(0, parseado.errores)
        assertEquals(original.sortedBy { it.numero }, parseado.filas)
    }

    @Test
    fun aceptaElFormatoAntiguoDeDosColumnasComoOtro() {
        val csv = """
            numero,motivo
            400123456,Publicidad
            +34 913 456 789,  Llamadas repetidas
        """.trimIndent()

        val parseado = CsvBloqueados.parsear(csv)

        assertEquals(
            listOf(
                FilaCsvBloqueados("400123456", "Publicidad", "Otro"),
                FilaCsvBloqueados("913456789", "Llamadas repetidas", "Otro")
            ),
            parseado.filas
        )
        assertEquals(0, parseado.errores)
    }

    @Test
    fun unaCategoriaVaciaSeConvierteEnOtro() {
        val csv = "numero,motivo,categoria\r\n400111111,Spam,\r\n"

        val parseado = CsvBloqueados.parsear(csv)

        assertEquals(
            listOf(FilaCsvBloqueados("400111111", "Spam", "Otro")),
            parseado.filas
        )
    }

    @Test
    fun sinCabeceraTambienSePuedeImportar() {
        val csv = "400111111,Publicidad,Spam\r\n400222222,Otra cosa,Otro"

        val parseado = CsvBloqueados.parsear(csv)

        assertEquals(2, parseado.filas.size)
        assertEquals("400111111", parseado.filas[0].numero)
        assertEquals("Spam", parseado.filas[0].categoria)
    }

    @Test
    fun cuentaComoErrorLasFilasSinNumero() {
        val csv = """
            numero,motivo,categoria
            ,Sin número,Spam
            400123456,Correcta,Spam
        """.trimIndent()

        val parseado = CsvBloqueados.parsear(csv)

        assertEquals(1, parseado.errores)
        assertEquals(
            listOf(FilaCsvBloqueados("400123456", "Correcta", "Spam")),
            parseado.filas
        )
    }

    @Test
    fun respetaComasComillasYSaltosDeLineaEntrecomillados() {
        val csv = "numero,motivo,categoria\r\n" +
            "400111111,\"Hola, mundo\",Spam\r\n" +
            "400222222,\"Linea 1\nLinea 2\",Otro\r\n"

        val parseado = CsvBloqueados.parsear(csv)

        assertEquals(2, parseado.filas.size)
        assertEquals("Hola, mundo", parseado.filas[0].motivo)
        assertEquals("Linea 1\nLinea 2", parseado.filas[1].motivo)
        assertEquals(0, parseado.errores)
    }

    @Test
    fun normalizaElNumeroAlImportar() {
        val csv = "numero,motivo,categoria\r\n+34 400-123-456,Spam,Spam\r\n"

        val parseado = CsvBloqueados.parsear(csv)

        assertEquals("400123456", parseado.filas.single().numero)
    }
}
