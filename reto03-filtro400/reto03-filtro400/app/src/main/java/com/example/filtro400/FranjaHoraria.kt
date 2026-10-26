package com.example.filtro400

import java.util.Locale

/*
 * Una franja horaria se guarda en minutos desde medianoche (0..1439).
 * Así es fácil compararla con la hora actual y sobrevive a cambios de zona
 * horaria: siempre representa "la hora del reloj del móvil".
 */
data class FranjaHoraria(
    val id: Long = 0,
    val inicioMinuto: Int,
    val finMinuto: Int,
    val activa: Boolean = true
) {

    /** true si la hora indicada (en minutos) cae dentro de esta franja. */
    fun contiene(minutoDelDia: Int): Boolean {
        if (!activa) return false

        val minuto = ((minutoDelDia % MINUTOS_DIA) + MINUTOS_DIA) % MINUTOS_DIA

        return when {
            // Inicio y fin iguales se interpretan como "todo el día".
            inicioMinuto == finMinuto -> true
            // Franja normal: 09:00 -> 14:00.
            inicioMinuto < finMinuto -> minuto in inicioMinuto until finMinuto
            // Franja nocturna: 22:00 -> 08:00 (cruza medianoche).
            else -> minuto >= inicioMinuto || minuto < finMinuto
        }
    }

    /** Duración en minutos, teniendo en cuenta si cruza la medianoche. */
    fun duracionMinutos(): Int = when {
        inicioMinuto == finMinuto -> MINUTOS_DIA
        finMinuto > inicioMinuto -> finMinuto - inicioMinuto
        else -> (MINUTOS_DIA - inicioMinuto) + finMinuto
    }

    val cruzaMedianoche: Boolean
        get() = inicioMinuto > finMinuto

    fun rangoTexto(): String = "${formatearHora(inicioMinuto)} – ${formatearHora(finMinuto)}"

    fun duracionTexto(): String {
        val total = duracionMinutos()
        val horas = total / 60
        val minutos = total % 60
        return buildString {
            if (horas > 0) append("$horas h")
            if (minutos > 0) {
                if (isNotEmpty()) append(" ")
                append("$minutos min")
            }
            if (isEmpty()) append("0 min")
        }
    }

    companion object {
        const val MINUTOS_DIA = 24 * 60
    }
}

data class ConfiguracionFranjas(
    val usarFranjas: Boolean = false,
    val franjas: List<FranjaHoraria> = emptyList()
) {
    val activas: List<FranjaHoraria> get() = franjas.filter { it.activa }
}

/*
 * Decisión de "¿toca bloquear ahora?". Separada de Android para poder
 * probarla con tests normales de JUnit.
 */
object ReglaHoraria {

    fun permiteBloquear(config: ConfiguracionFranjas, minutoDelDia: Int): Boolean {
        if (!config.usarFranjas) return true
        return config.activas.any { it.contiene(minutoDelDia) }
    }
}

fun formatearHora(minutoDelDia: Int): String {
    val minuto = ((minutoDelDia % FranjaHoraria.MINUTOS_DIA) + FranjaHoraria.MINUTOS_DIA) %
        FranjaHoraria.MINUTOS_DIA
    return String.format(Locale.getDefault(), "%02d:%02d", minuto / 60, minuto % 60)
}
