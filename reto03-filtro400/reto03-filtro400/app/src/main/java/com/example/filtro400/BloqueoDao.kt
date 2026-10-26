package com.example.filtro400

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BloqueoDao {

    // ---------- Números bloqueados ----------

    @Query("SELECT * FROM numeros_bloqueados ORDER BY numero ASC")
    fun observarNumeros(): Flow<List<NumeroBloqueado>>

    @Query("SELECT * FROM numeros_bloqueados ORDER BY numero ASC")
    suspend fun obtenerNumeros(): List<NumeroBloqueado>

    /**
     * Consulta única para búsqueda, categoría, estado y orden.
     *
     * Todo lo que viene del usuario (texto, categoría, estado y orden) viaja
     * como parámetro enlazado. No se concatena ningún valor en el SQL.
     */
    @Query(
        """
        SELECT n.id AS id,
               n.numero AS numero,
               n.motivo AS motivo,
               n.categoriaId AS categoriaId,
               c.nombre AS categoria,
               n.fechaAlta AS fechaAlta,
               n.tipo AS tipo,
               n.expiraEn AS expiraEn,
               COALESCE(l.intentos, 0) AS intentos
        FROM numeros_bloqueados n
        INNER JOIN categorias c ON c.id = n.categoriaId
        LEFT JOIN (
            SELECT numero, COUNT(*) AS intentos
            FROM llamadas_bloqueadas
            GROUP BY numero
        ) l ON l.numero = n.numero
        WHERE (
                :texto = ''
                OR (:textoNumero <> '' AND n.numero LIKE '%' || :textoNumero || '%')
                OR LOWER(n.motivo) LIKE '%' || :texto || '%'
              )
          AND (:categoriaId IS NULL OR n.categoriaId = :categoriaId)
          AND (
                :estado = 'TODOS'
                OR (
                    :estado = 'ACTIVOS'
                    AND (
                        n.tipo = 'PERMANENTE'
                        OR n.expiraEn IS NULL
                        OR n.expiraEn > :ahora
                    )
                )
                OR (
                    :estado = 'TEMPORALES'
                    AND n.tipo = 'TEMPORAL'
                    AND (n.expiraEn IS NULL OR n.expiraEn > :ahora)
                )
                OR (:estado = 'PERMANENTES' AND n.tipo = 'PERMANENTE')
                OR (
                    :estado = 'EXPIRADOS'
                    AND n.tipo = 'TEMPORAL'
                    AND n.expiraEn IS NOT NULL
                    AND n.expiraEn <= :ahora
                )
              )
        ORDER BY
            CASE WHEN :orden = 'FECHA' AND :ascendente = 1 THEN n.fechaAlta END ASC,
            CASE WHEN :orden = 'FECHA' AND :ascendente = 0 THEN n.fechaAlta END DESC,
            CASE WHEN :orden = 'NUMERO' AND :ascendente = 1 THEN n.numero END ASC,
            CASE WHEN :orden = 'NUMERO' AND :ascendente = 0 THEN n.numero END DESC,
            CASE WHEN :orden = 'INTENTOS' AND :ascendente = 1
                 THEN COALESCE(l.intentos, 0) END ASC,
            CASE WHEN :orden = 'INTENTOS' AND :ascendente = 0
                 THEN COALESCE(l.intentos, 0) END DESC,
            CASE WHEN :orden = 'CATEGORIA' AND :ascendente = 1
                 THEN c.nombre COLLATE NOCASE END ASC,
            CASE WHEN :orden = 'CATEGORIA' AND :ascendente = 0
                 THEN c.nombre COLLATE NOCASE END DESC,
            n.id ASC
        """
    )
    fun observarListado(
        texto: String,
        textoNumero: String,
        categoriaId: Long?,
        estado: String,
        orden: String,
        ascendente: Boolean,
        ahora: Long
    ): Flow<List<NumeroBloqueadoListado>>

    @Query("SELECT * FROM numeros_bloqueados WHERE numero = :numero LIMIT 1")
    suspend fun buscarPorNumero(numero: String): NumeroBloqueado?

    @Insert
    suspend fun insertarNumero(numero: NumeroBloqueado): Long

    @Update
    suspend fun actualizarNumero(numero: NumeroBloqueado)

    @Query("DELETE FROM numeros_bloqueados WHERE id = :id")
    suspend fun eliminarNumero(id: Long)

    // ---------- Lista blanca ----------

    @Query(
        """
        SELECT * FROM numeros_permitidos
        WHERE (
                :texto = ''
                OR (:textoNumero <> '' AND numero LIKE '%' || :textoNumero || '%')
                OR LOWER(motivo) LIKE '%' || :texto || '%'
              )
        ORDER BY fechaAlta DESC, numero ASC
        """
    )
    fun observarPermitidos(
        texto: String,
        textoNumero: String
    ): Flow<List<NumeroPermitido>>

    @Query("SELECT * FROM numeros_permitidos ORDER BY fechaAlta DESC, numero ASC")
    fun observarTodosPermitidos(): Flow<List<NumeroPermitido>>

    @Query("SELECT * FROM numeros_permitidos WHERE numero = :numero LIMIT 1")
    suspend fun buscarPermitidoPorNumero(numero: String): NumeroPermitido?

    @Insert
    suspend fun insertarPermitido(permitido: NumeroPermitido): Long

    @Update
    suspend fun actualizarPermitido(permitido: NumeroPermitido)

    @Query("DELETE FROM numeros_permitidos WHERE id = :id")
    suspend fun eliminarPermitido(id: Long)

    // ---------- Categorías ----------

    @Query("SELECT * FROM categorias ORDER BY nombre COLLATE NOCASE ASC")
    fun observarCategorias(): Flow<List<CategoriaBloqueo>>

    @Query("SELECT * FROM categorias ORDER BY nombre COLLATE NOCASE ASC")
    suspend fun obtenerCategorias(): List<CategoriaBloqueo>

    @Query("SELECT * FROM categorias WHERE nombre = :nombre COLLATE NOCASE LIMIT 1")
    suspend fun buscarCategoriaPorNombre(nombre: String): CategoriaBloqueo?

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    suspend fun buscarCategoriaPorId(id: Long): CategoriaBloqueo?

    @Query(
        """
        SELECT c.id AS id, c.nombre AS nombre, COUNT(n.id) AS total
        FROM categorias c
        LEFT JOIN numeros_bloqueados n ON n.categoriaId = c.id
        GROUP BY c.id, c.nombre
        ORDER BY c.nombre COLLATE NOCASE ASC
        """
    )
    fun observarCategoriasConConteo(): Flow<List<CategoriaConConteo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarCategoria(categoria: CategoriaBloqueo): Long

    @Update
    suspend fun actualizarCategoria(categoria: CategoriaBloqueo)

    @Query("DELETE FROM categorias WHERE id = :id")
    suspend fun eliminarCategoria(id: Long)

    @Query("UPDATE numeros_bloqueados SET categoriaId = :destino WHERE categoriaId = :origen")
    suspend fun reasignarCategoria(origen: Long, destino: Long)
}
