package co.autoscolombia.parqueadero.presentation.controller;

import co.autoscolombia.parqueadero.application.VehiculoService;
import co.autoscolombia.parqueadero.application.dto.Dtos.VehiculoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    /** GET /api/vehicles/ABC123 → datos del vehículo, cliente, vigencia y si puede ingresar. */
    @GetMapping("/{plate}")
    public VehiculoDto consultar(@PathVariable("plate") String placa) {
        return vehiculoService.consultarPorPlaca(placa);
    }
}
