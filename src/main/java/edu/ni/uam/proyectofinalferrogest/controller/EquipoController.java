package edu.ni.uam.proyectofinalferrogest.controller;

import java.util.List;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class EquipoController {
    @FXML private VBox root;
    @FXML private VBox formulario;
    @FXML private TableView<Equipo> tabla;
    @FXML private TableColumn<Equipo,String> colNombre,colCategoria,colCantidad,colEstado,colUnidad,colMantenimiento;
    @FXML private TextField buscar,nombre,categoria,cantidad;
    @FXML private ComboBox<EstadoEquipo> estado;
    @FXML private ComboBox<Unidad> unidad;
    @FXML private Label tituloForm,contador,mensaje;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final EquipoService servicio=new EquipoService();
    private final ObservableList<Equipo> registros=FXCollections.observableArrayList();
    private Integer id;
    private record Datos(List<Equipo> equipos,List<Unidad> unidades) {}
    public EquipoController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("equipos.fxml",this); }
    @FXML private void initialize() {
        estado.getItems().setAll(EstadoEquipo.values()); Ui.opciones(estado); Ui.visible(formulario,sesion.rol()==Rol.ADMIN); Ui.carga(root,cargando);
        Ui.columna(colNombre,Equipo::getNombre); Ui.columna(colCategoria,Equipo::getCategoria); Ui.columna(colCantidad,Equipo::getCantidad); Ui.estado(colEstado,Equipo::getEstado);
        Ui.columna(colUnidad,e -> e.getUnidad()==null?"Sin unidad":e.getUnidad().getCodigo()); Ui.columna(colMantenimiento,e -> Ui.fecha(e.getUltimoMantenimiento()));
        Ui.buscar(tabla,buscar,registros,e -> e.getNombre()+" "+e.getCategoria()+" "+Ui.nombre(e.getEstado())+" "+(e.getUnidad()==null?"":e.getUnidad().getCodigo()),contador);
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,e) -> {
            if (e==null) return; id=e.getId(); tituloForm.setText("Editar equipo"); nombre.setText(e.getNombre()); categoria.setText(e.getCategoria()); cantidad.setText(""+e.getCantidad()); estado.setValue(e.getEstado());
            unidad.setValue(e.getUnidad()==null?null:unidad.getItems().stream().filter(u -> u.getId().equals(e.getUnidad().getId())).findFirst().orElse(null));
        }); cargar();
    }
    @FXML private void cargar() { recargar("Selecciona un equipo para consultar o editar sus datos."); }
    private void recargar(String texto) { Ui.ejecutar(root,() -> new Datos(servicio.listar(sesion),servicio.unidades(sesion)),d -> {
        limpiar(); unidad.getItems().setAll(d.unidades()); registros.setAll(d.equipos()); mensaje.setText(texto);
    }); }
    @FXML private void limpiar() { id=null; tabla.getSelectionModel().clearSelection(); tituloForm.setText("Nuevo equipo"); nombre.clear(); categoria.clear(); cantidad.setText("1"); estado.setValue(EstadoEquipo.OPERATIVO); unidad.setValue(null); }
    @FXML private void quitarUnidad() { unidad.setValue(null); }
    @FXML private void guardar() { Ui.seguro(root,() -> {
        int c; try { c=Integer.parseInt(cantidad.getText().strip()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Cantidad debe ser un número entero."); }
        Integer actual=id,uid=unidad.getValue()==null?null:unidad.getValue().getId(); String n=nombre.getText(),cat=categoria.getText(); EstadoEquipo es=estado.getValue();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,cat,c,es,uid); return true; },ok -> recargar("Equipo guardado correctamente."));
    }); }
}
