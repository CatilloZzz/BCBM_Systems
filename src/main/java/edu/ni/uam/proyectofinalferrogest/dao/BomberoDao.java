package edu.ni.uam.proyectofinalferrogest.dao;

import java.util.List;
import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.Bombero;
public class BomberoDao extends BaseDao<Bombero> {
    public BomberoDao() { super(Bombero.class); }
    public List<Bombero> listar(Session s) { return s.createQuery("from Bombero b order by b.apellido, b.nombre", Bombero.class).getResultList(); }

}