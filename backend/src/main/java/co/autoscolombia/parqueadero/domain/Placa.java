package co.autoscolombia.parqueadero.domain;

import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;

import java.util.Locale;

/**
 * RN-01: normalización de placas.
 * <p>
 * Toda placa que entra al sistema (búsqueda, entrada, historial) pasa por aquí,
 * así " abc123 " y "ABC123" se tratan como la misma placa.
 */
public final class Placa {

    public static final int LONGITUD_MAXIMA = 10;

    private Placa() {
    }

    /** Quita espacios externos y pasa a mayúsculas. Lanza 400 si queda vacía o es demasiado larga. */
    public static String normalizar(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new SolicitudInvalidaException("PLACA_REQUERIDA", "La placa es obligatoria.");
        }
        String normalizada = placa.strip().toUpperCase(Locale.ROOT);
        if (normalizada.length() > LONGITUD_MAXIMA) {
            throw new SolicitudInvalidaException("PLACA_INVALIDA",
                    "La placa no puede tener más de " + LONGITUD_MAXIMA + " caracteres.");
        }
        return normalizada;
    }

    /** Variante para filtros opcionales: null o vacío significa "sin filtro". */
    public static String normalizarOpcional(String placa) {
        return (placa == null || placa.isBlank()) ? null : normalizar(placa);
    }
}
