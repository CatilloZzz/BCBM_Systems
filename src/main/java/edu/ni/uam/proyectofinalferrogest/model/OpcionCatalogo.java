package edu.ni.uam.proyectofinalferrogest.model;
import jakarta.persistence.*;

@Entity @Table(name="opcion_catalogo")
public class OpcionCatalogo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Version private long version;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private GrupoCatalogo grupo;
    @Column(nullable=false,length=100) private String nombre;
    @Column(nullable=false) private boolean activo=true;
    public OpcionCatalogo() {}
    public OpcionCatalogo(GrupoCatalogo grupo,String nombre) { this.grupo=grupo; this.nombre=nombre; }
    public Integer getId() { return id; }
    public long getVersion() { return version; }
    public GrupoCatalogo getGrupo() { return grupo; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public void cambiar(String nombre,boolean activo) { this.nombre=nombre; this.activo=activo; }
    @Override public String toString() { return nombre; }
}
