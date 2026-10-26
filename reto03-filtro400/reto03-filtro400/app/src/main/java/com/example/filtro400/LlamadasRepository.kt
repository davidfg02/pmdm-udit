package com.example.filtro400

import kotlinx.coroutines.flow.Flow

// Envuelve el DAO para que el servicio y la pantalla no dependan de Room.
class LlamadasRepository(private val dao: LlamadaDao) {

    fun observarTodas(): Flow<List<LlamadaBloqueada>> = dao.observarTodas()

    fun observarNoLeidas(): Flow<Int> = dao.observarNoLeidas()

    suspend fun registrar(numero: String, fechaMillis: Long = System.currentTimeMillis()) {
        dao.insertar(LlamadaBloqueada(numero = numero, fechaMillis = fechaMillis))
    }

    suspend fun marcarTodasLeidas() = dao.marcarTodasLeidas()

    suspend fun borrarTodas() = dao.borrarTodas()
}
