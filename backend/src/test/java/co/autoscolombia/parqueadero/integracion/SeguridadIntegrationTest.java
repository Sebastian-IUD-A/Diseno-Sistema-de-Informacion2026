package co.autoscolombia.parqueadero.integracion;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autenticación y autorización por roles. */
@EnabledIf("co.autoscolombia.parqueadero.integracion.IntegracionBase#dockerDisponible")
class SeguridadIntegrationTest extends IntegracionBase {

    @Test
    @DisplayName("sin token → 401 en endpoints protegidos")
    void sinToken() throws Exception {
        mvc.perform(get("/api/movements/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("token falso → 401")
    void tokenFalso() throws Exception {
        mvc.perform(get("/api/movements/active").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login con contraseña incorrecta → 401 con mensaje genérico")
    void loginInvalido() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("correo", OPERADOR, "contrasena", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("login correcto devuelve token y rol; /api/auth/me identifica al usuario")
    void loginCorrecto() throws Exception {
        String token = bearerAdmin();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(ADMIN))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("las contraseñas se guardan como hash BCrypt, nunca en texto plano")
    void contrasenasConHash() {
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE correo = ?", String.class, OPERADOR);
        assertThat(hash).startsWith("$2").doesNotContain(CLAVE_OPERADOR);
    }

    @Test
    @DisplayName("ADMINISTRADOR no puede registrar entradas (403)")
    void adminNoRegistraEntradas() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda c = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(v.getPlaca(), c.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISOS"));
        assertThat(estadoCelda(c.getId())).isEqualTo("DISPONIBLE");
    }

    @Test
    @DisplayName("OPERADOR no puede crear ni editar celdas (403)")
    void operadorNoAdministraCeldas() throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("codigo", "X-99", "zona", "Z", "tipoVehiculo", "CARRO", "estado", "DISPONIBLE"));
        mvc.perform(post("/api/cells").header(HttpHeaders.AUTHORIZATION, bearerOperador())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPERADOR no puede consultar el historial (403); ADMINISTRADOR sí")
    void historialSoloAdmin() throws Exception {
        mvc.perform(get("/api/movements").header(HttpHeaders.AUTHORIZATION, bearerOperador()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/movements").header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un userId enviado en el cuerpo se ignora: el operador sale del token")
    void usuarioSaleDelToken() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda c = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        String cuerpo = json.writeValueAsString(Map.of("placa", v.getPlaca(), "celdaId", c.getId(), "usuarioId", 999999));

        String respuesta = mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, bearerOperador())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = leer(respuesta).get("id").asLong();
        Long usuario = jdbc.queryForObject("SELECT usuario_entrada_id FROM movimiento_parqueo WHERE id = ?", Long.class, id);
        assertThat(usuario).isEqualTo(idOperador());
    }
}
