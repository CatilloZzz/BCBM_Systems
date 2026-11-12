package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="mantenimiento_equipo")
public class MantenimientoEquipo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    public MantenimientoEquipo() {}
    public Integer getId() { return id; }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="equipo_id", nullable=false)
    private Equipo equipo;
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo value) { this.equipo = value; }
    @Column(nullable=false)
    private LocalDate fecha;
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate value) { this.fecha = value; }
    @Column(nullable=false, columnDefinition="text")
    private String descripcion;
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String value) { this.descripcion = value; }
    @Column(name="proxima_fecha")
    private LocalDate proximaFecha;
    public LocalDate getProximaFecha() { return proximaFecha; }
    public void setProximaFecha(LocalDate value) { this.proximaFecha = value; }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="registrado_por", nullable=false)
    private Usuario registradoPor;
    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario value) { this.registradoPor = value; }
    @Override public String toString() { return fecha + " - " + descripcion; }
}
