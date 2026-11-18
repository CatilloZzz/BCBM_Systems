package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
import org.hibernate.*;
import java.util.*;

public class ConfiguracionService {
    private final SessionFactory factory;
    public ConfiguracionService(SessionFactory factory) { this.factory=factory; }
    public record Operador(Integer id,String nombre,boolean todas,Set<Integer> unidades) {
        @Override public String toString() { return nombre; }
    }
    public record Permisos(List<Operador> personal,List<Unidad> unidades) {}
    public List<OpcionCatalogo> listar(SesionUsuario sesion,GrupoCatalogo grupo) {
        return factory.fromTransaction(s -> {
            Acceso.admin(s,sesion);
            return s.createSelectionQuery("from OpcionCatalogo where grupo=:grupo order by lower(nombre)",OpcionCatalogo.class)
                .setParameter("grupo",grupo).getResultList();
        });
    }
    public Map<GrupoCatalogo,List<String>> opciones(SesionUsuario sesion) {
        return factory.fromTransaction(s -> {
            Acceso.usuario(s,sesion);
            Map<GrupoCatalogo,List<String>> datos=new EnumMap<>(GrupoCatalogo.class);
            for (var g:GrupoCatalogo.values()) datos.put(g,new ArrayList<>());
            for (var o:s.createSelectionQuery("from OpcionCatalogo where activo=true order by lower(nombre)",OpcionCatalogo.class).getResultList())
                datos.get(o.getGrupo()).add(o.getNombre());
            return datos;
        });
    }
    public void guardar(SesionUsuario sesion,Integer id,long version,GrupoCatalogo grupo,String nombre,boolean activo) {
        String n=Validaciones.texto(nombre,"Nombre",grupo.limite());
        Transacciones.ejecutar(factory,s -> {
            Acceso.admin(s,sesion);
            OpcionCatalogo opcion=id==null ? new OpcionCatalogo(grupo,n) : s.find(OpcionCatalogo.class,id,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (opcion==null || opcion.getVersion()!=version || opcion.getGrupo()!=grupo)
                throw new IllegalArgumentException("El catálogo cambió. Actualiza antes de guardar.");
            if (id!=null && !opcion.getNombre().equals(n) && enUso(s,grupo,opcion.getNombre()))
                throw new IllegalArgumentException("Este nombre tiene registros asociados. Puedes desactivarlo y crear su reemplazo.");
            opcion.cambiar(n,activo);
            if(id==null) s.persist(opcion);
            return null;
        });
    }
    private static String campo(GrupoCatalogo grupo) {
        return switch(grupo) {
            case RANGO -> "Bombero where rango"; case COMPANIA -> "Bombero where compania";
            case CATEGORIA_EQUIPO -> "Equipo where categoria"; case TIPO_UNIDAD -> "Unidad where tipo";
            case DEPARTAMENTO -> "OtroServicio where escuadraDepartamento"; case TIPO_SERVICIO -> "OtroServicio where tipo";
        };
    }
    private static boolean enUso(Session s,GrupoCatalogo grupo,String nombre) {
        return !s.createSelectionQuery("select id from "+campo(grupo)+"=:nombre",Integer.class)
            .setParameter("nombre",nombre).setMaxResults(1).getResultList().isEmpty();
    }
    public static void validar(Session s,GrupoCatalogo grupo,String nombre,String anterior) {
        if (Objects.equals(nombre,anterior)) return;
        if (nombre==null || s.createSelectionQuery("select count(*) from OpcionCatalogo where grupo=:grupo and nombre=:nombre and activo=true",Long.class)
            .setParameter("grupo",grupo).setParameter("nombre",nombre).getSingleResult()==0)
            throw new IllegalArgumentException("Selecciona una opción activa de "+grupo+". Se administra en Configuración.");
    }
    public Permisos permisos(SesionUsuario sesion) {
        return factory.fromTransaction(s -> {
            Acceso.admin(s,sesion);
            var personal=new BomberoDao().listar(s).stream().map(b -> new Operador(b.getId(),b.getNombre()+" "+b.getApellido(),
                b.isOperadorTodas(),b.getUnidadesOperador())).toList();
            return new Permisos(personal,new UnidadDao().listar(s));
        });
    }
    public void guardarOperador(SesionUsuario sesion,Integer id,boolean todas,Set<Integer> unidades) {
        if (id==null) throw new IllegalArgumentException("Selecciona una persona.");
        Set<Integer> seleccion=Set.copyOf(unidades);
        Transacciones.ejecutar(factory,s -> {
            Acceso.admin(s,sesion);
            Bombero bombero=new BomberoDao().bloquear(s,id);
            for (Integer unidad:seleccion) new UnidadDao().buscar(s,unidad);
            bombero.autorizarOperador(todas,seleccion);
            return null;
        });
    }
}
