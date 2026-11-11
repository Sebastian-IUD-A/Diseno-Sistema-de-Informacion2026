package co.autoscolombia.parqueadero.presentation.controller;

import co.autoscolombia.parqueadero.application.AuthService;
import co.autoscolombia.parqueadero.application.dto.Dtos.SesionDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.UsuarioDto;
import co.autoscolombia.parqueadero.presentation.dto.Solicitudes.LoginRequest;
import co.autoscolombia.parqueadero.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador = capa de presentación. Solo traduce HTTP ⇄ casos de uso:
 * recibe la petición, valida el formato (@Valid), llama al servicio y devuelve el resultado.
 * No contiene reglas de negocio.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioActual usuarioActual;

    public AuthController(AuthService authService, UsuarioActual usuarioActual) {
        this.authService = authService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/login")
    public SesionDto login(@Valid @RequestBody LoginRequest solicitud) {
        return authService.login(solicitud.correo(), solicitud.contrasena());
    }

    @GetMapping("/me")
    public UsuarioDto yo() {
        return authService.perfil(usuarioActual.id());
    }

    /**
     * Con JWT no hay sesión en el servidor: cerrar sesión = el frontend borra el token.
     * Este endpoint existe para que el flujo sea explícito y para auditoría futura.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
