package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.DashboardDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.ResumenTipoDto;
import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.infrastructure.persistence.CeldaRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Resumen para la pantalla de inicio. */
@Service
public class DashboardService {

    private final CeldaRepository celdaRepository;
    private final MovimientoRepository movimientoRepository;
    private final Clock clock;

    public DashboardService(CeldaRepository celdaRepository, MovimientoRepository movimientoRepository, Clock clock) {
        this.celdaRepository = celdaRepository;
        this.movimientoRepository = movimientoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardDto resumen() {
        long disponibles = celdaRepository.countByEstado(EstadoCelda.DISPONIBLE);
        long ocupadas = celdaRepository.countByEstado(EstadoCelda.OCUPADA);
        long fuera = celdaRepository.countByEstado(EstadoCelda.FUERA_SERVICIO);

        List<ResumenTipoDto> porTipo = Arrays.stream(TipoVehiculo.values()).map(tipo -> {
            long d = celdaRepository.countByEstadoAndTipoVehiculo(EstadoCelda.DISPONIBLE, tipo);
            long o = celdaRepository.countByEstadoAndTipoVehiculo(EstadoCelda.OCUPADA, tipo);
            long f = celdaRepository.countByEstadoAndTipoVehiculo(EstadoCelda.FUERA_SERVICIO, tipo);
            return new ResumenTipoDto(tipo, d, o, f, d + o + f);
        }).toList();

        return new DashboardDto(
                movimientoRepository.countByFechaHoraSalidaIsNull(),
                disponibles, ocupadas, fuera, disponibles + ocupadas + fuera,
                porTipo,
                LocalDateTime.now(clock));
    }
}
