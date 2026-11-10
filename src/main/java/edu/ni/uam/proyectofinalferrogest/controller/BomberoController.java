package edu.ni.uam.proyectofinalferrogest.controller;

import java.time.LocalDate;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;

public class BomberoController {
    private final SesionUsuario sesion;
    private final BomberoService servicio = new BomberoService();
    private final TableView<Bombero> tabla = Ui.tabla();
    private final TextField nombre = new TextField(), apellido = new TextField(), cedula = new TextField(), rango = new TextField(), telefono = new TextField();
    private final DatePicker ingreso = new DatePicker(LocalDate.now());
    private Integer id;
    private VBox root;
    public BomberoController(SesionUsuario sesion) { this.sesion = sesion; }
    public VBox vista() {
        Ui.columna(tabla,"ID",Bombero::getId); Ui.columna(tabla,"Nombre",b -> b.getNombre()+" "+b.getApellido());
        Ui.columna(tabla,"Cédula",Bombero::getCedula); Ui.columna(tabla,"Rango",Bombero::getRango); Ui.columna(tabla,"Estado",Bombero::getEstado);
        GridPane g = Ui.formulario(); Ui.campo(g,0,"Nombre",nombre); Ui.campo(g,1,"Apellido",apellido); Ui.campo(g,2,"Cédula",cedula);
        Ui.campo(g,3,"Rango",rango); Ui.campo(g,4,"Teléfono",telefono); Ui.campo(g,5,"Fecha de ingreso",ingreso);
        root = Ui.pagina("Personal",tabla,Ui.boton("Actualizar listado",this::cargar));
        if (sesion.rol() == Rol.ADMIN) root.getChildren().addAll(g,new HBox(10,
                Ui.boton("Nuevo",this::limpiar),Ui.boton("Guardar",this::guardar),
                Ui.boton("Dar de baja",() -> baja(true)),Ui.boton("Reactivar",() -> baja(false))));
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            if (b == null) return; id = b.getId(); nombre.setText(b.getNombre()); apellido.setText(b.getApellido());
            cedula.setText(b.getCedula()); rango.setText(b.getRango()); telefono.setText(b.getTelefono()); ingreso.setValue(b.getFechaIngreso());
        });
        cargar(); return root;
    }
    private void cargar() { Ui.ejecutar(root,() -> servicio.listar(sesion),rows -> { limpiar(); tabla.getItems().setAll(rows); }); }
    private void limpiar() { id = null; tabla.getSelectionModel().clearSelection(); nombre.clear(); apellido.clear(); cedula.clear(); rango.clear(); telefono.clear(); ingreso.setValue(LocalDate.now()); }
    private void guardar() {
        Integer actual = id; String n=nombre.getText(),a=apellido.getText(),c=cedula.getText(),r=rango.getText(),t=telefono.getText(); LocalDate f=ingreso.getValue();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,a,c,r,t,f); return true; },ok -> cargar());
    }
    private void baja(boolean baja) {
        if (id == null) throw new IllegalArgumentException("Selecciona un bombero."); Integer actual = id;
        if (!Ui.confirmar(baja ? "¿Dar de baja y desactivar su cuenta? Se conserva el historial." : "¿Reactivar al bombero? Su cuenta se reactiva por separado en Usuarios.")) return;
        Ui.ejecutar(root,() -> { servicio.cambiarBaja(sesion,actual,baja); return true; },ok -> cargar());
    }
}