package co.autoscolombia.parqueadero.infrastructure.persistence;

import co.autoscolombia.parqueadero.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a clientes. En la iteración 1 se cargan por migración; la gestión de clientes llega después. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
