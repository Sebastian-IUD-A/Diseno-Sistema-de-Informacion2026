package co.autoscolombia.parqueadero.integracion;

import co.autoscolombia.parqueadero.application.MovimientoService;
import co.autoscolombia.parqueadero.application.dto.Dtos.MovimientoDto;
import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RN-05 y RN-10: atomicidad (rollback) y protección ante solicitudes concurrentes. */
@EnabledIf("co.autoscolombia.parqueadero.integracion.IntegracionBase#dockerDisponible")
class TransaccionesConcurrenciaIntegrationTest extends IntegracionBase {

    @Autowired MovimientoService movimientoService;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("fallo intermedio durante la salida → rollback: el movimiento sigue abierto y la celda OCUPADA")
    void falloIntermedioHaceRollback() {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        MovimientoDto entrada = movimientoService.registrarEntrada(v.getPlaca(), celda.getId(), idOperador());

        // Simulamos que, DESPUÉS de cerrar el movimiento y liberar la celda (ya enviados a MySQL con flush),
        // algo falla antes del COMMIT (p. ej. se cae un servicio posterior). Todo debe deshacerse.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(estado -> {
            movimientoService.registrarSalida(entrada.id(), idOperador());
            throw new IllegalStateException("Falla simulada después de registrar la salida");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(estadoCelda(celda.getId())).isEqualTo("OCUPADA");
        assertThat(movimientosAbiertosDeCelda(celda.getId())).isEqualTo(1);
        String salida = jdbc.queryForObject("SELECT CAST(fecha_hora_salida AS CHAR) FROM movimiento_parqueo WHERE id = ?",
                String.class, entrada.id());
        assertThat(salida).isNull();
    }

    @Test
    @DisplayName("el índice único de MySQL impide un segundo movimiento abierto en la misma celda aunque el estado esté desincronizado")
    void indiceUnicoProtegeLaCelda() throws Exception {
        Vehiculo primero = crearVehiculoVigente(TipoVehiculo.CARRO);
        Vehiculo segundo = crearVehiculoVigente(TipoVehiculo.CARRO);
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        movimientoService.registrarEntrada(primero.getPlaca(), celda.getId(), idOperador());

        // Corrompemos a propósito el estado de la celda para saltarnos la validación de la aplicación.
        jdbc.update("UPDATE celda SET estado = 'DISPONIBLE' WHERE id = ?", celda.getId());

        mvc.perform(post("/api/movements/entries").header(HttpHeaders.AUTHORIZATION, bearerOperador())
                        .contentType(MediaType.APPLICATION_JSON).content(entradaJson(segundo.getPlaca(), celda.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_DATOS"));

        // Rollback completo: no quedó movimiento del segundo vehículo y la celda conserva un solo movimiento abierto.
        assertThat(movimientosAbiertosDeVehiculo(segundo.getId())).isZero();
        assertThat(movimientosAbiertosDeCelda(celda.getId())).isEqualTo(1);
        assertThat(estadoCelda(celda.getId())).isEqualTo("DISPONIBLE"); // el UPDATE a OCUPADA también se revirtió
    }

    @Test
    @DisplayName("dos solicitudes simultáneas por la misma celda → solo una entra, la otra recibe conflicto")
    void dosSolicitudesConcurrentesMismaCelda() throws Exception {
        Celda celda = crearCelda(TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        Vehiculo a = crearVehiculoVigente(TipoVehiculo.CARRO);
        Vehiculo b = crearVehiculoVigente(TipoVehiculo.CARRO);
        Long operador = idOperador();

        int intentos = 2;
        CountDownLatch listos = new CountDownLatch(intentos);
        CountDownLatch disparo = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(intentos);
        try {
            List<Future<Object>> resultados = new ArrayList<>();
            for (Vehiculo v : List.of(a, b)) {
                Callable<Object> tarea = () -> {
                    listos.countDown();
                    disparo.await(); // ambos hilos arrancan exactamente a la vez
                    try {
                        return movimientoService.registrarEntrada(v.getPlaca(), celda.getId(), operador);
                    } catch (RuntimeException e) {
                        return e;
                    }
                };
                resultados.add(hilos.submit(tarea));
            }
            listos.await(10, TimeUnit.SECONDS);
            disparo.countDown();

            int exitos = 0;
            int conflictos = 0;
            for (Future<Object> f : resultados) {
                Object r = f.get(60, TimeUnit.SECONDS);
                if (r instanceof MovimientoDto) {
                    exitos++;
                } else if (r instanceof ConflictoException
                        || r instanceof DataIntegrityViolationException
                        || r instanceof ConcurrencyFailureException) {
                    conflictos++;
                } else {
                    throw new AssertionError("Resultado inesperado: " + r, r instanceof Throwable t ? t : null);
                }
            }

            assertThat(exitos).isEqualTo(1);
            assertThat(conflictos).isEqualTo(1);
            assertThat(movimientosAbiertosDeCelda(celda.getId())).isEqualTo(1);
            assertThat(estadoCelda(celda.getId())).isEqualTo("OCUPADA");
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    @DisplayName("el mismo vehículo en dos celdas a la vez → solo un movimiento abierto")
    void mismoVehiculoConcurrente() throws Exception {
        Vehiculo v = crearVehiculoVigente(TipoVehiculo.MOTO);
        Celda c1 = crearCelda(TipoVehiculo.MOTO, EstadoCelda.DISPONIBLE);
        Celda c2 = crearCelda(TipoVehiculo.MOTO, EstadoCelda.DISPONIBLE);
        Long operador = idOperador();

        CountDownLatch disparo = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> resultados = new ArrayList<>();
            for (Celda c : List.of(c1, c2)) {
                resultados.add(hilos.submit(() -> {
                    disparo.await();
                    try {
                        return movimientoService.registrarEntrada(v.getPlaca(), c.getId(), operador);
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            disparo.countDown();
            int exitos = 0;
            for (Future<Object> f : resultados) {
                if (f.get(60, TimeUnit.SECONDS) instanceof MovimientoDto) {
                    exitos++;
                }
            }
            assertThat(exitos).isEqualTo(1);
            assertThat(movimientosAbiertosDeVehiculo(v.getId())).isEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }
    }
}
