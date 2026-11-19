package edu.ni.uam.proyectofinalferrogest.dao;

import java.util.List;
import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.Equipo;
public class EquipoDao extends BaseDao<Equipo> {
    public EquipoDao() { super(Equipo.class); }
    public List<Equipo> listar(Session s) {
        var equipos=s.createQuery("from Equipo e order by e.nombre", Equipo.class).getResultList();
        var dotaciones=s.createQuery("select d from DotacionEquipo d join fetch d.equipo join fetch d.unidad order by d.unidad.codigo",edu.ni.uam.proyectofinalferrogest.model.DotacionEquipo.class).getResultList();
        var porEquipo=new java.util.HashMap<Integer,java.util.List<String>>();
        for (var dotacion:dotaciones)
            porEquipo.computeIfAbsent(dotacion.getEquipo().getId(),k->new java.util.ArrayList<>())
                .add(dotacion.getUnidad().getCodigo());
        equipos.forEach(e->e.setUnidadesAsignadas(String.join(", ",porEquipo.getOrDefault(e.getId(),java.util.List.of()))));
        return equipos;
    }

}
