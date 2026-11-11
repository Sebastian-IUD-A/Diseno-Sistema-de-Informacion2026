package co.autoscolombia.parqueadero.domain;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.Rol;
import co.autoscolombia.parqueadero.domain.enums.TipoDocumento;
import co.autoscolombia.parqueadero.domain.enums.TipoNovedad;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.exception.NegocioException;
import co.autoscolombia.parqueadero.domain.exception.ReglaNegocioException;
import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Cliente;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.domain.model.Novedad;
import co.autoscolombia.parqueadero.domain.model.Usuario;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de las reglas que viven dentro de las entidades del dominio (sin Spring ni base de datos). */
class ReglasDominioTest {

    private final Cliente cliente = new Cliente(TipoDocumento.CC, "1", "Ana", "Pérez", null, null);
    private final Usuario operador = new Usuario("Op", "op@test.co", "$2a$10$x", Rol.OPERADOR);

    @Test
    @DisplayName("RN-01: la placa se normaliza a mayúsculas y sin espacios externos")
    void placaNormalizada() {
        assertThat(Placa.normalizar("  abc123 ")).isEqualTo("ABC123");
        assertThat(new Vehiculo(" xyz98d ", TipoVehiculo.MOTO, null, null, null, LocalDate.now(), cliente).getPlaca())
                .isEqualTo("XYZ98D");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ABCDEFGHIJK"})
    @DisplayName("RN-01: placa vacía o demasiado larga → 400")
    void placaInvalida(String placa) {
        assertThatThrownBy(() -> Placa.normalizar(placa)).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("RN-09: una celda ocupada no puede ponerse fuera de servicio")
    void celdaOcupadaNoPuedeFueraDeServicio() {
        Celda celda = new Celda("A-01", "Zona A", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        celda.ocupar(TipoVehiculo.CARRO);

        assertThatThrownBy(() -> celda.cambiarEstadoAdministrativo(EstadoCelda.FUERA_SERVICIO))
                .isInstanceOf(ConflictoException.class)
                .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("CELDA_OCUPADA_NO_MODIFICABLE");
        assertThat(celda.getEstado()).isEqualTo(EstadoCelda.OCUPADA);
    }

    @Test
    @DisplayName("una celda disponible sí puede ponerse fuera de servicio y volver")
    void celdaDisponibleFueraDeServicio() {
        Celda celda = new Celda("A-01", "Zona A", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        celda.cambiarEstadoAdministrativo(EstadoCelda.FUERA_SERVICIO);
        assertThat(celda.getEstado()).isEqualTo(EstadoCelda.FUERA_SERVICIO);
        celda.cambiarEstadoAdministrativo(EstadoCelda.DISPONIBLE);
        assertThat(celda.getEstado()).isEqualTo(EstadoCelda.DISPONIBLE);
    }

    @Test
    @DisplayName("el administrador no puede marcar OCUPADA a mano ni crear una celda OCUPADA")
    void ocupadaSoloLaAsignaElSistema() {
        Celda celda = new Celda("A-01", "Zona A", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        assertThatThrownBy(() -> celda.cambiarEstadoAdministrativo(EstadoCelda.OCUPADA))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> new Celda("A-02", "Zona A", TipoVehiculo.CARRO, EstadoCelda.OCUPADA))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("RN-04: no se puede ocupar dos veces la misma celda")
    void celdaNoSeOcupaDosVeces() {
        Celda celda = new Celda("A-01", "Zona A", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        celda.ocupar(TipoVehiculo.CARRO);
        assertThatThrownBy(() -> celda.ocupar(TipoVehiculo.CARRO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("RN-03: vigencia de la mensualidad (el último día cuenta como vigente)")
    void vigenciaAutorizacion() {
        LocalDate hoy = LocalDate.of(2026, 11, 8);
        Vehiculo venceHoy = new Vehiculo("AAA111", TipoVehiculo.CARRO, null, null, null, hoy, cliente);
        Vehiculo vencioAyer = new Vehiculo("BBB222", TipoVehiculo.CARRO, null, null, null, hoy.minusDays(1), cliente);

        assertThat(venceHoy.motivoRechazoIngreso(hoy)).isNull();
        assertThat(vencioAyer.motivoRechazoIngreso(hoy).getCodigo()).isEqualTo("AUTORIZACION_VENCIDA");
    }

    @Test
    @DisplayName("RN-07: una novedad sin descripción no se puede crear")
    void novedadSinDescripcion() {
        Celda celda = new Celda("A-01", "Zona A", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        Vehiculo v = new Vehiculo("AAA111", TipoVehiculo.CARRO, null, null, null, LocalDate.now(), cliente);
        MovimientoParqueo m = new MovimientoParqueo(v, celda, LocalDateTime.now(), operador);

        assertThatThrownBy(() -> new Novedad(m, TipoNovedad.OBSERVACION, "   ", LocalDateTime.now(), operador))
                .isInstanceOf(SolicitudInvalidaException.class)
                .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("DESCRIPCION_REQUERIDA");
    }

    @Test
    @DisplayName("RN-05/06: cerrar un movimiento guarda hora y usuario de salida, y no se puede cerrar dos veces")
    void cerrarMovimiento() {
        Celda celda = new Celda("A-01", "Zona A", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
        Vehiculo v = new Vehiculo("AAA111", TipoVehiculo.CARRO, null, null, null, LocalDate.now(), cliente);
        LocalDateTime entrada = LocalDateTime.of(2026, 11, 8, 8, 0);
        MovimientoParqueo m = new MovimientoParqueo(v, celda, entrada, operador);

        m.cerrar(entrada.plusMinutes(95), operador);

        assertThat(m.estaAbierto()).isFalse();
        assertThat(m.getUsuarioSalida()).isSameAs(operador);
        assertThat(m.minutosPermanencia(entrada.plusDays(1))).isEqualTo(95);
        assertThatThrownBy(() -> m.cerrar(entrada.plusHours(3), operador)).isInstanceOf(ConflictoException.class);
    }
}
