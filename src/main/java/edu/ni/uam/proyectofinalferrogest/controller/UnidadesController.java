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
    private List<Equipo> equipos = List.of();
    @FXML private VBox root;
    @FXML private VBox formulario;
    @FXML private HBox asignacion;
    @FXML private TableView<Unidad> tabla;
    @FXML private TableView<Equipo> dotacion;
    @FXML private TableColumn<Unidad,String> codigoCol,tipoCol,estadoCol;
    @FXML private TableColumn<Equipo,String> nombreCol,categoriaCol,cantidadCol,condicionCol;
    @FXML private TextField codigo,tipo,buscar;
    @FXML private ComboBox<EstadoUnidad> estado;
    @FXML private ComboBox<Equipo> disponibles;
    @FXML private Label aviso,tituloForm,contador;
    @FXML private ProgressIndicator cargando;
    public UnidadesController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("unidades.fxml",this); }
    @FXML private void initialize() {
        Ui.carga(root,cargando);
        Ui.columna(codigoCol,Unidad::getCodigo); Ui.columna(tipoCol,Unidad::getTipo); Ui.estado(estadoCol,Unidad::getEstado);
        Ui.columna(nombreCol,Equipo::getNombre); Ui.columna(categoriaCol,Equipo::getCategoria);
        Ui.columna(cantidadCol,Equipo::getCantidad); Ui.estado(condicionCol,Equipo::getEstado);
        estado.getItems().setAll(EstadoUnidad.DISPONIBLE,EstadoUnidad.MANTENIMIENTO); Ui.opciones(estado);
        Ui.visible(formulario,sesion.rol()==Rol.ADMIN);
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
        Ui.ejecutar(root,()->servicio.consultar(sesion),datos->{
            equipos=datos.equipos(); registros.setAll(datos.unidades());
            disponibles.getItems().setAll(equipos.stream().filter(e->e.getUnidad()==null && e.getCantidad()>0 && e.getEstado()==EstadoEquipo.OPERATIVO).toList());
            tabla.getSelectionModel().select(datos.unidades().stream().filter(u->u.getId().equals(id)).findFirst().orElse(null));
            mostrar(tabla.getSelectionModel().getSelectedItem());
        });
    }
    private void mostrar(Unidad u) {
        codigo.setText(u==null ? "" : u.getCodigo()); tipo.setText(u==null ? "" : u.getTipo());
        estado.setValue(u==null ? EstadoUnidad.DISPONIBLE : u.getEstado());
        tituloForm.setText(u==null ? "Nueva unidad" : "Detalle de "+u.getCodigo());
        dotacion.getItems().setAll(equipos.stream().filter(e->u!=null && e.getUnidad()!=null && u.getId().equals(e.getUnidad().getId())).toList());
        asignacion.setDisable(u==null || u.getEstado().enSalida());
        aviso.setText(u==null ? "Selecciona una unidad para ver su dotación." : "Dotación de "+u.getCodigo()+" · "+dotacion.getItems().size()+" lotes");
    }
    @FXML private void nueva() { tabla.getSelectionModel().clearSelection(); mostrar(null); codigo.requestFocus(); }
    @FXML private void guardar() {
        Integer id=idUnidad(); String c=codigo.getText(),t=tipo.getText(); EstadoUnidad valor=estado.getValue();
        Ui.ejecutar(root,()->servicio.guardar(sesion,id,c,t,valor),this::recargar);
    }
    @FXML private void asignar() { mover(disponibles.getValue(),true); }
    @FXML private void retirar() { mover(dotacion.getSelectionModel().getSelectedItem(),false); }
    private void mover(Equipo e,boolean asignar) {
        Integer id=idUnidad();
        if (id==null || e==null) { aviso.setText("Selecciona una unidad y un recurso."); return; }
        Ui.ejecutar(root,()->{ servicio.moverEquipo(sesion,id,e.getId(),asignar); return true; },ok->recargar(id));
    }
}
