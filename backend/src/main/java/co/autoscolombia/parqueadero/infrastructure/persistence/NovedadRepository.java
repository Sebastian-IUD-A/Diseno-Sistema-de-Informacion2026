package co.autoscolombia.parqueadero.infrastructure.persistence;

import co.autoscolombia.parqueadero.domain.model.Novedad;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Las novedades son un registro histórico: se crean y se consultan, nunca se editan ni se borran. */
public interface NovedadRepository extends Repository<Novedad, Long> {

    Novedad save(Novedad novedad);

    Novedad saveAndFlush(Novedad novedad);

    @Query("""
            select n from Novedad n
              join fetch n.usuario
            where n.movimiento.id = :movimientoId
            order by n.fechaHora desc
            """)
    List<Novedad> buscarPorMovimiento(@Param("movimientoId") Long movimientoId);

    /** Cuántas novedades tiene cada movimiento. Cada fila es [movimientoId, cantidad]. */
    @Query("""
            select n.movimiento.id, count(n) from Novedad n
            where n.movimiento.id in :ids
            group by n.movimiento.id
            """)
    List<Object[]> contarPorMovimientos(@Param("ids") Collection<Long> ids);
}
