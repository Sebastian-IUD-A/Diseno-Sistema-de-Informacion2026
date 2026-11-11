package co.autoscolombia.parqueadero.infrastructure.persistence;

import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RN-08 (no borrar históricos): a propósito extendemos {@link Repository} y NO JpaRepository.
 * Así este repositorio solo expone los métodos declarados aquí y no existe
 * ningún delete(...) que alguien pueda llamar por error.
 */
public interface MovimientoRepository extends Repository<MovimientoParqueo, Long> {

    MovimientoParqueo save(MovimientoParqueo movimiento);

    /** Guarda y envía el INSERT/UPDATE a MySQL de inmediato para detectar violaciones de índices únicos aquí mismo. */
    MovimientoParqueo saveAndFlush(MovimientoParqueo movimiento);

    void flush();

    Optional<MovimientoParqueo> findById(Long id);

    /** Bloquea la fila del movimiento: evita que dos salidas simultáneas cierren el mismo movimiento. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MovimientoParqueo m where m.id = :id")
    Optional<MovimientoParqueo> findByIdForUpdate(@Param("id") Long id);

    Optional<MovimientoParqueo> findByVehiculoIdAndFechaHoraSalidaIsNull(Long vehiculoId);

    long countByFechaHoraSalidaIsNull();

    /** Ocupación actual: movimientos abiertos, opcionalmente filtrados por parte de la placa. */
    @Query("""
            select m from MovimientoParqueo m
              join fetch m.vehiculo v
              join fetch v.cliente
              join fetch m.celda
              join fetch m.usuarioEntrada
            where m.fechaHoraSalida is null
              and (:placa is null or v.placa like concat('%', :placa, '%'))
            order by m.fechaHoraEntrada desc
            """)
    List<MovimientoParqueo> buscarAbiertos(@Param("placa") String placa);

    /** Detalle de un movimiento con todo lo necesario para mostrarlo. */
    @Query("""
            select m from MovimientoParqueo m
              join fetch m.vehiculo v
              join fetch v.cliente
              join fetch m.celda
              join fetch m.usuarioEntrada
              left join fetch m.usuarioSalida
            where m.id = :id
            """)
    Optional<MovimientoParqueo> buscarDetalle(@Param("id") Long id);

    /**
     * Historial por placa exacta y rango de fechas de ENTRADA [desde, hasta).
     * Cada filtro es opcional: si llega null, esa condición se ignora.
     */
    @Query("""
            select m from MovimientoParqueo m
              join fetch m.vehiculo v
              join fetch v.cliente
              join fetch m.celda
              join fetch m.usuarioEntrada
              left join fetch m.usuarioSalida
            where (:placa is null or v.placa = :placa)
              and (:desde is null or m.fechaHoraEntrada >= :desde)
              and (:hasta is null or m.fechaHoraEntrada < :hasta)
            order by m.fechaHoraEntrada desc
            """)
    List<MovimientoParqueo> buscarHistorial(@Param("placa") String placa,
                                            @Param("desde") LocalDateTime desde,
                                            @Param("hasta") LocalDateTime hasta,
                                            Pageable pagina);
}
