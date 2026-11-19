package edu.ni.uam.proyectofinalferrogest.service;

import java.util.List;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
public class EquipoService {
    private final EquipoDao dao = new EquipoDao();
    public List<Equipo> listar(SesionUsuario sesion) {
        return Transacciones.ejecutar(s -> { Acceso.usuario(s,sesion); return dao.listar(s); });
    }
    public List<Unidad> unidades(SesionUsuario sesion) {
        return Transacciones.ejecutar(s -> { Acceso.usuario(s,sesion); return new UnidadDao().listar(s); });
    }
    public void guardar(SesionUsuario sesion, Integer id, String nombre, String categoria,
                        int cantidad, EstadoEquipo estado) {
        String n = Validaciones.texto(nombre,"Nombre",100), c = Validaciones.texto(categoria,"Categoría",40);
        if (cantidad < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        if (estado == null) throw new IllegalArgumentException("Selecciona el estado del equipo.");
        Transacciones.ejecutar(s -> {
            Acceso.admin(s,sesion);
            Equipo e = id == null ? new Equipo() : dao.bloquear(s,id);
            long asignado=s.createSelectionQuery("select coalesce(sum(d.cantidad),0) from DotacionEquipo d where d.equipo.id=:id",Long.class)
                .setParameter("id",id==null ? -1 : id).getSingleResult();
            if (cantidad<asignado) throw new IllegalArgumentException("El inventario total no puede ser menor que las unidades ya distribuidas ("+asignado+"). Retira primero existencias de las unidades.");
            if (asignado>0 && estado!=EstadoEquipo.OPERATIVO) throw new IllegalArgumentException("Retira el equipo de las unidades antes de cambiar su estado.");
            ConfiguracionService.validar(s,GrupoCatalogo.CATEGORIA_EQUIPO,c,e.getCategoria());
            e.setNombre(n); e.setCategoria(c); e.setCantidad(cantidad); e.setEstado(estado);
            // ultimoMantenimiento solo cambia al registrar un mantenimiento.
            if (id == null) dao.crear(s,e); return null;
        });
    }
}
