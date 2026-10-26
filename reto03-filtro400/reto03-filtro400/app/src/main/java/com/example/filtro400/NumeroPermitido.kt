package com.example.filtro400

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Número de la lista blanca. Tiene prioridad sobre la lista de bloqueados y
 * sobre la regla automática del 400.
 */
@Entity(
    tableName = "numeros_permitidos",
    indices = [Index(value = ["numero"], unique = true)]
)
data class NumeroPermitido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: String,
    val motivo: String = "",
    @ColumnInfo(defaultValue = "0")
    val fechaAlta: Long = System.currentTimeMillis()
)
