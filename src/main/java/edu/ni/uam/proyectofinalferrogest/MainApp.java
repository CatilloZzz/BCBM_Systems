package edu.ni.uam.proyectofinalferrogest;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ni.uam.proyectofinalferrogest.controller.*;
import edu.ni.uam.proyectofinalferrogest.util.HibernateUtil;

/** Arranque de apoyo para ejecutar la entrega de Rafael por separado. */
public class MainApp extends Application {
    private Stage stage;
    @Override public void start(Stage stage) {
        this.stage=stage; stage.setTitle("BCBM Systems · Personal e inventario");
        stage.setMinWidth(1000); stage.setMinHeight(720); login(); stage.show();
    }
    private void login() {
        stage.setScene(new Scene(new LoginController().vista(sesion ->
                stage.setScene(new Scene(new MenuController().vista(sesion,this::login),1180,820))),1180,820));
    }
    @Override public void stop() { Ui.cerrar(); HibernateUtil.close(); }
    public static void main(String[] args) { launch(args); }
}