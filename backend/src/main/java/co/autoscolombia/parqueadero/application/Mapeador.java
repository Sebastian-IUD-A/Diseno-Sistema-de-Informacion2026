package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.CeldaDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.ClienteDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.MovimientoDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.NovedadDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.UsuarioDto;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.Cliente;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.domain.model.Novedad;
import co.autoscolombia.parqueadero.domain.model.Usuario;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convierte entidades del dominio en DTOs de respuesta.
 * Se llama SIEMPRE dentro de una transacción (desde los servicios) para que las relaciones LAZY se puedan leer.
 */
public final class Mapeador {

    private Mapeador() {
    }

    public static UsuarioDto usuario(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol());
    }

    public static ClienteDto cliente(Cliente c) {
        if (c == null) {
            return null;
        }
        return new ClienteDto(c.getId(), c.getTipoDocumento(), c.getNumeroDocumento(), c.getNombreCompleto(),
                c.getTelefono(), c.getCorreo(), c.getEstado());
    }

    public static CeldaDto celda(Celda c, MovimientoParqueo movimientoAbierto) {
        return new CeldaDto(c.getId(), c.getCodigo(), c.getZona(), c.getTipoVehiculo(), c.getEstado(),
                movimientoAbierto == null ? null : movimientoAbierto.getVehiculo().getPlaca(),
                movimientoAbierto == null ? null : movimientoAbierto.getId());
    }

    public static MovimientoDto movimiento(MovimientoParqueo m, LocalDateTime ahora, long cantidadNovedades) {
        Vehiculo v = m.getVehiculo();
        Celda c = m.getCelda();
        return new MovimientoDto(
                m.getId(),
                v.getPlaca(),
                v.getTipo(),
                descripcionVehiculo(v),
                v.getCliente() == null ? null : v.getCliente().getNombreCompleto(),
                c.getId(),
                c.getCodigo(),
                c.getZona(),
                m.getFechaHoraEntrada(),
                m.getFechaHoraSalida(),
                m.getUsuarioEntrada().getNombre(),
                m.getUsuarioSalida() == null ? null : m.getUsuarioSalida().getNombre(),
                m.estaAbierto(),
                m.minutosPermanencia(ahora),
                cantidadNovedades);
    }

    public static NovedadDto novedad(Novedad n) {
        return new NovedadDto(n.getId(), n.getMovimiento().getId(), n.getTipo(), n.getDescripcion(),
                n.getFechaHora(), n.getUsuario().getNombre());
    }

    public static String descripcionVehiculo(Vehiculo v) {
        return Stream.of(v.getMarca(), v.getModelo(), v.getColor())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" · "));
    }
}
