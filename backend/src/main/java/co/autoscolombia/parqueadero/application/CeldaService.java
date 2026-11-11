package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.CeldaDto;
import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.exception.RecursoNoEncontradoException;
import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.infrastructure.persistence.CeldaRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gestión de celdas: listado, disponibles por tipo y administración (crear / editar). */
@Service
public class CeldaService {

    private final CeldaRepository celdaRepository;
    private final MovimientoRepository movimientoRepository;

    public CeldaService(CeldaRepository celdaRepository, MovimientoRepository movimientoRepository) {
        this.celdaRepository = celdaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    /** Todas las celdas, indicando qué placa ocupa cada una. */
    @Transactional(readOnly = true)
    public List<CeldaDto> listar() {
        Map<Long, MovimientoParqueo> abiertosPorCelda = movimientoRepository.buscarAbiertos(null).stream()
                .collect(Collectors.toMap(m -> m.getCelda().getId(), Function.identity(), (a, b) -> a));
        return celdaRepository.findAllByOrderByCodigoAsc().stream()
                .map(c -> Mapeador.celda(c, abiertosPorCelda.get(c.getId())))
                .toList();
    }

    /** Celdas DISPONIBLES compatibles con el tipo de vehículo (para el formulario de entrada). */
    @Transactional(readOnly = true)
    public List<CeldaDto> disponibles(TipoVehiculo tipo) {
        if (tipo == null) {
            throw new SolicitudInvalidaException("TIPO_REQUERIDO", "Indique el tipo de vehículo (CARRO o MOTO).");
        }
        return celdaRepository.findByEstadoAndTipoVehiculoOrderByCodigoAsc(EstadoCelda.DISPONIBLE, tipo).stream()
                .map(c -> Mapeador.celda(c, null))
                .toList();
    }

    @Transactional
    public CeldaDto crear(String codigo, String zona, TipoVehiculo tipo, EstadoCelda estado) {
        String codigoNormalizado = normalizarCodigo(codigo);
        if (celdaRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new ConflictoException("CODIGO_CELDA_DUPLICADO", "Ya existe una celda con el código " + codigoNormalizado + ".");
        }
        Celda celda = new Celda(codigoNormalizado, zona.strip(), tipo, estado);
        return Mapeador.celda(celdaRepository.saveAndFlush(celda), null);
    }

    /**
     * Edita una celda. Se bloquea la fila (FOR UPDATE) para que no se pueda poner fuera de servicio
     * justo en el instante en que un operador la está ocupando (RN-09 + RN-10).
     */
    @Transactional
    public CeldaDto actualizar(Long id, String codigo, String zona, TipoVehiculo tipo, EstadoCelda estado) {
        Celda celda = celdaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CELDA_NO_ENCONTRADA", "La celda " + id + " no existe."));
        String codigoNormalizado = normalizarCodigo(codigo);
        if (celdaRepository.existsByCodigoIgnoreCaseAndIdNot(codigoNormalizado, id)) {
            throw new ConflictoException("CODIGO_CELDA_DUPLICADO", "Ya existe otra celda con el código " + codigoNormalizado + ".");
        }
        celda.cambiarEstadoAdministrativo(estado);
        celda.actualizarDatos(codigoNormalizado, zona.strip(), tipo);
        celdaRepository.flush();
        MovimientoParqueo abierto = movimientoRepository.buscarAbiertos(null).stream()
                .filter(m -> m.getCelda().getId().equals(celda.getId()))
                .findFirst().orElse(null);
        return Mapeador.celda(celda, abierto);
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new SolicitudInvalidaException("CODIGO_REQUERIDO", "El código de la celda es obligatorio.");
        }
        return codigo.strip().toUpperCase(Locale.ROOT);
    }
}
