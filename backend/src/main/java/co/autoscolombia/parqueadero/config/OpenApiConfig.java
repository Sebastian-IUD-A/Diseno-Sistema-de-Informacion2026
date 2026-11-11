package co.autoscolombia.parqueadero.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación interactiva de la API (Swagger UI) en http://localhost:8080/swagger-ui.html
 * Para probar endpoints protegidos: hacer login, copiar el token y pulsar "Authorize".
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        String esquema = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Autos Colombia - API del parqueadero")
                        .version("1.0.0 (Iteración 1)")
                        .description("Entradas, salidas, celdas, ocupación, historial y novedades."))
                .addSecurityItem(new SecurityRequirement().addList(esquema))
                .components(new Components().addSecuritySchemes(esquema,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
