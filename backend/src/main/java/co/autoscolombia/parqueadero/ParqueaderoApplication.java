package co.autoscolombia.parqueadero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend.
 * <p>
 * {@code @SpringBootApplication} activa la configuración automática y el escaneo de
 * componentes en este paquete y sus subpaquetes (presentation, application, domain,
 * infrastructure, security, config).
 */
@SpringBootApplication
public class ParqueaderoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParqueaderoApplication.class, args);
    }
}
