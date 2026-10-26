package com.example.filtro400

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de los controles de búsqueda, filtros y orden del listado. */
data class FiltroNumeros(
    val texto: String = "",
    val categoriaId: Long? = null,
    val estado: EstadoNumeros = EstadoNumeros.TODOS,
    val orden: OrdenNumeros = OrdenNumeros.FECHA,
    val ascendente: Boolean = false
) {
    val hayFiltros: Boolean
        get() = texto.isNotBlank() ||
            categoriaId != null ||
            estado != EstadoNumeros.TODOS
}

@OptIn(ExperimentalCoroutinesApi::class)
class BloqueosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio =
        BloqueosRepository(AppDatabase.obtener(application).bloqueoDao())

    val numeros: StateFlow<List<NumeroBloqueado>> = repositorio.observarNumeros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categorias: StateFlow<List<CategoriaBloqueo>> = repositorio.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoriasConConteo: StateFlow<List<CategoriaConConteo>> =
        repositorio.observarCategoriasConConteo()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val permitidos: StateFlow<List<NumeroPermitido>> =
        repositorio.observarTodosPermitidos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filtro = MutableStateFlow(FiltroNumeros())
    val filtro: StateFlow<FiltroNumeros> = _filtro.asStateFlow()

    // Se actualiza cada minuto para que los temporales caduquen también
    // mientras la pantalla está abierta.
    private val reloj: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val listado: StateFlow<List<NumeroBloqueadoListado>> =
        combine(_filtro, reloj) { filtro, ahora -> filtro to ahora }
            .flatMapLatest { (filtro, ahora) ->
                repositorio.observarListado(
                    texto = filtro.texto,
                    categoriaId = filtro.categoriaId,
                    estado = filtro.estado,
                    orden = filtro.orden,
                    ascendente = filtro.ascendente,
                    ahora = ahora
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busquedaPermitidos = MutableStateFlow("")
    val busquedaPermitidos: StateFlow<String> = _busquedaPermitidos.asStateFlow()

    val permitidosListado: StateFlow<List<NumeroPermitido>> =
        _busquedaPermitidos
            .flatMapLatest { texto -> repositorio.observarPermitidos(texto) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Mantiene las copias rápidas que consulta el servicio de llamadas.
        viewModelScope.launch {
            repositorio.observarNumeros().collect { lista ->
                BloqueadosPreferencias.guardar(getApplication(), lista)
            }
        }
        viewModelScope.launch {
            repositorio.observarTodosPermitidos().collect { lista ->
                PermitidosPreferencias.guardar(
                    getApplication(),
                    lista.map { it.numero }.toSet()
                )
            }
        }
    }

    fun actualizarTexto(texto: String) {
        _filtro.update { it.copy(texto = texto) }
    }

    fun seleccionarCategoria(categoriaId: Long?) {
        _filtro.update { it.copy(categoriaId = categoriaId) }
    }

    fun seleccionarEstado(estado: EstadoNumeros) {
        _filtro.update { it.copy(estado = estado) }
    }

    fun ordenar(orden: OrdenNumeros, ascendente: Boolean) {
        _filtro.update { it.copy(orden = orden, ascendente = ascendente) }
    }

    fun limpiarFiltros() {
        _filtro.value = FiltroNumeros()
    }

    fun guardarNumero(numero: NumeroBloqueado) {
        viewModelScope.launch { repositorio.guardarNumero(numero) }
    }

    fun eliminarNumero(id: Long) {
        viewModelScope.launch { repositorio.eliminarNumero(id) }
    }

    fun buscarPermitidos(texto: String) {
        _busquedaPermitidos.value = texto
    }

    fun guardarPermitido(permitido: NumeroPermitido) {
        viewModelScope.launch { repositorio.guardarPermitido(permitido) }
    }

    fun eliminarPermitido(id: Long) {
        viewModelScope.launch { repositorio.eliminarPermitido(id) }
    }

    fun crearCategoria(nombre: String) {
        viewModelScope.launch { repositorio.crearCategoria(nombre) }
    }

    fun renombrarCategoria(id: Long, nombre: String) {
        viewModelScope.launch { repositorio.renombrarCategoria(id, nombre) }
    }

    fun eliminarCategoria(id: Long) {
        viewModelScope.launch { repositorio.eliminarCategoria(id) }
    }

    suspend fun importarCsv(texto: String): ResumenImportacion =
        repositorio.importarCsv(texto)

    suspend fun exportarCsv(): String = repositorio.exportarCsv()
}
