package co.autoscolombia.parqueadero.presentation.controller;

import co.autoscolombia.parqueadero.application.MovimientoService;
import co.autoscolombia.parqueadero.application.NovedadService;
import co.autoscolombia.parqueadero.application.dto.Dtos.MovimientoDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.NovedadDto;
import co.autoscolombia.parqueadero.presentation.dto.Solicitudes.EntradaRequest;
import co.autoscolombia.parqueadero.presentation.dto.Solicitudes.NovedadRequest;
import co.autoscolombia.parqueadero.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Entradas, salidas, ocupación, historial y novedades. */
@RestController
@RequestMapping("/api/movements")
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final NovedadService novedadService;
    private final UsuarioActual usuarioActual;

    public MovimientoController(MovimientoService movimientoService, NovedadService novedadService,
                                UsuarioActual usuarioActual) {
        this.movimientoService = movimientoService;
        this.novedadService = novedadService;
        this.usuarioActual = usuarioActual;
    }

    /** Registrar entrada → 201 Created con el movimiento abierto. */
    @PostMapping("/entries")
    public ResponseEntity<MovimientoDto> registrarEntrada(@Valid @RequestBody EntradaRequest solicitud) {
        MovimientoDto movimiento = movimientoService.registrarEntrada(solicitud.placa(), solicitud.celdaId(), usuarioActual.id());
        return ResponseEntity.created(URI.create("/api/movements/" + movimiento.id())).body(movimiento);
    }

    /** Registrar salida → 200 con el movimiento ya cerrado. */
    @PostMapping("/{id}/exit")
    public MovimientoDto registrarSalida(@PathVariable("id") Long id) {
        return movimientoService.registrarSalida(id, usuarioActual.id());
    }

    /** Ocupación actual. ?plate= filtra por coincidencia parcial de la placa. */
    @GetMapping("/active")
    public List<MovimientoDto> activos(@RequestParam(value = "plate", required = false) String placa) {
        return movimientoService.listarActivos(placa);
    }

    /** Historial: GET /api/movements?plate=ABC123&from=2026-11-01&to=2026-11-30 (todos opcionales). */
    @GetMapping
    public List<MovimientoDto> historial(
            @RequestParam(value = "plate", required = false) String placa,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return movimientoService.historial(placa, desde, hasta);
    }

    @GetMapping("/{id}")
    public MovimientoDto detalle(@PathVariable("id") Long id) {
        return movimientoService.obtener(id);
    }

    @PostMapping("/{id}/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public NovedadDto registrarNovedad(@PathVariable("id") Long id, @Valid @RequestBody NovedadRequest solicitud) {
        return novedadService.registrar(id, solicitud.tipo(), solicitud.descripcion(), usuarioActual.id());
    }

    @GetMapping("/{id}/incidents")
    public List<NovedadDto> novedades(@PathVariable("id") Long id) {
        return novedadService.listar(id);
    }
}
