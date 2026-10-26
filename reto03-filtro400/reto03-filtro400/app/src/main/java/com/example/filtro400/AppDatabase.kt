package com.example.filtro400

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Versión 3: fecha de alta, bloqueos temporales y lista blanca.
// Las migraciones conservan el historial y los números que ya tuviera el usuario.
@Database(
    entities = [
        LlamadaBloqueada::class,
        CategoriaBloqueo::class,
        NumeroBloqueado::class,
        NumeroPermitido::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Convertidores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun llamadaDao(): LlamadaDao

    abstract fun bloqueoDao(): BloqueoDao

    companion object {
        // Una sola instancia por proceso: Room no admite dos conexiones abiertas.
        @Volatile
        private var instancia: AppDatabase? = null

        private const val SQL_CREAR_CATEGORIAS =
            "CREATE TABLE IF NOT EXISTS `categorias` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`nombre` TEXT NOT NULL)"

        private const val SQL_INDICE_CATEGORIAS =
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categorias_nombre` " +
                "ON `categorias` (`nombre`)"

        private const val SQL_CREAR_NUMEROS =
            "CREATE TABLE IF NOT EXISTS `numeros_bloqueados` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`numero` TEXT NOT NULL, " +
                "`motivo` TEXT NOT NULL, " +
                "`categoriaId` INTEGER NOT NULL)"

        private const val SQL_INDICE_NUMEROS =
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_numeros_bloqueados_numero` " +
                "ON `numeros_bloqueados` (`numero`)"

        private const val SQL_INDICE_NUMEROS_CATEGORIA =
            "CREATE INDEX IF NOT EXISTS `index_numeros_bloqueados_categoriaId` " +
                "ON `numeros_bloqueados` (`categoriaId`)"

        private const val SQL_CREAR_PERMITIDOS =
            "CREATE TABLE IF NOT EXISTS `numeros_permitidos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`numero` TEXT NOT NULL, " +
                "`motivo` TEXT NOT NULL, " +
                "`fechaAlta` INTEGER NOT NULL DEFAULT 0)"

        private const val SQL_INDICE_PERMITIDOS =
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_numeros_permitidos_numero` " +
                "ON `numeros_permitidos` (`numero`)"

        private const val SQL_INDICE_LLAMADAS_NUMERO =
            "CREATE INDEX IF NOT EXISTS `index_llamadas_bloqueadas_numero` " +
                "ON `llamadas_bloqueadas` (`numero`)"

        // Categorías de arranque: el usuario puede renombrarlas o eliminarlas.
        private fun sembrarCategorias(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO `categorias` (`nombre`) VALUES " +
                    "('Spam'), ('Telemarketing'), ('Fraude'), ('Otro')"
            )
        }

        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_CREAR_CATEGORIAS)
                db.execSQL(SQL_INDICE_CATEGORIAS)
                db.execSQL(SQL_CREAR_NUMEROS)
                db.execSQL(SQL_INDICE_NUMEROS)
                db.execSQL(SQL_INDICE_NUMEROS_CATEGORIA)
                sembrarCategorias(db)
            }
        }

        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `numeros_bloqueados` " +
                        "ADD COLUMN `fechaAlta` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `numeros_bloqueados` " +
                        "ADD COLUMN `tipo` TEXT NOT NULL DEFAULT 'PERMANENTE'"
                )
                db.execSQL(
                    "ALTER TABLE `numeros_bloqueados` " +
                        "ADD COLUMN `expiraEn` INTEGER"
                )

                // Los números que ya existían se consideran permanentes y se
                // les asigna la fecha de la migración para poder ordenarlos.
                db.execSQL(
                    "UPDATE `numeros_bloqueados` " +
                        "SET `fechaAlta` = CAST(strftime('%s','now') AS INTEGER) * 1000 " +
                        "WHERE `fechaAlta` = 0"
                )

                db.execSQL(SQL_CREAR_PERMITIDOS)
                db.execSQL(SQL_INDICE_PERMITIDOS)
                db.execSQL(SQL_INDICE_LLAMADAS_NUMERO)
            }
        }

        // En una instalación nueva Room crea las tablas y después llama aquí.
        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                sembrarCategorias(db)
            }
        }

        fun obtener(contexto: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "filtro400.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .addCallback(CALLBACK)
                    .build()
                    .also { instancia = it }
            }
    }
}
