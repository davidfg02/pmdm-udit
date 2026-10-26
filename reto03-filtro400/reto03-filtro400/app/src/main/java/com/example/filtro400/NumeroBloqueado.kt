package com.example.filtro400

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Número bloqueado manualmente. Se suma a la regla automática del prefijo 400.
 * El número se guarda normalizado (sin +34, espacios ni guiones).
 *
 * fechaAlta permite ordenar la lista. tipo/expiraEn permiten tener bloqueos
 * temporales: un temporal deja de bloquear en cuanto pasa su fecha.
 */
@Entity(
    tableName = "numeros_bloqueados",
    indices = [
        Index(value = ["numero"], unique = true),
        Index(value = ["categoriaId"])
    ]
)
data class NumeroBloqueado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: String,
    val motivo: String = "",
    val categoriaId: Long,
    @ColumnInfo(defaultValue = "0")
    val fechaAlta: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'PERMANENTE'")
    val tipo: TipoBloqueo = TipoBloqueo.PERMANENTE,
    val expiraEn: Long? = null
) {
    fun estaExpirado(ahora: Long = System.currentTimeMillis()): Boolean =
        tipo == TipoBloqueo.TEMPORAL && expiraEn != null && expiraEn <= ahora

    fun estaActivo(ahora: Long = System.currentTimeMillis()): Boolean =
        !estaExpirado(ahora)
}

/**
 * Proyección que devuelve la consulta filtrada: añade el nombre de la
 * categoría y el número de llamadas bloqueadas de ese número.
 */
data class NumeroBloqueadoListado(
    val id: Long,
    val numero: String,
    val motivo: String,
    val categoriaId: Long,
    val categoria: String,
    val fechaAlta: Long,
    val tipo: TipoBloqueo,
    val expiraEn: Long?,
    val intentos: Int
) {
    fun comoEntidad(): NumeroBloqueado = NumeroBloqueado(
        id = id,
        numero = numero,
        motivo = motivo,
        categoriaId = categoriaId,
        fechaAlta = fechaAlta,
        tipo = tipo,
        expiraEn = expiraEn
    )
}
