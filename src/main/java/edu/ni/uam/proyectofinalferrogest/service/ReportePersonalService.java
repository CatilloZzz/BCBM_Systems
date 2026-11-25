package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.Rol;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.util.List;

/** Consulta la participación de cada bombero en salidas dentro de un período. */
public class ReportePersonalService {
    public record Fila(String persona, String organizacion, long emergencias,
                       long otrosServicios, long participaciones, double porcentaje) {}

    private static final String CONSULTA = """
        WITH conteos AS (
            SELECT b.id,
                   concat(b.rango, ' ', b.nombre, ' ', b.apellido) AS persona,
                   concat(coalesce(b.compania, 'Sin compañía'), ' · Escuadra ',
                          coalesce(b.escuadra, 'Sin asignar')) AS organizacion,
                   count(DISTINCT s.id) FILTER (WHERE s.emergencia_id IS NOT NULL) AS emergencias,
                   count(DISTINCT s.id) FILTER (WHERE s.otro_servicio_id IS NOT NULL) AS otros_servicios,
                   count(DISTINCT s.id) AS participaciones
            FROM salida_unidad s
            JOIN salida_participante sp ON sp.salida_id = s.id
            JOIN bombero b ON b.id = sp.bombero_id
            WHERE s.salida >= :desde AND s.salida < :hasta
            GROUP BY b.id, b.rango, b.nombre, b.apellido, b.compania, b.escuadra
        )
        SELECT persona, organizacion, emergencias, otros_servicios, participaciones,
               CASE WHEN sum(participaciones) OVER () = 0 THEN 0.0
                    ELSE participaciones::double precision / sum(participaciones) OVER () END
        FROM conteos
        ORDER BY participaciones DESC, persona
        """;

    private final SessionFactory factory;

    public ReportePersonalService(SessionFactory factory) {
        this.factory = factory;
    }

    public List<Fila> consultar(SesionUsuario sesion, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
        return factory.fromTransaction(s -> {
            if (Acceso.usuario(s, sesion).getRol() == Rol.BOMBERO) {
                throw new SecurityException("Se requiere acceso de cabina.");
            }
            return s.createNativeQuery(CONSULTA, Object[].class)
                    .setParameter("desde", desde.atStartOfDay())
                    .setParameter("hasta", hasta.plusDays(1).atStartOfDay())
                    .getResultList().stream()
                    .map(r -> new Fila((String) r[0], (String) r[1],
                            ((Number) r[2]).longValue(), ((Number) r[3]).longValue(),
                            ((Number) r[4]).longValue(), ((Number) r[5]).doubleValue()))
                    .toList();
        });
    }
}
