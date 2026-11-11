package co.autoscolombia.parqueadero.infrastructure.persistence;

import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    /** Búsqueda normal; @EntityGraph trae también el cliente en la misma consulta (evita consultas extra). */
    @EntityGraph(attributePaths = "cliente")
    Optional<Vehiculo> findByPlaca(String placa);

    /**
     * SELECT ... FOR UPDATE: bloquea la fila del vehículo hasta que termine la transacción.
     * Si dos operadores registran la entrada del MISMO vehículo a la vez, el segundo espera
     * a que el primero termine y luego ve que ya está dentro (RN-02, RN-10).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehiculo v where v.placa = :placa")
    Optional<Vehiculo> findByPlacaForUpdate(@Param("placa") String placa);
}
