package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.dao.EmergenciaDao;
import edu.ni.uam.proyectofinalferrogest.dao.EmergenciaDao.Registro;
import edu.ni.uam.proyectofinalferrogest.dao.TipoEmergenciaDao;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.CierreEmergencia;
import jakarta.persistence.OptimisticLockException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmergenciasController {
    private static final Logger LOG = Logger.getLogger(EmergenciasController.class.getName());
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox modulo;
    @FXML private VBox contenido;
    @FXML private Label aviso;
    @FXML private ComboBox<EstadoEmergencia> estado;
    @FXML private ComboBox<TipoEmergencia> tipo;
    @FXML private DatePicker desde;
    @FXML private DatePicker hasta;
    @FXML private TableView<Registro> tabla;
    @FXML private TableColumn<Registro, String> colFecha;
    @FXML private TableColumn<Registro, String> colDireccion;
    @FXML private TableColumn<Registro, String> colTipo;
    @FXML private TableColumn<Registro, String> colPrioridad;
    @FXML private TableColumn<Registro, String> colEstado;
    @FXML private TableColumn<Registro, String> colUsuario;
    @FXML private Button nueva;
    @FXML private Button editar;
    @FXML private Button eliminar;
    @FXML private Button detalle;
    @FXML private Button cerrar;

    private EmergenciaDao emergencias;
    private TipoEmergenciaDao tipos;
    private Supplier<Integer> usuarioActual;
    private CierreEmergencia servicioCierre;

    /** Punto de entrada desde el menú de la aplicación después del inicio de sesión. */
    public static Parent cargar(SessionFactory factory, Supplier<Integer> usuarioActual,
                                CierreEmergencia servicioCierre) throws IOException {
        var loader = new FXMLLoader(EmergenciasController.class.getResource(
                "/edu/ni/uam/proyectofinalferrogest/fxml/emergencias.fxml"));
        Parent vista = loader.load();
        EmergenciasController controller = loader.getController();
        controller.conectar(factory, usuarioActual, servicioCierre);
        return vista;
    }

    public void conectar(SessionFactory factory, Supplier<Integer> usuarioActual,
                         CierreEmergencia servicioCierre) {
        this.emergencias = new EmergenciaDao(factory);
        this.tipos = new TipoEmergenciaDao(factory);
        this.usuarioActual = usuarioActual;
        this.servicioCierre = servicioCierre;
        contenido.setDisable(false);
        actualizarBotones();
        filtrar();
    }

    @FXML
    private void initialize() {
        estado.getItems().setAll(EstadoEmergencia.values());
        columna(colFecha, r -> FECHA.format(r.emergencia().getFechaRegistro()));
        columna(colDireccion, r -> r.emergencia().getDireccion());
        columna(colTipo, r -> r.emergencia().getTipo().getNombre());
        columna(colPrioridad, r -> r.emergencia().getPrioridad().toString());
        columna(colEstado, r -> r.emergencia().getEstado().toString());
        columna(colUsuario, Registro::usuario);
        tabla.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, actual) -> actualizarBotones());
        tabla.setRowFactory(view -> {
            var fila = new TableRow<Registro>();
            fila.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !fila.isEmpty()) verDetalle();
            });
            return fila;
        });
        actualizarBotones();
    }

    private void actualizarBotones() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        EstadoEmergencia actual = seleccion == null ? null : seleccion.emergencia().getEstado();
        nueva.setDisable(usuarioActual == null || usuarioActual.get() == null);
        detalle.setDisable(seleccion == null);
        editar.setDisable(seleccion == null || actual == EstadoEmergencia.CERRADA);
        eliminar.setDisable(actual != EstadoEmergencia.PENDIENTE);
        cerrar.setDisable(servicioCierre == null || actual == null
                || !actual.permiteTransicionA(EstadoEmergencia.CERRADA));
    }

    @FXML
    private void filtrar() {
        intentar(() -> {
            TipoEmergencia seleccionado = tipo.getValue();
            Integer tipoId = seleccionado == null ? null : seleccionado.getId();
            List<TipoEmergencia> catalogo = tipos.listar();
            tipo.getItems().setAll(catalogo);
            tipo.setValue(catalogo.stream().filter(t -> t.getId().equals(tipoId))
                    .findFirst().orElse(null));
            tabla.getItems().setAll(emergencias.listar(estado.getValue(),
                    tipo.getValue() == null ? null : tipo.getValue().getId(),
                    desde.getValue(), hasta.getValue()));
            aviso.setText(servicioCierre == null
                    ? "El cierre requiere el servicio transaccional de despacho." : "");
        });
        actualizarBotones();
    }

    @FXML
    private void limpiarFiltros() {
        estado.setValue(null);
        tipo.setValue(null);
        desde.setValue(null);
        hasta.setValue(null);
        filtrar();
    }

    @FXML
    private void registrar() {
        intentar(() -> formularioEmergencia(null));
    }

    @FXML
    private void editar() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion != null) {
            intentar(() -> formularioEmergencia(emergencias.buscar(seleccion.emergencia().getId()).emergencia()));
        }
    }

    private void formularioEmergencia(Emergencia emergencia) {
        var direccion = new TextField();
        var descripcion = new TextArea();
        descripcion.setPrefRowCount(3);
        descripcion.setWrapText(true);
        var categoria = new ComboBox<TipoEmergencia>();
        categoria.getItems().setAll(tipos.listar());
        if (categoria.getItems().isEmpty()) {
            throw new IllegalStateException("Cree un tipo de emergencia antes de registrar o editar.");
        }
        var prioridad = new ComboBox<PrioridadEmergencia>();
        prioridad.getItems().setAll(PrioridadEmergencia.values());
        if (emergencia != null) {
            direccion.setText(emergencia.getDireccion());
            descripcion.setText(emergencia.getDescripcion());
            categoria.getItems().stream().filter(t -> t.getId().equals(emergencia.getTipo().getId()))
                    .findFirst().ifPresent(categoria::setValue);
            prioridad.setValue(emergencia.getPrioridad());
        }
        var campos = new GridPane();
        campos.setHgap(10);
        campos.setVgap(10);
        campos.addRow(0, etiqueta("Dirección", direccion), direccion);
        campos.addRow(1, etiqueta("Tipo", categoria), categoria);
        campos.addRow(2, etiqueta("Prioridad", prioridad), prioridad);
        campos.addRow(3, etiqueta("Descripción", descripcion), descripcion);
        guardarEnDialogo(emergencia == null ? "Registrar emergencia" : "Editar emergencia", campos, () -> {
            Integer tipoId = categoria.getValue() == null ? null : categoria.getValue().getId();
            if (emergencia == null) {
                emergencias.registrar(direccion.getText(), descripcion.getText(),
                        tipoId, prioridad.getValue(), usuarioActual.get());
            } else {
                emergencias.actualizar(emergencia.getId(), emergencia.getVersion(), direccion.getText(),
                        descripcion.getText(), tipoId, prioridad.getValue());
            }
        });
    }

    @FXML
    private void verDetalle() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null) return;
        intentar(() -> {
            Registro registro = emergencias.buscar(seleccion.emergencia().getId());
            Emergencia e = registro.emergencia();
            var direccion = new Label("Dirección · " + e.getDireccion());
            direccion.setWrapText(true);
            var descripcion = new Label(e.getDescripcion() == null ? "Sin descripción" : e.getDescripcion());
            descripcion.setWrapText(true);
            var datos = new VBox(8, direccion, new Label("Tipo · " + e.getTipo().getNombre()),
                    new Label("Estado · " + e.getEstado()), new Label("Prioridad · " + e.getPrioridad()),
                    new Label("Registro · " + FECHA.format(e.getFechaRegistro()) + " · " + registro.usuario()),
                    descripcion, new Label("Recursos asignados"));
            if (emergencias.asignacionesDisponibles()) {
                var recursos = new TableView<EmergenciaDao.RecursoAsignado>();
                var unidad = new TableColumn<EmergenciaDao.RecursoAsignado, String>("Unidad");
                var bombero = new TableColumn<EmergenciaDao.RecursoAsignado, String>("Bombero");
                columna(unidad, r -> r.unidad() == null ? "Sin unidad" : r.unidad());
                columna(bombero, r -> r.nombre() == null ? "Sin bombero" : r.nombre() + " " + r.apellido());
                recursos.getColumns().add(unidad);
                recursos.getColumns().add(bombero);
                recursos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
                recursos.setPlaceholder(new Label("Sin asignaciones registradas."));
                recursos.setPrefHeight(160);
                try {
                    recursos.getItems().setAll(emergencias.listarAsignaciones(e.getId()));
                    datos.getChildren().add(recursos);
                } catch (RuntimeException error) {
                    LOG.log(Level.WARNING, "Error al consultar las asignaciones.", error);
                    datos.getChildren().add(new Label("No se pudieron consultar las asignaciones."));
                }
            } else {
                datos.getChildren().add(new Label("La consulta requiere el módulo de asignaciones."));
            }
            Dialog<ButtonType> dialogo = dialogo("Emergencia " + e.getId(), datos);
            dialogo.getDialogPane().setPrefWidth(620);
            dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialogo.showAndWait();
        });
    }

    @FXML
    private void eliminar() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null || !confirmar("Eliminar la emergencia seleccionada")) return;
        Emergencia e = seleccion.emergencia();
        if (intentar(() -> emergencias.eliminar(e.getId(), e.getVersion()))) {
            filtrar();
            mensaje(Alert.AlertType.INFORMATION, "Emergencia eliminada.");
        }
    }

    @FXML
    private void cerrar() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (servicioCierre == null || seleccion == null
                || !confirmar("Cerrar la emergencia y liberar sus recursos")) return;
        Integer id = seleccion.emergencia().getId();
        boolean confirmado = intentar(() -> {
            if (!emergencias.buscar(id).emergencia().getEstado().permiteTransicionA(EstadoEmergencia.CERRADA)) {
                throw new IllegalStateException("La emergencia ya está cerrada.");
            }
            servicioCierre.cerrar(id);
            if (emergencias.buscar(id).emergencia().getEstado() != EstadoEmergencia.CERRADA) {
                throw new IllegalStateException("La base de datos no confirmó el cierre.");
            }
        });
        filtrar();
        if (confirmado) mensaje(Alert.AlertType.INFORMATION, "Emergencia cerrada.");
    }

    @FXML
    private void gestionarTipos() {
        intentar(() -> {
            var lista = new ListView<TipoEmergencia>();
            lista.getItems().setAll(tipos.listar());
            lista.setPrefHeight(220);
            var agregar = new Button("Nuevo tipo");
            var modificar = new Button("Editar tipo");
            var borrar = new Button("Eliminar tipo");
            modificar.disableProperty().bind(lista.getSelectionModel().selectedItemProperty().isNull());
            borrar.disableProperty().bind(lista.getSelectionModel().selectedItemProperty().isNull());
            var panel = new VBox(10, lista, new javafx.scene.layout.HBox(8, agregar, modificar, borrar));
            Dialog<ButtonType> dialogo = dialogo("Tipos de emergencia", panel);
            dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            agregar.setOnAction(event -> formularioTipo(null, lista));
            modificar.setOnAction(event -> formularioTipo(lista.getSelectionModel().getSelectedItem(), lista));
            borrar.setOnAction(event -> {
                TipoEmergencia seleccionado = lista.getSelectionModel().getSelectedItem();
                if (seleccionado != null && confirmar("Eliminar el tipo " + seleccionado.getNombre())
                        && intentar(() -> tipos.eliminar(seleccionado.getId(), seleccionado.getVersion()))) {
                    intentar(() -> lista.getItems().setAll(tipos.listar()));
                    mensaje(Alert.AlertType.INFORMATION, "Tipo de emergencia eliminado.");
                }
            });
            dialogo.showAndWait();
            filtrar();
        });
    }

    private void formularioTipo(TipoEmergencia actual, ListView<TipoEmergencia> lista) {
        var nombre = new TextField(actual == null ? "" : actual.getNombre());
        var campos = new VBox(6, etiqueta("Nombre", nombre), nombre);
        if (guardarEnDialogo(actual == null ? "Nuevo tipo de emergencia" : "Editar tipo de emergencia",
                campos, () -> tipos.guardar(actual == null ? null : actual.getId(),
                        actual == null ? 0 : actual.getVersion(), nombre.getText()))) {
            intentar(() -> lista.getItems().setAll(tipos.listar()));
        }
    }

    private boolean guardarEnDialogo(String titulo, Parent campos, Runnable guardar) {
        Dialog<ButtonType> dialogo = dialogo(titulo, campos);
        var boton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(boton, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(boton).addEventFilter(ActionEvent.ACTION, event -> {
            if (!intentar(guardar)) event.consume();
        });
        boolean guardado = dialogo.showAndWait().filter(boton::equals).isPresent();
        if (guardado) {
            filtrar();
            mensaje(Alert.AlertType.INFORMATION, "Cambios guardados.");
        }
        return guardado;
    }

    private Dialog<ButtonType> dialogo(String titulo, Parent contenido) {
        var dialogo = new Dialog<ButtonType>();
        dialogo.initOwner(modulo.getScene().getWindow());
        dialogo.setTitle(titulo);
        DialogPane panel = dialogo.getDialogPane();
        panel.setPadding(new Insets(12));
        panel.setContent(contenido);
        return dialogo;
    }

    private Label etiqueta(String texto, Control control) {
        var etiqueta = new Label(texto);
        etiqueta.setLabelFor(control);
        return etiqueta;
    }

    private <T> void columna(TableColumn<T, String> columna, Function<T, String> texto) {
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(texto.apply(celda.getValue())));
    }

    private boolean confirmar(String pregunta) {
        var confirmacion = alerta(Alert.AlertType.CONFIRMATION, "¿" + pregunta + "?");
        return confirmacion.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        alerta(tipo, texto).showAndWait();
    }

    private Alert alerta(Alert.AlertType tipo, String texto) {
        var alerta = new Alert(tipo, texto);
        if (modulo.getScene() != null) alerta.initOwner(modulo.getScene().getWindow());
        alerta.setHeaderText(null);
        return alerta;
    }

    private boolean intentar(Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (IllegalArgumentException | IllegalStateException error) {
            mensaje(Alert.AlertType.ERROR, error.getMessage());
        } catch (OptimisticLockException error) {
            mensaje(Alert.AlertType.ERROR, "El registro cambió. Actualice el listado e intente de nuevo.");
        } catch (ConstraintViolationException error) {
            String texto = "23505".equals(error.getSQLState())
                    ? "Ya existe un tipo con ese nombre."
                    : "No se pudo guardar o eliminar el registro por sus referencias en la base de datos.";
            mensaje(Alert.AlertType.ERROR, texto);
        } catch (RuntimeException error) {
            LOG.log(Level.WARNING, "Error en el módulo de emergencias.", error);
            mensaje(Alert.AlertType.ERROR, "No se pudo completar la operación. Consulte el registro de la aplicación.");
        }
        return false;
    }
}
