package com.example.filtro400

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FiltradoNumerosTest {

    private lateinit var baseDatos: AppDatabase
    private lateinit var repositorio: BloqueosRepository

    private val ahora = 1_700_000_000_000L

    @Before
    fun crearBaseDeDatos() {
        baseDatos = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repositorio = BloqueosRepository(baseDatos.bloqueoDao())
    }

    @After
    fun cerrarBaseDeDatos() {
        baseDatos.close()
    }

    @Test
    fun buscaParcialmentePorNumeroYPorMotivoSinDistinguirMayusculas() = runBlocking {
        val spam = repositorio.crearCategoria("Spam")
        val fraude = repositorio.crearCategoria("Fraude")

        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400123456",
                motivo = "Llamadas repetidas",
                categoriaId = spam,
                fechaAlta = ahora
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "913456789",
                motivo = "Cobros de prueba",
                categoriaId = fraude,
                fechaAlta = ahora
            )
        )

        val porNumero = repositorio.observarListado(
            texto = "123",
            ahora = ahora
        ).first()
        assertEquals(listOf("400123456"), porNumero.map { it.numero })

        val porMotivo = repositorio.observarListado(
            texto = "COBROS",
            ahora = ahora
        ).first()
        assertEquals(listOf("913456789"), porMotivo.map { it.numero })

        val porNumeroConPrefijo = repositorio.observarListado(
            texto = "+34 123",
            ahora = ahora
        ).first()
        assertEquals(listOf("400123456"), porNumeroConPrefijo.map { it.numero })
    }

    @Test
    fun combinaCategoriaYEstadoConLaBusqueda() = runBlocking {
        val spam = repositorio.crearCategoria("Spam")
        val fraude = repositorio.crearCategoria("Fraude")

        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400111111",
                motivo = "Spam temporal activo",
                categoriaId = spam,
                fechaAlta = ahora + 3,
                tipo = TipoBloqueo.TEMPORAL,
                expiraEn = ahora + 60_000
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400222222",
                motivo = "Spam temporal expirado",
                categoriaId = spam,
                fechaAlta = ahora + 2,
                tipo = TipoBloqueo.TEMPORAL,
                expiraEn = ahora - 60_000
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400333333",
                motivo = "Spam permanente",
                categoriaId = spam,
                fechaAlta = ahora + 1,
                tipo = TipoBloqueo.PERMANENTE
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400444444",
                motivo = "Fraude temporal activo",
                categoriaId = fraude,
                fechaAlta = ahora,
                tipo = TipoBloqueo.TEMPORAL,
                expiraEn = ahora + 60_000
            )
        )

        val temporalesSpam = repositorio.observarListado(
            categoriaId = spam,
            estado = EstadoNumeros.TEMPORALES,
            ahora = ahora
        ).first()
        assertEquals(listOf("400111111"), temporalesSpam.map { it.numero })

        val expiradosSpam = repositorio.observarListado(
            categoriaId = spam,
            estado = EstadoNumeros.EXPIRADOS,
            ahora = ahora
        ).first()
        assertEquals(listOf("400222222"), expiradosSpam.map { it.numero })

        val permanentesSpam = repositorio.observarListado(
            categoriaId = spam,
            estado = EstadoNumeros.PERMANENTES,
            ahora = ahora
        ).first()
        assertEquals(listOf("400333333"), permanentesSpam.map { it.numero })

        val activos = repositorio.observarListado(
            estado = EstadoNumeros.ACTIVOS,
            ahora = ahora
        ).first()
        assertEquals(
            setOf("400111111", "400333333", "400444444"),
            activos.map { it.numero }.toSet()
        )

        // Texto + categoría + estado en la misma consulta.
        val combinado = repositorio.observarListado(
            texto = "spam",
            categoriaId = spam,
            estado = EstadoNumeros.TEMPORALES,
            ahora = ahora
        ).first()
        assertEquals(listOf("400111111"), combinado.map { it.numero })
    }

    @Test
    fun unaBusquedaSinResultadosDevuelveUnaListaVacia() = runBlocking {
        val spam = repositorio.crearCategoria("Spam")
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400123456",
                motivo = "Publicidad",
                categoriaId = spam,
                fechaAlta = ahora
            )
        )

        val resultado = repositorio.observarListado(
            texto = "no-existe-123",
            ahora = ahora
        ).first()

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun ordenaPorFechaNumeroIntentosYCategoria() = runBlocking {
        val spam = repositorio.crearCategoria("Spam")
        val fraude = repositorio.crearCategoria("Fraude")
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400300000",
                motivo = "Tres llamadas",
                categoriaId = spam,
                fechaAlta = ahora + 3
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400100000",
                motivo = "Una llamada",
                categoriaId = spam,
                fechaAlta = ahora + 1
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400200000",
                motivo = "Sin llamadas",
                categoriaId = fraude,
                fechaAlta = ahora + 2
            )
        )

        repeat(3) {
            baseDatos.llamadaDao().insertar(
                LlamadaBloqueada(numero = "400300000", fechaMillis = ahora)
            )
        }
        baseDatos.llamadaDao().insertar(
            LlamadaBloqueada(numero = "400100000", fechaMillis = ahora)
        )

        val fechaAscendente = repositorio.observarListado(
            orden = OrdenNumeros.FECHA,
            ascendente = true,
            ahora = ahora
        ).first()
        assertEquals(
            listOf("400100000", "400200000", "400300000"),
            fechaAscendente.map { it.numero }
        )

        val fechaDescendente = repositorio.observarListado(
            orden = OrdenNumeros.FECHA,
            ascendente = false,
            ahora = ahora
        ).first()
        assertEquals(
            listOf("400300000", "400200000", "400100000"),
            fechaDescendente.map { it.numero }
        )

        val numeroAscendente = repositorio.observarListado(
            orden = OrdenNumeros.NUMERO,
            ascendente = true,
            ahora = ahora
        ).first()
        assertEquals(
            listOf("400100000", "400200000", "400300000"),
            numeroAscendente.map { it.numero }
        )

        val masIntentos = repositorio.observarListado(
            orden = OrdenNumeros.INTENTOS,
            ascendente = false,
            ahora = ahora
        ).first()
        assertEquals("400300000", masIntentos.first().numero)
        assertEquals(3, masIntentos.first().intentos)
        assertEquals(0, masIntentos.last().intentos)

        val categoriaAscendente = repositorio.observarListado(
            orden = OrdenNumeros.CATEGORIA,
            ascendente = true,
            ahora = ahora
        ).first()
        assertEquals(
            listOf("Fraude", "Spam", "Spam"),
            categoriaAscendente.map { it.categoria }
        )

        val categoriaDescendente = repositorio.observarListado(
            orden = OrdenNumeros.CATEGORIA,
            ascendente = false,
            ahora = ahora
        ).first()
        assertEquals(
            listOf("Spam", "Spam", "Fraude"),
            categoriaDescendente.map { it.categoria }
        )
    }

    @Test
    fun buscaParcialmenteEnLaListaBlanca() = runBlocking {
        repositorio.guardarPermitido(
            NumeroPermitido(
                numero = "400999999",
                motivo = "Proveedor de confianza",
                fechaAlta = ahora
            )
        )
        repositorio.guardarPermitido(
            NumeroPermitido(
                numero = "913111222",
                motivo = "Familia",
                fechaAlta = ahora
            )
        )

        val porMotivo = repositorio.observarPermitidos("PROVEEDOR").first()
        assertEquals(listOf("400999999"), porMotivo.map { it.numero })

        val porNumero = repositorio.observarPermitidos("+34 999").first()
        assertEquals(listOf("400999999"), porNumero.map { it.numero })

        val sinResultados = repositorio.observarPermitidos("no-existe").first()
        assertTrue(sinResultados.isEmpty())
    }

    @Test
    fun noPermiteDuplicadosEnLaListaBlanca() = runBlocking {
        assertTrue(
            repositorio.guardarPermitido(
                NumeroPermitido(numero = "400999999", motivo = "Uno")
            )
        )
        assertTrue(
            !repositorio.guardarPermitido(
                NumeroPermitido(numero = "+34 400 999 999", motivo = "Dos")
            )
        )
    }
}
