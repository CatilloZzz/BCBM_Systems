package edu.ni.uam.proyectofinalferrogest;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ni.uam.proyectofinalferrogest.controller.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class MainApp extends Application {
    private Scene scene;
    @Override public void start(Stage stage) {
        stage.setTitle("BCBM Systems · Benemérito Cuerpo de Bomberos de Managua");
        stage.getIcons().add(Vistas.logo()); stage.setMinWidth(1120); stage.setMinHeight(760);
        scene=new Scene(new LoginController().vista(this::entrar),1320,860);
        scene.getStylesheets().add(Vistas.estilos()); stage.setScene(scene); stage.show();
    }
    private void entrar(edu.ni.uam.proyectofinalferrogest.service.SesionUsuario sesion) {
        scene.setRoot(new MenuController().vista(sesion,this::salir));
    }
    private void salir() { scene.setRoot(new LoginController().vista(this::entrar)); }
    @Override public void stop() { Ui.cerrar(); }
    public static void main(String[] args) { launch(args); }
}
