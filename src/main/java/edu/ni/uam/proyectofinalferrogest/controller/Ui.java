package edu.ni.uam.proyectofinalferrogest.controller;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Window;
import javafx.util.StringConverter;
import edu.ni.uam.proyectofinalferrogest.util.*;

/** Comportamiento compartido; la estructura visual reside en los archivos FXML. */
public final class Ui {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> new Thread(r,"bcbm-database"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Ui() {}
    public static void visible(Node node,boolean value) { node.setVisible(value); node.setManaged(value); }
    public static String fecha(LocalDate value) { return value == null ? "Sin registro" : FECHA.format(value); }
    public static String nombre(Object value) {
        if (value == null) return "";
        return switch (value.toString()) {
            case "ADMIN" -> "Administrador"; case "DESPACHADOR" -> "Despachador"; case "BOMBERO" -> "Bombero";
            case "DISPONIBLE" -> "Disponible"; case "EN_EMERGENCIA" -> "En emergencia"; case "DE_BAJA", "BAJA" -> "De baja";
            case "OPERATIVO" -> "Operativo"; case "EN_REPARACION" -> "En reparación"; case "MANTENIMIENTO" -> "Mantenimiento";
            default -> value.toString();
        };
    }
    public static <T> void opciones(ComboBox<T> combo) {
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(T value) { return nombre(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Selector no editable"); }
        });
    }
    public static void fecha(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : FECHA.format(date); }
            @Override public LocalDate fromString(String text) { return text == null || text.isBlank() ? null : LocalDate.parse(text,FECHA); }
        });
        picker.setEditable(false);
    }
    public static <T> void columna(TableColumn<T,String> column,Function<T,?> value) {
        column.setCellValueFactory(c -> new SimpleStringProperty(Objects.toString(value.apply(c.getValue()),"—")));
        column.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String text,boolean empty) {
                super.updateItem(text,empty);
                setText(empty ? null : text);
                setTooltip(empty || text==null ? null : new Tooltip(text));
            }
        });
    }
    public static <T> void estado(TableColumn<T,String> column,Function<T,?> value) {
        columna(column,item -> nombre(value.apply(item)));
        column.setCellFactory(c -> new TableCell<>() {
            private final Label label = new Label();
            @Override protected void updateItem(String text,boolean empty) {
                super.updateItem(text,empty); setText(null);
                if (empty || text == null) { setGraphic(null); return; }
                label.setText(text); label.getStyleClass().setAll("status-badge");
                label.getStyleClass().add(switch (text) {
                    case "Disponible", "Operativo", "Activa" -> "status-ok";
                    case "En emergencia", "De baja", "Inactiva" -> "status-danger";
                    case "En reparación", "Mantenimiento" -> "status-warning";
                    default -> "status-neutral";
                });
                setGraphic(label);
            }
        });
    }
    private static String normalizar(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).strip();
    }
    public static <T> void buscar(TableView<T> tabla,TextField entrada,ObservableList<T> datos,Function<T,String> texto,Label contador) {
        FilteredList<T> filtro = new FilteredList<>(datos);
        entrada.textProperty().addListener((o,a,b) -> {
            String q=normalizar(b); filtro.setPredicate(item -> normalizar(texto.apply(item)).contains(q));
        });
        SortedList<T> ordenados = new SortedList<>(filtro); ordenados.comparatorProperty().bind(tabla.comparatorProperty()); tabla.setItems(ordenados);
        contador(contador,filtro);
    }
    public static void contador(Label label,ObservableList<?> rows) {
        label.textProperty().bind(Bindings.createStringBinding(() -> rows.size()==1 ? "1 registro" : rows.size()+" registros",rows));
    }
    public static void carga(Node root,ProgressIndicator indicador) {
        indicador.visibleProperty().bind(root.disableProperty()); indicador.managedProperty().bind(indicador.visibleProperty());
    }
    public static <T> void ejecutar(Node root,Supplier<T> trabajo,Consumer<T> despues) {
        ejecutar(root,trabajo,despues,e -> error(root,e));
    }
    public static <T> void ejecutar(Node root,Supplier<T> trabajo,Consumer<T> despues,Consumer<Throwable> fallo) {
        root.setDisable(true);
        CompletableFuture.supplyAsync(trabajo,WORKER).whenComplete((resultado,error) -> Platform.runLater(() -> {
            root.setDisable(false);
            if (error != null) fallo.accept(error.getCause() == null ? error : error.getCause());
            else seguro(root,() -> despues.accept(resultado));
        }));
    }
    public static void seguro(Node root,Runnable accion) { try { accion.run(); } catch (RuntimeException e) { error(root,e); } }
    public static String mensaje(Throwable e) {
        if (e instanceof IllegalArgumentException || e instanceof SecurityException) return e.getMessage();
        if (e instanceof IllegalStateException && e.getMessage()!=null && e.getMessage().startsWith("Falta configurar"))
            return "Falta configurar la conexión. Solicita ayuda al administrador del sistema.";
        return "No se completó la operación. Verifica que PostgreSQL esté disponible y vuelve a intentarlo.";
    }
    public static void dialogo(Dialog<?> dialog,Node root) {
        dialog.getDialogPane().getStylesheets().add(Vistas.estilos());
        if (root!=null && root.getScene()!=null && root.getScene().getWindow()!=null) dialog.initOwner(root.getScene().getWindow());
    }
    private static Alert alerta(Node root,Alert.AlertType tipo,String titulo,String texto,ButtonType... botones) {
        Alert a=new Alert(tipo,texto,botones); a.setTitle("BCBM Systems"); a.setHeaderText(titulo); dialogo(a,root); return a;
    }
    public static void informar(Node root,String texto) { alerta(root,Alert.AlertType.INFORMATION,"Operación completada",texto,ButtonType.OK).showAndWait(); }
    public static boolean confirmar(Node root,String texto) {
        ButtonType aceptar=new ButtonType("Confirmar",ButtonBar.ButtonData.OK_DONE);
        return alerta(root,Alert.AlertType.CONFIRMATION,"Confirmar cambio",texto,aceptar,ButtonType.CANCEL).showAndWait().orElse(ButtonType.CANCEL)==aceptar;
    }
    private static void error(Node root,Throwable e) { alerta(root,Alert.AlertType.ERROR,"Revisa la operación",mensaje(e),ButtonType.OK).showAndWait(); }
    /** Deja terminar una operación pendiente antes de cerrar la conexión. */
    public static void cerrar() { WORKER.execute(HibernateUtil::close); WORKER.shutdown(); }
}
