package co.autoscolombia.parqueadero.presentation.dto;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoNovedad;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos de las peticiones (lo que el frontend envía) con sus reglas de Bean Validation.
 * Si algo no cumple, Spring responde 400 antes de llegar al servicio.
 * <p>
 * Ninguna solicitud incluye "usuarioId": el usuario se toma del token (ver UsuarioActual).
 */
public final class Solicitudes {

    private Solicitudes() {
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "El correo no tiene un formato válido.")
            @Size(max = 120)
            String correo,
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(max = 100)
            String contrasena) {
    }

    public record EntradaRequest(
            @NotBlank(message = "La placa es obligatoria.")
            @Size(max = 12, message = "La placa es demasiado larga.")
            @Pattern(regexp = "^\\s*[A-Za-z0-9-]{3,10}\\s*$", message = "La placa solo admite letras, números y guion (3 a 10 caracteres).")
            String placa,
            @NotNull(message = "Debe seleccionar una celda.")
            @Positive
            Long celdaId) {
    }

    public record NovedadRequest(
            @NotNull(message = "El tipo de novedad es obligatorio.")
            TipoNovedad tipo,
            @NotBlank(message = "La descripción de la novedad es obligatoria.")
            @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
            String descripcion) {
    }

    public record CeldaRequest(
            @NotBlank(message = "El código es obligatorio.")
            @Size(max = 20, message = "El código no puede superar 20 caracteres.")
            @Pattern(regexp = "^\\s*[A-Za-z0-9-]+\\s*$", message = "El código solo admite letras, números y guion.")
            String codigo,
            @NotBlank(message = "La zona es obligatoria.")
            @Size(max = 40, message = "La zona no puede superar 40 caracteres.")
            String zona,
            @NotNull(message = "El tipo de vehículo es obligatorio.")
            TipoVehiculo tipoVehiculo,
            @NotNull(message = "El estado es obligatorio.")
            EstadoCelda estado) {
    }
}
