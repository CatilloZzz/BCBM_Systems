package edu.ni.uam.proyectofinalferrogest.controller;

import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

/** Utilidades visuales. Las operaciones de base de datos se ejecutan fuera del hilo JavaFX. */
public final class Ui {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r,"bcbm-database"); t.setDaemon(true); return t;
    });
    private Ui() {}
    public static VBox pagina(String titulo, Node... contenido) {
        Label label = new Label(titulo); label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        VBox root = new VBox(12,label); root.setPadding(new Insets(20)); root.getChildren().addAll(contenido); return root;
    }
    public static GridPane formulario() { GridPane g = new GridPane(); g.setHgap(10); g.setVgap(9); return g; }
    public static void campo(GridPane g,int fila,String titulo,Node control) {
        g.addRow(fila,new Label(titulo),control); GridPane.setHgrow(control,Priority.ALWAYS);
        if (control instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
    }
    public static Button boton(String texto,Runnable accion) {
        Button b = new Button(texto); b.setOnAction(e -> seguro(accion)); return b;
    }
    public static void seguro(Runnable accion) { try { accion.run(); } catch (RuntimeException e) { error(e); } }
    public static <T> void columna(TableView<T> tabla,String titulo,Function<T,?> valor) {
        TableColumn<T,String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(x -> new SimpleStringProperty(java.util.Objects.toString(valor.apply(x.getValue()),"")));
        c.setPrefWidth(150); tabla.getColumns().add(c);
    }
    public static <T> TableView<T> tabla() {
        TableView<T> t = new TableView<>(); t.setPlaceholder(new Label("No hay registros."));
        t.setPrefHeight(260); VBox.setVgrow(t,Priority.ALWAYS); return t;
    }
    public static <T> void ejecutar(Node root,Supplier<T> trabajo,Consumer<T> despues) {
        root.setDisable(true);
        CompletableFuture.supplyAsync(trabajo,WORKER).whenComplete((result,error) -> Platform.runLater(() -> {
            root.setDisable(false);
            if (error != null) error(error.getCause() == null ? error : error.getCause());
            else seguro(() -> despues.accept(result));
        }));
    }
    public static void informar(String texto) { new Alert(Alert.AlertType.INFORMATION,texto,ButtonType.OK).showAndWait(); }
    public static boolean confirmar(String texto) {
        return new Alert(Alert.AlertType.CONFIRMATION,texto,ButtonType.YES,ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
    private static void error(Throwable e) {
        String mensaje = e instanceof IllegalArgumentException || e instanceof SecurityException || e instanceof IllegalStateException
                ? e.getMessage() : "No se completó la operación. Revisa la conexión y la configuración de la base de datos.";
        new Alert(Alert.AlertType.ERROR,mensaje,ButtonType.OK).showAndWait();
    }
    public static void cerrar() { WORKER.shutdown(); }
}