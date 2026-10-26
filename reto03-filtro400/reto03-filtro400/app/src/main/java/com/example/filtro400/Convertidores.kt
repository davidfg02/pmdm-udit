package com.example.filtro400

import androidx.room.TypeConverter

class Convertidores {

    @TypeConverter
    fun tipoATexto(tipo: TipoBloqueo): String = tipo.name

    @TypeConverter
    fun textoATipo(valor: String): TipoBloqueo =
        TipoBloqueo.entries.firstOrNull { it.name == valor } ?: TipoBloqueo.PERMANENTE
}
