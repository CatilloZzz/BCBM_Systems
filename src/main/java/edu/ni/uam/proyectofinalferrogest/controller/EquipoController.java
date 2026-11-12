package edu.ni.uam.proyectofinalferrogest.controller;

import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;

public class EquipoController {
    private final SesionUsuario sesion;
    private final EquipoService servicio = new EquipoService();
    private final TableView<Equipo> tabla = Ui.tabla();
    private final TextField nombre = new TextField(),categoria = new TextField(),cantidad = new TextField("1");
    private final ComboBox<EstadoEquipo> estado = new ComboBox<>();
    private final ComboBox<Unidad> unidad = new ComboBox<>();
    private Integer id;
    private VBox root;
    private record Datos(List<Equipo> equipos,List<Unidad> unidades) {}
    public EquipoController(SesionUsuario sesion) { this.sesion=sesion; }
    public VBox vista() {
        estado.getItems().setAll(EstadoEquipo.values()); estado.setValue(EstadoEquipo.OPERATIVO);
        unidad.setPromptText("Sin unidad");
        Ui.columna(tabla,"Nombre",Equipo::getNombre); Ui.columna(tabla,"Categoría",Equipo::getCategoria); Ui.columna(tabla,"Cantidad",Equipo::getCantidad);
        Ui.columna(tabla,"Estado",Equipo::getEstado); Ui.columna(tabla,"Unidad",Equipo::getUnidad); Ui.columna(tabla,"Último mantenimiento",Equipo::getUltimoMantenimiento);
        GridPane g=Ui.formulario(); Ui.campo(g,0,"Nombre",nombre); Ui.campo(g,1,"Categoría",categoria); Ui.campo(g,2,"Cantidad",cantidad); Ui.campo(g,3,"Estado",estado);
        Ui.campo(g,4,"Unidad",new HBox(8,unidad,Ui.boton("Quitar unidad",() -> unidad.setValue(null))));
        root=Ui.pagina("Inventario de equipos",tabla,Ui.boton("Actualizar listado",this::cargar));
        if (sesion.rol()==Rol.ADMIN) root.getChildren().addAll(g,new HBox(10,Ui.boton("Nuevo",this::limpiar),Ui.boton("Guardar",this::guardar)),
                new Label("Para dar de baja, selecciona el equipo, elige BAJA y guarda. Una fila representa un grupo de piezas."));
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,e) -> {
            if (e==null) return; id=e.getId(); nombre.setText(e.getNombre()); categoria.setText(e.getCategoria()); cantidad.setText(""+e.getCantidad()); estado.setValue(e.getEstado());
            unidad.setValue(e.getUnidad()==null?null:unidad.getItems().stream().filter(u -> u.getId().equals(e.getUnidad().getId())).findFirst().orElse(null));
        }); cargar(); return root;
    }
    private void cargar() { Ui.ejecutar(root,() -> new Datos(servicio.listar(sesion),servicio.unidades(sesion)),d -> {
        limpiar(); unidad.getItems().setAll(d.unidades()); tabla.getItems().setAll(d.equipos());
    }); }
    private void limpiar() { id=null; tabla.getSelectionModel().clearSelection(); nombre.clear(); categoria.clear(); cantidad.setText("1"); estado.setValue(EstadoEquipo.OPERATIVO); unidad.setValue(null); }
    private void guardar() {
        int c; try { c=Integer.parseInt(cantidad.getText().strip()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Cantidad debe ser un número entero."); }
        Integer actual=id,uid=unidad.getValue()==null?null:unidad.getValue().getId(); String n=nombre.getText(),cat=categoria.getText(); EstadoEquipo es=estado.getValue();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,cat,c,es,uid); return true; },ok -> cargar());
    }
}
