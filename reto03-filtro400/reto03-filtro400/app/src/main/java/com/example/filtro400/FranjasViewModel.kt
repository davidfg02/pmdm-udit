package com.example.filtro400

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado de las franjas horarias. Cada cambio se guarda al momento para que
// el servicio de llamadas lea siempre la última configuración.
class FranjasViewModel(application: Application) : AndroidViewModel(application) {

    private val _configuracion = MutableStateFlow(FranjasPreferencias.cargar(application))
    val configuracion: StateFlow<ConfiguracionFranjas> = _configuracion.asStateFlow()

    fun activarFranjas(activar: Boolean) {
        actualizar { config ->
            if (activar && config.franjas.isEmpty()) {
                // Si es la primera vez, proponemos una franja nocturna típica.
                config.copy(
                    usarFranjas = true,
                    franjas = listOf(
                        FranjaHoraria(
                            id = System.currentTimeMillis(),
                            inicioMinuto = 22 * 60,
                            finMinuto = 8 * 60
                        )
                    )
                )
            } else {
                config.copy(usarFranjas = activar)
            }
        }
    }

    fun guardarFranja(franja: FranjaHoraria) {
        val definitiva = if (franja.id == 0L) {
            franja.copy(id = System.currentTimeMillis())
        } else {
            franja
        }
        actualizar { config ->
            config.copy(
                franjas = (config.franjas.filterNot { it.id == definitiva.id } + definitiva)
                    .sortedBy { it.inicioMinuto }
            )
        }
    }

    fun cambiarActiva(id: Long, activa: Boolean) {
        actualizar { config ->
            config.copy(
                franjas = config.franjas.map {
                    if (it.id == id) it.copy(activa = activa) else it
                }
            )
        }
    }

    fun eliminarFranja(id: Long) {
        actualizar { config ->
            config.copy(franjas = config.franjas.filterNot { it.id == id })
        }
    }

    private fun actualizar(transformacion: (ConfiguracionFranjas) -> ConfiguracionFranjas) {
        val nueva = transformacion(_configuracion.value)
        _configuracion.value = nueva
        FranjasPreferencias.guardar(getApplication(), nueva)
    }
}
