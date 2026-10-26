package com.example.filtro400

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class CategoriaBloqueo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String
)

// Proyección para la pantalla de categorías: cuántos números usa cada una.
data class CategoriaConConteo(
    val id: Long,
    val nombre: String,
    val total: Int
)
