package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.dao.TipoEmergenciaDao;
import edu.ni.uam.proyectofinalferrogest.model.EstadoEmergencia;
import edu.ni.uam.proyectofinalferrogest.model.PrioridadEmergencia;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasExportacion;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.Datos;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.Filtro;
import edu.ni.uam.proyectofinalferrogest.service.ReporteJasperService;
import edu.ni.uam.proyectofinalferrogest.service.SesionUsuario;
import edu.ni.uam.proyectofinalferrogest.util.HibernateUtil;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Reporte de emergencias: actas por período, tipo, prioridad y estado, con dirección y resultado. */
public class ReporteEmergenciasController {
    private record Opcion<T>(T valor, String etiqueta) {
        @Override public String toString() { return etiqueta; }
    }
    private record Preparado(Datos datos, JasperPrint documento) {}

    private final SesionUsuario sesion;
    private final LocalDate inicioInicial, finInicial;
    private Datos consultado;
    @FXML private VBox root;
    @FXML private DatePicker desde, hasta;
    @FXML private ComboBox<Opcion<Integer>> tipo;
    @FXML private ComboBox<Opcion<PrioridadEmergencia>> prioridad;
    @FXML private ComboBox<Opcion<EstadoEmergencia>> estado;
    @FXML private Label resumen, frecuencia, pendientes, corte;
    @FXML private StackPane contenido;
    @FXML private Button previsualizar;
    @FXML private ProgressIndicator cargando;

    public ReporteEmergenciasController(SesionUsuario sesion) {
        this(sesion, LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }

    public ReporteEmergenciasController(SesionUsuario sesion, LocalDate desde, LocalDate hasta) {
        this.sesion = sesion;
        boolean valido = desde != null && hasta != null && !desde.isAfter(hasta);
        this.inicioInicial = valido ? desde : LocalDate.now().withDayOfMonth(1);
        this.finInicial = valido ? hasta : LocalDate.now();
    }

    public Parent vista() { return Vistas.cargar("reporte-emergencias.fxml", this); }

    public void mostrar(Node propietario) {
        Stage ventana = new Stage();
        ventana.setTitle("BCBM · Reporte de emergencias");
        ventana.initOwner(propietario.getScene().getWindow());
        ventana.initModality(Modality.WINDOW_MODAL);
        Scene scene = new Scene(vista(), 1100, 760);
        scene.getStylesheets().add(Vistas.estilos());
        ventana.setMinWidth(780);
        ventana.setMinHeight(560);
        ventana.setScene(scene);
        ventana.show();
    }

    @FXML private void initialize() {
        Ui.fecha(desde); Ui.fecha(hasta);
        desde.setValue(inicioInicial); hasta.setValue(finInicial);
        tipo.getItems().add(new Opcion<>(null, "Todos"));
        tipo.getSelectionModel().selectFirst();
        prioridad.getItems().add(new Opcion<>(null, "Todas"));
        for (PrioridadEmergencia p : new PrioridadEmergencia[]{PrioridadEmergencia.CRITICA, PrioridadEmergencia.ALTA,
                PrioridadEmergencia.MEDIA, PrioridadEmergencia.BAJA}) prioridad.getItems().add(new Opcion<>(p, p.toString()));
        prioridad.getSelectionModel().selectFirst();
        estado.getItems().add(new Opcion<>(null, "Todos"));
        for (EstadoEmergencia e : EstadoEmergencia.values()) estado.getItems().add(new Opcion<>(e, e.toString()));
        estado.getSelectionModel().selectFirst();
        Ui.carga(root, cargando);
        // Primero el catálogo de tipos y luego la consulta, para no lanzar dos tareas a la vez.
        Ui.ejecutar(root, () -> new TipoEmergenciaDao(HibernateUtil.getSessionFactory()).listar(), lista -> {
            lista.forEach(t -> tipo.getItems().add(new Opcion<>(t.getId(), t.getNombre())));
            consultar();
        });
    }

    @FXML private void consultar() {
        LocalDate inicio = desde.getValue(), fin = hasta.getValue();
        if (inicio == null || fin == null || inicio.isAfter(fin)) {
            Ui.seguro(root, () -> { throw new IllegalArgumentException("Selecciona fechas válidas: Desde no puede ser posterior a Hasta."); });
            return;
        }
        Opcion<Integer> t = tipo.getValue();
        Opcion<PrioridadEmergencia> p = prioridad.getValue();
        Opcion<EstadoEmergencia> e = estado.getValue();
        Filtro filtro = new Filtro(inicio, fin, t == null ? null : t.valor(), t == null ? null : t.etiqueta(),
                p == null ? null : p.valor(), e == null ? null : e.valor());
        previsualizar.setDisable(true);
        Ui.ejecutar(root, () -> new ReporteEmergenciasService(HibernateUtil.getSessionFactory()).consultar(sesion, filtro), datos -> {
            consultado = datos;
            contenido.getChildren().setAll(new ReporteEmergenciasDatosController(datos).vista());
            resumen.setText(datos.resumen());
            frecuencia.setText(datos.frecuencia());
            pendientes.setText(datos.pendientes());
            corte.setText("Período " + filtro.periodo() + " · " + filtro.descripcion() + " · Consultado el "
                    + datos.generado().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            previsualizar.setDisable(false);
        });
    }

    @FXML private void previsualizar() {
        if (consultado == null) return;
        Datos datos = consultado;
        Ui.ejecutar(root, () -> {
            try { return new Preparado(datos, new ReporteEmergenciasExportacion().preparar(datos)); }
            catch (IOException | JRException ex) { throw new IllegalStateException("No se pudo preparar el reporte de emergencias.", ex); }
        }, reporte -> ReporteVistaPrevia.mostrar(root, "Reporte de emergencias", reporte.documento(),
                new ReporteEmergenciasDatosController(reporte.datos()).vista(),
                "BCBM-emergencias-" + datos.filtro().desde() + "-a-" + datos.filtro().hasta(),
                destino -> {
                    try { new ReporteJasperService().exportarPdf(reporte.documento(), destino); }
                    catch (JRException ex) { throw new IllegalStateException("No se pudo guardar el PDF.", ex); }
                },
                destino -> new ReporteEmergenciasExportacion().guardarExcel(reporte.datos(), destino)));
    }
}
