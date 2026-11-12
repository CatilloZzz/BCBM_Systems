package edu.ni.uam.proyectofinalferrogest.dao;

import java.util.List;
import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.MantenimientoEquipo;
public class MantenimientoEquipoDao extends BaseDao<MantenimientoEquipo> {
    public MantenimientoEquipoDao() { super(MantenimientoEquipo.class); }
    public List<MantenimientoEquipo> listar(Session s, Integer equipoId) {
        return s.createQuery("select m from MantenimientoEquipo m join fetch m.equipo join fetch m.registradoPor where m.equipo.id = :id order by m.fecha desc, m.id desc", MantenimientoEquipo.class)
                .setParameter("id", equipoId).getResultList();
    }
}

