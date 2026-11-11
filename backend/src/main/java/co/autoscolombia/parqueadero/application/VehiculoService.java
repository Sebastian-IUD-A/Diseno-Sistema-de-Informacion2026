package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.MotivoDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.VehiculoDto;
import co.autoscolombia.parqueadero.domain.Placa;
import co.autoscolombia.parqueadero.domain.exception.RecursoNoEncontradoException;
import co.autoscolombia.parqueadero.domain.exception.ReglaNegocioException;
import co.autoscolombia.parqueadero.domain.model.MovimientoParqueo;
import co.autoscolombia.parqueadero.domain.model.Vehiculo;
import co.autoscolombia.parqueadero.infrastructure.persistence.MovimientoRepository;
import co.autoscolombia.parqueadero.infrastructure.persistence.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Consulta de vehículos por placa: muestra cliente, estado, vigencia y si puede ingresar. */
@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final MovimientoRepository movimientoRepository;
    private final Clock clock;

    public VehiculoService(VehiculoRepository vehiculoRepository, MovimientoRepository movimientoRepository, Clock clock) {
        this.vehiculoRepository = vehiculoRepository;
        this.movimientoRepository = movimientoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public VehiculoDto consultarPorPlaca(String placaIngresada) {
        String placa = Placa.normalizar(placaIngresada);
        Vehiculo v = vehiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RecursoNoEncontradoException("VEHICULO_NO_ENCONTRADO",
                        "No existe un vehículo registrado con la placa " + placa + "."));

        LocalDate hoy = LocalDate.now(clock);
        MovimientoParqueo abierto = movimientoRepository.findByVehiculoIdAndFechaHoraSalidaIsNull(v.getId()).orElse(null);

        MotivoDto motivo = null;
        ReglaNegocioException rechazo = v.motivoRechazoIngreso(hoy);
        if (rechazo != null) {
            motivo = new MotivoDto(rechazo.getCodigo(), rechazo.getMessage());
        } else if (abierto != null) {
            motivo = new MotivoDto("VEHICULO_YA_DENTRO",
                    "El vehículo ya se encuentra dentro (celda " + abierto.getCelda().getCodigo() + ").");
        }

        return new VehiculoDto(
                v.getId(), v.getPlaca(), v.getTipo(), v.getMarca(), v.getModelo(), v.getColor(), v.getEstado(),
                v.getAutorizadoHasta(),
                v.autorizacionVigenteEn(hoy),
                ChronoUnit.DAYS.between(hoy, v.getAutorizadoHasta()),
                Mapeador.cliente(v.getCliente()),
                abierto != null,
                abierto == null ? null : abierto.getId(),
                abierto == null ? null : abierto.getCelda().getCodigo(),
                abierto == null ? null : abierto.getFechaHoraEntrada(),
                motivo == null,
                motivo);
    }
}
