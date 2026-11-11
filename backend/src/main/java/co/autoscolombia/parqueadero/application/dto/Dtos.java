package co.autoscolombia.parqueadero.application.dto;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.EstadoRegistro;
import co.autoscolombia.parqueadero.domain.enums.Rol;
import co.autoscolombia.parqueadero.domain.enums.TipoDocumento;
import co.autoscolombia.parqueadero.domain.enums.TipoNovedad;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objetos de RESPUESTA de los casos de uso (DTO = Data Transfer Object).
 * <p>
 * ¿Por qué no devolver las entidades JPA directamente?
 * <ul>
 *   <li>Las entidades tienen relaciones LAZY que fallarían al serializarse fuera de la transacción.</li>
 *   <li>No queremos exponer datos internos (p. ej. el hash de la contraseña).</li>
 *   <li>El contrato de la API puede evolucionar sin cambiar la base de datos y viceversa.</li>
 * </ul>
 * Son {@code record}: clases inmutables que Java genera con constructor, getters, equals y toString.
 */
public final class Dtos {

    private Dtos() {
    }

    public record UsuarioDto(Long id, String nombre, String correo, Rol rol) {
    }

    public record SesionDto(String token, String tipo, Instant expiraEn, UsuarioDto usuario) {
    }

    public record ClienteDto(Long id, TipoDocumento tipoDocumento, String numeroDocumento, String nombreCompleto,
                             String telefono, String correo, EstadoRegistro estado) {
    }

    public record MotivoDto(String codigo, String mensaje) {
    }

    public record VehiculoDto(Long id, String placa, TipoVehiculo tipo, String marca, String modelo, String color,
                              EstadoRegistro estado, LocalDate autorizadoHasta, boolean autorizacionVigente,
                              long diasParaVencer, ClienteDto cliente, boolean dentro, Long movimientoAbiertoId,
                              String celdaActual, LocalDateTime fechaHoraEntradaActual,
                              boolean puedeIngresar, MotivoDto motivoNoIngreso) {
    }

    public record CeldaDto(Long id, String codigo, String zona, TipoVehiculo tipoVehiculo, EstadoCelda estado,
                           String placaActual, Long movimientoId) {
    }

    public record MovimientoDto(Long id, String placa, TipoVehiculo tipoVehiculo, String descripcionVehiculo,
                                String clienteNombre, Long celdaId, String celdaCodigo, String zona,
                                LocalDateTime fechaHoraEntrada, LocalDateTime fechaHoraSalida,
                                String usuarioEntrada, String usuarioSalida, boolean abierto,
                                long minutosPermanencia, long cantidadNovedades) {
    }

    public record NovedadDto(Long id, Long movimientoId, TipoNovedad tipo, String descripcion,
                             LocalDateTime fechaHora, String usuario) {
    }

    public record ResumenTipoDto(TipoVehiculo tipo, long disponibles, long ocupadas, long fueraServicio, long total) {
    }

    public record DashboardDto(long vehiculosDentro, long celdasDisponibles, long celdasOcupadas,
                               long celdasFueraServicio, long totalCeldas, List<ResumenTipoDto> porTipo,
                               LocalDateTime generadoEn) {
    }
}
