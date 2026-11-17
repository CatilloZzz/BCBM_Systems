package edu.ni.uam.proyectofinalferrogest.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
/** Una salida por unidad, con tripulación propia y un único destino. */
@Entity @Table(name="salida_unidad")
public class SalidaUnidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="emergencia_id") private Emergencia emergencia;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="otro_servicio_id") private OtroServicio otroServicio;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="unidad_id",nullable=false) private Unidad unidad;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="operador_id",nullable=false) private Bombero operador;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="jefe_id") private Bombero jefe;
    @ManyToMany
    @JoinTable(name="salida_participante",joinColumns=@JoinColumn(name="salida_id"),inverseJoinColumns=@JoinColumn(name="bombero_id"))
    private Set<Bombero> participantes=new LinkedHashSet<>();
    @Column(name="registrado_por",nullable=false) private Integer registradoPor;
    @Column(nullable=false) private LocalDateTime salida;
    private LocalDateTime llegada;
    private LocalDateTime retorno;
    protected SalidaUnidad() {}
    public SalidaUnidad(Emergencia emergencia, OtroServicio servicio, Unidad unidad, Bombero operador,
                        Bombero jefe, Collection<Bombero> participantes, Integer usuario, LocalDateTime salida) {
        if ((emergencia==null)==(servicio==null)) throw new IllegalArgumentException("Seleccione una emergencia o un servicio.");
        if (unidad==null || operador==null || usuario==null || salida==null)
            throw new IllegalArgumentException("Unidad, operador, usuario y fecha de salida son obligatorios.");
        if (participantes==null || participantes.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Tripulación inválida.");
        if (salida.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("La salida no puede estar en el futuro.");
        this.emergencia=emergencia; this.otroServicio=servicio; this.unidad=unidad; this.operador=operador;
        this.jefe=jefe; this.registradoPor=usuario; this.salida=salida;
        this.participantes.addAll(participantes); this.participantes.add(operador);
        if (jefe!=null) this.participantes.add(jefe);
    }
    public void marcarLlegada(LocalDateTime fecha) {
        if (emergencia==null) throw new IllegalArgumentException("Otros servicios solo registran salida y entrada.");
        if (llegada!=null || retorno!=null) throw new IllegalArgumentException("La llegada ya fue registrada o la salida terminó.");
        validarFecha(fecha,salida); llegada=fecha;
    }
    public void registrarRetorno(LocalDateTime fecha) {
        if (retorno!=null) throw new IllegalArgumentException("El retorno ya fue registrado.");
        if (emergencia!=null && llegada==null) throw new IllegalArgumentException("Registre primero la llegada de la unidad.");
        validarFecha(fecha,llegada==null ? salida : llegada); retorno=fecha;
    }
    private void validarFecha(LocalDateTime fecha,LocalDateTime anterior) {
        if (fecha==null || fecha.isBefore(anterior)) throw new IllegalArgumentException("La fecha no puede ser anterior al movimiento previo.");
        if (fecha.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("El movimiento no puede estar en el futuro.");
    }
    public Integer getId() { return id; }
    public Emergencia getEmergencia() { return emergencia; }
    public OtroServicio getOtroServicio() { return otroServicio; }
    public Unidad getUnidad() { return unidad; }
    public Bombero getOperador() { return operador; }
    public Bombero getJefe() { return jefe; }
    public Set<Bombero> getParticipantes() { return Collections.unmodifiableSet(participantes); }
    public Integer getRegistradoPor() { return registradoPor; }
    public LocalDateTime getSalida() { return salida; }
    public LocalDateTime getLlegada() { return llegada; }
    public LocalDateTime getRetorno() { return retorno; }
}
