package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="bombero")
public class Bombero {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    public Bombero() {}
    public Integer getId() { return id; }
    @Column(nullable=false, length=100)
    private String nombre;
    public String getNombre() { return nombre; }
    public void setNombre(String value) { this.nombre = value; }
    @Column(nullable=false, length=100)
    private String apellido;
    public String getApellido() { return apellido; }
    public void setApellido(String value) { this.apellido = value; }
    @Column(nullable=false, unique=true, length=20)
    private String cedula;
    public String getCedula() { return cedula; }
    public void setCedula(String value) { this.cedula = value; }
    @Column(nullable=false, length=40)
    private String rango;
    public String getRango() { return rango; }
    public void setRango(String value) { this.rango = value; }
    @Column(length=20)
    private String telefono;
    public String getTelefono() { return telefono; }
    public void setTelefono(String value) { this.telefono = value; }
    @Column(name="fecha_ingreso", nullable=false)
    private LocalDate fechaIngreso = LocalDate.now();
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate value) { this.fechaIngreso = value; }
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private EstadoBombero estado = EstadoBombero.DISPONIBLE;
    public EstadoBombero getEstado() { return estado; }
    public void setEstado(EstadoBombero value) { this.estado = value; }
    @Override public String toString() { return nombre + " " + apellido + " (" + cedula + ")"; }
}