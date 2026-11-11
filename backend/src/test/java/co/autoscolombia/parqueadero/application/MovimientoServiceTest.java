package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.MovimientoDto;
import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.EstadoRegistro;
import co.autoscolombia.parqueadero.domain.enums.Rol;
import co.autoscolombia.parqueadero.domain.enums.TipoDocumento;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.exception.AccesoDenegadoException;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.exception.NegocioException;
import co.autoscolombia.parqueadero.domain.exception.RecursoNoEncontradoException;
import co.autoscolombia.parqueadero.domain.exception.ReglaNegocioException;
import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Cliente;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.domain.model.Usuario;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import co.autoscolombia.parqueadero.infrastructure.persistence.CeldaRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.MovimientoRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.NovedadRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.UsuarioRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas UNITARIAS del caso de uso de entradas y salidas.
 * <p>
 * "Unitaria" = se prueba solo MovimientoService. Los repositorios se reemplazan por
 * dobles (mocks de Mockito) que devuelven lo que cada prueba necesita; no hay base de datos.
 * Son rápidas y verifican las reglas de negocio de forma aislada.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    /** Reloj fijo: 8 de noviembre de 2026, 10:00 a. m. en Bogotá. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate HOY = LocalDate.of(2026, 11, 8);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 8, 10, 0);

    @Mock MovimientoRepository movimientoRepository;
    @Mock VehiculoRepository vehiculoRepository;
    @Mock CeldaRepository celdaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock NovedadRepository novedadRepository;

    private MovimientoService servicio;
    private Usuario operador;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        servicio = new MovimientoService(movimientoRepository, vehiculoRepository, celdaRepository,
                usuarioRepository, novedadRepository, RELOJ);
        operador = conId(new Usuario("Operador Prueba", "op@test.co", "$2a$10$hash", Rol.OPERADOR), 2L);
        cliente = conId(new Cliente(TipoDocumento.CC, "123", "Ana", "Pérez", null, null), 1L);
        lenient().when(usuarioRepository.findById(2L)).thenReturn(Optional.of(operador));
    }

    private static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    private Vehiculo vehiculo(String placa, TipoVehiculo tipo, LocalDate autorizadoHasta) {
        return conId(new Vehiculo(placa, tipo, "Mazda", "3", "Gris", autorizadoHasta, cliente), 10L);
    }

    private Celda celda(String codigo, TipoVehiculo tipo, EstadoCelda estado) {
        Celda c = conId(new Celda(codigo, "Zona A", tipo, EstadoCelda.DISPONIBLE), 5L);
        ReflectionTestUtils.setField(c, "estado", estado);
        return c;
    }

    @Nested
    @DisplayName("Registrar entrada")
    class Entrada {

        @Test
        @DisplayName("placa inexistente → 404 VEHICULO_NO_ENCONTRADO")
        void placaInexistente() {
            when(vehiculoRepository.findByPlacaForUpdate("ZZZ999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrarEntrada("zzz999", 5L, 2L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("VEHICULO_NO_ENCONTRADO");
            verify(movimientoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("autorización vencida → 422 AUTORIZACION_VENCIDA y no se guarda nada")
        void autorizacionVencida() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY.minusDays(1));
            when(vehiculoRepository.findByPlacaForUpdate("ABC123")).thenReturn(Optional.of(v));

            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("AUTORIZACION_VENCIDA");
            verify(movimientoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("el día del vencimiento la autorización todavía es válida")
        void autorizacionVenceHoyEsValida() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY);
            Celda c = celda("A-01", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
            when(vehiculoRepository.findByPlacaForUpdate("ABC123")).thenReturn(Optional.of(v));
            when(movimientoRepository.findByVehiculoIdAndFechaHoraSalidaIsNull(10L)).thenReturn(Optional.empty());
            when(celdaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(c));
            when(movimientoRepository.saveAndFlush(any())).thenAnswer(inv -> conId(inv.getArgument(0), 99L));

            MovimientoDto resultado = servicio.registrarEntrada("ABC123", 5L, 2L);

            assertThat(resultado.abierto()).isTrue();
        }

        @Test
        @DisplayName("vehículo inactivo → 422 VEHICULO_INACTIVO")
        void vehiculoInactivo() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY.plusMonths(1));
            v.setEstado(EstadoRegistro.INACTIVO);
            when(vehiculoRepository.findByPlacaForUpdate("ABC123")).thenReturn(Optional.of(v));

            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("VEHICULO_INACTIVO");
        }

        @Test
        @DisplayName("vehículo que ya está dentro → 409 VEHICULO_YA_DENTRO")
        void vehiculoYaDentro() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY.plusMonths(1));
            Celda otra = celda("A-09", TipoVehiculo.CARRO, EstadoCelda.OCUPADA);
            MovimientoParqueo abierto = conId(new MovimientoParqueo(v, otra, AHORA.minusHours(3), operador), 50L);
            when(vehiculoRepository.findByPlacaForUpdate("ABC123")).thenReturn(Optional.of(v));
            when(movimientoRepository.findByVehiculoIdAndFechaHoraSalidaIsNull(10L)).thenReturn(Optional.of(abierto));

            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .isInstanceOf(ConflictoException.class)
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("VEHICULO_YA_DENTRO");
            verify(movimientoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("celda ocupada → 409 CELDA_OCUPADA")
        void celdaOcupada() {
            prepararVehiculoValido();
            when(celdaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(celda("A-01", TipoVehiculo.CARRO, EstadoCelda.OCUPADA)));

            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .isInstanceOf(ConflictoException.class)
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("CELDA_OCUPADA");
            verify(movimientoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("celda fuera de servicio → 422 CELDA_FUERA_SERVICIO")
        void celdaFueraDeServicio() {
            prepararVehiculoValido();
            when(celdaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(celda("A-01", TipoVehiculo.CARRO, EstadoCelda.FUERA_SERVICIO)));

            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("CELDA_FUERA_SERVICIO");
        }

        @Test
        @DisplayName("celda para motos con un carro → 422 CELDA_INCOMPATIBLE")
        void celdaIncompatible() {
            prepararVehiculoValido();
            when(celdaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(celda("M-01", TipoVehiculo.MOTO, EstadoCelda.DISPONIBLE)));

            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("CELDA_INCOMPATIBLE");
        }

        @Test
        @DisplayName("entrada correcta → movimiento abierto con hora y operador, celda OCUPADA")
        void entradaCorrecta() {
            prepararVehiculoValido();
            Celda c = celda("A-01", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
            when(celdaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(c));
            when(movimientoRepository.saveAndFlush(any())).thenAnswer(inv -> conId(inv.getArgument(0), 99L));

            MovimientoDto resultado = servicio.registrarEntrada("  abc123 ", 5L, 2L);

            ArgumentCaptor<MovimientoParqueo> guardado = ArgumentCaptor.forClass(MovimientoParqueo.class);
            verify(movimientoRepository).saveAndFlush(guardado.capture());
            assertThat(guardado.getValue().getFechaHoraEntrada()).isEqualTo(AHORA);
            assertThat(guardado.getValue().getUsuarioEntrada()).isSameAs(operador);
            assertThat(guardado.getValue().estaAbierto()).isTrue();
            assertThat(c.getEstado()).isEqualTo(EstadoCelda.OCUPADA);
            assertThat(resultado.placa()).isEqualTo("ABC123");
            assertThat(resultado.celdaCodigo()).isEqualTo("A-01");
            assertThat(resultado.usuarioEntrada()).isEqualTo("Operador Prueba");
        }

        @Test
        @DisplayName("usuario desactivado → 403 aunque tenga token")
        void usuarioInactivo() {
            operador.setActivo(false);
            assertThatThrownBy(() -> servicio.registrarEntrada("ABC123", 5L, 2L))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        private void prepararVehiculoValido() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY.plusMonths(1));
            when(vehiculoRepository.findByPlacaForUpdate("ABC123")).thenReturn(Optional.of(v));
            when(movimientoRepository.findByVehiculoIdAndFechaHoraSalidaIsNull(10L)).thenReturn(Optional.empty());
        }
    }

    @Nested
    @DisplayName("Registrar salida")
    class Salida {

        @Test
        @DisplayName("salida correcta → cierra el movimiento y libera la celda")
        void salidaCorrecta() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY.plusMonths(1));
            Celda c = celda("A-01", TipoVehiculo.CARRO, EstadoCelda.OCUPADA);
            MovimientoParqueo m = conId(new MovimientoParqueo(v, c, AHORA.minusHours(2), operador), 50L);
            when(movimientoRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(m));
            when(celdaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(c));
            when(novedadRepository.contarPorMovimientos(List.of(50L))).thenReturn(List.of());

            MovimientoDto resultado = servicio.registrarSalida(50L, 2L);

            assertThat(m.estaAbierto()).isFalse();
            assertThat(m.getFechaHoraSalida()).isEqualTo(AHORA);
            assertThat(m.getUsuarioSalida()).isSameAs(operador);
            assertThat(c.getEstado()).isEqualTo(EstadoCelda.DISPONIBLE);
            assertThat(resultado.minutosPermanencia()).isEqualTo(120);
            verify(movimientoRepository).flush();
        }

        @Test
        @DisplayName("salida de un movimiento ya cerrado → 409")
        void salidaDuplicada() {
            Vehiculo v = vehiculo("ABC123", TipoVehiculo.CARRO, HOY.plusMonths(1));
            Celda c = celda("A-01", TipoVehiculo.CARRO, EstadoCelda.DISPONIBLE);
            MovimientoParqueo m = conId(new MovimientoParqueo(v, c, AHORA.minusHours(2), operador), 50L);
            m.cerrar(AHORA.minusHours(1), operador);
            when(movimientoRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(m));

            assertThatThrownBy(() -> servicio.registrarSalida(50L, 2L))
                    .isInstanceOf(ConflictoException.class)
                    .extracting(e -> ((NegocioException) e).getCodigo()).isEqualTo("MOVIMIENTO_YA_CERRADO");
        }

        @Test
        @DisplayName("movimiento inexistente → 404")
        void movimientoInexistente() {
            when(movimientoRepository.findByIdForUpdate(77L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.registrarSalida(77L, 2L)).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Test
    @DisplayName("historial con fecha inicial posterior a la final → 400")
    void historialRangoInvertido() {
        assertThatThrownBy(() -> servicio.historial(null, HOY, HOY.minusDays(1)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}
