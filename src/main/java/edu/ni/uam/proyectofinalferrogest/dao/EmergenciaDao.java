package edu.ni.uam.proyectofinalferrogest.dao;

import edu.ni.uam.proyectofinalferrogest.model.Emergencia;
import edu.ni.uam.proyectofinalferrogest.model.EstadoEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.PrioridadEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.TipoEmergencia;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class EmergenciaDao {
    public record Registro(Emergencia emergencia, String usuario) {
    }

    public record RecursoAsignado(String unidad, String nombre, String apellido) {
    }

    private static final String CONSULTA_REGISTROS = """
            select e, u.username
            from Emergencia e
            join fetch e.tipo
            left join Usuario u on u.id = e.usuarioId
            """;

    private final SessionFactory factory;

    public EmergenciaDao(SessionFactory factory) {
        this.factory = factory;
    }

    public List<Registro> listar(EstadoEmergencia estado, Integer tipoId,
                                LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la final.");
        }
        exigirUsuarioMapeado();
        return factory.fromSession(session -> {
            String hql = CONSULTA_REGISTROS + " where 1 = 1";
            if (estado != null) hql += " and e.estado = :estado";
            if (tipoId != null) hql += " and e.tipo.id = :tipo";
            if (desde != null) hql += " and e.fechaRegistro >= :desde";
            if (hasta != null) hql += " and e.fechaRegistro < :hasta";

            var consulta = session.createSelectionQuery(
                    hql + " order by e.fechaRegistro desc, e.id desc", Registro.class);
            if (estado != null) consulta.setParameter("estado", estado);
            if (tipoId != null) consulta.setParameter("tipo", tipoId);
            if (desde != null) consulta.setParameter("desde", desde.atStartOfDay());
            if (hasta != null) consulta.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
            return consulta.getResultList();
        });
    }

    public Registro buscar(Integer id) {
        exigirUsuarioMapeado();
        return factory.fromSession(session -> session.createSelectionQuery(
                CONSULTA_REGISTROS + " where e.id = :id", Registro.class)
                .setParameter("id", id)
                .uniqueResultOptional()
                .orElseThrow(() -> new IllegalStateException("La emergencia ya no existe.")));
    }

    public boolean asignacionesDisponibles() {
        return tieneEntidad("Asignacion");
    }

    public List<RecursoAsignado> listarAsignaciones(Integer id) {
        if (!asignacionesDisponibles()) {
            throw new IllegalStateException("La consulta requiere el mapeo de Asignacion.");
        }
        return factory.fromSession(session -> session.createSelectionQuery("""
                select u.codigo, b.nombre, b.apellido
                from Asignacion a
                left join a.unidad u
                left join a.bombero b
                where a.emergencia.id = :id
                order by a.id
                """, RecursoAsignado.class)
                .setParameter("id", id)
                .getResultList());
    }

    public void registrar(String direccion, String descripcion, Integer tipoId,
                          PrioridadEmergencia prioridad, Integer usuarioId) {
        registrar(direccion, descripcion, tipoId, prioridad, usuarioId, null, null);
    }
    public void registrar(String direccion, String descripcion, Integer tipoId,
                          PrioridadEmergencia prioridad, Integer usuarioId, Double latitud, Double longitud) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Inicie sesión para registrar una emergencia.");
        }
        exigirUsuarioMapeado();
        factory.inTransaction(session -> {
            boolean usuarioActivo = !session.createSelectionQuery("""
                    select u.id from Usuario u where u.id = :id and u.activo = true
                    """, Integer.class)
                    .setParameter("id", usuarioId)
                    .getResultList().isEmpty();
            if (!usuarioActivo) {
                throw new IllegalStateException("El usuario de la sesión ya no está activo.");
            }
            var emergencia = new Emergencia(direccion, descripcion, buscarTipo(session, tipoId), prioridad, usuarioId);
            emergencia.ubicar(latitud, longitud);
            session.persist(emergencia);
        });
    }

    public void actualizar(Integer id, long version, String direccion, String descripcion,
                           Integer tipoId, PrioridadEmergencia prioridad) {
        factory.inTransaction(session -> buscarVigente(session, id, version)
                .actualizarDatos(direccion, descripcion, buscarTipo(session, tipoId), prioridad));
    }

    public void actualizar(Integer id, long version, String direccion, String descripcion, Integer tipoId,
                           PrioridadEmergencia prioridad, Double latitud, Double longitud) {
        factory.inTransaction(session -> {
            var emergencia = buscarVigente(session, id, version);
            emergencia.actualizarDatos(direccion, descripcion, buscarTipo(session, tipoId), prioridad);
            emergencia.ubicar(latitud, longitud);
        });
    }
    public void eliminar(Integer id, long version) {
        factory.inTransaction(session -> {
            Emergencia emergencia = buscarVigente(session, id, version);
            if (emergencia.getEstado() != EstadoEmergencia.PENDIENTE) {
                throw new IllegalStateException("Solo se pueden eliminar emergencias pendientes.");
            }
            if (asignacionesDisponibles() && !session.createSelectionQuery("""
                    select a.id from Asignacion a where a.emergencia.id = :id
                    """, Object.class)
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList().isEmpty()) {
                throw new IllegalStateException("La emergencia tiene asignaciones registradas.");
            }
            session.remove(emergencia);
        });
    }

    private boolean tieneEntidad(String nombre) {
        return factory.getMetamodel().getEntities().stream()
                .anyMatch(entidad -> entidad.getName().equals(nombre));
    }

    private void exigirUsuarioMapeado() {
        if (!tieneEntidad("Usuario")) {
            throw new IllegalStateException("La consulta requiere el mapeo de Usuario.");
        }
    }

    private TipoEmergencia buscarTipo(Session session, Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Seleccione un tipo de emergencia.");
        }
        TipoEmergencia tipo = session.find(TipoEmergencia.class, id);
        if (tipo == null) {
            throw new IllegalStateException("El tipo seleccionado ya no existe.");
        }
        return tipo;
    }

    private Emergencia buscarVigente(Session session, Integer id, long version) {
        Emergencia emergencia = session.find(Emergencia.class, id);
        if (emergencia == null || emergencia.getVersion() != version) {
            throw new IllegalStateException("La emergencia cambió. Actualice el listado e intente de nuevo.");
        }
        return emergencia;
    }
}
