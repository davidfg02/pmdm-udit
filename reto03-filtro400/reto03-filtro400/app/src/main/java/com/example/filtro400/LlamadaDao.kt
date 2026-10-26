package com.example.filtro400

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// El DAO solo describe las consultas. Room genera la implementación.
@Dao
interface LlamadaDao {

    @Insert
    suspend fun insertar(llamada: LlamadaBloqueada)

    // Flow: la pantalla se actualiza sola cuando cambia la tabla.
    @Query("SELECT * FROM llamadas_bloqueadas ORDER BY fechaMillis DESC")
    fun observarTodas(): Flow<List<LlamadaBloqueada>>

    @Query("SELECT COUNT(*) FROM llamadas_bloqueadas WHERE leida = 0")
    fun observarNoLeidas(): Flow<Int>

    @Query("UPDATE llamadas_bloqueadas SET leida = 1")
    suspend fun marcarTodasLeidas()

    @Query("DELETE FROM llamadas_bloqueadas")
    suspend fun borrarTodas()
}
