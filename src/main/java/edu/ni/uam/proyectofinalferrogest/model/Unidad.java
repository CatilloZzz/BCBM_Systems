package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="unidad")
public class Unidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    public Unidad() {}
    public Integer getId() { return id; }
    @Column(nullable=false, unique=true, length=20)
    private String codigo;
    public String getCodigo() { return codigo; }
    public void setCodigo(String value) { this.codigo = value; }
    @Column(nullable=false, length=30)
    private String tipo;
    public String getTipo() { return tipo; }
    public void setTipo(String value) { this.tipo = value; }
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private EstadoUnidad estado = EstadoUnidad.DISPONIBLE;
    public EstadoUnidad getEstado() { return estado; }
    public void setEstado(EstadoUnidad value) { this.estado = value; }
    @Override public String toString() { return codigo; }
}