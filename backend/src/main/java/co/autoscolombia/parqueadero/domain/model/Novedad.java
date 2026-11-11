package co.autoscolombia.parqueadero.domain.model;

import co.autoscolombia.parqueadero.domain.enums.TipoNovedad;
import co.autoscolombia.parqueadero.domain.exception.SolicitudInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** RN-07: novedad ligada a un movimiento, con autor y fecha. Es inmutable una vez creada. */
@Entity
@Table(name = "novedad")
public class Novedad {

    public static final int LONGITUD_MAXIMA_DESCRIPCION = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimiento_id", nullable = false, updatable = false)
    private MovimientoParqueo movimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 25, updatable = false)
    private TipoNovedad tipo;

    @Column(nullable = false, length = LONGITUD_MAXIMA_DESCRIPCION, updatable = false)
    private String descripcion;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    protected Novedad() {
    }

    public Novedad(MovimientoParqueo movimiento, TipoNovedad tipo, String descripcion,
                   LocalDateTime fechaHora, Usuario usuario) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new SolicitudInvalidaException("DESCRIPCION_REQUERIDA", "La descripción de la novedad es obligatoria.");
        }
        String limpia = descripcion.strip();
        if (limpia.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new SolicitudInvalidaException("DESCRIPCION_MUY_LARGA",
                    "La descripción no puede superar " + LONGITUD_MAXIMA_DESCRIPCION + " caracteres.");
        }
        if (tipo == null) {
            throw new SolicitudInvalidaException("TIPO_REQUERIDO", "El tipo de novedad es obligatorio.");
        }
        this.movimiento = movimiento;
        this.tipo = tipo;
        this.descripcion = limpia;
        this.fechaHora = fechaHora;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public MovimientoParqueo getMovimiento() { return movimiento; }
    public TipoNovedad getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Usuario getUsuario() { return usuario; }
}
