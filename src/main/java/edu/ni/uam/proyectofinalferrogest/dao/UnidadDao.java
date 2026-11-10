package edu.ni.uam.proyectofinalferrogest.dao;

import java.util.List;
import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.Unidad;
public class UnidadDao extends BaseDao<Unidad> {
    public UnidadDao() { super(Unidad.class); }
    public List<Unidad> listar(Session s) { return s.createQuery("from Unidad u order by u.codigo", Unidad.class).getResultList(); }

}