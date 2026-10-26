package com.example.filtro400

import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalTime

// CallScreeningService: Android nos avisa de cada llamada entrante
// ANTES de que suene, y nosotros decidimos qué hacer con ella.
// Solo funciona con números que NO están en la agenda del usuario.
class FiltroLlamadasService : CallScreeningService() {

    // Trabajo en segundo plano para guardar en la base de datos sin
    // bloquear el hilo principal. SupervisorJob: un fallo no cancela el resto.
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repositorio by lazy {
        LlamadasRepository(AppDatabase.obtener(applicationContext).llamadaDao())
    }

    // Android llama a este método automáticamente por cada llamada.
    override fun onScreenCall(callDetails: Call.Details) {

        // Solo filtramos llamadas entrantes (no las que haces tú).
        val esEntrante = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        // handle = la dirección de la llamada, algo como "tel:+34400123456".
        // schemeSpecificPart nos devuelve solo el número.
        val numero = callDetails.handle?.schemeSpecificPart.orEmpty()

        // Primero las reglas rápidas (sin tocar disco). La lista blanca tiene
        // prioridad: si el número está en ella, no se bloquea nunca.
        val numeroNormalizado = ReglaBloqueo.normalizar(numero)
        val enListaBlanca = esEntrante && numero.isNotEmpty() &&
            numeroNormalizado in PermitidosPreferencias.leer(applicationContext)
        val enLista = esEntrante && numero.isNotEmpty() && !enListaBlanca &&
            numeroNormalizado in BloqueadosPreferencias.leer(applicationContext)
        val coincidePrefijo = esEntrante && numero.isNotEmpty() &&
            !enListaBlanca && ReglaBloqueo.debeBloquearse(numero)

        val minutoActual = LocalTime.now().let { it.hour * 60 + it.minute }

        // Se bloquea cuando coincide la regla Y estamos dentro de una franja
        // activa (o cuando el modo por franjas está desactivado).
        val bloquear = (coincidePrefijo || enLista) &&
            ReglaHoraria.permiteBloquear(
                FranjasPreferencias.cargar(applicationContext),
                minutoActual
            )

        // Respondemos SIEMPRE y PRIMERO: Android exige una respuesta rápida,
        // así que no esperamos a que termine la base de datos.
        val respuesta = CallResponse.Builder()
            .setDisallowCall(bloquear)      // impide que suene
            .setRejectCall(bloquear)        // la rechaza
            .setSkipNotification(bloquear)  // sin notificación de llamada perdida
            .build()

        respondToCall(callDetails, respuesta)

        // Solo guardamos lo bloqueado, y fuera del hilo principal.
        if (bloquear) {
            alcance.launch {
                repositorio.registrar(numeroNormalizado)
            }
        }
    }

    override fun onDestroy() {
        // Cerramos el alcance para no dejar corrutinas vivas.
        alcance.cancel()
        super.onDestroy()
    }
}
