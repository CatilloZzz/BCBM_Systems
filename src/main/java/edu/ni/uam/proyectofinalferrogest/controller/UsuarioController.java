package edu.ni.uam.proyectofinalferrogest.controller;

import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;

public class UsuarioController {
    private final SesionUsuario sesion;
    private final UsuarioService servicio = new UsuarioService();
    private final TableView<Usuario> tabla = Ui.tabla();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final ComboBox<Rol> rol = new ComboBox<>();
    private final ComboBox<Bombero> bombero = new ComboBox<>();
    private final CheckBox activo = new CheckBox("Cuenta activa");
    private Integer id;
    private VBox root;
    private record Datos(List<Usuario> usuarios,List<Bombero> bomberos) {}
    public UsuarioController(SesionUsuario sesion) { this.sesion = sesion; }
    public VBox vista() {
        rol.getItems().setAll(Rol.values()); bombero.setPromptText("Sin bombero asociado");
        Ui.columna(tabla,"Usuario",Usuario::getUsername); Ui.columna(tabla,"Rol",Usuario::getRol);
        Ui.columna(tabla,"Bombero",Usuario::getBombero); Ui.columna(tabla,"Activa",u -> u.getActivo() ? "Sí" : "No");
        password.setPromptText("Al editar, vacío conserva la contraseña");
        GridPane g=Ui.formulario(); Ui.campo(g,0,"Usuario",username); Ui.campo(g,1,"Contraseña",password); Ui.campo(g,2,"Rol",rol);
        Ui.campo(g,3,"Bombero",new HBox(8,bombero,Ui.boton("Quitar vínculo",() -> bombero.setValue(null)))); Ui.campo(g,4,"Acceso",activo);
        root=Ui.pagina("Usuarios",tabla,g,new HBox(10,Ui.boton("Nuevo",this::limpiar),Ui.boton("Guardar",this::guardar),Ui.boton("Actualizar",this::cargar)),
                new Label("Para desactivar, selecciona la cuenta, desmarca Cuenta activa y guarda."));
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,u) -> {
            if (u==null) return; id=u.getId(); username.setText(u.getUsername()); password.clear(); rol.setValue(u.getRol()); activo.setSelected(u.getActivo());
            bombero.setValue(u.getBombero()==null ? null : bombero.getItems().stream().filter(b -> b.getId().equals(u.getBombero().getId())).findFirst().orElse(null));
        });
        cargar(); return root;
    }
    private void cargar() { Ui.ejecutar(root,() -> new Datos(servicio.listar(sesion),new BomberoService().listar(sesion)),d -> {
        limpiar(); bombero.getItems().setAll(d.bomberos()); tabla.getItems().setAll(d.usuarios());
    }); }
    private void limpiar() { id=null; tabla.getSelectionModel().clearSelection(); username.clear(); password.clear(); rol.setValue(Rol.BOMBERO); bombero.setValue(null); activo.setSelected(true); }
    private void guardar() {
        Integer actual=id,bid=bombero.getValue()==null?null:bombero.getValue().getId();
        String n=username.getText(),p=password.getText(); Rol r=rol.getValue(); boolean a=activo.isSelected(); password.clear();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,p,r,bid,a); return true; },ok -> cargar());
    }
}