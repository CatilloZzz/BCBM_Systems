package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.Rol;
import org.hibernate.SessionFactory;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.List;

public class ReporteCabinaService {
    public record Fila(String seccion,String grupo,String tipo,long cantidad,long total,double porcentaje) {
        public Fila(String seccion,String grupo,String tipo,long cantidad,long total) {
            this(seccion,grupo,tipo,cantidad,total,0.0);
        }
    }
    private final SessionFactory factory;
    public ReporteCabinaService(SessionFactory factory) { this.factory=factory; }

    // El mes de un movimiento se determina por su salida y el de un acta por su apertura.
    private static final String CONSULTA = """
        WITH movimientos AS (
            SELECT s.id, s.unidad_id, CASE WHEN s.emergencia_id IS NOT NULL
                THEN concat('Emergencia · ',coalesce(t.nombre,'Sin clasificar'))
                ELSE concat('Servicio · ', initcap(replace(o.tipo,'_',' '))) END AS tipo
            FROM salida_unidad s LEFT JOIN emergencia e ON e.id=s.emergencia_id
            LEFT JOIN tipo_emergencia t ON t.id=e.tipo_id LEFT JOIN otro_servicio o ON o.id=s.otro_servicio_id
            WHERE s.salida>=:desde AND s.salida<:hasta
        ), filas AS (
            SELECT 'Unidades' AS seccion,u.codigo AS grupo,coalesce(m.tipo,'Sin salidas') AS tipo,count(m.id) AS cantidad,
                   sum(count(m.id)) OVER (PARTITION BY u.id) AS total,
                   CASE WHEN sum(count(m.id)) OVER (PARTITION BY u.id)=0 THEN 0.0
                        ELSE CAST(count(m.id) AS double precision)/sum(count(m.id)) OVER (PARTITION BY u.id) END AS porcentaje
            FROM unidad u LEFT JOIN movimientos m ON m.unidad_id=u.id GROUP BY u.id,u.codigo,m.tipo
            UNION ALL
            SELECT 'Personal',concat(b.rango,' ',b.nombre,' ',b.apellido,' (#',b.id,')'),m.tipo,count(*),
                   sum(count(*)) OVER (PARTITION BY b.id),
                   CAST(count(*) AS double precision)/sum(count(*)) OVER (PARTITION BY b.id)
            FROM movimientos m JOIN salida_participante sp ON sp.salida_id=m.id JOIN bombero b ON b.id=sp.bombero_id
            GROUP BY b.id,b.rango,b.nombre,b.apellido,m.tipo
        ), actas AS (
            SELECT coalesce(t.nombre,'Sin clasificar') AS grupo,count(*) AS cantidad
            FROM emergencia e LEFT JOIN tipo_emergencia t ON t.id=e.tipo_id
            WHERE e.fecha_hora>=:desde AND e.fecha_hora<:hasta GROUP BY t.id,t.nombre
        ), emergencias AS (
            SELECT 'Emergencias' AS seccion,grupo,'Actas registradas' AS tipo,cantidad,
                   sum(cantidad) OVER () AS total,
                   CAST(cantidad AS double precision)/sum(cantidad) OVER () AS porcentaje
            FROM actas
        )
        SELECT seccion,grupo,tipo,cantidad,total,porcentaje FROM (
            SELECT * FROM filas UNION ALL SELECT * FROM emergencias
        ) reporte ORDER BY seccion,total DESC,grupo,cantidad DESC,tipo
        """;

    public List<Fila> consultar(SesionUsuario sesion,YearMonth mes) {
        return consultar(sesion,mes.atDay(1),mes.atEndOfMonth());
    }

    public List<Fila> consultar(SesionUsuario sesion,LocalDate desde,LocalDate hasta) {
        if (desde==null || hasta==null || desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la fecha final.");
        return factory.fromTransaction(s -> {
            if (Acceso.usuario(s,sesion).getRol()==Rol.BOMBERO) throw new SecurityException("Se requiere acceso de cabina.");
            return s.createNativeQuery(CONSULTA,Object[].class)
                .setParameter("desde",desde.atStartOfDay()).setParameter("hasta",hasta.plusDays(1).atStartOfDay())
                .getResultList().stream().map(r -> new Fila((String)r[0],(String)r[1],(String)r[2],
                    ((Number)r[3]).longValue(),((Number)r[4]).longValue(),((Number)r[5]).doubleValue())).toList();
        });
    }
}
