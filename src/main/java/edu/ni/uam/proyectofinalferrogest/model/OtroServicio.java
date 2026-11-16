package edu.ni.uam.proyectofinalferrogest.model;
import jakarta.persistence.*;
@Entity @Table(name="otro_servicio")
public class OtroServicio {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="escuadra_departamento",nullable=false,length=100) private String escuadraDepartamento;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private TipoServicio tipo;
    @Column(nullable=false,columnDefinition="text") private String detalle;
    protected OtroServicio() {}
    public OtroServicio(String escuadraDepartamento, TipoServicio tipo, String detalle) {
        if (escuadraDepartamento==null || escuadraDepartamento.isBlank() || escuadraDepartamento.strip().length()>100)
            throw new IllegalArgumentException("Ingrese la escuadra o departamento (hasta 100 caracteres).");
        if (tipo==null) throw new IllegalArgumentException("Seleccione el tipo de servicio.");
        if (detalle==null || detalle.isBlank() || detalle.strip().length()>4000)
            throw new IllegalArgumentException("Ingrese el detalle del servicio (hasta 4000 caracteres).");
        this.escuadraDepartamento=escuadraDepartamento.strip(); this.tipo=tipo; this.detalle=detalle.strip();
    }
    public Integer getId() { return id; }
    public String getEscuadraDepartamento() { return escuadraDepartamento; }
    public TipoServicio getTipo() { return tipo; }
    public String getDetalle() { return detalle; }
}
