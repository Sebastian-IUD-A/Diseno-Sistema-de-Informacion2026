package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.NovedadDto;
import co.autoscolombia.parqueadero.domain.enums.TipoNovedad;
import co.autoscolombia.parqueadero.domain.exception.AccesoDenegadoException;
import co.autoscolombia.parqueadero.domain.exception.RecursoNoEncontradoException;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.domain.model.Novedad;
import co.autoscolombia.parqueadero.domain.model.Usuario;
import co.autoscolombia.parqueadero.infrastructure.persistence.MovimientoRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.NovedadRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RN-07: registrar y consultar novedades de un movimiento.
 * Se permiten novedades tanto en movimientos abiertos como cerrados (p. ej. un reclamo
 * que el cliente hace después de salir); siempre quedan ligadas a una permanencia concreta.
 */
@Service
public class NovedadService {

    private final NovedadRepository novedadRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public NovedadService(NovedadRepository novedadRepository, MovimientoRepository movimientoRepository,
                          UsuarioRepository usuarioRepository, Clock clock) {
        this.novedadRepository = novedadRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional
    public NovedadDto registrar(Long movimientoId, TipoNovedad tipo, String descripcion, Long usuarioId) {
        Usuario autor = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException("USUARIO_INACTIVO",
                        "Su usuario no está activo. Contacte al administrador."));
        MovimientoParqueo movimiento = buscarMovimiento(movimientoId);
        Novedad novedad = new Novedad(movimiento, tipo, descripcion, LocalDateTime.now(clock), autor);
        return Mapeador.novedad(novedadRepository.saveAndFlush(novedad));
    }

    @Transactional(readOnly = true)
    public List<NovedadDto> listar(Long movimientoId) {
        buscarMovimiento(movimientoId);
        return novedadRepository.buscarPorMovimiento(movimientoId).stream()
                .map(Mapeador::novedad)
                .toList();
    }

    private MovimientoParqueo buscarMovimiento(Long movimientoId) {
        return movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("MOVIMIENTO_NO_ENCONTRADO",
                        "No existe el movimiento " + movimientoId + "."));
    }
}
