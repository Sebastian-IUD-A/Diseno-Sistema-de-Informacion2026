package co.autoscolombia.parqueadero.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Reglas de autenticación y autorización (quién puede llamar qué endpoint).
 * <p>
 * Todas las reglas están en un solo lugar para que sea fácil revisarlas:
 * <pre>
 *  Endpoint                              ADMINISTRADOR  OPERADOR
 *  POST /api/auth/login                  (público)
 *  GET  /api/dashboard/summary                ✔            ✔
 *  GET  /api/vehicles/{placa}                 ✔            ✔
 *  GET  /api/cells, /api/cells/available      ✔            ✔
 *  POST/PUT /api/cells                        ✔            ✘
 *  POST /api/movements/entries                ✘            ✔
 *  POST /api/movements/{id}/exit              ✘            ✔
 *  GET  /api/movements/active                 ✔            ✔
 *  GET  /api/movements/{id}                   ✔            ✔
 *  GET  /api/movements (historial)            ✔            ✘
 *  POST /api/movements/{id}/incidents         ✘            ✔
 *  GET  /api/movements/{id}/incidents         ✔            ✔
 * </pre>
 * Cualquier otra ruta de /api se rechaza (denyAll) por seguridad.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String OPERADOR = "OPERADOR";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ManejadorErroresSeguridad manejadorErrores,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                // CSRF protege sesiones basadas en cookies. Esta API usa tokens en la cabecera
                // Authorization (no cookies), por eso CSRF no aplica y se desactiva.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // STATELESS: el servidor no guarda sesión; cada petición trae su token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/summary").hasAnyRole(ADMIN, OPERADOR)

                        .requestMatchers(HttpMethod.GET, "/api/vehicles/*").hasAnyRole(ADMIN, OPERADOR)

                        .requestMatchers(HttpMethod.GET, "/api/cells", "/api/cells/available").hasAnyRole(ADMIN, OPERADOR)
                        .requestMatchers(HttpMethod.POST, "/api/cells").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/cells/*").hasRole(ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/movements/entries").hasRole(OPERADOR)
                        .requestMatchers(HttpMethod.POST, "/api/movements/*/exit").hasRole(OPERADOR)
                        .requestMatchers(HttpMethod.POST, "/api/movements/*/incidents").hasRole(OPERADOR)
                        .requestMatchers(HttpMethod.GET, "/api/movements/*/incidents").hasAnyRole(ADMIN, OPERADOR)
                        .requestMatchers(HttpMethod.GET, "/api/movements/active").hasAnyRole(ADMIN, OPERADOR)
                        .requestMatchers(HttpMethod.GET, "/api/movements/*").hasAnyRole(ADMIN, OPERADOR)
                        .requestMatchers(HttpMethod.GET, "/api/movements").hasRole(ADMIN)

                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores));
        return http.build();
    }

    /** Lee el claim "roles" del token y lo convierte en autoridades ROLE_ADMINISTRADOR / ROLE_OPERADOR. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(TokenService.CLAIM_ROLES);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    /**
     * CORS: permite que el frontend en otro origen (p. ej. Vite en :5173) llame a la API.
     * Con Docker el frontend y la API comparten origen gracias al proxy de Nginx.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origenes:http://localhost:5173}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
