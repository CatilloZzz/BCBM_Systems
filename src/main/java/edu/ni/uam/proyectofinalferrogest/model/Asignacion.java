package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Mapeo de las asignaciones existentes para consultas y protección del historial. */
@Entity
@Table(name = "asignacion", uniqueConstraints = @UniqueConstraint(columnNames = {"emergencia_id", "bombero_id"}))
public class Asignacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergencia_id", nullable = false)
    private Emergencia emergencia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bombero_id", nullable = false)
    private Bombero bombero;
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
    protected Asignacion() {}
    public Integer getId() { return id; }
    public Emergencia getEmergencia() { return emergencia; }
    public Unidad getUnidad() { return unidad; }
    public Bombero getBombero() { return bombero; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
}
