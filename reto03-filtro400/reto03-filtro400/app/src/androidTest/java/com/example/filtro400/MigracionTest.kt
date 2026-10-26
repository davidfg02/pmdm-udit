package com.example.filtro400

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigracionTest {

    @Test
    fun migrarDeVersion1ConservaElHistorialYCreaLasCategorias() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion_filtro400_test.db"
        context.deleteDatabase(nombre)

        // Simulamos una instalación antigua: solo existía el historial.
        val vieja = context.openOrCreateDatabase(nombre, Context.MODE_PRIVATE, null)
        vieja.execSQL(
            "CREATE TABLE IF NOT EXISTS `llamadas_bloqueadas` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`numero` TEXT NOT NULL, " +
                "`fechaMillis` INTEGER NOT NULL, " +
                "`leida` INTEGER NOT NULL)"
        )
        vieja.execSQL(
            "INSERT INTO `llamadas_bloqueadas` (`numero`, `fechaMillis`, `leida`) " +
                "VALUES ('400111111', 1000, 0)"
        )
        vieja.version = 1
        vieja.close()

        val baseDatos = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            nombre
        )
            .addMigrations(
                AppDatabase.MIGRACION_1_2,
                AppDatabase.MIGRACION_2_3
            )
            .allowMainThreadQueries()
            .build()

        try {
            val llamadas = baseDatos.llamadaDao().observarTodas().first()
            assertEquals(1, llamadas.size)
            assertEquals("400111111", llamadas[0].numero)

            val categorias = baseDatos.bloqueoDao()
                .observarCategorias()
                .first()
                .map { it.nombre }

            assertTrue(
                categorias.containsAll(
                    listOf("Spam", "Telemarketing", "Fraude", "Otro")
                )
            )
            assertTrue(baseDatos.bloqueoDao().observarTodosPermitidos().first().isEmpty())
        } finally {
            baseDatos.close()
            context.deleteDatabase(nombre)
        }
    }

    @Test
    fun migrarDeVersion2ConservaNumerosYLosConvierteEnPermanentes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion_filtro400_v2_test.db"
        context.deleteDatabase(nombre)

        // Esquema exacto de la versión 2: números sin fecha ni tipo, y sin
        // la tabla de lista blanca.
        val vieja = context.openOrCreateDatabase(nombre, Context.MODE_PRIVATE, null)
        vieja.execSQL(
            "CREATE TABLE IF NOT EXISTS `llamadas_bloqueadas` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`numero` TEXT NOT NULL, " +
                "`fechaMillis` INTEGER NOT NULL, " +
                "`leida` INTEGER NOT NULL)"
        )
        vieja.execSQL(
            "CREATE TABLE IF NOT EXISTS `categorias` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`nombre` TEXT NOT NULL)"
        )
        vieja.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categorias_nombre` " +
                "ON `categorias` (`nombre`)"
        )
        vieja.execSQL(
            "CREATE TABLE IF NOT EXISTS `numeros_bloqueados` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`numero` TEXT NOT NULL, " +
                "`motivo` TEXT NOT NULL, " +
                "`categoriaId` INTEGER NOT NULL)"
        )
        vieja.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_numeros_bloqueados_numero` " +
                "ON `numeros_bloqueados` (`numero`)"
        )
        vieja.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_numeros_bloqueados_categoriaId` " +
                "ON `numeros_bloqueados` (`categoriaId`)"
        )
        vieja.execSQL("INSERT INTO `categorias` (`nombre`) VALUES ('Spam')")
        vieja.execSQL(
            "INSERT INTO `numeros_bloqueados` " +
                "(`numero`, `motivo`, `categoriaId`) " +
                "VALUES ('913456789', 'Cobros', 1)"
        )
        vieja.version = 2
        vieja.close()

        val baseDatos = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            nombre
        )
            .addMigrations(AppDatabase.MIGRACION_1_2, AppDatabase.MIGRACION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val numero = baseDatos.bloqueoDao().buscarPorNumero("913456789")
            assertNotNull(numero)
            assertEquals("Cobros", numero!!.motivo)
            assertEquals(TipoBloqueo.PERMANENTE, numero.tipo)
            assertNull(numero.expiraEn)
            assertTrue(numero.fechaAlta > 0L)

            val listado = baseDatos.bloqueoDao().observarListado(
                texto = "cobros",
                textoNumero = "",
                categoriaId = null,
                estado = EstadoNumeros.TODOS.name,
                orden = OrdenNumeros.FECHA.name,
                ascendente = false,
                ahora = System.currentTimeMillis()
            ).first()

            assertEquals(1, listado.size)
            assertEquals("Spam", listado.single().categoria)
            assertEquals(0, listado.single().intentos)

            // La tabla nueva se creó en la migración y acepta inserciones.
            baseDatos.bloqueoDao().insertarPermitido(
                NumeroPermitido(numero = "400999999", motivo = "Confianza")
            )
            val permitidos = baseDatos.bloqueoDao()
                .observarTodosPermitidos()
                .first()
            assertEquals(1, permitidos.size)
            assertEquals("400999999", permitidos.single().numero)
        } finally {
            baseDatos.close()
            context.deleteDatabase(nombre)
        }
    }
}
