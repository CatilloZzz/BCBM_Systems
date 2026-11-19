package edu.ni.uam.proyectofinalferrogest.model;
import jakarta.persistence.*;
@Entity @Table(name="otro_servicio")
public class OtroServicio {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="escuadra_departamento",nullable=false,length=100) private String escuadraDepartamento;
    @Column(nullable=false,length=100) private String tipo;
    @Column(nullable=false,columnDefinition="text") private String detalle;
    protected OtroServicio() {}
    public OtroServicio(String escuadraDepartamento, TipoServicio tipo, String detalle) {
        this(escuadraDepartamento,tipo==null ? null : tipo.name(),detalle);
    }
    public OtroServicio(String escuadraDepartamento, String tipo, String detalle) {
        if (escuadraDepartamento==null || escuadraDepartamento.isBlank() || escuadraDepartamento.strip().length()>100)
            throw new IllegalArgumentException("Ingrese la escuadra o departamento (hasta 100 caracteres).");
        if (tipo==null || tipo.isBlank() || tipo.length()>100) throw new IllegalArgumentException("Seleccione el tipo de servicio.");
        if (detalle==null || detalle.isBlank() || detalle.strip().length()>4000)
            throw new IllegalArgumentException("Ingrese el detalle del servicio (hasta 4000 caracteres).");
        this.escuadraDepartamento=escuadraDepartamento.strip(); this.tipo=tipo; this.detalle=detalle.strip();
    }
    public Integer getId() { return id; }
    public String getEscuadraDepartamento() { return escuadraDepartamento; }
    public TipoServicio getTipo() {
        try { return TipoServicio.valueOf(tipo); } catch (IllegalArgumentException e) { return null; }
    }
    public String getTipoNombre() {
        TipoServicio anterior=getTipo();
        return anterior==null ? tipo : anterior.toString();
    }
    public String getDetalle() { return detalle; }
}
