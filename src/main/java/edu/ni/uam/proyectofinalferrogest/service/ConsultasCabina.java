package edu.ni.uam.proyectofinalferrogest.service;

import com.google.gson.*;
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
}
