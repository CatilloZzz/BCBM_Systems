package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.ReporteJasperService;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.util.function.Consumer;

/** Vista previa paginada del PDF de Jasper con una pestaña para revisar los datos tabulares. */
public final class ReporteVistaPrevia {
    private ReporteVistaPrevia() {}

    public static void mostrar(Node owner, String titulo, JasperPrint reporte, Node datos,
                               String nombreBase, Consumer<Path> guardarPdf,
                               Consumer<Path> guardarExcel) {
        Stage ventana = new Stage();
        ventana.setTitle("Vista previa · " + titulo);
        ventana.initOwner(owner.getScene().getWindow());
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setMinWidth(820);
        ventana.setMinHeight(580);

        Label pagina = new Label();
        ImageView imagen = new ImageView();
        imagen.setPreserveRatio(true);
        imagen.setFitWidth(1040);
        imagen.setSmooth(true);
        ScrollPane pdfScroll = new ScrollPane(imagen);
        pdfScroll.setFitToWidth(true);
        pdfScroll.setPannable(true);
        pdfScroll.getStyleClass().add("page-scroll");
        StackPane pdf = new StackPane(pdfScroll);
        pdf.getStyleClass().add("list-card");
        Tab pdfTab = new Tab("Vista PDF", pdf);
        pdfTab.setClosable(false);
        Tab datosTab = new Tab("Datos del reporte", datos);
        datosTab.setClosable(false);
        TabPane pestañas = new TabPane(pdfTab, datosTab);

        Button anterior = new Button("Anterior");
        Button siguiente = new Button("Siguiente");
        int[] numeroPagina = {0};
        Runnable actualizarPagina = () -> {
            int total = Math.max(1, reporte.getPages().size());
            numeroPagina[0] = Math.max(0, Math.min(numeroPagina[0], total - 1));
            pagina.setText("Página " + (numeroPagina[0] + 1) + " de " + total);
            anterior.setDisable(numeroPagina[0] == 0);
            siguiente.setDisable(numeroPagina[0] >= total - 1);
            try {
                java.awt.Image awt = ReporteJasperService.renderizarPagina(reporte, numeroPagina[0]);
                int ancho = awt.getWidth(null), alto = awt.getHeight(null);
                BufferedImage buffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = buffer.createGraphics();
                try { graphics.drawImage(awt, 0, 0, null); } finally { graphics.dispose(); }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(buffer, "png", bytes);
                imagen.setImage(new Image(new ByteArrayInputStream(bytes.toByteArray())));
                pdfScroll.setVvalue(0);
            } catch (JRException | IOException ex) {
                Ui.seguro(owner, () -> { throw new IllegalStateException("No se pudo mostrar la página del PDF.", ex); });
            }
        };
        anterior.setOnAction(e -> { numeroPagina[0]--; actualizarPagina.run(); });
        siguiente.setOnAction(e -> { numeroPagina[0]++; actualizarPagina.run(); });

        Label estado = new Label("Revisa el PDF o los datos antes de guardarlos.");
        estado.getStyleClass().add("muted");
        Button pdfButton = new Button("Guardar PDF");
        pdfButton.getStyleClass().add("primary");
        Button excelButton = new Button("Guardar Excel");
        excelButton.getStyleClass().add("secondary");
        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventana.close());
        pdfButton.setOnAction(e -> seleccionarDestino(ventana, nombreBase + ".pdf", "PDF (*.pdf)", "*.pdf", path -> {
            try { guardarPdf.accept(path); estado.setText("PDF guardado: " + path.getFileName()); }
            catch (RuntimeException ex) { mostrarError(owner, ex); }
        }));
        excelButton.setOnAction(e -> seleccionarDestino(ventana, nombreBase + ".xlsx", "Excel (*.xlsx)", "*.xlsx", path -> {
            try { guardarExcel.accept(path); estado.setText("Excel guardado: " + path.getFileName()); }
            catch (RuntimeException ex) { mostrarError(owner, ex); }
        }));

        HBox navegador = new HBox(10, anterior, pagina, siguiente);
        navegador.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox acciones = new HBox(10, estado, new Region(), pdfButton, excelButton, cerrar);
        HBox.setHgrow(acciones.getChildren().get(1), Priority.ALWAYS);
        acciones.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        acciones.setPadding(new javafx.geometry.Insets(12));
        VBox contenido = new VBox(10, navegador, pestañas, acciones);
        contenido.setPadding(new javafx.geometry.Insets(14));
        VBox.setVgrow(pestañas, Priority.ALWAYS);
        Scene scene = new Scene(contenido, 1040, 760);
        scene.getStylesheets().add(Vistas.estilos());
        ventana.setScene(scene);
        actualizarPagina.run();
        ventana.showAndWait();
    }

    private static void seleccionarDestino(Stage owner, String nombre, String descripcion,
                                          String patron, Consumer<Path> accion) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte");
        chooser.setInitialFileName(nombre);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(descripcion, patron));
        File archivo = chooser.showSaveDialog(owner);
        if (archivo == null) return;
        String extension = patron.substring(1);
        if (!archivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + extension);
        }
        accion.accept(archivo.toPath());
    }

    private static void mostrarError(Node owner, RuntimeException ex) {
        Alert alerta = new Alert(Alert.AlertType.ERROR,
                ex.getMessage() == null ? "Revisa la carpeta de destino e inténtalo de nuevo." : ex.getMessage(),
                ButtonType.OK);
        alerta.setTitle("BCBM Systems");
        alerta.setHeaderText("No se pudo guardar el reporte");
        Ui.dialogo(alerta, owner);
        alerta.showAndWait();
    }
}
