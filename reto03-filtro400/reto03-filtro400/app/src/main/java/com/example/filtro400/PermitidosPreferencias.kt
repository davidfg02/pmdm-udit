package com.example.filtro400

import android.content.Context

/*
 * Copia rápida de la lista blanca para el servicio de llamadas.
 * Un número permitido tiene prioridad sobre cualquier bloqueo.
 */
object PermitidosPreferencias {

    private const val NOMBRE = "filtro400_permitidos"
    private const val CLAVE_NUMEROS = "numeros"

    fun leer(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
        return prefs.getStringSet(CLAVE_NUMEROS, emptySet())?.toSet() ?: emptySet()
    }

    fun guardar(context: Context, numeros: Set<String>) {
        context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(CLAVE_NUMEROS, numeros)
            .apply()
    }
}
