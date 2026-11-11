package co.autoscolombia.parqueadero.domain.model;

import co.autoscolombia.parqueadero.domain.enums.EstadoCelda;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import co.autoscolombia.parqueadero.domain.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Espacio físico de parqueo. Las transiciones de estado se hacen SOLO con los métodos
 * de esta clase ({@link #ocupar}, {@link #liberar}, {@link #cambiarEstadoAdministrativo}),
 * así las reglas RN-04 y RN-09 quedan en un único lugar.
 */
@Entity
@Table(name = "celda")
public class Celda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String zona;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_vehiculo", nullable = false, length = 10)
    private TipoVehiculo tipoVehiculo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private EstadoCelda estado = EstadoCelda.DISPONIBLE;

    /**
     * Bloqueo optimista: Hibernate incrementa este número en cada UPDATE y verifica que no
     * haya cambiado desde que se leyó. Si dos administradores editan la misma celda a la vez,
     * el segundo recibe un error en lugar de sobrescribir en silencio.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    protected Celda() {
    }

    public Celda(String codigo, String zona, TipoVehiculo tipoVehiculo, EstadoCelda estadoInicial) {
        this.codigo = codigo;
        this.zona = zona;
        this.tipoVehiculo = tipoVehiculo;
        if (estadoInicial == EstadoCelda.OCUPADA) {
            throw new ReglaNegocioException("ESTADO_CELDA_INVALIDO",
                    "Una celda nueva no puede crearse como OCUPADA; ese estado lo asigna el sistema al registrar una entrada.");
        }
        this.estado = estadoInicial == null ? EstadoCelda.DISPONIBLE : estadoInicial;
    }

    /** Asigna la celda a un vehículo que entra. Valida RN-04 y compatibilidad de tipo. */
    public void ocupar(TipoVehiculo tipoDelVehiculo) {
        if (estado == EstadoCelda.FUERA_SERVICIO) {
            throw new ReglaNegocioException("CELDA_FUERA_SERVICIO",
                    "La celda " + codigo + " está fuera de servicio.");
        }
        if (estado == EstadoCelda.OCUPADA) {
            throw new ConflictoException("CELDA_OCUPADA",
                    "La celda " + codigo + " ya está ocupada. Seleccione otra celda.");
        }
        if (tipoVehiculo != tipoDelVehiculo) {
            throw new ReglaNegocioException("CELDA_INCOMPATIBLE",
                    "La celda " + codigo + " es para " + tipoVehiculo + " y el vehículo es " + tipoDelVehiculo + ".");
        }
        this.estado = EstadoCelda.OCUPADA;
    }

    /** Libera la celda al registrar la salida (RN-05). */
    public void liberar() {
        this.estado = EstadoCelda.DISPONIBLE;
    }

    /**
     * Cambios de estado hechos por el administrador. Solo permite DISPONIBLE <-> FUERA_SERVICIO.
     * RN-09: una celda ocupada no puede ponerse fuera de servicio.
     */
    public void cambiarEstadoAdministrativo(EstadoCelda nuevoEstado) {
        if (nuevoEstado == estado) {
            return;
        }
        if (nuevoEstado == EstadoCelda.OCUPADA) {
            throw new ReglaNegocioException("ESTADO_CELDA_INVALIDO",
                    "El estado OCUPADA solo lo asigna el sistema al registrar una entrada.");
        }
        if (estado == EstadoCelda.OCUPADA) {
            throw new ConflictoException("CELDA_OCUPADA_NO_MODIFICABLE",
                    "La celda " + codigo + " está ocupada; registre primero la salida del vehículo.");
        }
        this.estado = nuevoEstado;
    }

    /** Cambia datos descriptivos. El tipo no puede cambiar mientras la celda esté ocupada. */
    public void actualizarDatos(String nuevoCodigo, String nuevaZona, TipoVehiculo nuevoTipo) {
        if (estado == EstadoCelda.OCUPADA && nuevoTipo != tipoVehiculo) {
            throw new ConflictoException("CELDA_OCUPADA_NO_MODIFICABLE",
                    "No se puede cambiar el tipo de la celda " + codigo + " mientras está ocupada.");
        }
        this.codigo = nuevoCodigo;
        this.zona = nuevaZona;
        this.tipoVehiculo = nuevoTipo;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getZona() { return zona; }
    public TipoVehiculo getTipoVehiculo() { return tipoVehiculo; }
    public EstadoCelda getEstado() { return estado; }
    public Long getVersion() { return version; }
}
