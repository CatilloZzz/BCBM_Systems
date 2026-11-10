package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;

@Entity
@Table(name = "unidad")
public class Unidad {

    public enum Estado {
        DISPONIBLE,
        EN_EMERGENCIA,
        MANTENIMIENTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.DISPONIBLE;

    public Unidad() {
    }

    public Unidad(String codigo, String tipo) {
        this.codigo = codigo;
        this.tipo = tipo;
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return codigo + " - " + tipo;
    }
}