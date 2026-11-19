package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.*;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/** API compartida para Julián (actas) y Rafael (consultas mensuales). */
public class DespachoService {
    private final SessionFactory factory;
    public DespachoService(SessionFactory factory) { this.factory=Objects.requireNonNull(factory); }
    public record Persona(Integer id,String nombre) { @Override public String toString() { return nombre; } }
    public record Vehiculo(Integer id,String codigo,String estado) { @Override public String toString() { return codigo+" · "+estado; } }
    public record Acta(Integer id,String detalle) { @Override public String toString() { return "#"+id+" · "+detalle; } }
    public record Catalogos(List<Vehiculo> unidades,List<Persona> personal,List<Acta> emergencias,Map<Integer,Set<Integer>> permisos,Set<Integer> todas,Map<GrupoCatalogo,List<String>> opciones) {
        public List<Persona> operadores(Integer unidad) {
            if (unidad==null) return List.of();
            return personal.stream().filter(p->todas.contains(p.id()) || permisos.getOrDefault(p.id(),Set.of()).contains(unidad)).toList();
        }
    }
    public record Salida(Integer id,Integer emergenciaId,Integer servicioId,Integer unidadId,String unidad,
                         Persona operador,Persona jefe,List<Persona> participantes,Integer registradoPor,
                         LocalDateTime salida,LocalDateTime llegada,LocalDateTime retorno,
                         String escuadraDepartamento,String tipoServicio,String detalle,String numeroActa) {}
    public record Tripulacion(Integer unidadId,Integer operadorId,Integer jefeId,List<Integer> participantes,LocalDateTime salida) {
        public Tripulacion { participantes=participantes==null ? List.of() : List.copyOf(participantes); }
    }
    private <T> T transaccion(Function<Session,T> accion) {
        return factory.fromTransaction(accion::apply);
    }
    private Usuario autorizar(Session s,SesionUsuario sesion) {
        Usuario u=Acceso.usuario(s,sesion);
        if (u.getRol()!=Rol.ADMIN && u.getRol()!=Rol.DESPACHADOR)
            throw new SecurityException("Solo el administrador o despachador puede acceder a las salidas.");
        return u;
    }
    public Catalogos catalogos(SesionUsuario sesion) {
        return transaccion(s -> {
            autorizar(s,sesion);
            var unidades=s.createSelectionQuery("from Unidad order by codigo",Unidad.class).getResultList().stream()
                .map(u -> new Vehiculo(u.getId(),u.getCodigo(),u.getEstado().toString())).toList();
            var personal=s.createSelectionQuery("from Bombero where estado = :disponible order by nombre, apellido",Bombero.class)
                .setParameter("disponible",EstadoBombero.DISPONIBLE).getResultList().stream().map(this::persona).toList();
            var actas=s.createSelectionQuery("from Emergencia where estado <> :cerrada order by fechaRegistro desc",Emergencia.class)
                .setParameter("cerrada",EstadoEmergencia.CERRADA).getResultList().stream()
                .map(e -> new Acta(e.getId(),(e.getNumeroActa() == null ? "" : e.getNumeroActa() + " · ")
                        + Objects.toString(e.getDireccion(), "Sin dirección"))).toList();
            Map<Integer,Set<Integer>> permisos=new HashMap<>();
            Set<Integer> todas=new HashSet<>();
            for (Bombero b:s.createSelectionQuery("from Bombero",Bombero.class).getResultList()) {
                permisos.put(b.getId(),b.getUnidadesOperador());
                if (b.isOperadorTodas()) todas.add(b.getId());
            }
            Map<GrupoCatalogo,List<String>> opciones=new EnumMap<>(GrupoCatalogo.class);
            for (GrupoCatalogo g:GrupoCatalogo.values()) opciones.put(g,new ArrayList<>());
            for (OpcionCatalogo o:s.createSelectionQuery("from OpcionCatalogo where activo=true order by lower(nombre)",OpcionCatalogo.class).getResultList())
                opciones.get(o.getGrupo()).add(o.getNombre());
            return new Catalogos(unidades,personal,actas,permisos,todas,opciones);
        });
    }
    public Integer asignarUnidad(SesionUsuario sesion,Integer emergenciaId,Tripulacion datos) {
        return transaccion(s -> {
            Usuario usuario=autorizar(s,sesion);
            return asignarUnidad(s,usuario,emergenciaId,datos);
        });
    }
    public static Integer asignarUnidad(Session s, Usuario usuario, Integer emergenciaId, Tripulacion datos) {
            if (!usuario.getActivo() || usuario.getRol()==Rol.BOMBERO) throw new SecurityException("Se requiere acceso de despacho.");
            Unidad unidad=reservarUnidad(s,datos);
            Emergencia emergencia=s.find(Emergencia.class,exigirId(emergenciaId),LockModeType.PESSIMISTIC_WRITE);
            if (emergencia==null || emergencia.getEstado()==EstadoEmergencia.CERRADA)
                throw new IllegalArgumentException("Seleccione una emergencia abierta.");
            SalidaUnidad salida=crear(s,emergencia,null,unidad,usuario.getId(),datos);
            if (emergencia.getEstado()==EstadoEmergencia.PENDIENTE) emergencia.cambiarEstado(EstadoEmergencia.EN_PROCESO);
            s.persist(salida); return salida.getId();
    }
    public Integer registrarServicio(SesionUsuario sesion,String escuadraDepartamento,TipoServicio tipo,String detalle,Tripulacion datos) {
        return registrarServicio(sesion,escuadraDepartamento,tipo==null ? null : tipo.name(),detalle,datos);
    }
    public Integer registrarServicio(SesionUsuario sesion,String escuadraDepartamento,String tipo,String detalle,Tripulacion datos) {
        return transaccion(s -> {
            Usuario usuario=autorizar(s,sesion);
            Unidad unidad=reservarUnidad(s,datos);
            ConfiguracionService.validar(s,GrupoCatalogo.TIPO_SERVICIO,tipo,null);
            ConfiguracionService.validar(s,GrupoCatalogo.DEPARTAMENTO,escuadraDepartamento,null);
            OtroServicio servicio=new OtroServicio(escuadraDepartamento,tipo,detalle); s.persist(servicio);
            SalidaUnidad salida=crear(s,null,servicio,unidad,usuario.getId(),datos);
            s.persist(salida); return salida.getId();
        });
    }
    private static Unidad reservarUnidad(Session s,Tripulacion datos) {
        if (datos==null) throw new IllegalArgumentException("Ingrese los datos de la salida.");
        Unidad unidad=s.find(Unidad.class,exigirId(datos.unidadId()),LockModeType.PESSIMISTIC_WRITE);
        if (unidad==null || unidad.getEstado()!=EstadoUnidad.DISPONIBLE || !s.createSelectionQuery(
                "select id from SalidaUnidad where unidad.id=:id and retorno is null",Integer.class)
                .setParameter("id",unidad.getId()).setMaxResults(1).getResultList().isEmpty())
            throw new IllegalArgumentException("La unidad está ocupada, en mantenimiento o ya no existe. Actualice la lista.");
        if (datos.salida()==null) throw new IllegalArgumentException("Seleccione la fecha y hora de salida.");
        if (!s.createSelectionQuery("select id from SalidaUnidad where unidad.id=:id and retorno>:salida",Integer.class)
                .setParameter("id",unidad.getId()).setParameter("salida",datos.salida()).setMaxResults(1).getResultList().isEmpty())
            throw new IllegalArgumentException("La salida es anterior al último retorno de esa unidad. Revise la fecha y hora.");
        // Bloqueo + índice único parcial: ambas actividades comparten la misma disponibilidad.
        return unidad;
    }
    private static SalidaUnidad crear(Session s,Emergencia e,OtroServicio servicio,Unidad unidad,Integer usuario,Tripulacion datos) {
        Set<Integer> ids=new TreeSet<>(datos.participantes()); ids.add(exigirId(datos.operadorId()));
        if (datos.jefeId()!=null) ids.add(exigirId(datos.jefeId()));
        Map<Integer,Bombero> personal=new LinkedHashMap<>();
        for (Integer id:ids) {
            Bombero b=s.find(Bombero.class,exigirId(id),LockModeType.PESSIMISTIC_WRITE);
            if (b==null || b.getEstado()!=EstadoBombero.DISPONIBLE) throw new IllegalArgumentException("Un integrante ya no está disponible. Actualice la tripulación.");
            if (!s.createSelectionQuery("select x.id from SalidaUnidad x join x.participantes p where p.id=:id and x.retorno>:salida",Integer.class)
                    .setParameter("id",id).setParameter("salida",datos.salida()).setMaxResults(1).getResultList().isEmpty())
                throw new IllegalArgumentException("La salida es anterior al último retorno de un integrante.");
            b.setEstado(e==null ? EstadoBombero.EN_SERVICIO : EstadoBombero.EN_EMERGENCIA);
            personal.put(id,b);
        }
        if (!personal.get(datos.operadorId()).puedeOperar(unidad.getId()))
            throw new IllegalArgumentException("El operador no está autorizado para esta unidad. Revisa Configuración.");
        SalidaUnidad salida=new SalidaUnidad(e,servicio,unidad,personal.get(datos.operadorId()),personal.get(datos.jefeId()),personal.values(),usuario,datos.salida());
        unidad.setEstado(e==null ? EstadoUnidad.EN_SERVICIO : EstadoUnidad.EN_EMERGENCIA); return salida;
    }
    public void marcarLlegada(SesionUsuario sesion,Integer salidaId,LocalDateTime fecha) {
        transaccion(s -> { autorizar(s,sesion); bloquearSalida(s,salidaId).marcarLlegada(fecha); return null; });
    }
    public void registrarRetorno(SesionUsuario sesion,Integer salidaId,LocalDateTime fecha) {
        transaccion(s -> {
            autorizar(s,sesion); SalidaUnidad salida=bloquearSalida(s,salidaId);
            salida.registrarRetorno(fecha);
            salida.getUnidad().setEstado(EstadoUnidad.DISPONIBLE);
            s.flush();
            for (Integer id : salida.getParticipantes().stream().map(Bombero::getId).sorted().toList()) {
                Bombero b=s.find(Bombero.class,id,LockModeType.PESSIMISTIC_WRITE);
                boolean libre=s.createSelectionQuery("select x.id from SalidaUnidad x join x.participantes p where p.id=:id and x.retorno is null",Integer.class)
                        .setParameter("id",id).setMaxResults(1).getResultList().isEmpty();
                if (libre && b.getEstado()!=EstadoBombero.DE_BAJA) b.setEstado(EstadoBombero.DISPONIBLE);
            }
            return null;
        });
    }
    private SalidaUnidad bloquearSalida(Session s,Integer id) {
        Integer unidadId=s.createSelectionQuery("select unidad.id from SalidaUnidad where id=:id",Integer.class)
            .setParameter("id",exigirId(id)).uniqueResult();
        if (unidadId==null) throw new IllegalArgumentException("La salida no existe.");
        s.find(Unidad.class,unidadId,LockModeType.PESSIMISTIC_WRITE);
        return s.find(SalidaUnidad.class,id,LockModeType.PESSIMISTIC_WRITE);
    }
    public void cerrarEmergencia(SesionUsuario sesion,Integer emergenciaId) {
        cerrarEmergencia(sesion, emergenciaId, null, null, null);
    }

    public void cerrarEmergencia(SesionUsuario sesion, Integer emergenciaId, Long version,
                                  String descripcion, ResultadoEmergencia resultado) {
        transaccion(s -> {
            autorizar(s,sesion);
            Emergencia e=s.find(Emergencia.class,exigirId(emergenciaId),LockModeType.PESSIMISTIC_WRITE);
            if (e==null) throw new IllegalArgumentException("La emergencia no existe.");
            if (!s.createSelectionQuery("select id from SalidaUnidad where emergencia.id=:id and retorno is null",Integer.class)
                    .setParameter("id",emergenciaId).setMaxResults(1).getResultList().isEmpty())
                throw new IllegalArgumentException("Hay unidades que siguen fuera. Registre sus retornos antes de cerrar.");
            // Los registros antiguos no contienen horarios: no inventar retornos ni liberar recursos.
            if (!s.createSelectionQuery("select id from Asignacion where emergencia.id=:id",Integer.class)
                    .setParameter("id",emergenciaId).setMaxResults(1).getResultList().isEmpty())
                throw new IllegalArgumentException("Esta emergencia tiene asignaciones antiguas; revise sus retornos antes de migrarla al nuevo despacho.");
            if (version != null) {
                if (e.getVersion() != version) {
                    throw new IllegalStateException("El acta cambió. Vuelva a abrirla antes de cerrar.");
                }
                e.prepararCierre(descripcion, resultado);
            } else if (e.getNumeroActa() != null) {
                throw new IllegalArgumentException("Cierre el acta desde su ficha para guardar el resultado y la descripción final.");
            }
            e.cambiarEstado(EstadoEmergencia.CERRADA); return null;
        });
    }
    public List<Salida> consultarSalidas(SesionUsuario sesion,Integer emergenciaId) {
        return consultarSalidas(sesion, List.of(exigirId(emergenciaId)));
    }
    public List<Salida> consultarSalidas(SesionUsuario sesion, Collection<Integer> emergencias) {
        if (emergencias.isEmpty()) return List.of();
        return consultar(sesion, " where x.emergencia.id in :actas", null, emergencias);
    }
    public List<Salida> listarMes(SesionUsuario sesion,YearMonth mes,boolean otrosServicios) {
        Objects.requireNonNull(mes,"Seleccione el mes.");
        return consultar(sesion," where x.salida>=:desde and x.salida<:hasta and x."+
            (otrosServicios ? "otroServicio" : "emergencia")+" is not null",mes,null);
    }
    public List<Salida> listarActivas(SesionUsuario sesion,boolean otrosServicios) {
        return consultar(sesion," where x.retorno is null and x."+(otrosServicios ? "otroServicio" : "emergencia")+" is not null",null,null);
    }
    private List<Salida> consultar(SesionUsuario sesion,String filtro,YearMonth mes,Collection<Integer> actas) {
        return transaccion(s -> {
            autorizar(s,sesion);
            var q=s.createSelectionQuery("select distinct x from SalidaUnidad x join fetch x.unidad join fetch x.operador "+
                "left join fetch x.jefe left join fetch x.emergencia left join fetch x.otroServicio left join fetch x.participantes"+
                filtro+" order by x.salida desc, x.id desc",SalidaUnidad.class);
            if (mes!=null) { q.setParameter("desde",mes.atDay(1).atStartOfDay()); q.setParameter("hasta",mes.plusMonths(1).atDay(1).atStartOfDay()); }
            if (actas!=null) q.setParameterList("actas",actas);
            return q.getResultList().stream().map(this::registro).toList();
        });
    }
    private Salida registro(SalidaUnidad x) {
        OtroServicio o=x.getOtroServicio();
        return new Salida(x.getId(),x.getEmergencia()==null ? null : x.getEmergencia().getId(),o==null ? null : o.getId(),
            x.getUnidad().getId(),x.getUnidad().getCodigo(),persona(x.getOperador()),x.getJefe()==null ? null : persona(x.getJefe()),
            x.getParticipantes().stream().map(this::persona).sorted(Comparator.comparing(Persona::id)).toList(),
            x.getRegistradoPor(),x.getSalida(),x.getLlegada(),x.getRetorno(),o==null ? Objects.toString(x.getEmergencia().getEscuadra(),"") : o.getEscuadraDepartamento(),
            o==null ? null : o.getTipoNombre(),o==null ? x.getEmergencia().getDireccion() : o.getDetalle(),o==null ? x.getEmergencia().getNumeroActa() : null);
    }
    private Persona persona(Bombero b) { return new Persona(b.getId(),b.getNombre()+" "+b.getApellido()); }
    private static Integer exigirId(Integer id) {
        if (id==null || id<=0) throw new IllegalArgumentException("Seleccione un registro válido."); return id;
    }
}
