package com.example.filtro400

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloqueosRepositoryTest {

    private lateinit var baseDatos: AppDatabase
    private lateinit var dao: BloqueoDao
    private lateinit var repositorio: BloqueosRepository

    @Before
    fun crearBaseDeDatos() {
        baseDatos = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = baseDatos.bloqueoDao()
        repositorio = BloqueosRepository(dao)
    }

    @After
    fun cerrarBaseDeDatos() {
        baseDatos.close()
    }

    @Test
    fun altaYEdicionDeNumeroConCategoria() = runBlocking {
        val spamId = repositorio.crearCategoria("Spam")
        val fraudeId = repositorio.crearCategoria("Fraude")
        assertTrue(spamId > 0)
        assertTrue(fraudeId > 0)

        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "913456789",
                motivo = "Llamadas repetidas",
                categoriaId = spamId
            )
        )

        val creado = dao.buscarPorNumero("913456789")
        assertNotNull(creado)
        assertEquals("Llamadas repetidas", creado!!.motivo)
        assertEquals(spamId, creado.categoriaId)

        assertTrue(
            repositorio.guardarNumero(
                creado.copy(motivo = "Cobros", categoriaId = fraudeId)
            )
        )

        val editado = dao.buscarPorNumero("913456789")!!
        assertEquals("Cobros", editado.motivo)
        assertEquals(fraudeId, editado.categoriaId)
    }

    @Test
    fun noPermiteDosNumerosIguales() = runBlocking {
        val categoriaId = repositorio.crearCategoria("Spam")

        assertTrue(
            repositorio.guardarNumero(
                NumeroBloqueado(numero = "913456789", categoriaId = categoriaId)
            )
        )
        assertFalse(
            repositorio.guardarNumero(
                NumeroBloqueado(numero = "+34 913 456 789", categoriaId = categoriaId)
            )
        )
    }

    @Test
    fun importaFormatoAntiguoYCreaLaCategoriaOtro() = runBlocking {
        val resumen = repositorio.importarCsv(
            "numero,motivo\r\n400111111,Publicidad\r\n"
        )

        assertEquals(1, resumen.nuevos)
        assertEquals(0, resumen.actualizados)
        assertEquals(1, resumen.categoriasCreadas)

        val numero = dao.buscarPorNumero("400111111")!!
        val categoria = dao.buscarCategoriaPorId(numero.categoriaId)!!
        assertEquals("Otro", categoria.nombre)
        assertEquals("Publicidad", numero.motivo)
    }

    @Test
    fun importaFormatoNuevoYCreaCategoriasQueNoExisten() = runBlocking {
        val resumen = repositorio.importarCsv(
            "numero,motivo,categoria\r\n" +
                "400111111,Publicidad,Encuestas\r\n" +
                "913456789,Cobros,Fraude\r\n"
        )

        assertEquals(2, resumen.nuevos)
        assertEquals(2, resumen.categoriasCreadas)

        val encuestas = dao.buscarCategoriaPorNombre("Encuestas")
        val fraude = dao.buscarCategoriaPorNombre("Fraude")
        assertNotNull(encuestas)
        assertNotNull(fraude)
        assertEquals(
            encuestas!!.id,
            dao.buscarPorNumero("400111111")!!.categoriaId
        )
        assertEquals(
            fraude!!.id,
            dao.buscarPorNumero("913456789")!!.categoriaId
        )
    }

    @Test
    fun exportarEImportarDeNuevoConservaTodo() = runBlocking {
        val spamId = repositorio.crearCategoria("Spam")
        val otroId = repositorio.crearCategoria("Otro")
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "400111111",
                motivo = "Publicidad con, coma",
                categoriaId = spamId
            )
        )
        repositorio.guardarNumero(
            NumeroBloqueado(
                numero = "913456789",
                motivo = "Llamadas repetidas",
                categoriaId = otroId
            )
        )

        val csv = repositorio.exportarCsv()

        dao.obtenerNumeros().forEach { repositorio.eliminarNumero(it.id) }
        assertTrue(dao.obtenerNumeros().isEmpty())

        val resumen = repositorio.importarCsv(csv)

        assertEquals(2, resumen.nuevos)
        val numeros = dao.obtenerNumeros()
        assertEquals(listOf("400111111", "913456789"), numeros.map { it.numero })
        assertEquals("Publicidad con, coma", numeros[0].motivo)
        assertEquals("Spam", dao.buscarCategoriaPorId(numeros[0].categoriaId)!!.nombre)
        assertEquals("Otro", dao.buscarCategoriaPorId(numeros[1].categoriaId)!!.nombre)
    }

    @Test
    fun eliminarCategoriaReasignaSusNumerosAOtra() = runBlocking {
        val spamId = repositorio.crearCategoria("Spam")
        val otroId = repositorio.crearCategoria("Otro")
        repositorio.guardarNumero(
            NumeroBloqueado(numero = "913456789", categoriaId = spamId)
        )

        assertTrue(repositorio.eliminarCategoria(spamId))

        assertNotNull(dao.buscarCategoriaPorId(otroId))
        assertEquals(
            otroId,
            dao.buscarPorNumero("913456789")!!.categoriaId
        )
        assertEquals(1, dao.obtenerCategorias().size)
    }

    @Test
    fun noEliminaLaUltimaCategoria() = runBlocking {
        val unicaId = repositorio.crearCategoria("Spam")

        assertFalse(repositorio.eliminarCategoria(unicaId))
        assertNotNull(dao.buscarCategoriaPorId(unicaId))
    }

    @Test
    fun renombrarCategoriaConservaSusNumeros() = runBlocking {
        val categoriaId = repositorio.crearCategoria("Spam")
        repositorio.guardarNumero(
            NumeroBloqueado(numero = "913456789", categoriaId = categoriaId)
        )

        assertTrue(repositorio.renombrarCategoria(categoriaId, "Publicidad"))

        assertEquals("Publicidad", dao.buscarCategoriaPorId(categoriaId)!!.nombre)
        assertEquals(categoriaId, dao.buscarPorNumero("913456789")!!.categoriaId)
    }
}
