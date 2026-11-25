package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.EstadoEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.EstadoEmergenciaConverter;
import edu.ni.uam.proyectofinalferrogest.model.PrioridadEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.ResultadoEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.Rol;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Reporte de emergencias: actas abiertas en un período, filtradas por tipo, prioridad y estado.
 * Las actas se ubican en el período por su fecha de apertura. El estado y el resultado
 * corresponden al momento de la consulta.
 */
public class ReporteEmergenciasService {
    public static final String SIN_TIPO = "Sin clasificar";
    public static final String SIN_PRIORIDAD = "Sin prioridad";
    public static final String PENDIENTE_CIERRE = "Pendiente de cierre";
    public static final String SIN_RESULTADO = "Sin resultado";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Filtros elegidos en pantalla. Un filtro nulo significa "todos". */
    public record Filtro(LocalDate desde, LocalDate hasta, Integer tipoId, String tipo,
                         PrioridadEmergencia prioridad, EstadoEmergencia estado) {
        public Filtro {
            if (desde == null || hasta == null || desde.isAfter(hasta))
                throw new IllegalArgumentException("Selecciona fechas válidas: Desde no puede ser posterior a Hasta.");
            if (tipoId == null) tipo = null;
        }
        public Filtro(LocalDate desde, LocalDate hasta) { this(desde, hasta, null, null, null, null); }
        public String periodo() { return FECHA.format(desde) + " al " + FECHA.format(hasta); }
        public String descripcion() {
            return "Tipo: " + (tipoId == null ? "Todos" : tipo)
                    + " · Prioridad: " + (prioridad == null ? "Todas" : prioridad.toString())
                    + " · Estado: " + (estado == null ? "Todos" : estado.toString());
        }
    }

    /** Una fila por acta de emergencia. */
    public record Acta(int id, String numero, LocalDateTime apertura, String tipo, String prioridad,
                       String estado, String direccion, String resultado, LocalDateTime cierre) {
        public boolean abierta() { return !EstadoEmergencia.CERRADA.toString().equals(estado); }
        public String referencia() { return numero == null || numero.isBlank() ? "#" + id : numero; }
        public String direccionVisible() { return direccion == null || direccion.isBlank() ? "Sin dirección" : direccion; }
        public String resultadoVisible() { return resultado != null ? resultado : abierta() ? PENDIENTE_CIERRE : SIN_RESULTADO; }
    }

    /** Cantidad de actas de una categoría (tipo, prioridad, estado o resultado). */
    public record Conteo(String categoria, long actas, long abiertas, double porcentaje) {
        public long cerradas() { return actas - abiertas; }
    }

    public record Datos(Filtro filtro, LocalDateTime generado, List<Acta> actas, long abiertasAnteriores) {
        public Datos {
            actas = List.copyOf(actas);
        }
        public long total() { return actas.size(); }
        public long abiertas() { return actas.stream().filter(Acta::abierta).count(); }
        public long cerradas() { return total() - abiertas(); }

        /** Tipos ordenados del más frecuente al menos frecuente. */
        public List<Conteo> porTipo() {
            List<Conteo> conteos = new ArrayList<>(contar(Acta::tipo, List.of()));
            conteos.sort(Comparator.comparingLong(Conteo::actas).reversed()
                    .thenComparing(Conteo::categoria, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(conteos);
        }
        /** De la prioridad más grave a la más leve; incluye las prioridades sin actas. */
        public List<Conteo> porPrioridad() {
            return contar(Acta::prioridad, List.of(PrioridadEmergencia.CRITICA.toString(), PrioridadEmergencia.ALTA.toString(),
                    PrioridadEmergencia.MEDIA.toString(), PrioridadEmergencia.BAJA.toString()));
        }
        public List<Conteo> porEstado() {
            List<String> estados = new ArrayList<>();
            for (EstadoEmergencia estado : EstadoEmergencia.values()) estados.add(estado.toString());
            return contar(Acta::estado, estados);
        }
        public List<Conteo> porResultado() {
            List<String> resultados = new ArrayList<>();
            for (ResultadoEmergencia resultado : ResultadoEmergencia.values()) resultados.add(resultado.toString());
            return contar(Acta::resultadoVisible, resultados);
        }
        /** Tipo o tipos con más actas en el período. */
        public List<Conteo> masFrecuentes() {
            List<Conteo> tipos = porTipo();
            if (tipos.isEmpty()) return List.of();
            long maximo = tipos.get(0).actas();
            return tipos.stream().filter(c -> c.actas() == maximo).toList();
        }
        public String resumen() {
            return total() + (total() == 1 ? " acta · " : " actas · ") + abiertas()
                    + (abiertas() == 1 ? " sigue abierta · " : " siguen abiertas · ") + cerradas()
                    + (cerradas() == 1 ? " cerrada" : " cerradas");
        }
        public String frecuencia() {
            List<Conteo> lideres = masFrecuentes();
            if (lideres.isEmpty()) return "Sin actas para los filtros seleccionados.";
            Conteo primero = lideres.get(0);
            String nombres = String.join(", ", lideres.stream().map(Conteo::categoria).toList());
            return (lideres.size() == 1 ? "Tipo más frecuente: " : "Tipos más frecuentes: ") + nombres + " ("
                    + primero.actas() + (primero.actas() == 1 ? " acta; " : " actas; ")
                    + String.format(java.util.Locale.forLanguageTag("es-NI"), "%.1f%%", primero.porcentaje() * 100)
                    + " del período)";
        }
        public String pendientes() {
            return abiertasAnteriores + (abiertasAnteriores == 1 ? " acta abierta" : " actas abiertas")
                    + " de fechas anteriores al período (mismos filtros de tipo, prioridad y estado).";
        }

        private List<Conteo> contar(Function<Acta, String> categoria, List<String> fijas) {
            Map<String, long[]> conteo = new LinkedHashMap<>();
            for (String fija : fijas) conteo.put(fija, new long[2]);
            for (Acta acta : actas) {
                long[] valores = conteo.computeIfAbsent(categoria.apply(acta), c -> new long[2]);
                valores[0]++;
                if (acta.abierta()) valores[1]++;
            }
            long total = total();
            List<Conteo> filas = new ArrayList<>();
            conteo.forEach((nombre, valores) -> filas.add(new Conteo(nombre, valores[0], valores[1],
                    total == 0 ? 0.0 : (double) valores[0] / total)));
            return List.copyOf(filas);
        }
    }

    private final SessionFactory factory;

    public ReporteEmergenciasService(SessionFactory factory) {
        this.factory = factory;
    }

    public Datos consultar(SesionUsuario sesion, Filtro filtro) {
        if (filtro == null) throw new IllegalArgumentException("Selecciona el período del reporte.");
        EstadoEmergenciaConverter estados = new EstadoEmergenciaConverter();
        return factory.fromTransaction(s -> {
            // Misma instantánea para el listado y el conteo de actas abiertas anteriores.
            s.doWork(c -> { try (var st = c.createStatement()) { st.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ"); } });
            if (Acceso.usuario(s, sesion).getRol() == Rol.BOMBERO)
                throw new SecurityException("Se requiere acceso de administración o despacho.");
            LocalDateTime generado = LocalDateTime.now();

            StringBuilder condiciones = new StringBuilder();
            if (filtro.tipoId() != null) condiciones.append(" AND e.tipo_id = :tipo");
            if (filtro.prioridad() != null) condiciones.append(" AND e.prioridad = :prioridad");

            StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.numero_acta, e.fecha_hora, coalesce(t.nombre, :sinTipo), e.prioridad,
                       e.estado, e.direccion, e.resultado, e.fecha_cierre
                FROM emergencia e LEFT JOIN tipo_emergencia t ON t.id = e.tipo_id
                WHERE e.fecha_hora >= :desde AND e.fecha_hora < :hasta
                """).append(condiciones);
            if (filtro.estado() != null) sql.append(" AND e.estado = :estado");
            sql.append(" ORDER BY e.fecha_hora DESC, e.id DESC");

            var consulta = s.createNativeQuery(sql.toString(), Object[].class)
                    .setParameter("sinTipo", SIN_TIPO)
                    .setParameter("desde", filtro.desde().atStartOfDay())
                    .setParameter("hasta", filtro.hasta().plusDays(1).atStartOfDay());
            if (filtro.tipoId() != null) consulta.setParameter("tipo", filtro.tipoId());
            if (filtro.prioridad() != null) consulta.setParameter("prioridad", filtro.prioridad().name());
            if (filtro.estado() != null) consulta.setParameter("estado", estados.convertToDatabaseColumn(filtro.estado()));
            List<Acta> actas = consulta.getResultList().stream().map(r -> new Acta(
                    ((Number) r[0]).intValue(), (String) r[1], fechaHora(r[2]), (String) r[3],
                    prioridad((String) r[4]), estados.convertToEntityAttribute((String) r[5]).toString(),
                    (String) r[6], resultado((String) r[7]), fechaHora(r[8]))).toList();

            long anteriores = 0;
            if (filtro.estado() != EstadoEmergencia.CERRADA) {
                String conteo = "SELECT count(*) FROM emergencia e WHERE e.fecha_hora < :desde AND e.estado <> 'CERRADA'" + condiciones;
                if (filtro.estado() != null) conteo += " AND e.estado = :estado";
                var abiertas = s.createNativeQuery(conteo, Long.class)
                        .setParameter("desde", filtro.desde().atStartOfDay());
                if (filtro.tipoId() != null) abiertas.setParameter("tipo", filtro.tipoId());
                if (filtro.prioridad() != null) abiertas.setParameter("prioridad", filtro.prioridad().name());
                if (filtro.estado() != null) abiertas.setParameter("estado", estados.convertToDatabaseColumn(filtro.estado()));
                anteriores = ((Number) abiertas.getSingleResult()).longValue();
            }
            return new Datos(filtro, generado, actas, anteriores);
        });
    }

    private static LocalDateTime fechaHora(Object valor) {
        if (valor == null) return null;
        if (valor instanceof LocalDateTime fecha) return fecha;
        if (valor instanceof java.sql.Timestamp fecha) return fecha.toLocalDateTime();
        throw new IllegalStateException("Fecha con formato inesperado: " + valor.getClass().getName());
    }

    private static String prioridad(String valor) {
        return valor == null ? SIN_PRIORIDAD : PrioridadEmergencia.valueOf(valor).toString();
    }

    private static String resultado(String valor) {
        return valor == null ? null : ResultadoEmergencia.valueOf(valor).toString();
    }
}
