package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.dao.EmergenciaDao;
import edu.ni.uam.proyectofinalferrogest.dao.EmergenciaDao.Registro;
import edu.ni.uam.proyectofinalferrogest.dao.TipoEmergenciaDao;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.DespachoService;
import edu.ni.uam.proyectofinalferrogest.service.DespachoService.Salida;
import edu.ni.uam.proyectofinalferrogest.service.SesionUsuario;
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
import java.time.*;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmergenciasController {
    private static final Logger LOG = Logger.getLogger(EmergenciasController.class.getName());
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox modulo;
    @FXML private VBox contenido;
    @FXML private Label aviso;
    @FXML private ComboBox<OpcionFiltro<EstadoEmergencia>> estado;
    @FXML private ComboBox<OpcionFiltro<Integer>> tipo;
    @FXML private ComboBox<Month> mes;
    @FXML private Spinner<Integer> anio;
    @FXML private TextField numeroFiltro;
    @FXML private ComboBox<String> escuadraFiltro;
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
    private SesionUsuario sesion;
    private DespachoService despacho;
    private Map<Integer, List<Salida>> salidas = Map.of();

    private record OpcionFiltro<T>(T valor, String etiqueta) {
        @Override public String toString() { return etiqueta; }
    }

    /** Punto de entrada desde el menú de la aplicación después del inicio de sesión. */
    public static Parent cargar(SessionFactory factory, SesionUsuario sesion) throws IOException {
        var loader = new FXMLLoader(EmergenciasController.class.getResource(
                "/edu/ni/uam/proyectofinalferrogest/fxml/emergencias.fxml"));
        Parent vista = loader.load();
        EmergenciasController controller = loader.getController();
        controller.conectar(factory, sesion);
        return vista;
    }

    private void conectar(SessionFactory factory, SesionUsuario sesion) {
        this.emergencias = new EmergenciaDao(factory);
        this.tipos = new TipoEmergenciaDao(factory);
        this.sesion = sesion;
        this.despacho = new DespachoService(factory);
        contenido.setDisable(false);
        actualizarBotones();
        filtrar();
    }

    @FXML
    private void initialize() {
        estado.getItems().add(new OpcionFiltro<>(null, "Todos"));
        for (EstadoEmergencia valor : EstadoEmergencia.values()) {
            estado.getItems().add(new OpcionFiltro<>(valor, valor.toString()));
        }
        estado.getSelectionModel().selectFirst();
        tipo.getItems().add(new OpcionFiltro<>(null, "Todos"));
        tipo.getSelectionModel().selectFirst();
        mes.getItems().setAll(Month.values());
        mes.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Month valor) {
                return valor == null ? "" : valor.getDisplayName(java.time.format.TextStyle.FULL,
                        java.util.Locale.forLanguageTag("es-NI"));
            }
            @Override public Month fromString(String valor) { return Month.valueOf(valor); }
        });
        mes.setValue(LocalDate.now().getMonth());
        anio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1900, 9998, LocalDate.now().getYear()));
        agregarColumna("N.º de acta", 115, r -> numero(r.emergencia()), 0);
        agregarColumna("Escuadra", 125, r -> r.emergencia().getEscuadra(), 2);
        agregarColumna("Solicitante", 145, r -> r.emergencia().getSolicitante(), 4);
        agregarColumna("Teléfono", 120, r -> r.emergencia().getTelefono(), 5);
        agregarColumna("Descripción", 210, r -> r.emergencia().getDescripcion(), 7);
        agregarColumna("Unidad", 130, r -> resumen(r, Salida::unidad));
        agregarColumna("Operador", 180, r -> resumen(r, x -> x.operador().nombre()));
        agregarColumna("Jefe de unidad", 180, r -> resumen(r, x -> texto(x.jefe())));
        agregarColumna("Sale", 230, r -> resumen(r, x -> fecha(x.salida())));
        agregarColumna("Llega", 230, r -> resumen(r, x -> fecha(x.llegada())));
        agregarColumna("Retorna", 230, r -> resumen(r, x -> fecha(x.retorno())));
        columna(colFecha, r -> FECHA.format(r.emergencia().getFechaRegistro()));
        columna(colDireccion, r -> r.emergencia().getDireccion());
        columna(colTipo, r -> texto(r.emergencia().getTipo()));
        columna(colPrioridad, r -> texto(r.emergencia().getPrioridad()));
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
        nueva.setDisable(sesion == null);
        detalle.setDisable(seleccion == null);
        editar.setDisable(seleccion == null || actual == EstadoEmergencia.CERRADA);
        eliminar.setDisable(actual != EstadoEmergencia.PENDIENTE);
        cerrar.setDisable(despacho == null || actual == null
                || !actual.permiteTransicionA(EstadoEmergencia.CERRADA));
    }

    @FXML
    private void filtrar() {
        intentar(() -> {
            Integer tipoId = tipo.getValue().valor();
            List<TipoEmergencia> catalogo = tipos.listar();
            tipo.getItems().setAll(new OpcionFiltro<>(null, "Todos"));
            catalogo.forEach(t -> tipo.getItems().add(new OpcionFiltro<>(t.getId(), t.getNombre())));
            tipo.setValue(tipo.getItems().stream().filter(t -> java.util.Objects.equals(t.valor(), tipoId))
                    .findFirst().orElse(tipo.getItems().getFirst()));
            String escuadra = escuadraFiltro.getValue();
            escuadraFiltro.getItems().setAll(emergencias.escuadras(sesion));
            escuadraFiltro.setValue(escuadra);
            var periodo = YearMonth.of(anio.getValue(), mes.getValue());
            var registros = emergencias.listarMes(sesion, periodo, numeroFiltro.getText(), escuadra,
                    tipo.getValue().valor(), estado.getValue().valor());
            salidas = despacho.consultarSalidas(sesion, registros.stream().map(r -> r.emergencia().getId()).toList())
                    .stream().collect(Collectors.groupingBy(Salida::emergenciaId));
            tabla.getItems().setAll(registros);
            tabla.refresh();
            aviso.setText(registros.size() + " actas");
        });
        actualizarBotones();
    }

    @FXML
    private void limpiarFiltros() {
        estado.getSelectionModel().selectFirst();
        tipo.getSelectionModel().selectFirst();
        numeroFiltro.clear();
        escuadraFiltro.setValue(null);
        mes.setValue(LocalDate.now().getMonth());
        anio.getValueFactory().setValue(LocalDate.now().getYear());
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
            intentar(() -> formularioEmergencia(emergencias.buscar(sesion, seleccion.emergencia().getId()).emergencia()));
        }
    }

    private void formularioEmergencia(Emergencia emergencia) {
        var ubicacion = new BuscadorUbicacion(emergencia);
        var direccion = new TextField();
        var descripcion = new TextArea();
        descripcion.setPrefRowCount(3);
        descripcion.setWrapText(true);
        var categoria = new ComboBox<TipoEmergencia>();
        categoria.getItems().setAll(tipos.listar());
        var prioridad = new ComboBox<PrioridadEmergencia>();
        prioridad.getItems().setAll(PrioridadEmergencia.values());
        var acta = new TextField(emergencia == null ? "" : emergencia.getNumeroActa());
        var escuadra = new ComboBox<String>();
        escuadra.setEditable(true);
        escuadra.getItems().setAll(emergencias.escuadras(sesion));
        var solicitante = new TextField(emergencia == null ? "" : emergencia.getSolicitante());
        var telefono = new TextField(emergencia == null ? "" : emergencia.getTelefono());
        LocalDateTime fechaInicial = emergencia == null ? LocalDateTime.now() : emergencia.getFechaRegistro();
        var fechaReporte = new DespachoController.FechaHora(fechaInicial);
        if (emergencia != null) {
            direccion.setText(emergencia.getDireccion());
            descripcion.setText(emergencia.getDescripcion());
            if (emergencia.getTipo() != null) {
                categoria.getItems().stream().filter(t -> t.getId().equals(emergencia.getTipo().getId()))
                        .findFirst().ifPresent(categoria::setValue);
            }
            escuadra.setValue(emergencia.getEscuadra());
            prioridad.setValue(emergencia.getPrioridad());
        }
        var campos = new GridPane();
        campos.setHgap(10);
        campos.setVgap(10);
        campos.addRow(0, etiqueta("N.º de acta", acta), acta);
        campos.addRow(1, new Label("Fecha del reporte"), fechaReporte.contenido);
        campos.addRow(2, etiqueta("Escuadra", escuadra), escuadra);
        campos.addRow(3, etiqueta("Solicitante", solicitante), solicitante);
        campos.addRow(4, etiqueta("Teléfono", telefono), telefono);
        campos.addRow(5, etiqueta("Dirección", direccion), direccion);
        campos.addRow(6, etiqueta("Tipo", categoria), opcional(categoria));
        campos.addRow(7, etiqueta("Prioridad", prioridad), opcional(prioridad));
        campos.addRow(8, etiqueta("Descripción", descripcion), descripcion);
        campos.add(ubicacion, 0, 9, 2, 1);
        guardarEnDialogo(emergencia == null ? "Abrir acta" : "Editar acta", campos, () -> {
            var datos = new Emergencia.DatosActa(acta.getText(), fechaReporte.valor(), escuadra.getEditor().getText(),
                    solicitante.getText(), telefono.getText(), direccion.getText(), descripcion.getText(),
                    categoria.getValue() == null ? null : categoria.getValue().getId(), prioridad.getValue(),
                    ubicacion.latitud(), ubicacion.longitud());
            emergencias.guardarActa(sesion, emergencia == null ? null : emergencia.getId(),
                    emergencia == null ? 0 : emergencia.getVersion(), datos);
        });
    }

    @FXML
    private void verDetalle() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null) return;
        intentar(() -> ficha(seleccion.emergencia().getId()));
    }

    private void ficha(Integer id) {
        Registro registro = emergencias.buscar(sesion, id);
        Emergencia e = registro.emergencia();
        var informacion = new Label(String.join("\n",
                "Reporte · " + fecha(e.getFechaRegistro()) + " · " + texto(registro.usuario()),
                "Tipo · " + texto(e.getTipo()) + "    Escuadra · " + texto(e.getEscuadra()),
                "Solicitante · " + texto(e.getSolicitante()) + "    Teléfono · " + texto(e.getTelefono()),
                "Dirección · " + texto(e.getDireccion()),
                "Ubicación · " + (e.getLatitud() == null ? "—" : e.getLatitud() + ", " + e.getLongitud()),
                "Estado · " + e.getEstado() + "    Prioridad · " + texto(e.getPrioridad()),
                "Resultado · " + texto(e.getResultado()), "Descripción · " + texto(e.getDescripcion()),
                "Descripción final · " + texto(e.getDescripcionFinal())));
        informacion.setWrapText(true);

        var historial = new Label(emergencias.listarAsignaciones(sesion, id).stream()
                .map(x -> texto(x.unidad()) + " · " + texto(x.nombre()) + " " + texto(x.apellido()))
                .collect(Collectors.joining("\n")));
        historial.setWrapText(true);
        var movimientos = new DespachoController(sesion, false, despacho).vistaActa(id);
        VBox datos = new VBox(8, informacion);
        if (!historial.getText().isEmpty()) {
            datos.getChildren().addAll(new Label("Asignaciones anteriores sin horarios"), historial);
        }
        datos.getChildren().add(movimientos);
        VBox.setVgrow(movimientos, javafx.scene.layout.Priority.ALWAYS);
        var ventana = dialogo("Acta " + numero(e), datos);
        ventana.setResizable(true);
        ventana.getDialogPane().setPrefSize(1060, 650);
        ventana.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        var editarActa = new ButtonType("Editar acta", ButtonBar.ButtonData.OTHER);
        var cerrarActa = new ButtonType("Cerrar acta", ButtonBar.ButtonData.OTHER);
        if (e.getEstado() != EstadoEmergencia.CERRADA) {
            ventana.getDialogPane().getButtonTypes().addAll(editarActa, cerrarActa);
        }
        ButtonType accion = ventana.showAndWait().orElse(ButtonType.CLOSE);
        if (accion == editarActa) formularioEmergencia(emergencias.buscar(sesion, id).emergencia());
        if (accion == cerrarActa) formularioCierre(id);
        filtrar();
    }

    @FXML
    private void eliminar() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null || !confirmar("Eliminar la emergencia seleccionada")) return;
        Emergencia e = seleccion.emergencia();
        if (intentar(() -> emergencias.eliminar(sesion, e.getId(), e.getVersion()))) {
            filtrar();
            mensaje(Alert.AlertType.INFORMATION, "Emergencia eliminada.");
        }
    }

    @FXML
    private void cerrar() {
        Registro seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion != null) intentar(() -> formularioCierre(seleccion.emergencia().getId()));
    }

    private void formularioCierre(Integer id) {
        Emergencia e = emergencias.buscar(sesion, id).emergencia();
        List<Salida> pendientes = despacho.consultarSalidas(sesion, id).stream()
                .filter(x -> x.retorno() == null).toList();
        if (!pendientes.isEmpty()) {
            throw new IllegalStateException("Unidades pendientes de retorno: " + pendientes.stream()
                    .map(Salida::unidad).distinct().collect(Collectors.joining(", ")));
        }
        var descripcion = new TextArea(e.getDescripcion());
        descripcion.setWrapText(true);
        descripcion.setPrefRowCount(4);
        var resultado = new ComboBox<ResultadoEmergencia>();
        resultado.getItems().setAll(ResultadoEmergencia.values());
        guardarEnDialogo("Cerrar acta " + numero(e),
                new VBox(8, etiqueta("Descripción final", descripcion), descripcion,
                        etiqueta("Resultado", resultado), resultado),
                () -> despacho.cerrarEmergencia(sesion, id, e.getVersion(), descripcion.getText(), resultado.getValue()));
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
                        && intentar(() -> tipos.eliminar(sesion, seleccionado.getId(), seleccionado.getVersion()))) {
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
                campos, () -> tipos.guardar(sesion, actual == null ? null : actual.getId(),
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

    private javafx.scene.layout.HBox opcional(ComboBox<?> campo) {
        var limpiar = new Button("Quitar");
        limpiar.setOnAction(evento -> campo.setValue(null));
        return new javafx.scene.layout.HBox(8, campo, limpiar);
    }

    private void agregarColumna(String titulo, double ancho, Function<Registro, String> valor) {
        agregarColumna(titulo, ancho, valor, tabla.getColumns().size());
    }

    private void agregarColumna(String titulo, double ancho, Function<Registro, String> valor, int posicion) {
        var columna = new TableColumn<Registro, String>(titulo);
        columna.setPrefWidth(ancho);
        columna(columna, valor);
        tabla.getColumns().add(posicion, columna);
    }

    private String resumen(Registro acta, Function<Salida, String> valor) {
        List<Salida> movimientos = salidas.getOrDefault(acta.emergencia().getId(), List.of());
        return movimientos.isEmpty() ? "—" : movimientos.stream().map(x -> "#" + x.id() + " " + valor.apply(x))
                .collect(Collectors.joining(" | "));
    }

    private static String numero(Emergencia e) {
        return e.getNumeroActa() == null ? "#" + e.getId() : e.getNumeroActa();
    }

    private static String fecha(LocalDateTime valor) {
        return valor == null ? "Pendiente" : FECHA.format(valor);
    }

    private static String texto(Object valor) {
        return valor == null ? "—" : valor.toString();
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
        } catch (IllegalArgumentException | IllegalStateException | SecurityException error) {
            mensaje(Alert.AlertType.ERROR, error.getMessage());
        } catch (OptimisticLockException error) {
            mensaje(Alert.AlertType.ERROR, "El registro cambió. Actualice el listado e intente de nuevo.");
        } catch (ConstraintViolationException error) {
            String texto = "23505".equals(error.getSQLState())
                    ? "Ya existe un registro con ese número de acta o nombre de tipo."
                    : "No se pudo guardar o eliminar el registro por sus referencias en la base de datos.";
            mensaje(Alert.AlertType.ERROR, texto);
        } catch (RuntimeException error) {
            LOG.log(Level.WARNING, "Error en el módulo de emergencias.", error);
            mensaje(Alert.AlertType.ERROR, "No se pudo completar la operación. Consulte el registro de la aplicación.");
        }
        return false;
    }
}
