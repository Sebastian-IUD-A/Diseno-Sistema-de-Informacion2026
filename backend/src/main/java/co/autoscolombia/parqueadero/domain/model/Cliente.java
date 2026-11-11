package co.autoscolombia.parqueadero.domain.model;

import co.autoscolombia.parqueadero.domain.enums.EstadoRegistro;
import co.autoscolombia.parqueadero.domain.enums.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA: cada instancia corresponde a una fila de la tabla {@code cliente}.
 * El cliente es el titular de la mensualidad de uno o varios vehículos.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    /** Constructor vacío exigido por JPA. */
    protected Cliente() {
    }

    public Cliente(TipoDocumento tipoDocumento, String numeroDocumento, String nombres, String apellidos,
                   String telefono, String correo) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.correo = correo;
        this.estado = EstadoRegistro.ACTIVO;
    }

    public boolean estaActivo() {
        return estado == EstadoRegistro.ACTIVO;
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public Long getId() { return id; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }
    public EstadoRegistro getEstado() { return estado; }

    public void setEstado(EstadoRegistro estado) { this.estado = estado; }
}
