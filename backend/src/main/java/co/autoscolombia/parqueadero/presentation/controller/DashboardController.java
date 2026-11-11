package co.autoscolombia.parqueadero.presentation.controller;

import co.autoscolombia.parqueadero.application.DashboardService;
import co.autoscolombia.parqueadero.application.dto.Dtos.DashboardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardDto resumen() {
        return dashboardService.resumen();
    }
}
