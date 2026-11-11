package co.autoscolombia.parqueadero.presentation.controller;

import co.autoscolombia.parqueadero.application.CeldaService;
import co.autoscolombia.parqueadero.application.dto.Dtos.CeldaDto;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.presentation.dto.Solicitudes.CeldaRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Celdas. Los permisos por rol están en SecurityConfig (POST/PUT solo ADMINISTRADOR). */
@RestController
@RequestMapping("/api/cells")
public class CeldaController {

    private final CeldaService celdaService;

    public CeldaController(CeldaService celdaService) {
        this.celdaService = celdaService;
    }

    @GetMapping
    public List<CeldaDto> listar() {
        return celdaService.listar();
    }

    /** GET /api/cells/available?type=CARRO */
    @GetMapping("/available")
    public List<CeldaDto> disponibles(@RequestParam("type") TipoVehiculo tipo) {
        return celdaService.disponibles(tipo);
    }

    @PostMapping
    public ResponseEntity<CeldaDto> crear(@Valid @RequestBody CeldaRequest solicitud) {
        CeldaDto creada = celdaService.crear(solicitud.codigo(), solicitud.zona(), solicitud.tipoVehiculo(), solicitud.estado());
        return ResponseEntity.created(URI.create("/api/cells/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public CeldaDto actualizar(@PathVariable("id") Long id, @Valid @RequestBody CeldaRequest solicitud) {
        return celdaService.actualizar(id, solicitud.codigo(), solicitud.zona(), solicitud.tipoVehiculo(), solicitud.estado());
    }
}
