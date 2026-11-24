package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.util.Transacciones;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Consultas agregadas para el panel de inicio. */
public class InicioService {
    public record PuntoDia(String dia, long cantidad) {}
    public record EstadoUnidadResumen(String nombre, long cantidad) {}
    public record EmergenciaReciente(String acta, String tipo, String direccion, LocalDateTime fecha, String estado) {}
    public record MantenimientoProximo(String equipo, LocalDate fecha, String descripcion) {}
    public record Resumen(boolean institucional, String nombreBombero, String estadoBombero,
                          long emergenciasActivas, long emergenciasHoy,
                          long unidadesDisponibles, long unidadesTotal,
                          long personalDisponible, long personalActivo,
                          long piezasInventario, long piezasReparacion,
                          List<PuntoDia> emergenciasSemana,
                          List<EstadoUnidadResumen> estadoUnidades,
                          List<EmergenciaReciente> recientes,
                          List<MantenimientoProximo> mantenimientos) {}

    public Resumen consultar(SesionUsuario sesion) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioSemana = hoy.minusDays(6).atStartOfDay();
        LocalDate finMantenimientos = hoy.plusDays(30);

        return Transacciones.ejecutar(s -> {
            Usuario usuario = Acceso.usuario(s, sesion);
            if (usuario.getRol() == Rol.BOMBERO) {
                Bombero bombero = usuario.getBombero();
                return new Resumen(false,
                        bombero == null ? usuario.getUsername() : bombero.getNombre() + " " + bombero.getApellido(),
                        bombero == null ? "Sin ficha de personal asociada" : bombero.getEstado().toString(),
                        0, 0, 0, 0, 0, 0, 0, 0,
                        List.of(), List.of(), List.of(), List.of());
            }

            long emergenciasActivas = s.createQuery(
                    "select count(e) from Emergencia e where e.estado <> :cerrada", Long.class)
                    .setParameter("cerrada", EstadoEmergencia.CERRADA).getSingleResult();
            long emergenciasHoy = s.createQuery(
                    "select count(e) from Emergencia e where e.fechaRegistro >= :inicio and e.fechaRegistro < :fin", Long.class)
                    .setParameter("inicio", inicioHoy).setParameter("fin", inicioManana).getSingleResult();
            long unidadesTotal = s.createQuery("select count(u) from Unidad u", Long.class).getSingleResult();
            long unidadesDisponibles = s.createQuery(
                    "select count(u) from Unidad u where u.estado = :estado", Long.class)
                    .setParameter("estado", EstadoUnidad.DISPONIBLE).getSingleResult();
            long personalActivo = s.createQuery(
                    "select count(b) from Bombero b where b.estado <> :baja", Long.class)
                    .setParameter("baja", EstadoBombero.DE_BAJA).getSingleResult();
            long personalDisponible = s.createQuery(
                    "select count(b) from Bombero b where b.estado = :estado", Long.class)
                    .setParameter("estado", EstadoBombero.DISPONIBLE).getSingleResult();
            long piezasInventario = suma(s.createQuery(
                    "select sum(e.cantidad) from Equipo e where e.estado <> :baja", Number.class)
                    .setParameter("baja", EstadoEquipo.BAJA).getSingleResult());
            long piezasReparacion = suma(s.createQuery(
                    "select sum(e.cantidad) from Equipo e where e.estado = :estado", Number.class)
                    .setParameter("estado", EstadoEquipo.EN_REPARACION).getSingleResult());

            Map<LocalDate, Long> porDia = new HashMap<>();
            List<Object[]> dias = s.createQuery("""
                    select year(e.fechaRegistro), month(e.fechaRegistro), day(e.fechaRegistro), count(e)
                    from Emergencia e
                    where e.fechaRegistro >= :inicio and e.fechaRegistro < :fin
                    group by year(e.fechaRegistro), month(e.fechaRegistro), day(e.fechaRegistro)
                    """, Object[].class)
                    .setParameter("inicio", inicioSemana).setParameter("fin", inicioManana).getResultList();
            for (Object[] fila : dias) {
                LocalDate fecha = LocalDate.of(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue(),
                        ((Number) fila[2]).intValue());
                porDia.put(fecha, ((Number) fila[3]).longValue());
            }
            List<PuntoDia> emergenciasSemana = new ArrayList<>();
            for (LocalDate fecha = hoy.minusDays(6); !fecha.isAfter(hoy); fecha = fecha.plusDays(1)) {
                emergenciasSemana.add(new PuntoDia(fecha.toString(), porDia.getOrDefault(fecha, 0L)));
            }

            List<Object[]> estados = s.createQuery(
                    "select u.estado, count(u) from Unidad u group by u.estado", Object[].class).getResultList();
            List<EstadoUnidadResumen> estadoUnidades = new ArrayList<>();
            for (Object[] fila : estados) {
                estadoUnidades.add(new EstadoUnidadResumen(nombre((EstadoUnidad) fila[0]),
                        ((Number) fila[1]).longValue()));
            }

            List<Object[]> ultimas = s.createQuery("""
                    select e.id, e.numeroActa, t.nombre, e.direccion, e.fechaRegistro, e.estado
                    from Emergencia e left join e.tipo t
                    order by e.fechaRegistro desc, e.id desc
                    """, Object[].class).setMaxResults(3).getResultList();
            List<EmergenciaReciente> recientes = new ArrayList<>();
            for (Object[] fila : ultimas) {
                String acta = fila[1] == null ? "Acta #" + fila[0] : fila[1].toString();
                recientes.add(new EmergenciaReciente(acta, Objects.toString(fila[2], "Sin clasificar"),
                        Objects.toString(fila[3], "Sin dirección"), (LocalDateTime) fila[4], fila[5].toString()));
            }

            List<Object[]> mantenimientosRows = s.createQuery("""
                    select e.nombre, m.proximaFecha, m.descripcion
                    from MantenimientoEquipo m join m.equipo e
                    where m.proximaFecha is not null and m.proximaFecha between :desde and :hasta
                      and e.estado <> :baja
                    order by m.proximaFecha, m.id
                    """, Object[].class)
                    .setParameter("desde", hoy).setParameter("hasta", finMantenimientos)
                    .setParameter("baja", EstadoEquipo.BAJA).setMaxResults(3).getResultList();
            List<MantenimientoProximo> mantenimientos = new ArrayList<>();
            for (Object[] fila : mantenimientosRows) {
                mantenimientos.add(new MantenimientoProximo(fila[0].toString(), (LocalDate) fila[1],
                        Objects.toString(fila[2], "Mantenimiento programado")));
            }

            return new Resumen(true, null, null, emergenciasActivas, emergenciasHoy,
                    unidadesDisponibles, unidadesTotal, personalDisponible, personalActivo,
                    piezasInventario, piezasReparacion, List.copyOf(emergenciasSemana),
                    List.copyOf(estadoUnidades), List.copyOf(recientes), List.copyOf(mantenimientos));
        });
    }

    private static long suma(Number valor) { return valor == null ? 0 : valor.longValue(); }

    private static String nombre(EstadoUnidad estado) {
        return switch (estado) {
            case DISPONIBLE -> "Disponible";
            case EN_EMERGENCIA -> "En emergencia";
            case EN_SERVICIO -> "En servicio";
            case MANTENIMIENTO -> "Mantenimiento";
        };
    }
}
