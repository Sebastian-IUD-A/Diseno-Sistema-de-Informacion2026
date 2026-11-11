package co.autoscolombia.parqueadero.domain.model;

import co.autoscolombia.parqueadero.domain.Placa;
import co.autoscolombia.parqueadero.domain.enums.EstadoRegistro;
import co.autoscolombia.parqueadero.domain.enums.TipoVehiculo;
import co.autoscolombia.parqueadero.domain.exception.ReglaNegocioException;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Vehículo con mensualidad. Contiene la regla RN-03 ("solo ingresa un vehículo
 * activo con autorización vigente") porque es información del propio vehículo.
 */
@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TipoVehiculo tipo;

    @Column(length = 40)
    private String marca;

    @Column(length = 40)
    private String modelo;

    @Column(length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    /** Último día (inclusive) en que la mensualidad está vigente. */
    @Column(name = "autorizado_hasta", nullable = false)
    private LocalDate autorizadoHasta;

    /** LAZY: el cliente solo se consulta en la BD si realmente se usa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    protected Vehiculo() {
    }

    public Vehiculo(String placa, TipoVehiculo tipo, String marca, String modelo, String color,
                    LocalDate autorizadoHasta, Cliente cliente) {
        this.placa = Placa.normalizar(placa);
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.color = color;
        this.autorizadoHasta = autorizadoHasta;
        this.cliente = cliente;
        this.estado = EstadoRegistro.ACTIVO;
    }

    /** true si la mensualidad cubre la fecha indicada (el día de vencimiento todavía es válido). */
    public boolean autorizacionVigenteEn(LocalDate fecha) {
        return autorizadoHasta != null && !autorizadoHasta.isBefore(fecha);
    }

    /**
     * Devuelve el motivo por el que el vehículo NO puede ingresar, o {@code null} si sí puede.
     * Se usa tanto para validar (lanzar la excepción) como para informar en la búsqueda por placa.
     */
    public ReglaNegocioException motivoRechazoIngreso(LocalDate hoy) {
        if (estado != EstadoRegistro.ACTIVO) {
            return new ReglaNegocioException("VEHICULO_INACTIVO",
                    "El vehículo " + placa + " está inactivo y no puede ingresar.");
        }
        if (cliente != null && !cliente.estaActivo()) {
            return new ReglaNegocioException("CLIENTE_INACTIVO",
                    "El cliente titular del vehículo " + placa + " está inactivo.");
        }
        if (!autorizacionVigenteEn(hoy)) {
            return new ReglaNegocioException("AUTORIZACION_VENCIDA",
                    "La mensualidad del vehículo " + placa + " venció el "
                            + autorizadoHasta.format(FORMATO_FECHA) + ".");
        }
        return null;
    }

    /** RN-03: lanza la excepción si el vehículo no puede ingresar. */
    public void validarPuedeIngresar(LocalDate hoy) {
        ReglaNegocioException motivo = motivoRechazoIngreso(hoy);
        if (motivo != null) {
            throw motivo;
        }
    }

    public Long getId() { return id; }
    public String getPlaca() { return placa; }
    public TipoVehiculo getTipo() { return tipo; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getColor() { return color; }
    public EstadoRegistro getEstado() { return estado; }
    public LocalDate getAutorizadoHasta() { return autorizadoHasta; }
    public Cliente getCliente() { return cliente; }

    public void setEstado(EstadoRegistro estado) { this.estado = estado; }
    public void setAutorizadoHasta(LocalDate autorizadoHasta) { this.autorizadoHasta = autorizadoHasta; }
}
