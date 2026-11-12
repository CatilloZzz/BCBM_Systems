package edu.ni.uam.proyectofinalferrogest.util;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;

/** Punto único de carga de vistas FXML y recursos institucionales. */
public final class Vistas {
    public static final String BASE = "/edu/ni/uam/proyectofinalferrogest/";
    private Vistas() {}
    public static <T extends Parent> T cargar(String archivo, Object controlador) {
        FXMLLoader loader = new FXMLLoader(recurso("fxml/" + archivo));
        loader.setControllerFactory(tipo -> {
            if (!tipo.isInstance(controlador)) throw new IllegalArgumentException("Controlador incompatible: " + archivo);
            return controlador;
        });
        try { return loader.load(); }
        catch (IOException e) { throw new IllegalStateException("No se pudo abrir la pantalla " + archivo, e); }
    }
    public static URL recurso(String ruta) { return Objects.requireNonNull(Vistas.class.getResource(BASE+ ruta), "Recurso ausente: " + ruta); }
    public static String estilos() { return recurso("css/bcbm.css").toExternalForm(); }
    public static Image logo() { return new Image(recurso("images/logo-bcbm.png").toExternalForm()); }
}
