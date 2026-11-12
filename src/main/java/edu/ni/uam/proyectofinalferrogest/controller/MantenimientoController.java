package edu.ni.uam.proyectofinalferrogest.controller;

import java.time.LocalDate;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;

public class MantenimientoController {
    private final SesionUsuario sesion;
    private final MantenimientoService servicio = new MantenimientoService();
    private final TableView<MantenimientoEquipo> tabla=Ui.tabla();
    private final ComboBox<Equipo> equipo=new ComboBox<>();
    private final DatePicker fecha=new DatePicker(LocalDate.now()),proxima=new DatePicker();
    private final TextArea descripcion=new TextArea();
    private VBox root;
    public MantenimientoController(SesionUsuario sesion) { this.sesion=sesion; }
    public VBox vista() {
        Ui.columna(tabla,"Fecha",MantenimientoEquipo::getFecha); Ui.columna(tabla,"Descripción",MantenimientoEquipo::getDescripcion);
        Ui.columna(tabla,"Próxima fecha",MantenimientoEquipo::getProximaFecha); Ui.columna(tabla,"Registrado por",MantenimientoEquipo::getRegistradoPor);
        equipo.setPromptText("Selecciona un equipo"); descripcion.setPrefRowCount(3);
        GridPane g=Ui.formulario(); Ui.campo(g,0,"Fecha",fecha); Ui.campo(g,1,"Trabajo realizado",descripcion); Ui.campo(g,2,"Próxima fecha (opcional)",proxima);
        root=Ui.pagina("Mantenimiento de equipos",new HBox(10,equipo,Ui.boton("Actualizar equipos",this::cargarEquipos),Ui.boton("Consultar historial",this::historial)),tabla);
        if (sesion.rol()==Rol.ADMIN) root.getChildren().addAll(g,Ui.boton("Registrar mantenimiento",this::guardar));
        equipo.valueProperty().addListener((o,a,b) -> tabla.getItems().clear());
        cargarEquipos(); return root;
    }
    private void cargarEquipos() { Ui.ejecutar(root,() -> new EquipoService().listar(sesion),rows -> {
        equipo.setValue(null); equipo.getItems().setAll(rows); tabla.getItems().clear();
    }); }
    private Integer equipoId() { if (equipo.getValue()==null) throw new IllegalArgumentException("Selecciona un equipo."); return equipo.getValue().getId(); }
    private void historial() { Integer id=equipoId(); Ui.ejecutar(root,() -> servicio.listar(sesion,id),rows -> tabla.getItems().setAll(rows)); }
    private void guardar() {
        Integer id=equipoId(); LocalDate f=fecha.getValue(),p=proxima.getValue(); String d=descripcion.getText();
        Ui.ejecutar(root,() -> { servicio.registrar(sesion,id,f,d,p); return true; },ok -> {
            descripcion.clear(); fecha.setValue(LocalDate.now()); proxima.setValue(null); historial();
        });
    }
}