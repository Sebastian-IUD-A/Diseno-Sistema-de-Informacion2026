package co.autoscolombia.parqueadero.integracion;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Entradas y salidas de punta a punta: HTTP → seguridad → servicio → MySQL. */
@EnabledIf("co.autoscolombia.parqueadero.integracion.IntegracionBase#dockerDisponible")
class EntradaSalidaIntegrationTest extends IntegracionBase {

    @Test
    @DisplayName("placa inexistente → 404 en consulta y en entrada")
    void placaInexistente() throws Exception {
        String token = bearerOperador();
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);

        mvc.perform(get("/api/vehicles/NOEXISTE1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEHICULO_NO_ENCONTRADO"));

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson("NOEXISTE1", celda.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEHICULO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("autorización vencida → 422 y la celda sigue disponible")
    void autorizacionVencida() throws Exception {
        Vehiculo vencido = crearVehiculo(TipoVehiculo.CARRO, LocalDate.now().minusDays(5));
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);

        mvc.perform(get("/api/vehicles/" + vencido.getPlaca()).header(HttpHeaders.AUTHORIZATION, bearerOperador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizacionVigente").value(false))
                .andExpect(jsonPath("$.puedeIngresar").value(false))
                .andExpect(jsonPath("$.motivoNoIngreso.codigo").value("AUTORIZACION_VENCIDA"));

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, bearerOperador())
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(vencido.getPlaca(), celda.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AUTORIZACION_VENCIDA"));

        assertThat(estadoCelda(celda.getId())).isEqualTo("DISPONIBLE");
        assertThat(movimientosAbiertosDeVehiculo(vencido.getId())).isZero();
    }

    @Test
    @DisplayName("entrada correcta → 201, celda OCUPADA, usuario tomado del token y placa normalizada")
    void entradaCorrecta() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        String placaDesordenada = "  " + v.getPlaca().toLowerCase() + " ";

        String cuerpo = mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, bearerOperador())
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(placaDesordenada, celda.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value(v.getPlaca()))
                .andExpect(jsonPath("$.abierto").value(true))
                .andReturn().getResponse().getContentAsString();

        long movimientoId = leer(cuerpo).get("id").asLong();
        assertThat(estadoCelda(celda.getId())).isEqualTo("OCUPADA");
        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT usuario_entrada_id, fecha_hora_entrada FROM movimiento_parqueo WHERE id = ?", movimientoId);
        assertThat(((Number) fila.get("usuario_entrada_id")).longValue()).isEqualTo(idOperador());
        assertThat(fila.get("fecha_hora_entrada")).isNotNull();
    }

    @Test
    @DisplayName("vehículo que ya está dentro → 409 VEHICULO_YA_DENTRO")
    void vehiculoYaDentro() throws Exception {
        String token = bearerOperador();
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda primera = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        Celda segunda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(v.getPlaca(), primera.getId())))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(v.getPlaca(), segunda.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VEHICULO_YA_DENTRO"));

        assertThat(estadoCelda(segunda.getId())).isEqualTo("DISPONIBLE");
    }

    @Test
    @DisplayName("celda ocupada → 409; celda fuera de servicio → 422")
    void celdaOcupadaOFueraDeServicio() throws Exception {
        String token = bearerOperador();
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        Celda fuera = crearCelda(TipoVehiculo.CARRO, EstadoCelda.FUERA_SERVICIO);
        Vehiculo primero = crearVehiculoVigente(TipoVehiculo.CARRO);
        Vehiculo segundo = crearVehiculoVigente(TipoVehiculo.CARRO);

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(primero.getPlaca(), celda.getId())))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(segundo.getPlaca(), celda.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CELDA_OCUPADA"));

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(segundo.getPlaca(), fuera.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CELDA_FUERA_SERVICIO"));

        assertThat(movimientosAbiertosDeVehiculo(segundo.getId())).isZero();
    }

    @Test
    @DisplayName("salida correcta → cierra el movimiento, guarda el operador y libera la celda")
    void salidaCorrecta() throws Exception {
        String token = bearerOperador();
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.MOTO);
        Celda celda = crearCelda(TipoVehiculo.MOTO, EstadoCelda.DISPONIBLE);

        String cuerpo = mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(v.getPlaca(), celda.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = leer(cuerpo).get("id").asLong();

        mvc.perform(get("/api/movements/active").param("plate", v.getPlaca()).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String salida = mvc.perform(post("/api/movements/" + id + "/exit").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abierto").value(false))
                .andReturn().getResponse().getContentAsString();
        JsonNode cerrado = leer(salida);
        assertThat(cerrado.get("fechaHoraSalida").isNull()).isFalse();
        assertThat(cerrado.get("usuarioSalida").asText()).isNotBlank();

        assertThat(estadoCelda(celda.getId())).isEqualTo("DISPONIBLE");
        assertThat(movimientosAbiertosDeCelda(celda.getId())).isZero();
        Long usuarioSalida = jdbc.queryForObject("SELECT usuario_salida_id FROM movimiento_parqueo WHERE id = ?", Long.class, id);
        assertThat(usuarioSalida).isEqualTo(idOperador());

        // Una segunda salida del mismo movimiento se rechaza
        mvc.perform(post("/api/movements/" + id + "/exit").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MOVIMIENTO_YA_CERRADO"));

        // El vehículo puede volver a entrar
        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(v.getPlaca(), celda.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("celdas disponibles solo lista celdas libres del tipo pedido")
    void celdasDisponibles() throws Exception {
        Celda moto = crearCelda(TipoVehiculo.MOTO, EstadoCelda.DISPONIBLE);
        Celda motoFuera = crearCelda(TipoVehiculo.MOTO, EstadoCelda.FUERA_SERVICIO);

        String cuerpo = mvc.perform(get("/api/cells/available").param("type", "MOTO")
                        .header(HttpHeaders.AUTHORIZATION, bearerOperador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode lista = leer(cuerpo);
        assertThat(lista.findValuesAsText("codigo")).contains(moto.getCodigo()).doesNotContain(motoFuera.getCodigo());
        assertThat(lista.findValuesAsText("tipoVehiculo")).containsOnly("MOTO");
        assertThat(lista.findValuesAsText("estado")).containsOnly("DISPONIBLE");
    }
}
