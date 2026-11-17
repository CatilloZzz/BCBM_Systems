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
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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
    @FXML private TableColumn<Salida,String> colActividad,colEscuadra,colUnidad,colOperador,colJefe,colPersonal,colSalida,colLlegada,colRetorno,colTipo,colDetalle;
    @FXML private Button llegada,retorno;
    @FXML private ProgressIndicator cargando;

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
        titulo.setText(otros ? "Otros servicios del mes" : "Despacho y movimientos");
        subtitulo.setText(otros ? "Registra coberturas, gestiones, taller y capacitaciones." : "Consulta las salidas, tripulaciones y retornos de cada unidad.");
        for (int n=1;n<=12;n++) mes.getItems().add(n);
        mes.setValue(LocalDate.now().getMonthValue());
        anio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2000,2100,LocalDate.now().getYear()));
        Ui.columna(colActividad,x -> otros ? "Servicio #"+x.servicioId() : "Emergencia #"+x.emergenciaId());
        Ui.columna(colEscuadra,Salida::escuadraDepartamento); Ui.columna(colUnidad,Salida::unidad);
        Ui.columna(colOperador,x -> x.operador().nombre()); Ui.columna(colJefe,x -> x.jefe()==null ? "—" : x.jefe().nombre());
        Ui.columna(colPersonal,x -> nombres(x.participantes()));
        Ui.columna(colSalida,x -> fecha(x.salida())); Ui.columna(colLlegada,x -> fecha(x.llegada()));
        Ui.columna(colRetorno,x -> fecha(x.retorno())); Ui.columna(colTipo,Salida::tipoServicio); Ui.columna(colDetalle,Salida::detalle);
        colEscuadra.setVisible(otros); colTipo.setVisible(otros); colLlegada.setVisible(!otros);
        colRetorno.setText(otros ? "Entrada" : "Retorna"); Ui.visible(llegada,!otros);
        retorno.setText(otros ? "Registrar entrada" : "Registrar retorno");
        if (actaFija != null) {
            titulo.setText("Salidas del acta");
            mes.setDisable(true);
            anio.setDisable(true);
            activas.setDisable(true);
        }
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> botones());
        activas.selectedProperty().addListener((o,a,b) -> { mes.setDisable(b); anio.setDisable(b); actualizar(); });
        Ui.carga(root,cargando); botones(); actualizar();
    }
    private static String nombres(List<Persona> personas) { return personas.stream().map(Persona::nombre).collect(Collectors.joining(", ")); }
    private static String fecha(LocalDateTime valor) { return valor==null ? "Pendiente" : FECHA.format(valor); }
    private void botones() {
        Salida x=tabla.getSelectionModel().getSelectedItem();
        llegada.setDisable(x==null || x.llegada()!=null || x.retorno()!=null);
        retorno.setDisable(x==null || x.retorno()!=null || (!otros && x.llegada()==null));
    }
    @FXML private void actualizar() {
        YearMonth periodo=YearMonth.of(anio.getValue(),mes.getValue()); boolean soloActivas=activas.isSelected();
        Ui.ejecutar(root,() -> actaFija != null ? servicio.consultarSalidas(sesion,actaFija)
                : soloActivas ? servicio.listarActivas(sesion,otros) : servicio.listarMes(sesion,periodo,otros),rows -> {
            tabla.getItems().setAll(rows); contador.setText(rows.size()+" salidas"); botones();
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
        unidad.getItems().setAll(actaFija == null ? datos.unidades()
                : datos.unidades().stream().filter(u -> u.estado().equals(
                        edu.ni.uam.proyectofinalferrogest.model.EstadoUnidad.DISPONIBLE.toString())).toList());
        if (actaFija != null) {
            acta.getItems().stream().filter(a -> a.id().equals(actaFija)).findFirst().ifPresent(acta::setValue);
            acta.setDisable(true);
        }
        ComboBox<Persona> operador=new ComboBox<>(); operador.getItems().setAll(datos.personal());
        ComboBox<Persona> jefe=new ComboBox<>(); jefe.getItems().setAll(datos.personal());
        Button quitarJefe=new Button("Sin jefe"); quitarJefe.setOnAction(e -> jefe.setValue(null));
        ListView<Persona> personal=new ListView<>(); personal.getItems().setAll(datos.personal()); personal.setPrefHeight(140);
        personal.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TextField escuadra=new TextField(); ComboBox<TipoServicio> tipo=new ComboBox<>(); tipo.getItems().setAll(TipoServicio.values());
        TextArea detalle=new TextArea(); detalle.setPrefRowCount(3); detalle.setWrapText(true);
        FechaHora fecha=new FechaHora();
        VBox campos=new VBox(8);
        if (!otros) campos.getChildren().addAll(new Label("Emergencia *"),acta);
        else campos.getChildren().addAll(new Label("Escuadra o departamento *"),escuadra,new Label("Tipo de gestión *"),tipo,new Label("Detalle *"),detalle);
        campos.getChildren().addAll(new Label("Unidad * (las ocupadas no pueden salir)"),unidad,new Label("Operador *"),operador,
            new Label("Jefe de unidad (opcional)"),new HBox(8,jefe,quitarJefe),
            new Label("Otros participantes (Ctrl para seleccionar varios)"),personal,
            new Label("Operador y jefe se incluyen automáticamente en la tripulación."),new Label("Fecha y hora de salida *"),fecha.contenido);
        ScrollPane scroll=new ScrollPane(campos); scroll.setFitToWidth(true); scroll.setPrefViewportWidth(530); scroll.setPrefViewportHeight(570);
        dialog.getDialogPane().setContent(scroll); dialog.setResizable(true);
        dialog.getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
            e.consume(); Ui.seguro(root,() -> {
                if (unidad.getValue()==null || operador.getValue()==null || (!otros && acta.getValue()==null))
                    throw new IllegalArgumentException("Seleccione unidad, operador y emergencia cuando corresponda.");
                var tripulacion=new Tripulacion(unidad.getValue().id(),operador.getValue().id(),jefe.getValue()==null ? null : jefe.getValue().id(),
                    personal.getSelectionModel().getSelectedItems().stream().map(Persona::id).toList(),fecha.valor());
                String departamento=escuadra.getText(),texto=detalle.getText(); TipoServicio gestion=tipo.getValue();
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
        Dialog<ButtonType> dialog=new Dialog<>(); Ui.dialogo(dialog,root);
        dialog.setTitle(esLlegada ? "Registrar llegada" : otros ? "Registrar entrada" : "Registrar retorno");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        FechaHora fecha=new FechaHora(); Label error=new Label(); error.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(10,new Label("Unidad "+seleccion.unidad()+" · salida "+fecha(seleccion.salida())),fecha.contenido,error));
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
            e.consume();
            try {
                LocalDateTime valor=fecha.valor(); dialog.getDialogPane().setDisable(true);
                Ui.ejecutar(root,() -> {
                    if (esLlegada) servicio.marcarLlegada(sesion,seleccion.id(),valor);
                    else servicio.registrarRetorno(sesion,seleccion.id(),valor); return true;
                },ok -> { dialog.close(); mensaje.setText(esLlegada ? "Llegada registrada." : "Retorno registrado. La unidad está disponible."); actualizar(); },
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
        final HBox contenido=new HBox(8,dia,new Label("Hora"),hora,new Label(":"),minuto);
        FechaHora() { Ui.fecha(dia); hora.setPrefWidth(78); minuto.setPrefWidth(78); }
        private LocalDateTime inicial;
        FechaHora(LocalDateTime valor) {
            this();
            inicial = valor;
            dia.setValue(valor.toLocalDate());
            hora.getValueFactory().setValue(valor.getHour());
            minuto.getValueFactory().setValue(valor.getMinute());
        }
        LocalDateTime valor() {
            if (dia.getValue()==null) throw new IllegalArgumentException("Seleccione la fecha.");
            LocalDateTime valor = dia.getValue().atTime(hora.getValue(),minuto.getValue());
            return inicial != null && inicial.withSecond(0).withNano(0).equals(valor) ? inicial : valor;
        }
    }
}
