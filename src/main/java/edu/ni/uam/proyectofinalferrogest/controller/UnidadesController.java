package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public class UnidadesController {
    private final SesionUsuario sesion;
    private final FlotaService servicio = new FlotaService(edu.ni.uam.proyectofinalferrogest.util.HibernateUtil.getSessionFactory());
    private final ObservableList<Unidad> registros = FXCollections.observableArrayList();
    private record Datos(FlotaService.Inventario inventario,List<String> tipos) {}
    private List<Equipo> equipos = List.of();
    private List<DotacionEquipo> dotaciones=List.of();
    @FXML private VBox root;
    @FXML private VBox formulario;
    @FXML private FlowPane asignacion;
    @FXML private TableView<Unidad> tabla;
    @FXML private TableView<DotacionEquipo> dotacion;
    @FXML private TableColumn<Unidad,String> codigoCol,tipoCol,estadoCol;
    @FXML private TableColumn<DotacionEquipo,String> nombreCol,categoriaCol,cantidadCol,condicionCol;
    @FXML private TextField codigo,buscar;
    @FXML private ComboBox<String> tipo;
    @FXML private ComboBox<EstadoUnidad> estado;
    @FXML private ComboBox<Equipo> disponibles;
    @FXML private Spinner<Integer> cantidadAsignar;
    @FXML private Label aviso,tituloForm,contador;
    @FXML private ProgressIndicator cargando;
    @FXML private Button guardarUnidad,nuevaUnidad;
    public UnidadesController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("unidades.fxml",this); }
    @FXML private void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Ui.carga(root,cargando);
        Ui.columna(codigoCol,Unidad::getCodigo); Ui.columna(tipoCol,Unidad::getTipo); Ui.estado(estadoCol,Unidad::getEstado);
        Ui.columna(nombreCol,d->d.getEquipo().getNombre()); Ui.columna(categoriaCol,d->d.getEquipo().getCategoria());
        Ui.columna(cantidadCol,DotacionEquipo::getCantidad); Ui.estado(condicionCol,d->d.getEquipo().getEstado());
        cantidadAsignar.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,100000,1));
        cantidadAsignar.setEditable(true);
        estado.getItems().setAll(EstadoUnidad.DISPONIBLE,EstadoUnidad.MANTENIMIENTO); Ui.opciones(estado);
        Ui.visible(guardarUnidad,sesion.rol()==Rol.ADMIN);
        Ui.visible(nuevaUnidad,sesion.rol()==Rol.ADMIN);
        Ui.visible(asignacion,sesion.rol()==Rol.ADMIN);
        Ui.buscar(tabla,buscar,registros,u -> u.getCodigo()+" "+u.getTipo()+" "+Ui.nombre(u.getEstado()),contador);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs,a,b)->mostrar(b));
        cargar();
    }
    @FXML private void cargar() { recargar(idUnidad()); }
    private Integer idUnidad() {
        Unidad unidad=tabla.getSelectionModel().getSelectedItem();
        return unidad==null ? null : unidad.getId();
    }
    private void recargar(Integer id) {
        Ui.ejecutar(root,()->new Datos(servicio.consultar(sesion),new ConsultasCabina.ConfiguracionService(edu.ni.uam.proyectofinalferrogest.util.HibernateUtil.getSessionFactory()).opciones(sesion).get(GrupoCatalogo.TIPO_UNIDAD)), resultado->{
            var datos=resultado.inventario(); tipo.getItems().setAll(resultado.tipos());
            equipos=datos.equipos(); dotaciones=datos.dotaciones(); registros.setAll(datos.unidades());
            disponibles.getItems().setAll(equipos.stream().filter(e->e.getEstado()==EstadoEquipo.OPERATIVO && disponible(e)>0).toList());
            disponibles.setCellFactory(v->new ListCell<>() { @Override protected void updateItem(Equipo e,boolean empty) { super.updateItem(e,empty); setText(empty||e==null?null:e.getNombre()+" · "+e.getCategoria()+" · disponibles: "+disponible(e)); } });
            disponibles.setButtonCell(new ListCell<>() { @Override protected void updateItem(Equipo e,boolean empty) { super.updateItem(e,empty); setText(empty||e==null?null:e.getNombre()+" · "+e.getCategoria()+" · disponibles: "+disponible(e)); } });
            tabla.getSelectionModel().select(datos.unidades().stream().filter(u->u.getId().equals(id)).findFirst().orElse(null));
            mostrar(tabla.getSelectionModel().getSelectedItem());
        });
    }
    private int disponible(Equipo equipo) {
        int asignado=dotaciones.stream().filter(d->d.getEquipo().getId().equals(equipo.getId())).mapToInt(DotacionEquipo::getCantidad).sum();
        return Math.max(0,equipo.getCantidad()-asignado);
    }
    private void mostrar(Unidad u) {
        codigo.setText(u==null ? "" : u.getCodigo()); Ui.seleccionActual(tipo,u==null ? null : u.getTipo());
        boolean editable=sesion.rol()==Rol.ADMIN && (u==null || !u.getEstado().enSalida());
        codigo.setEditable(editable); tipo.setDisable(!editable); estado.setDisable(!editable); guardarUnidad.setDisable(!editable);
        estado.getItems().setAll(EstadoUnidad.DISPONIBLE,EstadoUnidad.MANTENIMIENTO);
        if (u!=null && u.getEstado().enSalida()) estado.getItems().add(u.getEstado());
        estado.getSelectionModel().select(u==null ? EstadoUnidad.DISPONIBLE : u.getEstado());
        tituloForm.setText(u==null ? "Nueva unidad" : "Detalle de "+u.getCodigo());
        dotacion.getItems().setAll(dotaciones.stream().filter(d->u!=null && u.getId().equals(d.getUnidad().getId())).toList());
        asignacion.setDisable(u==null || u.getEstado().enSalida());
        aviso.setText(u==null ? "Selecciona una unidad para ver su dotación." : "Dotación de "+u.getCodigo()+" · "+dotacion.getItems().size()+" lotes");
    }
    @FXML private void nueva() { tabla.getSelectionModel().clearSelection(); mostrar(null); codigo.requestFocus(); }
    @FXML private void guardar() {
        Integer id=idUnidad(); String c=codigo.getText(),t=tipo.getValue(); EstadoUnidad valor=estado.getValue();
        Ui.ejecutar(root,()->servicio.guardar(sesion,id,c,t,valor),this::recargar);
    }
    @FXML private void asignar() { mover(disponibles.getValue(),true); }
    @FXML private void retirar() {
        DotacionEquipo seleccion=dotacion.getSelectionModel().getSelectedItem();
        if (seleccion==null) { aviso.setText("Selecciona un recurso de la dotación."); return; }
        mover(seleccion.getEquipo(),false);
    }
    private void mover(Equipo e,boolean asignar) {
        Integer id=idUnidad();
        if (id==null || e==null) { aviso.setText("Selecciona una unidad y un recurso."); return; }
        int cantidad=cantidadAsignar.getValue();
        int cantidadFinal=cantidad;
        Ui.ejecutar(root,()->{ servicio.moverEquipo(sesion,id,e.getId(),cantidadFinal,asignar); return true; },ok->recargar(id));
    }
}
