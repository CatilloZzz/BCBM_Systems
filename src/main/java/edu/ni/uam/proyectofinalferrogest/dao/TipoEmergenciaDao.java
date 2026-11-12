package edu.ni.uam.proyectofinalferrogest.dao;

import edu.ni.uam.proyectofinalferrogest.model.TipoEmergencia;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class TipoEmergenciaDao {
    private final SessionFactory factory;

    public TipoEmergenciaDao(SessionFactory factory) {
        this.factory = factory;
    }

    public List<TipoEmergencia> listar() {
        return factory.fromSession(session -> session.createSelectionQuery(
                "from TipoEmergencia t order by lower(t.nombre), t.id", TipoEmergencia.class)
                .getResultList());
    }

    public void guardar(Integer id, long version, String nombre) {
        factory.inTransaction(session -> {
            if (id == null) {
                session.persist(new TipoEmergencia(nombre));
            } else {
                buscarVigente(session, id, version).renombrar(nombre);
            }
        });
    }

    public void eliminar(Integer id, long version) {
        factory.inTransaction(session -> {
            TipoEmergencia tipo = buscarVigente(session, id, version);
            boolean enUso = !session.createSelectionQuery(
                    "select e.id from Emergencia e where e.tipo.id = :id", Integer.class)
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList().isEmpty();
            if (enUso) {
                throw new IllegalStateException("El tipo está en uso por una emergencia.");
            }
            session.remove(tipo);
        });
    }

    private TipoEmergencia buscarVigente(Session session, Integer id, long version) {
        TipoEmergencia tipo = session.find(TipoEmergencia.class, id);
        if (tipo == null || tipo.getVersion() != version) {
            throw new IllegalStateException("El tipo cambió. Actualice el listado e intente de nuevo.");
        }
        return tipo;
    }
}
