package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="equipo")
public class Equipo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    public Equipo() {}
    public Integer getId() { return id; }
    @Column(nullable=false, length=100)
    private String nombre;
    public String getNombre() { return nombre; }
    public void setNombre(String value) { this.nombre = value; }
    @Column(nullable=false, length=40)
    private String categoria;
    public String getCategoria() { return categoria; }
    public void setCategoria(String value) { this.categoria = value; }
    @Column(nullable=false)
    private int cantidad = 1;
    public int getCantidad() { return cantidad; }
    public void setCantidad(int value) { this.cantidad = value; }
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private EstadoEquipo estado = EstadoEquipo.OPERATIVO;
    public EstadoEquipo getEstado() { return estado; }
    public void setEstado(EstadoEquipo value) { this.estado = value; }
    @Column(name="ultimo_mantenimiento")
    private LocalDate ultimoMantenimiento;
    public LocalDate getUltimoMantenimiento() { return ultimoMantenimiento; }
    public void setUltimoMantenimiento(LocalDate value) { this.ultimoMantenimiento = value; }
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="unidad_id")
    private Unidad unidad;
    public Unidad getUnidad() { return unidad; }
    public void setUnidad(Unidad value) { this.unidad = value; }
    @Override public String toString() { return nombre; }
}