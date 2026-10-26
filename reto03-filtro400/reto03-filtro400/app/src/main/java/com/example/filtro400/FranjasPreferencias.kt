package com.example.filtro400

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/*
 * Las franjas se guardan en SharedPreferences, no en Room.
 *
 * Motivo: el CallScreeningService tiene que contestar a Android en milisegundos.
 * Leer un JSON pequeño de SharedPreferences es inmediato (y queda en memoria),
 * mientras que una consulta a Room obligaría a esperar a la base de datos.
 */
object FranjasPreferencias {

    private const val NOMBRE = "filtro400_franjas"
    private const val CLAVE_USAR = "usar_franjas"
    private const val CLAVE_FRANJAS = "franjas"

    fun cargar(context: Context): ConfiguracionFranjas {
        val prefs = context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
        val usar = prefs.getBoolean(CLAVE_USAR, false)

        val franjas = runCatching {
            val array = JSONArray(prefs.getString(CLAVE_FRANJAS, "[]"))
            (0 until array.length()).map { indice ->
                val objeto = array.getJSONObject(indice)
                FranjaHoraria(
                    id = objeto.optLong("id", indice.toLong() + 1),
                    inicioMinuto = objeto.getInt("inicio"),
                    finMinuto = objeto.getInt("fin"),
                    activa = objeto.optBoolean("activa", true)
                )
            }
        }.getOrDefault(emptyList())

        return ConfiguracionFranjas(
            usarFranjas = usar,
            franjas = franjas.sortedBy { it.inicioMinuto }
        )
    }

    fun guardar(context: Context, configuracion: ConfiguracionFranjas) {
        val array = JSONArray()
        configuracion.franjas.forEach { franja ->
            array.put(
                JSONObject().apply {
                    put("id", franja.id)
                    put("inicio", franja.inicioMinuto)
                    put("fin", franja.finMinuto)
                    put("activa", franja.activa)
                }
            )
        }

        context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CLAVE_USAR, configuracion.usarFranjas)
            .putString(CLAVE_FRANJAS, array.toString())
            .apply()
    }
}
