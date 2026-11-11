package co.autoscolombia.parqueadero.integracion;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoDocumento;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Cliente;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import co.autoscolombia.parqueadero.infrastructure.persistence.CeldaRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.ClienteRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.UsuarioRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.VehiculoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de INTEGRACIÓN: levantan la aplicación completa (Spring + seguridad + JPA)
 * contra un MySQL 8 REAL que Testcontainers arranca en Docker. Así se prueban de verdad los
 * índices únicos, los bloqueos FOR UPDATE y las transacciones.
 * <p>
 * Si Docker no está disponible, estas pruebas se OMITEN (no fallan) y solo corren las unitarias.
 * <p>
 * Patrón "contenedor único": el MySQL se inicia una sola vez para todas las clases de prueba.
 * Cada prueba crea sus propios datos (placas y códigos aleatorios) para no interferir con las demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracionBase {

    public static final String OPERADOR = "operador@autoscolombia.co";
    public static final String CLAVE_OPERADOR = "Operador123*";
    public static final String ADMIN = "admin@autoscolombia.co";
    public static final String CLAVE_ADMIN = "Admin123*";

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("autos_colombia")
            .withUsername("autos")
            .withPassword("autos_test")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true");

    private static final boolean DOCKER = detectarDocker();

    static {
        if (DOCKER) {
            MYSQL.start();
        }
    }

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    /** Usado por @EnabledIf en cada clase de prueba. */
    public static boolean dockerDisponible() {
        return DOCKER;
    }

    private static boolean detectarDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registro.add("spring.datasource.username", MYSQL::getUsername);
        registro.add("spring.datasource.password", MYSQL::getPassword);
        registro.add("app.jwt.secret", () -> "clave-solo-para-pruebas-de-integracion-0123456789");
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ClienteRepository clienteRepository;
    @Autowired protected VehiculoRepository vehiculoRepository;
    @Autowired protected CeldaRepository celdaRepository;
    @Autowired protected UsuarioRepository usuarioRepository;

    /** Inicia sesión por la API real y devuelve el valor "Bearer <token>" listo para la cabecera. */
    protected String bearer(String correo, String clave) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("correo", correo, "contrasena", clave))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(cuerpo).get("token").asText();
    }

    protected String bearerOperador() throws Exception {
        return bearer(OPERADOR, CLAVE_OPERADOR);
    }

    protected String bearerAdmin() throws Exception {
        return bearer(ADMIN, CLAVE_ADMIN);
    }

    protected Long idOperador() {
        return usuarioRepository.findByCorreoIgnoreCase(OPERADOR).orElseThrow().getId();
    }

    protected String placaUnica() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    }

    protected Vehiculo crearVehiculo(TipoVehiculo tipo, LocalDate autorizadoHasta) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Cliente cliente = clienteRepository.save(new Cliente(TipoDocumento.CC, "T" + sufijo, "Cliente", "Prueba " + sufijo,
                "3000000000", "prueba" + sufijo + "@example.com"));
        return vehiculoRepository.save(new Vehiculo(placaUnica(), tipo, "Marca", "Modelo", "Color", autorizadoHasta, cliente));
    }

    protected Vehiculo crearVehiculoVigente(TipoVehiculo tipo) {
        return crearVehiculo(tipo, LocalDate.now().plusMonths(1));
    }

    protected Celda crearCelda(TipoVehiculo tipo, EstadoCelda estado) {
        String codigo = "T" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return celdaRepository.save(new Celda(codigo, "Zona Pruebas", tipo, estado));
    }

    protected String entradaJson(String placa, Long celdaId) throws Exception {
        return json.writeValueAsString(Map.of("placa", placa, "celdaId", celdaId));
    }

    protected JsonNode leer(String cuerpo) throws Exception {
        return json.readTree(cuerpo);
    }

    protected String estadoCelda(Long celdaId) {
        return jdbc.queryForObject("SELECT estado FROM celda WHERE id = ?", String.class, celdaId);
    }

    protected int movimientosAbiertosDeCelda(Long celdaId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movimiento_parqueo WHERE celda_id = ? AND fecha_hora_salida IS NULL", Integer.class, celdaId);
        return n == null ? 0 : n;
    }

    protected int movimientosAbiertosDeVehiculo(Long vehiculoId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movimiento_parqueo WHERE vehiculo_id = ? AND fecha_hora_salida IS NULL", Integer.class, vehiculoId);
        return n == null ? 0 : n;
    }
}
