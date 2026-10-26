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

// Base de datos en memoria: cada test empieza limpio y no toca disco.
@RunWith(AndroidJUnit4::class)
class LlamadaDaoTest {

    private lateinit var baseDatos: AppDatabase
    private lateinit var dao: LlamadaDao

    @Before
    fun crearBaseDeDatos() {
        baseDatos = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = baseDatos.llamadaDao()
    }

    @After
    fun cerrarBaseDeDatos() {
        baseDatos.close()
    }

    @Test
    fun insertarYOrdenarDeMasRecienteAMasAntigua() = runBlocking {
        dao.insertar(LlamadaBloqueada(numero = "400111111", fechaMillis = 1_000))
        dao.insertar(LlamadaBloqueada(numero = "400222222", fechaMillis = 3_000))
        dao.insertar(LlamadaBloqueada(numero = "400333333", fechaMillis = 2_000))

        val llamadas = dao.observarTodas().first()

        assertEquals(
            listOf("400222222", "400333333", "400111111"),
            llamadas.map { it.numero }
        )
    }

    @Test
    fun cuentaNoLeidasYLasMarcaTodas() = runBlocking {
        dao.insertar(LlamadaBloqueada(numero = "400111111", fechaMillis = 1_000))
        dao.insertar(
            LlamadaBloqueada(numero = "400222222", fechaMillis = 2_000, leida = true)
        )

        assertEquals(1, dao.observarNoLeidas().first())

        dao.marcarTodasLeidas()

        assertEquals(0, dao.observarNoLeidas().first())
        assertTrue(dao.observarTodas().first().all { it.leida })
    }

    @Test
    fun borrarTodasDejaLaTablaVacia() = runBlocking {
        dao.insertar(LlamadaBloqueada(numero = "400111111", fechaMillis = 1_000))

        dao.borrarTodas()

        assertTrue(dao.observarTodas().first().isEmpty())
    }
}
