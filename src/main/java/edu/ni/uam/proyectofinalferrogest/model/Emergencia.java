package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergencia")
public class Emergencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private long version;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(columnDefinition = "text")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoEmergencia tipo;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Integer usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadEmergencia prioridad;

    @Convert(converter = EstadoEmergenciaConverter.class)
    @Column(nullable = false, length = 20)
    private EstadoEmergencia estado = EstadoEmergencia.PENDIENTE;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    public LocalDateTime getFechaCierre() { return fechaCierre; }

    private Double latitud;
    private Double longitud;
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public void ubicar(Double latitud, Double longitud) {
        if ((latitud == null) != (longitud == null) || (latitud != null &&
                (!Double.isFinite(latitud) || Math.abs(latitud)>90 || !Double.isFinite(longitud) || Math.abs(longitud)>180)))
            throw new IllegalArgumentException("Coordenadas inválidas.");
        this.latitud = latitud;
        this.longitud = longitud;
    }
    protected Emergencia() {
    }

    public Emergencia(String direccion, String descripcion, TipoEmergencia tipo,
                      PrioridadEmergencia prioridad, Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Inicie sesión para registrar una emergencia.");
        }
        this.usuarioId = usuarioId;
        actualizarDatos(direccion, descripcion, tipo, prioridad);
    }

    public void actualizarDatos(String direccion, String descripcion, TipoEmergencia tipo,
                                PrioridadEmergencia prioridad) {
        if (estado == EstadoEmergencia.CERRADA) {
            throw new IllegalStateException("Una emergencia cerrada no se puede editar.");
        }
        if (direccion == null || direccion.isBlank() || direccion.strip().length() > 200) {
            throw new IllegalArgumentException("Ingrese una dirección de hasta 200 caracteres.");
        }
        if (descripcion != null && descripcion.strip().length() > 1000) {
            throw new IllegalArgumentException("La descripción admite hasta 1000 caracteres.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Seleccione un tipo de emergencia.");
        }
        if (prioridad == null) {
            throw new IllegalArgumentException("Seleccione una prioridad.");
        }
        this.direccion = direccion.strip();
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
        this.tipo = tipo;
        this.prioridad = prioridad;
    }

    /** El servicio de despacho usa este método dentro de su transacción. */
    public void cambiarEstado(EstadoEmergencia destino) {
        if (!estado.permiteTransicionA(destino)) {
            throw new IllegalStateException("El cambio de estado solicitado no está permitido.");
        }
        estado = destino;
        if (destino == EstadoEmergencia.CERRADA) fechaCierre = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public TipoEmergencia getTipo() {
        return tipo;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public PrioridadEmergencia getPrioridad() {
        return prioridad;
    }

    public EstadoEmergencia getEstado() {
        return estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
