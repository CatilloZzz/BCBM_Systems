package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.ReporteCabinaService.Fila;
import edu.ni.uam.proyectofinalferrogest.util.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class ReportesCabinaController {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy",Locale.forLanguageTag("es-NI"));
    private final SesionUsuario sesion;
    private List<Fila> filas=List.of();
    private YearMonth mes=YearMonth.now();
    @FXML private VBox root;
    @FXML private ProgressIndicator cargando;
    @FXML private ComboBox<String> seccion;
    @FXML private Label periodo,resumen;
    @FXML private TableView<Fila> tabla;
    @FXML private TableColumn<Fila,String> grupo,tipo,cantidad,total;
    public ReportesCabinaController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("reportes-cabina.fxml",this); }
    @FXML private void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Ui.carga(root,cargando);
        Ui.columna(grupo,Fila::grupo); Ui.columna(tipo,Fila::tipo); Ui.columna(cantidad,Fila::cantidad); Ui.columna(total,Fila::total);
        seccion.getItems().setAll("Unidades","Emergencias","Personal"); seccion.setValue("Unidades");
        seccion.valueProperty().addListener((o,a,b)->mostrar()); cargar();
    }
    @FXML private void anterior() { mes=mes.minusMonths(1); cargar(); }
    @FXML private void siguiente() { mes=mes.plusMonths(1); cargar(); }
    @FXML private void cargar() {
        periodo.setText(mes.format(FORMATO)); filas=List.of(); mostrar();
        Ui.ejecutar(root,()->new ReporteCabinaService(HibernateUtil.getSessionFactory()).consultar(sesion,mes),
            datos->{ filas=datos; mostrar(); });
    }
    private void mostrar() {
        var visibles=filas.stream().filter(f->f.seccion().equals(seccion.getValue())).toList();
        tabla.getItems().setAll(visibles);
        String titulo=switch(seccion.getValue()) {
            case "Personal" -> "Personal con más salidas";
            case "Emergencias" -> "Emergencia más frecuente";
            default -> "Unidad con más salidas";
        };
        long max=visibles.stream().mapToLong(Fila::total).max().orElse(0);
        String lideres=String.join(", ",visibles.stream().filter(f->f.total()==max).map(Fila::grupo).distinct().toList());
        resumen.setText(max==0 ? "Sin actividad en este mes." : titulo+" · "+lideres+" ("+max+")");
    }
    @FXML private void exportar() {
        FileChooser selector=new FileChooser(); selector.setTitle("Guardar reporte mensual de cabina");
        selector.setInitialFileName("BCBM-cabina-"+mes+".csv"); selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        File archivo=selector.showSaveDialog(root.getScene().getWindow()); if (archivo==null) return;
        try (var out=Files.newBufferedWriter(archivo.toPath(),StandardCharsets.UTF_8)) {
            out.write("\uFEFFMes;Reporte;Grupo;Tipo;Cantidad;Total del grupo"); out.newLine();
            for (Fila f:filas) {
                out.write(String.join(";",mes.toString(),celda(f.seccion()),celda(f.grupo()),celda(f.tipo()),
                    Long.toString(f.cantidad()),Long.toString(f.total()))); out.newLine();
            }
            resumen.setText("Reporte guardado en "+archivo.getName());
        } catch (IOException ex) { new Alert(Alert.AlertType.ERROR,"No se pudo guardar el archivo. Revisa la carpeta de destino.").showAndWait(); }
    }
    private static String celda(String valor) {
        String limpio=valor.stripLeading();
        if (!limpio.isEmpty() && "=+-@".indexOf(limpio.charAt(0))>=0) valor="'"+valor;
        return "\""+valor.replace("\"","\"\"")+"\"";
    }
}
