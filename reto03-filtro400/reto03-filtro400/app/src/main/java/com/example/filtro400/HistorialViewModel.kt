package com.example.filtro400

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Estado del historial. Room avisa de cada cambio, así que la lista
// se actualiza sola aunque la app esté abierta.
class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio =
        LlamadasRepository(AppDatabase.obtener(application).llamadaDao())

    // WhileSubscribed: solo escuchamos la base de datos mientras hay pantalla.
    val llamadas: StateFlow<List<LlamadaBloqueada>> = repositorio.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noLeidas: StateFlow<Int> = repositorio.observarNoLeidas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun marcarTodasLeidas() {
        viewModelScope.launch { repositorio.marcarTodasLeidas() }
    }

    fun borrarTodas() {
        viewModelScope.launch { repositorio.borrarTodas() }
    }
}
