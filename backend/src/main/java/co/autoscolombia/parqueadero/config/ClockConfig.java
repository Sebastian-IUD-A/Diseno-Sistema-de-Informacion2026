package co.autoscolombia.parqueadero.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj de la aplicación en la zona horaria de Colombia.
 * <p>
 * Todos los servicios piden la hora a este {@link Clock} en lugar de llamar a LocalDateTime.now()
 * directamente. Ventajas: la hora no depende de la zona del servidor (Docker suele usar UTC)
 * y en las pruebas podemos usar un reloj fijo para resultados predecibles.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
