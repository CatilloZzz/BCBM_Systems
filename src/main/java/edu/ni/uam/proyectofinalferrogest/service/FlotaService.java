package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
import java.util.List;

public class FlotaService {
    private final org.hibernate.SessionFactory factory;
    public FlotaService(org.hibernate.SessionFactory factory) { this.factory=factory; }
    public record Inventario(List<Unidad> unidades, List<Equipo> equipos,List<DotacionEquipo> dotaciones) {}
    public Inventario consultar(SesionUsuario sesion) {
        return Transacciones.ejecutar(factory,s -> {
            Acceso.usuario(s,sesion);
            var dotaciones=s.createSelectionQuery("select d from DotacionEquipo d join fetch d.equipo join fetch d.unidad order by d.unidad.codigo,d.equipo.nombre",DotacionEquipo.class).getResultList();
            return new Inventario(new UnidadDao().listar(s), new EquipoDao().listar(s),dotaciones);
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
            ConsultasCabina.ConfiguracionService.validar(s,GrupoCatalogo.TIPO_UNIDAD,t,unidad.getTipo());
            unidad.setCodigo(c); unidad.setTipo(t); unidad.setEstado(estado);
            if (id==null) s.persist(unidad);
            return unidad.getId();
        });
    }
    public void moverEquipo(SesionUsuario sesion,int unidadId,int equipoId,int cantidad,boolean asignar) {
        if (cantidad<1) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        Transacciones.ejecutar(factory,s -> {
            Acceso.admin(s,sesion);
            Equipo equipo=new EquipoDao().bloquear(s,equipoId);
            Unidad unidad=new UnidadDao().bloquear(s,unidadId);
            if (unidad.getEstado().enSalida())
                throw new IllegalArgumentException("Registra el retorno antes de cambiar la dotación.");
            var existente=s.createSelectionQuery("from DotacionEquipo d where d.equipo.id=:equipo and d.unidad.id=:unidad",DotacionEquipo.class)
                .setParameter("equipo",equipoId).setParameter("unidad",unidadId).uniqueResult();
            if (asignar) {
                if (equipo.getEstado()!=EstadoEquipo.OPERATIVO) throw new IllegalArgumentException("Solo se puede asignar equipo operativo.");
                long asignado=s.createSelectionQuery("select coalesce(sum(d.cantidad),0) from DotacionEquipo d where d.equipo.id=:id",Long.class).setParameter("id",equipoId).getSingleResult();
                long disponible=equipo.getCantidad()-asignado;
                if (cantidad>disponible) throw new IllegalArgumentException("Solo hay "+disponible+" unidades disponibles de este recurso.");
                if (existente==null) s.persist(new DotacionEquipo(equipo,unidad,cantidad));
                else existente.setCantidad(Math.addExact(existente.getCantidad(),cantidad));
            } else {
                if (existente==null || cantidad>existente.getCantidad()) throw new IllegalArgumentException("La cantidad a retirar supera lo asignado a esa unidad.");
                if (cantidad==existente.getCantidad()) s.remove(existente); else existente.setCantidad(existente.getCantidad()-cantidad);
            }
            return null;
        });
    }
}
