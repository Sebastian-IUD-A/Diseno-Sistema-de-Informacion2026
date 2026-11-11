package co.autoscolombia.parqueadero.infrastructure.persistence;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.model.Celda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CeldaRepository extends JpaRepository<Celda, Long> {

    /**
     * SELECT ... FOR UPDATE sobre la celda. Es la pieza clave de RN-04/RN-10:
     * si dos operadores eligen la misma celda al mismo tiempo, MySQL hace esperar al
     * segundo hasta que el primero confirme; entonces el segundo lee la celda ya OCUPADA
     * y recibe un error 409 en lugar de crear un segundo movimiento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Celda c where c.id = :id")
    Optional<Celda> findByIdForUpdate(@Param("id") Long id);

    List<Celda> findByEstadoAndTipoVehiculoOrderByCodigoAsc(EstadoCelda estado, TipoVehiculo tipoVehiculo);

    List<Celda> findAllByOrderByCodigoAsc();

    long countByEstado(EstadoCelda estado);

    long countByEstadoAndTipoVehiculo(EstadoCelda estado, TipoVehiculo tipoVehiculo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}
