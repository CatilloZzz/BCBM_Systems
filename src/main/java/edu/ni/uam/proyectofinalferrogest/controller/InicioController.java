package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.model.Rol;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class InicioController {
    @FXML private StackPane root;
    @FXML private GridPane metricasGrid,graficosGrid,listasGrid,accesosGrid;
    @FXML private VBox panelInstitucional, panelPersonal, listaEmergencias, listaMantenimientos;
    @FXML private Label saludo, fechaResumen, nombreBombero, estadoBombero;
    @FXML private Label emergenciasActivas, emergenciasHoy, unidadesDisponibles, unidadesDetalle;
    @FXML private Label personalDisponible, personalDetalle, piezasInventario, piezasReparacion;
    @FXML private Label sinUnidades, clima, estadoClima;
    @FXML private ProgressIndicator cargando, cargandoClima;
    @FXML private Button consultarClima;
    @FXML private LineChart<String, Number> emergenciasChart;
    @FXML private PieChart unidadesChart;

    private final SesionUsuario sesion;
    private final Runnable personal, equipos, mantenimiento;
    private final InicioService servicio = new InicioService();
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("es-NI"));
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-NI"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-NI"));

    public InicioController(SesionUsuario sesion, Runnable personal, Runnable equipos, Runnable mantenimiento) {
        this.sesion = sesion;
        this.personal = personal;
        this.equipos = equipos;
        this.mantenimiento = mantenimiento;
    }

    public Parent vista() { return Vistas.cargar("inicio.fxml", this); }

    @FXML private void initialize() {
        saludo.setText("Hola, " + sesion.username());
        fechaResumen.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-NI"))));
        Ui.carga(root, cargando);
        if (sesion.rol() == Rol.BOMBERO) {
            panelInstitucional.setVisible(false);
            panelInstitucional.setManaged(false);
            panelPersonal.setVisible(true);
            panelPersonal.setManaged(true);
        }
        adaptar(metricasGrid,3);
        adaptar(graficosGrid,2);
        adaptar(listasGrid,2);
        adaptar(accesosGrid,3);
        actualizar();
    }
    private void adaptar(GridPane grid,int columnasAmplias) {
        grid.widthProperty().addListener((o,a,b)->{
            int columnas=b.doubleValue()<650 ? 1 : b.doubleValue()<900 ? 2 : columnasAmplias;
            if (columnasAmplias==2 && b.doubleValue()<900) columnas=1;
            if(grid.getColumnConstraints().size()==columnas) return;
            grid.getColumnConstraints().clear();
            for(int i=0;i<columnas;i++) {
                ColumnConstraints columna=new ColumnConstraints();
                columna.setPercentWidth(100.0/columnas); columna.setMinWidth(0);
                grid.getColumnConstraints().add(columna);
            }
            int indice=0;
            for(var nodo:grid.getChildren()) {
                GridPane.setColumnIndex(nodo,indice%columnas);
                GridPane.setRowIndex(nodo,indice/columnas); indice++;
            }
        });
    }

    @FXML private void actualizar() {
        Ui.ejecutar(root, () -> servicio.consultar(sesion), resumen -> {
            if (!resumen.institucional()) {
                nombreBombero.setText(resumen.nombreBombero());
                estadoBombero.setText(Ui.nombre(resumen.estadoBombero()));
                estadoBombero.getStyleClass().removeAll("personal-status-ok", "personal-status-busy");
                estadoBombero.getStyleClass().add("personal-status-" + ("DISPONIBLE".equals(resumen.estadoBombero()) ? "ok" : "busy"));
                return;
            }
            emergenciasActivas.setText(Long.toString(resumen.emergenciasActivas()));
            emergenciasHoy.setText(Long.toString(resumen.emergenciasHoy()));
            unidadesDisponibles.setText(Long.toString(resumen.unidadesDisponibles()));
            unidadesDetalle.setText("de " + resumen.unidadesTotal() + " registradas");
            personalDisponible.setText(Long.toString(resumen.personalDisponible()));
            personalDetalle.setText("de " + resumen.personalActivo() + " activos");
            piezasInventario.setText(Long.toString(resumen.piezasInventario()));
            piezasReparacion.setText(Long.toString(resumen.piezasReparacion()));
            cargarGraficoEmergencias(resumen);
            cargarGraficoUnidades(resumen);
            cargarEmergencias(resumen);
            cargarMantenimientos(resumen);
            fechaResumen.setText("Actualizado " + LocalDate.now().format(FECHA));
        });
    }

    private void cargarGraficoEmergencias(InicioService.Resumen resumen) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (InicioService.PuntoDia punto : resumen.emergenciasSemana()) {
            LocalDate dia = LocalDate.parse(punto.dia());
            String etiqueta = FECHA_CORTA.format(dia);
            etiqueta = Character.toUpperCase(etiqueta.charAt(0)) + etiqueta.substring(1);
            serie.getData().add(new XYChart.Data<>(etiqueta, punto.cantidad()));
        }
        emergenciasChart.setData(FXCollections.observableArrayList(serie));
    }

    private void cargarGraficoUnidades(InicioService.Resumen resumen) {
        var datos = FXCollections.<PieChart.Data>observableArrayList();
        for (InicioService.EstadoUnidadResumen estado : resumen.estadoUnidades()) {
            datos.add(new PieChart.Data(estado.nombre() + " · " + estado.cantidad(), estado.cantidad()));
        }
        unidadesChart.setData(datos);
        boolean vacio = datos.isEmpty();
        sinUnidades.setVisible(vacio);
        sinUnidades.setManaged(vacio);
        unidadesChart.setVisible(!vacio);
        unidadesChart.setManaged(!vacio);
    }

    private void cargarEmergencias(InicioService.Resumen resumen) {
        listaEmergencias.getChildren().clear();
        if (resumen.recientes().isEmpty()) {
            listaEmergencias.getChildren().add(vacio("No hay emergencias registradas."));
            return;
        }
        for (InicioService.EmergenciaReciente dato : resumen.recientes()) {
            VBox fila = new VBox(3);
            fila.getStyleClass().add("dashboard-list-row");
            Label titulo = new Label(dato.acta() + " · " + dato.tipo());
            titulo.getStyleClass().add("dashboard-row-title");
            titulo.setWrapText(true);
            Label detalle = new Label(dato.direccion() + " · " + FECHA_HORA.format(dato.fecha()));
            detalle.getStyleClass().add("dashboard-row-detail");
            detalle.setWrapText(true);
            Label estado = new Label(dato.estado());
            estado.getStyleClass().setAll("status-badge", switch (dato.estado()) {
                case "Cerrada" -> "status-ok";
                case "Pendiente" -> "status-warning";
                case "En proceso" -> "status-danger";
                default -> "status-neutral";
            });
            fila.getChildren().addAll(titulo, detalle, estado);
            listaEmergencias.getChildren().add(fila);
        }
    }

    private void cargarMantenimientos(InicioService.Resumen resumen) {
        listaMantenimientos.getChildren().clear();
        if (resumen.mantenimientos().isEmpty()) {
            listaMantenimientos.getChildren().add(vacio("No hay mantenimientos programados para los próximos 30 días."));
            return;
        }
        for (InicioService.MantenimientoProximo dato : resumen.mantenimientos()) {
            VBox fila = new VBox(3);
            fila.getStyleClass().add("dashboard-list-row");
            Label titulo = new Label(dato.equipo() + " · " + FECHA.format(dato.fecha()));
            titulo.getStyleClass().add("dashboard-row-title");
            titulo.setWrapText(true);
            Label detalle = new Label(dato.descripcion());
            detalle.getStyleClass().add("dashboard-row-detail");
            detalle.setWrapText(true);
            fila.getChildren().addAll(titulo, detalle);
            listaMantenimientos.getChildren().add(fila);
        }
    }

    private Label vacio(String mensaje) {
        Label label = new Label(mensaje);
        label.getStyleClass().add("dashboard-empty");
        label.setWrapText(true);
        return label;
    }

    @FXML private void actualizarClima() {
        consultarClima.setDisable(true);
        cargandoClima.setVisible(true);
        cargandoClima.setManaged(true);
        estadoClima.setText("Consultando Open-Meteo…");
        ConsultasCabina.clima().whenComplete((datos, error) -> Platform.runLater(() -> {
            consultarClima.setDisable(false);
            cargandoClima.setVisible(false);
            cargandoClima.setManaged(false);
            if (error == null) {
                clima.setText(datos);
                estadoClima.setText("Actualizado ahora · Fuente: Open-Meteo");
            } else {
                estadoClima.setText("No se pudo actualizar. Revisa la conexión e inténtalo de nuevo.");
            }
        }));
    }

    @FXML private void verPersonal() { personal.run(); }
    @FXML private void verEquipos() { equipos.run(); }
    @FXML private void verMantenimiento() { mantenimiento.run(); }
}
