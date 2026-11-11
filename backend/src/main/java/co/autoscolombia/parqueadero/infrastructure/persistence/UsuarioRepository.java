package co.autoscolombia.parqueadero.infrastructure.persistence;

import co.autoscolombia.parqueadero.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio = objeto que habla con la base de datos por nosotros.
 * Spring Data genera la implementación a partir del nombre del método:
 * "findByCorreoIgnoreCase" se traduce a SELECT ... WHERE LOWER(correo) = LOWER(?).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
