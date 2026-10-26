package com.example.filtro400

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Una llamada que hemos rechazado. La guardamos para mostrarla en la app.
@Entity(
    tableName = "llamadas_bloqueadas",
    indices = [Index(value = ["numero"])]
)
data class LlamadaBloqueada(
    // id = 0 significa "todavía no guardada": SQLite lo rellena solo.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: String,
    val fechaMillis: Long,
    // "No leída" permite avisar dentro de la app (sin notificaciones).
    val leida: Boolean = false
)
