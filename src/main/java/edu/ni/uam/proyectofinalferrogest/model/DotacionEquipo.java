package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;

/** Existencias de un tipo de equipo asignadas a una unidad concreta. */
@Entity
@Table(name="dotacion_equipo", uniqueConstraints=@UniqueConstraint(columnNames={"equipo_id","unidad_id"}))
public class DotacionEquipo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="equipo_id",nullable=false) private Equipo equipo;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="unidad_id",nullable=false) private Unidad unidad;
    @Column(nullable=false) private int cantidad;
    protected DotacionEquipo() {}
    public DotacionEquipo(Equipo equipo,Unidad unidad,int cantidad) {
        if (equipo==null || unidad==null || cantidad<1) throw new IllegalArgumentException("Seleccione una cantidad válida y los recursos correspondientes.");
        this.equipo=equipo; this.unidad=unidad; this.cantidad=cantidad;
    }
    public Integer getId() { return id; }
    public Equipo getEquipo() { return equipo; }
    public Unidad getUnidad() { return unidad; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { if (cantidad<1) throw new IllegalArgumentException("La cantidad debe ser mayor que cero."); this.cantidad=cantidad; }
}
