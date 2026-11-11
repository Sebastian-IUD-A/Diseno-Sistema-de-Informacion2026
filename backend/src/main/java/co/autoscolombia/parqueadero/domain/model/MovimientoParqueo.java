package co.autoscolombia.parqueadero.domain.model;

import co.autoscolombia.parqueadero.domain.exception.ConflictoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Una permanencia: desde que el vehículo entra hasta que sale.
 * <p>
 * "Abierto" = sin fecha de salida (el vehículo sigue dentro).
 * Las columnas generadas {@code vehiculo_abierto_id} y {@code celda_abierta_id} de la tabla
 * NO se mapean aquí: las calcula MySQL y solo existen para los índices únicos (RN-02, RN-04).
 * <p>
 * RN-08: no existe ningún método para borrar movimientos (ver {@code MovimientoRepository}).
 */
@Entity
@Table(name = "movimiento_parqueo")
public class MovimientoParqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false, updatable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "celda_id", nullable = false, updatable = false)
    private Celda celda;

    @Column(name = "fecha_hora_entrada", nullable = false, updatable = false)
    private LocalDateTime fechaHoraEntrada;

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_entrada_id", nullable = false, updatable = false)
    private Usuario usuarioEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_salida_id")
    private Usuario usuarioSalida;

    protected MovimientoParqueo() {
    }

    public MovimientoParqueo(Vehiculo vehiculo, Celda celda, LocalDateTime fechaHoraEntrada, Usuario usuarioEntrada) {
        this.vehiculo = vehiculo;
        this.celda = celda;
        this.fechaHoraEntrada = fechaHoraEntrada;
        this.usuarioEntrada = usuarioEntrada;
    }

    public boolean estaAbierto() {
        return fechaHoraSalida == null;
    }

    /** Cierra la permanencia guardando hora y usuario de salida (RN-05, RN-06). */
    public void cerrar(LocalDateTime fechaHoraSalida, Usuario usuarioSalida) {
        if (!estaAbierto()) {
            throw new ConflictoException("MOVIMIENTO_YA_CERRADO",
                    "Este movimiento ya registró su salida.");
        }
        this.fechaHoraSalida = fechaHoraSalida.isBefore(fechaHoraEntrada) ? fechaHoraEntrada : fechaHoraSalida;
        this.usuarioSalida = usuarioSalida;
    }

    /** Tiempo de permanencia en minutos (hasta {@code ahora} si sigue dentro). Solo informativo, no se cobra. */
    public long minutosPermanencia(LocalDateTime ahora) {
        LocalDateTime fin = estaAbierto() ? ahora : fechaHoraSalida;
        return Math.max(0, Duration.between(fechaHoraEntrada, fin).toMinutes());
    }

    public Long getId() { return id; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public Celda getCelda() { return celda; }
    public LocalDateTime getFechaHoraEntrada() { return fechaHoraEntrada; }
    public LocalDateTime getFechaHoraSalida() { return fechaHoraSalida; }
    public Usuario getUsuarioEntrada() { return usuarioEntrada; }
    public Usuario getUsuarioSalida() { return usuarioSalida; }
}
