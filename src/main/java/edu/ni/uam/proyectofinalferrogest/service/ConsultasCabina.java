package edu.ni.uam.proyectofinalferrogest.service;

import com.google.gson.*;
import edu.ni.uam.proyectofinalferrogest.dao.BomberoDao;
import edu.ni.uam.proyectofinalferrogest.dao.UnidadDao;
import edu.ni.uam.proyectofinalferrogest.model.Bombero;
import edu.ni.uam.proyectofinalferrogest.model.GrupoCatalogo;
import edu.ni.uam.proyectofinalferrogest.model.OpcionCatalogo;
import edu.ni.uam.proyectofinalferrogest.model.Unidad;
import edu.ni.uam.proyectofinalferrogest.util.Transacciones;
import edu.ni.uam.proyectofinalferrogest.util.Validaciones;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class ConsultasCabina {
    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6)).build();
    private static final Map<String,List<Lugar>> CACHE = new LinkedHashMap<>();
    private static long ultimaBusqueda;
    private ConsultasCabina() {}

    public record Lugar(String nombre, double latitud, double longitud) {
        @Override public String toString() { return nombre; }
    }

    private static CompletableFuture<JsonElement> obtener(String url) {
        var peticion = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("User-Agent", "BCBM-Systems/1.0 (https://github.com/CatilloZzz/BCBM_Systems)")
                .header("Accept", "application/json").GET().build();
        return CLIENTE.sendAsync(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(respuesta -> {
                    if (respuesta.statusCode() != 200)
                        throw new IllegalStateException("Servicio externo HTTP " + respuesta.statusCode());
                    return JsonParser.parseString(respuesta.body());
                });
    }

    public static CompletableFuture<String> clima() {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=12.1364&longitude=-86.2514"
                + "&timezone=America%2FManagua&forecast_days=1&daily=precipitation_probability_max"
                + "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_direction_10m";
        return obtener(url).thenApply(json -> {
            var raiz = json.getAsJsonObject();
            var actual = raiz.getAsJsonObject("current");
            var lluvia = raiz.getAsJsonObject("daily").getAsJsonArray("precipitation_probability_max").get(0);
            return String.format(Locale.forLanguageTag("es-NI"),
                    "%.1f °C   ·   Humedad %d %%   ·   Viento %.1f km/h a %d°%nPrecipitación %.1f mm   ·   Probabilidad máxima de lluvia hoy %s%nHora del dato en Managua: %s",
                    actual.get("temperature_2m").getAsDouble(), actual.get("relative_humidity_2m").getAsInt(),
                    actual.get("wind_speed_10m").getAsDouble(), actual.get("wind_direction_10m").getAsInt(),
                    actual.get("precipitation").getAsDouble(), lluvia.isJsonNull() ? "sin dato" : lluvia.getAsInt()+" %",
                    actual.get("time").getAsString().replace('T',' '));
        });
    }

    public static synchronized CompletableFuture<List<Lugar>> buscar(String referencia) {
        String clave = referencia.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (CACHE.containsKey(clave)) return CompletableFuture.completedFuture(CACHE.get(clave));
        long ahora = System.nanoTime();
        if (ultimaBusqueda != 0 && ahora - ultimaBusqueda < 1_100_000_000L)
            return CompletableFuture.failedFuture(new IllegalStateException("Espera un segundo para buscar."));
        ultimaBusqueda = ahora;
        String endpoint = System.getProperty("bcbm.nominatim.url","https://nominatim.openstreetmap.org/search");
        String consulta = URLEncoder.encode(clave + ", Managua, Nicaragua", StandardCharsets.UTF_8);
        return obtener(endpoint + "?format=jsonv2&countrycodes=ni&limit=5&accept-language=es&q=" + consulta)
                .thenApply(json -> {
                    List<Lugar> lugares = new ArrayList<>();
                    for (var item : json.getAsJsonArray()) {
                        var lugar = item.getAsJsonObject();
                        lugares.add(new Lugar(lugar.get("display_name").getAsString(),
                                lugar.get("lat").getAsDouble(), lugar.get("lon").getAsDouble()));
                    }
                    var resultado = List.copyOf(lugares);
                    synchronized (ConsultasCabina.class) {
                        if (CACHE.size() >= 200) CACHE.remove(CACHE.keySet().iterator().next());
                        CACHE.put(clave, resultado);
                    }
                    return resultado;
                });
    }

    public static class ConfiguracionService {
        private final SessionFactory factory;
        public ConfiguracionService(SessionFactory factory) { this.factory=factory; }
        public record Operador(Integer id,String nombre,boolean todas,Set<Integer> unidades) {
            @Override public String toString() { return nombre; }
        }
        public record Permisos(List<Operador> personal, List<Unidad> unidades) {}
        public List<OpcionCatalogo> listar(SesionUsuario sesion, GrupoCatalogo grupo) {
            return factory.fromTransaction(s -> {
                Acceso.admin(s,sesion);
                return s.createSelectionQuery("from OpcionCatalogo where grupo=:grupo order by lower(nombre)",OpcionCatalogo.class)
                    .setParameter("grupo",grupo).getResultList();
            });
        }
        public Map<GrupoCatalogo,List<String>> opciones(SesionUsuario sesion) {
            return factory.fromTransaction(s -> {
                Acceso.usuario(s,sesion);
                Map<GrupoCatalogo,List<String>> datos=new EnumMap<>(GrupoCatalogo.class);
                for (var g:GrupoCatalogo.values()) datos.put(g,new ArrayList<>());
                for (var o:s.createSelectionQuery("from OpcionCatalogo where activo=true order by lower(nombre)",OpcionCatalogo.class).getResultList())
                    datos.get(o.getGrupo()).add(o.getNombre());
                return datos;
            });
        }
        public void guardar(SesionUsuario sesion,Integer id,long version,GrupoCatalogo grupo,String nombre,boolean activo) {
            String n= Validaciones.texto(nombre,"Nombre",grupo.limite());
            Transacciones.ejecutar(factory, s -> {
                Acceso.admin(s,sesion);
                OpcionCatalogo opcion=id==null ? new OpcionCatalogo(grupo,n) : s.find(OpcionCatalogo.class,id,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                if (opcion==null || opcion.getVersion()!=version || opcion.getGrupo()!=grupo)
                    throw new IllegalArgumentException("El catálogo cambió. Actualiza antes de guardar.");
                if (id!=null && !opcion.getNombre().equals(n) && enUso(s,grupo,opcion.getNombre()))
                    throw new IllegalArgumentException("Este nombre tiene registros asociados. Puedes desactivarlo y crear su reemplazo.");
                opcion.cambiar(n,activo);
                if(id==null) s.persist(opcion);
                return null;
            });
        }
        private static String campo(GrupoCatalogo grupo) {
            return switch(grupo) {
                case RANGO -> "Bombero where rango"; case COMPANIA -> "Bombero where compania";
                case CATEGORIA_EQUIPO -> "Equipo where categoria"; case TIPO_UNIDAD -> "Unidad where tipo";
                case DEPARTAMENTO -> "OtroServicio where escuadraDepartamento"; case TIPO_SERVICIO -> "OtroServicio where tipo";
            };
        }
        private static boolean enUso(Session s, GrupoCatalogo grupo, String nombre) {
            return !s.createSelectionQuery("select id from "+campo(grupo)+"=:nombre",Integer.class)
                .setParameter("nombre",nombre).setMaxResults(1).getResultList().isEmpty();
        }
        public static void validar(Session s,GrupoCatalogo grupo,String nombre,String anterior) {
            if (Objects.equals(nombre,anterior)) return;
            if (nombre==null || s.createSelectionQuery("select count(*) from OpcionCatalogo where grupo=:grupo and nombre=:nombre and activo=true",Long.class)
                .setParameter("grupo",grupo).setParameter("nombre",nombre).getSingleResult()==0)
                throw new IllegalArgumentException("Selecciona una opción activa de "+grupo+". Se administra en Configuración.");
        }
        public Permisos permisos(SesionUsuario sesion) {
            return factory.fromTransaction(s -> {
                Acceso.admin(s,sesion);
                var personal=new BomberoDao().listar(s).stream().map(b -> new Operador(b.getId(),b.getNombre()+" "+b.getApellido(),
                    b.isOperadorTodas(),b.getUnidadesOperador())).toList();
                return new Permisos(personal,new UnidadDao().listar(s));
            });
        }
        public void guardarOperador(SesionUsuario sesion,Integer id,boolean todas,Set<Integer> unidades) {
            if (id==null) throw new IllegalArgumentException("Selecciona una persona.");
            Set<Integer> seleccion=Set.copyOf(unidades);
            Transacciones.ejecutar(factory,s -> {
                Acceso.admin(s,sesion);
                Bombero bombero=new BomberoDao().bloquear(s,id);
                for (Integer unidad:seleccion) new UnidadDao().buscar(s,unidad);
                bombero.autorizarOperador(todas,seleccion);
                return null;
            });
        }
    }
}
