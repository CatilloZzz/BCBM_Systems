package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.Acta;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.Conteo;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.Datos;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Tablas del reporte de emergencias; se usan en la ventana del reporte y en la vista previa. */
public class ReporteEmergenciasDatosController {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final Datos datos;
    @FXML private TableView<Acta> actas;
    @FXML private TableColumn<Acta, String> aNumero, aApertura, aTipo, aPrioridad, aEstado, aDireccion, aResultado, aCierre;
    @FXML private Tab porTipo, porPrioridad, porEstado, porResultado;

    public ReporteEmergenciasDatosController(Datos datos) { this.datos = datos; }

    public Parent vista() { return Vistas.cargar("reporte-emergencias-datos.fxml", this); }

    @FXML private void initialize() {
        Ui.columna(aNumero, Acta::referencia);
        Ui.columna(aApertura, a -> fechaHora(a.apertura()));
        Ui.columna(aTipo, Acta::tipo);
        Ui.columna(aPrioridad, Acta::prioridad);
        Ui.columna(aEstado, Acta::estado);
        Ui.columna(aDireccion, Acta::direccionVisible);
        Ui.columna(aResultado, Acta::resultadoVisible);
        Ui.columna(aCierre, a -> fechaHora(a.cierre()));
        actas.getItems().setAll(datos.actas());
        actas.setTableMenuButtonVisible(true);
        actas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        porTipo.setContent(tabla("Tipo de emergencia", datos.porTipo()));
        porPrioridad.setContent(tabla("Prioridad", datos.porPrioridad()));
        porEstado.setContent(tabla("Estado", datos.porEstado()));
        porResultado.setContent(tabla("Resultado", datos.porResultado()));
    }

    private static TableView<Conteo> tabla(String titulo, List<Conteo> filas) {
        TableView<Conteo> tabla = new TableView<>();
        tabla.setFixedCellSize(44);
        tabla.setPlaceholder(new Label("Sin actas para esta consulta."));
        TableColumn<Conteo, String> categoria = new TableColumn<>(titulo);
        TableColumn<Conteo, String> cantidad = new TableColumn<>("Actas");
        TableColumn<Conteo, String> porcentaje = new TableColumn<>("% del período");
        TableColumn<Conteo, String> abiertas = new TableColumn<>("Siguen abiertas");
        TableColumn<Conteo, String> cerradas = new TableColumn<>("Cerradas");
        categoria.setPrefWidth(280);
        Ui.columna(categoria, Conteo::categoria);
        Ui.columna(cantidad, Conteo::actas);
        Ui.columna(porcentaje, c -> String.format(Locale.forLanguageTag("es-NI"), "%.1f%%", c.porcentaje() * 100));
        Ui.columna(abiertas, Conteo::abiertas);
        Ui.columna(cerradas, Conteo::cerradas);
        tabla.getColumns().setAll(List.of(categoria, cantidad, porcentaje, abiertas, cerradas));
        tabla.getItems().setAll(filas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tabla;
    }

    private static String fechaHora(LocalDateTime fecha) { return fecha == null ? "Sin registro" : FECHA_HORA.format(fecha); }
}
