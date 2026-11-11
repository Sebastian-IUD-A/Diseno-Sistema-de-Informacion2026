package co.autoscolombia.parqueadero.presentation;

import co.autoscolombia.parqueadero.domain.enums.TipoNovedad;
import co.autoscolombia.parqueadero.presentation.dto.Solicitudes.EntradaRequest;
import co.autoscolombia.parqueadero.presentation.dto.Solicitudes.NovedadRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica las reglas de Bean Validation de los cuerpos de petición (lo que produce los 400). */
class ValidacionSolicitudesTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    @Test
    @DisplayName("novedad sin descripción → error en el campo 'descripcion'")
    void novedadSinDescripcion() {
        Set<ConstraintViolation<NovedadRequest>> errores = validador.validate(new NovedadRequest(TipoNovedad.OTRO, "  "));
        assertThat(errores).extracting(v -> v.getPropertyPath().toString()).containsExactly("descripcion");
    }

    @Test
    @DisplayName("novedad sin tipo → error en el campo 'tipo'")
    void novedadSinTipo() {
        Set<ConstraintViolation<NovedadRequest>> errores = validador.validate(new NovedadRequest(null, "Rayón"));
        assertThat(errores).extracting(v -> v.getPropertyPath().toString()).containsExactly("tipo");
    }

    @Test
    @DisplayName("entrada sin celda o con placa inválida → errores")
    void entradaInvalida() {
        assertThat(validador.validate(new EntradaRequest("ABC123", null))).hasSize(1);
        assertThat(validador.validate(new EntradaRequest("AB C$", 1L))).isNotEmpty();
        assertThat(validador.validate(new EntradaRequest(" abc123 ", 1L))).isEmpty();
    }
}
