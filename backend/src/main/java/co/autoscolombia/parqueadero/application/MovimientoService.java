package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.MovimientoDto;
import co.autoscolombia.parqueadero.domain.Placa;
import co.autoscolombia.parqueadero.domain.exception.AccesoDenegadoException;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.exception.RecursoNoEncontradoException;
import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;
import co.autoscolombia.parqueadero.domain.model.Celda;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.domain.model.Usuario;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import co.autoscolombia.parqueadero.infrastructure.persistence.CeldaRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.MovimientoRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.NovedadRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.UsuarioRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.VehiculoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso de entrada, salida, ocupación e historial.
 * <p>
 * {@code @Transactional}: todo lo que ocurre dentro del método se confirma junto (COMMIT)
 * o se deshace junto (ROLLBACK) si se lanza una excepción. Así se cumple RN-05: cerrar el
 * movimiento y liberar la celda nunca quedan "a medias".
 */
@Service
public class MovimientoService {

    /** Tope de filas del historial para no saturar el navegador. */
    public static final int MAXIMO_HISTORIAL = 500;

    private final MovimientoRepository movimientoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final CeldaRepository celdaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NovedadRepository novedadRepository;
    private final Clock clock;

    public MovimientoService(MovimientoRepository movimientoRepository, VehiculoRepository vehiculoRepository,
                             CeldaRepository celdaRepository, UsuarioRepository usuarioRepository,
                             NovedadRepository novedadRepository, Clock clock) {
        this.movimientoRepository = movimientoRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.celdaRepository = celdaRepository;
        this.usuarioRepository = usuarioRepository;
        this.novedadRepository = novedadRepository;
        this.clock = clock;
    }

    /**
     * Registrar entrada. Orden de validaciones:
     * 1) el vehículo existe (404)  2) está activo y con mensualidad vigente (422, RN-03)
     * 3) no está ya dentro (409, RN-02)  4) la celda existe, está disponible y es compatible (404/409/422, RN-04)
     * Los pasos 1 y 4 bloquean las filas (SELECT ... FOR UPDATE) para que dos solicitudes simultáneas
     * se atiendan una detrás de otra (RN-10). Si aun así algo se colara, los índices únicos de MySQL
     * rechazan el INSERT y la transacción se revierte.
     */
    @Transactional
    public MovimientoDto registrarEntrada(String placaIngresada, Long celdaId, Long usuarioId) {
        String placa = Placa.normalizar(placaIngresada);
        if (celdaId == null) {
            throw new SolicitudInvalidaException("CELDA_REQUERIDA", "Debe seleccionar una celda.");
        }
        Usuario operador = obtenerUsuarioActivo(usuarioId);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Vehiculo vehiculo = vehiculoRepository.findByPlacaForUpdate(placa)
                .orElseThrow(() -> new RecursoNoEncontradoException("VEHICULO_NO_ENCONTRADO",
                        "No existe un vehículo registrado con la placa " + placa + "."));

        vehiculo.validarPuedeIngresar(ahora.toLocalDate());

        movimientoRepository.findByVehiculoIdAndFechaHoraSalidaIsNull(vehiculo.getId()).ifPresent(abierto -> {
            throw new ConflictoException("VEHICULO_YA_DENTRO",
                    "El vehículo " + placa + " ya se encuentra dentro del parqueadero (celda "
                            + abierto.getCelda().getCodigo() + ").");
        });

        Celda celda = celdaRepository.findByIdForUpdate(celdaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("CELDA_NO_ENCONTRADA",
                        "La celda seleccionada no existe."));

        celda.ocupar(vehiculo.getTipo());

        MovimientoParqueo movimiento = new MovimientoParqueo(vehiculo, celda, ahora, operador);
        // saveAndFlush envía el INSERT ya mismo: si un índice único lo rechaza, el error ocurre
        // aquí (dentro de la transacción) y todo se revierte.
        movimiento = movimientoRepository.saveAndFlush(movimiento);
        return Mapeador.movimiento(movimiento, ahora, 0);
    }

    /**
     * Registrar salida (RN-05): cierra el movimiento y libera la celda en la MISMA transacción.
     * Si cualquiera de los dos pasos falla, ninguno queda guardado.
     */
    @Transactional
    public MovimientoDto registrarSalida(Long movimientoId, Long usuarioId) {
        Usuario operador = obtenerUsuarioActivo(usuarioId);
        LocalDateTime ahora = LocalDateTime.now(clock);

        MovimientoParqueo movimiento = movimientoRepository.findByIdForUpdate(movimientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("MOVIMIENTO_NO_ENCONTRADO",
                        "No existe el movimiento " + movimientoId + "."));
        if (!movimiento.estaAbierto()) {
            throw new ConflictoException("MOVIMIENTO_YA_CERRADO",
                    "La salida de este vehículo ya fue registrada.");
        }

        Celda celda = celdaRepository.findByIdForUpdate(movimiento.getCelda().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("CELDA_NO_ENCONTRADA",
                        "La celda del movimiento no existe."));

        movimiento.cerrar(ahora, operador);   // paso 1: cerrar el movimiento
        celda.liberar();                      // paso 2: liberar la celda
        movimientoRepository.flush();         // ambos UPDATE se envían juntos antes del COMMIT

        return Mapeador.movimiento(movimiento, ahora, contarNovedades(List.of(movimiento.getId()))
                .getOrDefault(movimiento.getId(), 0L));
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> listarActivos(String filtroPlaca) {
        String placa = filtroPlaca == null || filtroPlaca.isBlank() ? null : filtroPlaca.strip().toUpperCase();
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<MovimientoParqueo> abiertos = movimientoRepository.buscarAbiertos(placa);
        Map<Long, Long> novedades = contarNovedades(abiertos.stream().map(MovimientoParqueo::getId).toList());
        return abiertos.stream()
                .map(m -> Mapeador.movimiento(m, ahora, novedades.getOrDefault(m.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public MovimientoDto obtener(Long movimientoId) {
        MovimientoParqueo m = movimientoRepository.buscarDetalle(movimientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("MOVIMIENTO_NO_ENCONTRADO",
                        "No existe el movimiento " + movimientoId + "."));
        return Mapeador.movimiento(m, LocalDateTime.now(clock),
                contarNovedades(List.of(m.getId())).getOrDefault(m.getId(), 0L));
    }

    /**
     * Historial por placa (exacta) y rango de fechas de entrada, ambos opcionales.
     * "hasta" es inclusivo: hasta=2026-11-08 incluye todo ese día.
     */
    @Transactional(readOnly = true)
    public List<MovimientoDto> historial(String placaFiltro, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("RANGO_FECHAS_INVALIDO",
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }
        String placa = Placa.normalizarOpcional(placaFiltro);
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<MovimientoParqueo> movimientos = movimientoRepository.buscarHistorial(
                placa, inicio, finExclusivo, PageRequest.of(0, MAXIMO_HISTORIAL));
        Map<Long, Long> novedades = contarNovedades(movimientos.stream().map(MovimientoParqueo::getId).toList());
        return movimientos.stream()
                .map(m -> Mapeador.movimiento(m, ahora, novedades.getOrDefault(m.getId(), 0L)))
                .toList();
    }

    private Usuario obtenerUsuarioActivo(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException("USUARIO_INACTIVO",
                        "Su usuario no está activo. Contacte al administrador."));
    }

    private Map<Long, Long> contarNovedades(List<Long> ids) {
        Map<Long, Long> resultado = new HashMap<>();
        if (ids.isEmpty()) {
            return resultado;
        }
        for (Object[] fila : novedadRepository.contarPorMovimientos(ids)) {
            resultado.put((Long) fila[0], ((Number) fila[1]).longValue());
        }
        return resultado;
    }
}
