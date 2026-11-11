package co.autoscolombia.parqueadero.integracion;

import co.autoscolombia.parqueadero.application.MovimientoService;
import co.autoscolombia.parqueadero.application.dto.Dtos.MovimientoDto;
import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Novedades (RN-07), historial con filtros y administración de celdas (RN-09). */
@EnabledIf("co.autoscolombia.parqueadero.integracion.IntegracionBase#dockerDisponible")
class NovedadesHistorialCeldasIntegrationTest extends IntegracionBase {

    @Autowired MovimientoService movimientoService;

    @Test
    @DisplayName("novedad sin descripción → 400; novedad válida → 201 con autor y fecha")
    void novedades() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda c = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        MovimientoDto m = movimientoService.registrarEntrada(v.getPlaca(), c.getId(), idOperador());
        String token = bearerOperador();

        mvc.perform(post("/api/movements/" + m.id() + "/incidents").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("tipo", "OBSERVACION", "descripcion", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());

        mvc.perform(post("/api/movements/" + m.id() + "/incidents").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("tipo", "DANO_VEHICULO", "descripcion", "Rayón en el bómper"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movimientoId").value(m.id()))
                .andExpect(jsonPath("$.usuario").value("Carlos Operador"))
                .andExpect(jsonPath("$.fechaHora").exists());

        mvc.perform(get("/api/movements/" + m.id() + "/incidents").header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descripcion").value("Rayón en el bómper"));

        mvc.perform(post("/api/movements/999999999/incidents").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("tipo", "OTRO", "descripcion", "x"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("historial filtra por placa (normalizada) y por rango de fechas de entrada")
    void historialConFiltros() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda c = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        Long op = idOperador();
        insertarMovimientoCerrado(v.getId(), c.getId(), op, LocalDateTime.of(2025, 1, 10, 8, 0));
        insertarMovimientoCerrado(v.getId(), c.getId(), op, LocalDateTime.of(2025, 1, 20, 23, 30));
        insertarMovimientoCerrado(v.getId(), c.getId(), op, LocalDateTime.of(2025, 2, 5, 9, 15));
        String admin = bearerAdmin();

        mvc.perform(get("/api/movements").param("plate", " " + v.getPlaca().toLowerCase()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fechaHoraEntrada", startsWith("2025-02-05T09:15"))); // más reciente primero

        // "to" es inclusivo: el 20 de enero a las 23:30 entra en el rango.
        mvc.perform(get("/api/movements").param("plate", v.getPlaca())
                        .param("from", "2025-01-10").param("to", "2025-01-20").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/movements").param("plate", v.getPlaca())
                        .param("from", "2025-02-01").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/movements").param("plate", v.getPlaca())
                        .param("from", "2025-03-01").param("to", "2025-03-31").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/movements").param("from", "2025-02-01").param("to", "2025-01-01")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("RANGO_FECHAS_INVALIDO"));

        mvc.perform(get("/api/movements").param("from", "no-es-fecha").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RN-09: una celda ocupada no puede ponerse fuera de servicio (409)")
    void celdaOcupadaNoFueraDeServicio() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda c = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        movimientoService.registrarEntrada(v.getPlaca(), c.getId(), idOperador());

        mvc.perform(put("/api/cells/" + c.getId()).header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("codigo", c.getCodigo(), "zona", c.getZona(),
                                "tipoVehiculo", "CARRO", "estado", "FUERA_SERVICIO"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CELDA_OCUPADA_NO_MODIFICABLE"));

        assertThat(estadoCelda(c.getId())).isEqualTo("OCUPADA");
    }

    @Test
    @DisplayName("administrador crea celdas; el código duplicado se rechaza (409)")
    void crearCeldas() throws Exception {
        String admin = bearerAdmin();
        String codigo = "N-" + placaUnica();
        String cuerpo = json.writeValueAsString(Map.of("codigo", codigo.toLowerCase(), "zona", "Zona Nueva",
                "tipoVehiculo", "MOTO", "estado", "DISPONIBLE"));

        mvc.perform(post("/api/cells").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(codigo));

        mvc.perform(post("/api/cells").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_CELDA_DUPLICADO"));
    }

    @Test
    @DisplayName("el dashboard devuelve los totales de ocupación")
    void dashboard() throws Exception {
        mvc.perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, bearerOperador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCeldas").isNumber())
                .andExpect(jsonPath("$.porTipo.length()").value(2));
    }

    private void insertarMovimientoCerrado(Long vehiculoId, Long celdaId, Long usuarioId, LocalDateTime entrada) {
        jdbc.update("""
                        INSERT INTO movimiento_parqueo
                          (vehiculo_id, celda_id, fecha_hora_entrada, fecha_hora_salida, usuario_entrada_id, usuario_salida_id)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                vehiculoId, celdaId, entrada, entrada.plusHours(2), usuarioId, usuarioId);
    }
}
