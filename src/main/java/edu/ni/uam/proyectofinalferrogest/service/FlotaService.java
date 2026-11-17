package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
import java.util.List;

public class FlotaService {
    private final org.hibernate.SessionFactory factory;
    public FlotaService(org.hibernate.SessionFactory factory) { this.factory=factory; }
    public record Inventario(List<Unidad> unidades, List<Equipo> equipos) {}
    public Inventario consultar(SesionUsuario sesion) {
        return Transacciones.ejecutar(factory,s -> {
            Acceso.usuario(s,sesion);
            return new Inventario(new UnidadDao().listar(s), new EquipoDao().listar(s));
        });
    }
    public int guardar(SesionUsuario sesion,Integer id,String codigo,String tipo,EstadoUnidad estado) {
        String c=Validaciones.texto(codigo,"Código",20), t=Validaciones.texto(tipo,"Tipo",30);
        if (estado==null || estado.enSalida())
            throw new IllegalArgumentException("El despacho controla el estado de las unidades en salida.");
        return Transacciones.ejecutar(factory,s -> {
            Acceso.admin(s,sesion);
            Unidad unidad=id==null ? new Unidad() : new UnidadDao().bloquear(s,id);
            if (unidad.getEstado().enSalida())
                throw new IllegalArgumentException("La unidad se encuentra fuera.");
            unidad.setCodigo(c); unidad.setTipo(t); unidad.setEstado(estado);
            if (id==null) s.persist(unidad);
            return unidad.getId();
        });
    }
    public void moverEquipo(SesionUsuario sesion,int unidadId,int equipoId,boolean asignar) {
        Transacciones.ejecutar(factory,s -> {
            Acceso.admin(s,sesion);
            Equipo equipo=new EquipoDao().bloquear(s,equipoId);
            Unidad unidad=new UnidadDao().bloquear(s,unidadId);
            if (unidad.getEstado().enSalida())
                throw new IllegalArgumentException("Registra el retorno antes de cambiar la dotación.");
            if (asignar && (equipo.getUnidad()!=null || equipo.getCantidad()<1 || equipo.getEstado()!=EstadoEquipo.OPERATIVO))
                throw new IllegalArgumentException("El recurso ya no está disponible.");
            if (!asignar && (equipo.getUnidad()==null || !equipo.getUnidad().getId().equals(unidadId)))
                throw new IllegalArgumentException("El recurso ya no pertenece a esa unidad.");
            equipo.setUnidad(asignar ? unidad : null);
            return null;
        });
    }
}
