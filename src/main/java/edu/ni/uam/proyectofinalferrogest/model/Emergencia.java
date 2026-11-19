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

    @Column(length = 200)
    private String direccion;

    @Column(columnDefinition = "text")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_id")
    private TipoEmergencia tipo;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Integer usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PrioridadEmergencia prioridad;

    @Convert(converter = EstadoEmergenciaConverter.class)
    @Column(nullable = false, length = 20)
    private EstadoEmergencia estado = EstadoEmergencia.PENDIENTE;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "numero_acta", length = 30)
    private String numeroActa;
    @Column(length = 100)
    private String escuadra;
    @Column(length = 120)
    private String solicitante;
    @Column(length = 30)
    private String telefono;
    @Column(name = "descripcion_final", columnDefinition = "text")
    private String descripcionFinal;
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ResultadoEmergencia resultado;

    public record DatosActa(String numero, LocalDateTime reporte, String escuadra,
                           String solicitante, String telefono, String direccion, String descripcion,
                           Integer tipoId, PrioridadEmergencia prioridad, Double latitud, Double longitud) {}

    public static Emergencia abrir(DatosActa datos, TipoEmergencia tipo, Integer usuarioId) {
        var emergencia = new Emergencia();
        emergencia.usuarioId = usuarioId;
        emergencia.completar(datos, tipo);
        return emergencia;
    }

    public void completar(DatosActa datos, TipoEmergencia tipo) {
        exigirAbierta();
        String numero = texto(datos.numero(), 30, "Número de acta");
        if ((numero == null && (id == null || numeroActa != null))
                || (numero != null && !numero.matches("[\\p{L}\\p{N}][\\p{L}\\p{N}/._-]*"))) {
            throw new IllegalArgumentException("Ingrese un número de acta válido.");
        }
        if (datos.reporte() == null || datos.reporte().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ingrese una fecha de reporte que no esté en el futuro.");
        }
        String contacto = texto(datos.telefono(), 30, "Teléfono");
        if (contacto != null && (!contacto.matches("[+0-9 ()-]+") || !contacto.matches(".*[0-9].*"))) {
            throw new IllegalArgumentException("Ingrese un teléfono válido.");
        }
        this.numeroActa = numero;
        this.fechaRegistro = datos.reporte();
        this.escuadra = texto(datos.escuadra(), 100, "Escuadra");
        this.solicitante = texto(datos.solicitante(), 120, "Solicitante");
        this.telefono = contacto;
        this.direccion = texto(datos.direccion(), 200, "Dirección");
        this.descripcion = texto(datos.descripcion(), 1000, "Descripción");
        this.tipo = tipo;
        this.prioridad = datos.prioridad();
        ubicar(datos.latitud(), datos.longitud());
    }

    public void prepararCierre(String descripcion, ResultadoEmergencia resultado) {
        exigirAbierta();
        String descripcionFinal = texto(descripcion, 1000, "Descripción final");
        if (resultado == null || descripcionFinal == null) {
            throw new IllegalArgumentException("Ingrese la descripción final y el resultado.");
        }
        this.descripcionFinal = descripcionFinal;
        this.resultado = resultado;
    }

    private void exigirAbierta() {
        if (estado == EstadoEmergencia.CERRADA) {
            throw new IllegalStateException("Una emergencia cerrada no se puede editar.");
        }
    }

    private static String texto(String valor, int limite, String campo) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.strip();
        if (limpio.length() > limite) {
            throw new IllegalArgumentException(campo + " admite hasta " + limite + " caracteres.");
        }
        return limpio;
    }

    public String getNumeroActa() { return numeroActa; }
    public String getEscuadra() { return escuadra; }
    public String getSolicitante() { return solicitante; }
    public String getTelefono() { return telefono; }
    public String getDescripcionFinal() { return descripcionFinal; }
    public ResultadoEmergencia getResultado() { return resultado; }

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
