package edu.ni.uam.proyectofinalferrogest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource(
                        "/edu/ni/uam/proyectofinalferrogest/fxml/hello-view.fxml"
                )
        );

        Scene scene = new Scene(loader.load(), 1000, 650);

        stage.setTitle("Sistema de Gestión de Emergencias para Bomberos");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}