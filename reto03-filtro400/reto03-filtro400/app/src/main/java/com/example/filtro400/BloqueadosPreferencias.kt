package com.example.filtro400

import android.content.Context

/*
 * Copia rápida de los números bloqueados en SharedPreferences.
 *
 * El CallScreeningService debe responder a Android en milisegundos, así que
 * no puede esperar a una consulta de Room. La pantalla mantiene esta copia
 * sincronizada cada vez que la lista cambia.
 *
 * Los temporales se guardan como "numero|fechaDeCaducidad". Así el servicio
 * puede dejar de bloquearlos en cuanto caducan, sin tocar la base de datos.
 */
object BloqueadosPreferencias {

    private const val NOMBRE = "filtro400_bloqueados"
    private const val CLAVE_NUMEROS = "numeros"
    private const val SEPARADOR = "|"

    fun leer(
        context: Context,
        ahora: Long = System.currentTimeMillis()
    ): Set<String> {
        val prefs = context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
        // getStringSet puede devolver una colección interna: la copiamos.
        return prefs.getStringSet(CLAVE_NUMEROS, emptySet())
            .orEmpty()
            .mapNotNull { entrada ->
                val numero = entrada.substringBefore(SEPARADOR)
                if (numero.isBlank()) return@mapNotNull null

                val caduca = entrada.substringAfter(SEPARADOR, "")
                    .toLongOrNull()
                if (caduca == null || caduca > ahora) numero else null
            }
            .toSet()
    }

    fun guardar(context: Context, numeros: List<NumeroBloqueado>) {
        val entradas = numeros.mapTo(mutableSetOf()) { numero ->
            if (numero.tipo == TipoBloqueo.TEMPORAL && numero.expiraEn != null) {
                "${numero.numero}$SEPARADOR${numero.expiraEn}"
            } else {
                numero.numero
            }
        }
        context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(CLAVE_NUMEROS, entradas)
            .apply()
    }
}
