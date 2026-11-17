package edu.ni.uam.proyectofinalferrogest.dao;

import edu.ni.uam.proyectofinalferrogest.model.Emergencia;
import edu.ni.uam.proyectofinalferrogest.model.EstadoEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.TipoEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.Rol;
import edu.ni.uam.proyectofinalferrogest.model.Usuario;
import edu.ni.uam.proyectofinalferrogest.service.Acceso;
import edu.ni.uam.proyectofinalferrogest.service.SesionUsuario;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
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
            left join fetch e.tipo
            left join Usuario u on u.id = e.usuarioId
            """;

    private final SessionFactory factory;

    public List<Registro> listarMes(SesionUsuario sesion, YearMonth mes, String numero,
                                    String escuadra, Integer tipoId, EstadoEmergencia estado) {
        return factory.fromTransaction(session -> {
            autorizar(session, sesion);
            String hql = CONSULTA_REGISTROS + """
                    where e.fechaRegistro >= :desde and e.fechaRegistro < :hasta
                    """;
            boolean porNumero = numero != null && !numero.isBlank();
            boolean porEscuadra = escuadra != null && !escuadra.isBlank();
            if (porNumero) hql += " and lower(coalesce(e.numeroActa, concat('#', cast(e.id as String)))) = :numero";
            if (porEscuadra) hql += " and lower(e.escuadra) = :escuadra";
            if (tipoId != null) hql += " and e.tipo.id = :tipo";
            if (estado != null) hql += " and e.estado = :estado";
            var consulta = session.createSelectionQuery(hql + " order by e.fechaRegistro desc, e.id desc", Registro.class)
                    .setParameter("desde", mes.atDay(1).atStartOfDay())
                    .setParameter("hasta", mes.plusMonths(1).atDay(1).atStartOfDay());
            if (porNumero) consulta.setParameter("numero", numero.strip().toLowerCase(Locale.ROOT));
            if (porEscuadra) consulta.setParameter("escuadra", escuadra.strip().toLowerCase(Locale.ROOT));
            if (tipoId != null) consulta.setParameter("tipo", tipoId);
            if (estado != null) consulta.setParameter("estado", estado);
            return consulta.getResultList();
        });
    }

    public List<String> escuadras(SesionUsuario sesion) {
        return factory.fromTransaction(session -> {
            autorizar(session, sesion);
            return session.createSelectionQuery("""
                    select distinct e.escuadra from Emergencia e where e.escuadra is not null order by e.escuadra
                    """, String.class).getResultList();
        });
    }

    public Integer guardarActa(SesionUsuario sesion, Integer id, long version, Emergencia.DatosActa datos) {
        return factory.fromTransaction(session -> {
            Usuario usuario = autorizar(session, sesion);
            TipoEmergencia tipo = datos.tipoId() == null ? null : buscarTipo(session, datos.tipoId());
            if (id == null) {
                Emergencia emergencia = Emergencia.abrir(datos, tipo, usuario.getId());
                session.persist(emergencia);
                return emergencia.getId();
            }
            buscarVigente(session, id, version).completar(datos, tipo);
            return id;
        });
    }

    static Usuario autorizar(Session session, SesionUsuario sesion) {
        Usuario usuario = Acceso.usuario(session, sesion);
        if (usuario.getRol() != Rol.ADMIN && usuario.getRol() != Rol.DESPACHADOR) {
            throw new SecurityException("Solo administración y despacho pueden gestionar actas.");
        }
        return usuario;
    }

    public EmergenciaDao(SessionFactory factory) {
        this.factory = factory;
    }

    public Registro buscar(SesionUsuario sesion, Integer id) {
        return factory.fromTransaction(session -> {
            autorizar(session, sesion);
            return session.createSelectionQuery(
                    CONSULTA_REGISTROS + " where e.id = :id", Registro.class)
                    .setParameter("id", id)
                    .uniqueResultOptional()
                    .orElseThrow(() -> new IllegalStateException("La emergencia ya no existe."));
        });
    }

    private boolean asignacionesDisponibles() {
        return tieneEntidad("Asignacion");
    }

    public List<RecursoAsignado> listarAsignaciones(SesionUsuario sesion, Integer id) {
        if (!asignacionesDisponibles()) {
            throw new IllegalStateException("La consulta requiere el mapeo de Asignacion.");
        }
        return factory.fromTransaction(session -> {
            autorizar(session, sesion);
            return session.createSelectionQuery("""
                select u.codigo, b.nombre, b.apellido
                from Asignacion a
                left join a.unidad u
                left join a.bombero b
                where a.emergencia.id = :id
                order by a.id
                """, RecursoAsignado.class)
                .setParameter("id", id)
                .getResultList();
        });
    }

    public void eliminar(SesionUsuario sesion, Integer id, long version) {
        factory.inTransaction(session -> {
            autorizar(session, sesion);
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
            if (!session.createSelectionQuery("select id from SalidaUnidad where emergencia.id=:id",Integer.class)
                    .setParameter("id",id).setMaxResults(1).getResultList().isEmpty()) {
                throw new IllegalStateException("La emergencia tiene salidas registradas.");
            }
            session.remove(emergencia);
        });
    }

    private boolean tieneEntidad(String nombre) {
        return factory.getMetamodel().getEntities().stream()
                .anyMatch(entidad -> entidad.getName().equals(nombre));
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
