package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.Rol;
import org.hibernate.SessionFactory;
import java.time.*;
import java.util.List;

/** Inventario al consultar y mantenimiento por período, dentro de una misma instantánea PostgreSQL. */
public class ReporteEquiposService {
    public record Inventario(int id,String equipo,String categoria,String estado,long total,long asignado,long disponible) {}
    public record Dotacion(String equipo,String unidad,long cantidad) {}
    public record Mantenimiento(String equipo,LocalDate fecha,LocalDate proxima,String descripcion,String responsable) {}
    public record Datos(LocalDate desde,LocalDate hasta,LocalDateTime generado,List<Inventario> inventario,
                        List<Dotacion> dotacion,List<Mantenimiento> pendientes,List<Mantenimiento> historial) {
        public Datos {
            inventario=List.copyOf(inventario); dotacion=List.copyOf(dotacion);
            pendientes=List.copyOf(pendientes); historial=List.copyOf(historial);
        }
        public long total() { return inventario.stream().mapToLong(Inventario::total).sum(); }
        public long asignado() { return inventario.stream().mapToLong(Inventario::asignado).sum(); }
        public long disponible() { return inventario.stream().mapToLong(Inventario::disponible).sum(); }
        public long reparacion() { return inventario.stream().filter(i->i.estado().equals("En reparación")).mapToLong(Inventario::total).sum(); }
        public long baja() { return inventario.stream().filter(i->i.estado().equals("De baja")).mapToLong(Inventario::total).sum(); }
        public long vencidos() { return pendientes.stream().filter(m->m.proxima().isBefore(generado.toLocalDate())).count(); }
        public String resumen() {
            return total()+" piezas · "+asignado()+" asignadas · "+disponible()+" disponibles para asignar · "+
                reparacion()+" en reparación · "+baja()+" de baja";
        }
    }
    private final SessionFactory factory;
    public ReporteEquiposService(SessionFactory factory) { this.factory=factory; }
    public Datos consultar(SesionUsuario sesion,LocalDate desde,LocalDate hasta) {
        if(desde==null || hasta==null || desde.isAfter(hasta))
            throw new IllegalArgumentException("Selecciona un período válido para mantenimiento.");
        return factory.fromTransaction(s->{
            s.doWork(c->{try(var st=c.createStatement()){st.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");}});
            if(Acceso.usuario(s,sesion).getRol()==Rol.BOMBERO) throw new SecurityException("Se requiere acceso de administración o despacho.");
            LocalDateTime generado=LocalDateTime.now();
            var inventario=s.createNativeQuery("""
                WITH asignadas AS (SELECT equipo_id,sum(cantidad) cantidad FROM dotacion_equipo GROUP BY equipo_id)
                SELECT e.id,e.nombre,e.categoria,
                  CASE e.estado WHEN 'OPERATIVO' THEN 'Operativo' WHEN 'EN_REPARACION' THEN 'En reparación' ELSE 'De baja' END,
                  e.cantidad,coalesce(a.cantidad,0),
                  CASE WHEN e.estado='OPERATIVO' THEN greatest(0,e.cantidad-coalesce(a.cantidad,0)) ELSE 0 END
                FROM equipo e LEFT JOIN asignadas a ON a.equipo_id=e.id ORDER BY lower(e.nombre),e.id
                """,Object[].class).getResultList().stream().map(r->new Inventario(((Number)r[0]).intValue(),
                    (String)r[1],(String)r[2],(String)r[3],numero(r[4]),numero(r[5]),numero(r[6]))).toList();
            var dotacion=s.createNativeQuery("""
                SELECT e.nombre,u.codigo,d.cantidad FROM dotacion_equipo d
                JOIN equipo e ON e.id=d.equipo_id JOIN unidad u ON u.id=d.unidad_id
                ORDER BY u.codigo,lower(e.nombre),e.id
                """,Object[].class).getResultList().stream().map(r->new Dotacion((String)r[0],(String)r[1],numero(r[2]))).toList();
            var pendientes=s.createNativeQuery("""
                WITH ultimas AS (
                  SELECT DISTINCT ON (equipo_id) * FROM mantenimiento_equipo ORDER BY equipo_id,fecha DESC,id DESC
                )
                SELECT e.nombre,m.fecha,m.proxima_fecha,m.descripcion,u.username FROM ultimas m
                JOIN equipo e ON e.id=m.equipo_id JOIN usuario u ON u.id=m.registrado_por
                WHERE e.estado<>'BAJA' AND m.proxima_fecha<=:hasta
                  AND (m.proxima_fecha>=:desde OR m.proxima_fecha<:hoy)
                ORDER BY m.proxima_fecha,lower(e.nombre)
                """,Object[].class).setParameter("desde",desde).setParameter("hasta",hasta)
                .setParameter("hoy",generado.toLocalDate()).getResultList().stream().map(ReporteEquiposService::mantenimiento).toList();
            var historial=s.createNativeQuery("""
                SELECT e.nombre,m.fecha,m.proxima_fecha,m.descripcion,u.username FROM mantenimiento_equipo m
                JOIN equipo e ON e.id=m.equipo_id JOIN usuario u ON u.id=m.registrado_por
                WHERE m.fecha BETWEEN :desde AND :hasta ORDER BY m.fecha DESC,m.id DESC
                """,Object[].class).setParameter("desde",desde).setParameter("hasta",hasta)
                .getResultList().stream().map(ReporteEquiposService::mantenimiento).toList();
            return new Datos(desde,hasta,generado,inventario,dotacion,pendientes,historial);
        });
    }
    private static long numero(Object n) { return ((Number)n).longValue(); }
    private static LocalDate fecha(Object value) { return value==null ? null : ((java.sql.Date)value).toLocalDate(); }
    private static Mantenimiento mantenimiento(Object[] r) {
        return new Mantenimiento((String)r[0],fecha(r[1]),fecha(r[2]),(String)r[3],(String)r[4]);
    }
}
