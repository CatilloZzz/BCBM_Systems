package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.model.TipoServicio;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.DespachoService.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Pantallas JavaFX de despacho y otros servicios; ambas usan el mismo servicio. */
public class DespachoController {
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final SesionUsuario sesion;
    private final boolean otros;
    private final DespachoService servicio;
    private Integer actaFija;
    @FXML private VBox root;
    @FXML private Label titulo,subtitulo,contador,mensaje;
    @FXML private ComboBox<Integer> mes;
    @FXML private Spinner<Integer> anio;
    @FXML private CheckBox activas;
    @FXML private TableView<Salida> tabla;
    @FXML private TextField buscar;
    private final javafx.collections.ObservableList<Salida> registros=javafx.collections.FXCollections.observableArrayList();
    @FXML private TableColumn<Salida,String> colEstado,colActividad,colEscuadra,colUnidad,colOperador,colJefe,colPersonal,colSalida,colLlegada,colRetorno,colTipo,colDetalle;
    @FXML private Button llegada,retorno,crearSalida;
    @FXML private ProgressIndicator cargando;
    // Detail panel
    @FXML private FlowPane detalle;
    @FXML private Label detalleUnidad,detalleEstado,detalleTripulacion,detalleSalida;

    public DespachoController(SesionUsuario sesion,boolean otros) {
        this(sesion,otros,new DespachoService(HibernateUtil.getSessionFactory()));
    }
    public DespachoController(SesionUsuario sesion,boolean otros,DespachoService servicio) {
        this.sesion=sesion; this.otros=otros; this.servicio=servicio;
    }
    public Parent vista() { return Vistas.cargar("despacho.fxml",this); }
    public Parent vistaActa(Integer id) {
        actaFija = id;
        return vista();
    }
    @FXML private void initialize() {
        crearSalida.setText(otros ? "Nuevo servicio" : "Despachar unidad");
        titulo.setText(otros ? "Otros servicios del mes" : "Despacho y movimientos");
        subtitulo.setText(otros ? "Registra coberturas, gestiones, taller y capacitaciones." : "Consulta las salidas, tripulaciones y retornos de cada unidad.");
        for (int n=1;n<=12;n++) mes.getItems().add(n);
        mes.setValue(LocalDate.now().getMonthValue());
        anio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2000,2100,LocalDate.now().getYear()));

        // Status column with colored badges
        Ui.columna(colEstado, this::estadoTexto);
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final Label etiqueta=new Label();
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty); setText(null);
                if (empty || item==null) { setGraphic(null); return; }
                etiqueta.setText(item); etiqueta.getStyleClass().setAll("status-badge",estadoClase(item));
                setGraphic(etiqueta);
            }
        });

        Ui.columna(colActividad,this::actividad);
        Ui.buscar(tabla,buscar,registros,x -> String.join(" ",actividad(x),x.unidad(),
            nombres(x.participantes()),x.escuadraDepartamento(),Objects.toString(x.tipoServicio(),""),
            Objects.toString(x.detalle(),"")),contador);
        Ui.columna(colEscuadra,Salida::escuadraDepartamento); Ui.columna(colUnidad,Salida::unidad);
        Ui.columna(colOperador,x -> x.operador().nombre()); Ui.columna(colJefe,x -> x.jefe()==null ? "—" : x.jefe().nombre());
        Ui.columna(colPersonal,x -> nombres(x.participantes()));
        Ui.columna(colSalida,x -> fecha(x.salida())); Ui.columna(colLlegada,x -> fecha(x.llegada()));
        Ui.columna(colRetorno,x -> fecha(x.retorno())); Ui.columna(colTipo,Salida::tipoServicio); Ui.columna(colDetalle,Salida::detalle);
        colEscuadra.setVisible(true); colTipo.setVisible(otros); colLlegada.setVisible(!otros);
        colRetorno.setText(otros ? "Entrada" : "Retorna");
        retorno.setText(otros ? "Registrar entrada" : "Registrar retorno");
        if (actaFija != null) {
            titulo.setText("Salidas del acta");
            mes.setDisable(true);
            anio.setDisable(true);
            activas.setDisable(true);
        }
        // La cronología se consulta junto a la unidad sin desplazar las columnas de personal.
        tabla.getColumns().removeAll(List.of(colSalida,colLlegada,colRetorno));
        tabla.getColumns().remove(colUnidad);
        tabla.getColumns().add(2,colUnidad);
        tabla.getColumns().addAll(3,List.of(colSalida,colLlegada,colRetorno));
        tabla.setTableMenuButtonVisible(true);
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> botones());
        activas.selectedProperty().addListener((o,a,b) -> { mes.setDisable(b); anio.setDisable(b); actualizar(); });
        Ui.carga(root,cargando); botones(); actualizar();
    }

    // ── Movement state helpers ────────────────────────────────────────────
    private String estadoTexto(Salida s) {
        if (s.retorno() != null) return "Retornó";
        if (otros) return "En servicio";
        if (s.llegada() != null) return "En lugar";
        return "En camino";
    }
    private static String estadoClase(String estado) {
        return switch (estado) {
            case "En camino" -> "status-danger";
            case "En lugar", "En servicio"  -> "status-warning";
            case "Retornó"   -> "status-ok";
            default          -> "status-neutral";
        };
    }

    private String actividad(Salida salida) {
        if (otros) return "Servicio #"+salida.servicioId();
        return salida.numeroActa()==null ? "Emergencia #"+salida.emergenciaId() : "Acta "+salida.numeroActa();
    }
    private static String nombres(List<Persona> personas) { return personas.stream().map(Persona::nombre).collect(Collectors.joining(", ")); }
    private static String fecha(LocalDateTime valor) { return valor==null ? "Pendiente" : FECHA.format(valor); }

    // ── Detail panel + button state ───────────────────────────────────────
    private void botones() {
        Salida x=tabla.getSelectionModel().getSelectedItem();
        boolean haySeleccion = x != null;

        // Show / hide context panel
        detalle.setVisible(haySeleccion);
        detalle.setManaged(haySeleccion);

        if (haySeleccion) {
            // Unit name
            detalleUnidad.setText(x.unidad());

            // Status badge
            String estado = estadoTexto(x);
            detalleEstado.setText(estado);
            detalleEstado.getStyleClass().removeAll("status-ok","status-danger","status-warning","status-neutral");
            detalleEstado.getStyleClass().add(estadoClase(estado));

            // Crew info
            String tripulacion = "Operador: " + x.operador().nombre();
            if (x.jefe() != null) tripulacion += "  •  Jefe: " + x.jefe().nombre();
            if (!x.participantes().isEmpty()) tripulacion += "  •  " + nombres(x.participantes());
            detalleTripulacion.setText(tripulacion);

            // Last movement timestamp
            if (x.retorno() != null) {
                detalleSalida.setText("Retornó: " + FECHA.format(x.retorno()));
            } else if (x.llegada() != null) {
                detalleSalida.setText("Llegada: " + FECHA.format(x.llegada()));
            } else {
                detalleSalida.setText("Salida: " + FECHA.format(x.salida()));
            }

            // Contextual action buttons
            boolean puedeMarcarLlegada = x.llegada() == null && x.retorno() == null;
            boolean puedeRetornar = x.retorno() == null && (otros || x.llegada() != null);

            llegada.setDisable(!puedeMarcarLlegada);
            llegada.setVisible(!otros);
            llegada.setManaged(!otros);
            llegada.setText(puedeMarcarLlegada ? "Registrar llegada de " + x.unidad() : "Registrar llegada");

            if (x.retorno() != null) {
                retorno.setText("Completado");
                retorno.setDisable(true);
            } else {
                String accion = otros ? "Registrar entrada de " : "Registrar retorno de ";
                retorno.setText(puedeRetornar ? accion + x.unidad() : (otros ? "Registrar entrada" : "Registrar retorno"));
                retorno.setDisable(!puedeRetornar);
            }
        } else {
            // No selection — reset buttons
            llegada.setDisable(true);
            llegada.setVisible(!otros);
            llegada.setManaged(!otros);
            llegada.setText("Registrar llegada");
            retorno.setDisable(true);
            retorno.setText(otros ? "Registrar entrada" : "Registrar retorno");
        }
    }

    @FXML private void actualizar() {
        YearMonth periodo=YearMonth.of(anio.getValue(),mes.getValue()); boolean soloActivas=activas.isSelected();
        Ui.ejecutar(root,() -> actaFija != null ? servicio.consultarSalidas(sesion,actaFija)
                : soloActivas ? servicio.listarActivas(sesion,otros) : servicio.listarMes(sesion,periodo,otros),rows -> {
            registros.setAll(rows); botones();
        });
    }
    @FXML private void nueva() {
        Ui.ejecutar(root,() -> servicio.catalogos(sesion),this::formulario);
    }
    private void formulario(Catalogos datos) {
        Dialog<ButtonType> dialog=new Dialog<>(); dialog.setTitle(otros ? "Nueva salida de servicio" : "Despachar unidad"); Ui.dialogo(dialog,root);
        ButtonType guardar=new ButtonType("Confirmar salida",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardar,ButtonType.CANCEL);
        ComboBox<Acta> acta=new ComboBox<>(); acta.getItems().setAll(datos.emergencias());
        ComboBox<Vehiculo> unidad=new ComboBox<>();
        unidad.getItems().setAll(datos.unidades().stream().filter(u -> u.estado().equals(
                edu.ni.uam.proyectofinalferrogest.model.EstadoUnidad.DISPONIBLE.toString())).toList());
        unidad.setPromptText(unidad.getItems().isEmpty() ? "Sin unidades disponibles" : "Seleccionar unidad");
        if (actaFija != null) {
            acta.getItems().stream().filter(a -> a.id().equals(actaFija)).findFirst().ifPresent(acta::setValue);
            acta.setDisable(true);
        }
        ComboBox<Persona> operador=new ComboBox<>(); operador.getItems().setAll(datos.personal());
        ComboBox<Persona> jefe=new ComboBox<>(); jefe.getItems().setAll(datos.personal());
        Button quitarJefe=new Button("Sin jefe"); quitarJefe.setOnAction(e -> jefe.setValue(null));
        ListView<Persona> personal=new ListView<>(); personal.getItems().setAll(datos.personal()); personal.setPrefHeight(140);
        personal.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ComboBox<String> escuadra=new ComboBox<>(); escuadra.setEditable(true); escuadra.setMaxWidth(Double.MAX_VALUE);
        escuadra.getItems().setAll("Escuadra A","Escuadra B","Administración","DPC","Operaciones","Taller"); ComboBox<TipoServicio> tipo=new ComboBox<>(); tipo.getItems().setAll(TipoServicio.values());
        TextArea detalleText=new TextArea(); detalleText.setPrefRowCount(3); detalleText.setWrapText(true);
        FechaHora fecha=new FechaHora();
        VBox campos=new VBox(8);
        if (!otros) campos.getChildren().addAll(new Label("Emergencia *"),acta);
        else campos.getChildren().addAll(new Label("Escuadra o departamento *"),escuadra,new Label("Tipo de gestión *"),tipo,new Label("Detalle *"),detalleText);
        campos.getChildren().addAll(new Label("Unidad disponible *"),unidad,new Label("Operador *"),operador,
            new Label("Jefe de unidad (opcional)"),new HBox(8,jefe,quitarJefe),
            new Label("Otros participantes (Ctrl para seleccionar varios)"),personal,
            new Label("Operador y jefe se incluyen automáticamente en la tripulación."),new Label("Fecha y hora de salida *"),fecha.contenido);
        ScrollPane scroll=new ScrollPane(campos); scroll.setFitToWidth(true); scroll.setPrefViewportWidth(640); scroll.setPrefViewportHeight(480);
        for (var selector : List.of(unidad,operador,jefe,acta,tipo)) selector.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(scroll); dialog.setResizable(true);
        dialog.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
            e.consume(); Ui.seguro(root,() -> {
                if (unidad.getValue()==null || operador.getValue()==null || (!otros && acta.getValue()==null))
                    throw new IllegalArgumentException("Seleccione unidad, operador y emergencia cuando corresponda.");
                var tripulacion=new Tripulacion(unidad.getValue().id(),operador.getValue().id(),jefe.getValue()==null ? null : jefe.getValue().id(),
                    personal.getSelectionModel().getSelectedItems().stream().map(Persona::id).toList(),fecha.valor());
                String departamento=escuadra.getEditor().getText(),texto=detalleText.getText(); TipoServicio gestion=tipo.getValue();
                Integer actaId=acta.getValue()==null ? null : acta.getValue().id();
                dialog.getDialogPane().setDisable(true);
                Ui.ejecutar(root,() -> otros ? servicio.registrarServicio(sesion,departamento,gestion,texto,tripulacion)
                    : servicio.asignarUnidad(sesion,actaId,tripulacion),id -> {
                        dialog.close(); mensaje.setText("Salida #"+id+" registrada. La unidad está ocupada."); actualizar();
                    },error -> { dialog.getDialogPane().setDisable(false); mostrarError(error); });
            });
        });
        dialog.showAndWait();
    }
    @FXML private void marcarLlegada() { movimiento(true); }
    @FXML private void registrarRetorno() { movimiento(false); }
    private void movimiento(boolean esLlegada) {
        Salida seleccion=tabla.getSelectionModel().getSelectedItem(); if (seleccion==null) return;
        registrarMovimiento(root, sesion, servicio, List.of(seleccion), esLlegada, otros, () -> {
            mensaje.setText(esLlegada ? "Llegada registrada." : "Retorno registrado. La unidad está disponible.");
            actualizar();
        });
    }

    static void registrarMovimiento(Parent root, SesionUsuario sesion, DespachoService servicio,
                                    List<Salida> pendientes, boolean esLlegada, boolean otros, Runnable actualizar) {
        Dialog<ButtonType> dialog=new Dialog<>(); Ui.dialogo(dialog,root);
        dialog.setTitle(esLlegada ? "Registrar llegada" : otros ? "Registrar entrada" : "Registrar retorno");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        FechaHora fecha=new FechaHora(); Label error=new Label(); error.setWrapText(true);
        ComboBox<Salida> unidad = new ComboBox<>();
        unidad.getItems().setAll(pendientes);
        unidad.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Salida s) {
                return s == null ? "" : s.unidad() + " · salida " + fecha(s.salida());
            }
            @Override public Salida fromString(String valor) { throw new UnsupportedOperationException(); }
        });
        if (pendientes.size() == 1) unidad.getSelectionModel().selectFirst();
        unidad.setPromptText("Seleccionar unidad");
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(unidad.valueProperty().isNull());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Unidad despachada"), unidad,
                new Label(esLlegada ? "Fecha y hora de llegada al lugar" : "Fecha y hora de retorno"),
                fecha.contenido, error));
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
            e.consume();
            try {
                Salida seleccion = unidad.getValue();
                LocalDateTime valor=fecha.valor(); dialog.getDialogPane().setDisable(true);
                Ui.ejecutar(root,() -> {
                    if (esLlegada) servicio.marcarLlegada(sesion,seleccion.id(),valor);
                    else servicio.registrarRetorno(sesion,seleccion.id(),valor); return true;
                },ok -> { dialog.close(); actualizar.run(); },
                fallo -> { dialog.getDialogPane().setDisable(false); error.setText(Ui.mensaje(fallo)); });
            } catch (RuntimeException fallo) { error.setText(Ui.mensaje(fallo)); }
        });
        dialog.showAndWait();
    }
    private void mostrarError(Throwable error) {
        Alert aviso=new Alert(Alert.AlertType.ERROR,Ui.mensaje(error),ButtonType.OK); Ui.dialogo(aviso,root); aviso.showAndWait();
    }
    /** Fechas completas para conservar movimientos que cruzan medianoche. */
    static class FechaHora {
        final DatePicker dia=new DatePicker(LocalDate.now());
        final Spinner<Integer> hora=new Spinner<>(0,23,LocalTime.now().getHour());
        final Spinner<Integer> minuto=new Spinner<>(0,59,LocalTime.now().getMinute());
        final Spinner<Integer> segundo=new Spinner<>(0,59,LocalTime.now().getSecond());
        final HBox contenido=new HBox(6,dia,new Label("Hora"),hora,new Label(":"),minuto,new Label(":"),segundo);
        FechaHora() { this(LocalDateTime.now()); }
        private LocalDateTime inicial;
        FechaHora(LocalDateTime valor) {
            Ui.fecha(dia);
            for (var reloj : List.of(hora, minuto, segundo)) reloj.setPrefWidth(64);
            segundo.getValueFactory().setValue(valor.getSecond());
            inicial = valor;
            dia.setValue(valor.toLocalDate());
            hora.getValueFactory().setValue(valor.getHour());
            minuto.getValueFactory().setValue(valor.getMinute());
        }
        LocalDateTime valor() {
            if (dia.getValue()==null) throw new IllegalArgumentException("Seleccione la fecha.");
            LocalDateTime valor = dia.getValue().atTime(hora.getValue(),minuto.getValue(),segundo.getValue());
            return inicial.withNano(0).equals(valor) ? inicial : valor;
        }
    }
}
