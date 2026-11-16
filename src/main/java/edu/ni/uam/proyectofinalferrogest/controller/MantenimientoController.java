package edu.ni.uam.proyectofinalferrogest.controller;

import java.time.LocalDate;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class MantenimientoController {
    @FXML private VBox root;
    @FXML private VBox formulario;
    @FXML private ComboBox<Equipo> equipo;
    @FXML private TableView<MantenimientoEquipo> tabla;
    @FXML private TableColumn<MantenimientoEquipo,String> colFecha,colDescripcion,colProxima,colAutor;
    @FXML private DatePicker fecha,proxima;
    @FXML private TextArea descripcion;
    @FXML private Label contador,mensaje,detalle;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final MantenimientoService servicio=new MantenimientoService();
    public MantenimientoController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("mantenimientos.fxml",this); }
    @FXML private void initialize() {
        Ui.visible(formulario,sesion.rol()==Rol.ADMIN); Ui.carga(root,cargando); Ui.fecha(fecha); Ui.fecha(proxima); fecha.setValue(LocalDate.now());
        Ui.columna(colFecha,m -> Ui.fecha(m.getFecha())); Ui.columna(colDescripcion,MantenimientoEquipo::getDescripcion);
        Ui.columna(colProxima,m -> m.getProximaFecha()==null?"Sin programar":Ui.fecha(m.getProximaFecha())); Ui.columna(colAutor,m -> m.getRegistradoPor().getUsername());
        Ui.contador(contador,tabla.getItems());
        equipo.valueProperty().addListener((o,a,b) -> { tabla.getItems().clear(); detalle.setText("Selecciona una fila para leer la descripción completa."); });
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> { if (b!=null) detalle.setText(b.getDescripcion()); });
        cargarEquipos();
    }
    @FXML private void cargarEquipos() { Ui.ejecutar(root,() -> new EquipoService().listar(sesion),rows -> {
        equipo.setValue(null); equipo.getItems().setAll(rows); tabla.getItems().clear(); mensaje.setText("Selecciona un equipo y consulta su historial.");
    }); }
    private Integer equipoId() { if (equipo.getValue()==null) throw new IllegalArgumentException("Selecciona un equipo antes de continuar."); return equipo.getValue().getId(); }
    @FXML private void historial() { Ui.seguro(root,() -> {
        Integer id=equipoId(); Ui.ejecutar(root,() -> servicio.listar(sesion,id),rows -> {
            tabla.getItems().setAll(rows); mensaje.setText("Historial de "+equipo.getValue().getNombre()+".");
        });
    }); }
    @FXML private void limpiar() { fecha.setValue(LocalDate.now()); proxima.setValue(null); descripcion.clear(); }
    @FXML private void guardar() { Ui.seguro(root,() -> {
        Integer id=equipoId(); LocalDate f=fecha.getValue(),p=proxima.getValue(); String d=descripcion.getText();
        Ui.ejecutar(root,() -> { servicio.registrar(sesion,id,f,d,p); return servicio.listar(sesion,id); },rows -> {
            limpiar(); tabla.getItems().setAll(rows); mensaje.setText("Mantenimiento registrado correctamente.");
        });
    }); }
}
