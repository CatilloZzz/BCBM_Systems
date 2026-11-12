package edu.ni.uam.proyectofinalferrogest.dao;

import java.util.List;
import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.Equipo;
public class EquipoDao extends BaseDao<Equipo> {
    public EquipoDao() { super(Equipo.class); }
    public List<Equipo> listar(Session s) { return s.createQuery("select e from Equipo e left join fetch e.unidad order by e.nombre", Equipo.class).getResultList(); }

}