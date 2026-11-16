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

public class UsuarioController {
    @FXML private VBox root;
    @FXML private TableView<Usuario> tabla;
    @FXML private TableColumn<Usuario,String> colUsuario,colRol,colBombero,colActivo;
    @FXML private TextField buscar,username;
    @FXML private PasswordField password;
    @FXML private ComboBox<Rol> rol;
    @FXML private ComboBox<Bombero> bombero;
    @FXML private CheckBox activo;
    @FXML private Label tituloForm,contador,mensaje;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final UsuarioService servicio=new UsuarioService();
    private final ObservableList<Usuario> registros=FXCollections.observableArrayList();
    private Integer id;
    private record Datos(List<Usuario> usuarios,List<Bombero> bomberos) {}
    public UsuarioController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("usuarios.fxml",this); }
    @FXML private void initialize() {
        rol.getItems().setAll(Rol.values()); Ui.opciones(rol); Ui.carga(root,cargando);
        Ui.columna(colUsuario,Usuario::getUsername); Ui.estado(colRol,Usuario::getRol);
        Ui.columna(colBombero,u -> u.getBombero()==null?"Sin vínculo":u.getBombero().getNombre()+" "+u.getBombero().getApellido()); Ui.estado(colActivo,u -> u.getActivo()?"Activa":"Inactiva");
        Ui.buscar(tabla,buscar,registros,u -> u.getUsername()+" "+Ui.nombre(u.getRol())+" "+(u.getBombero()==null?"":u.getBombero()),contador);
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,u) -> {
            if (u==null) return; id=u.getId(); tituloForm.setText("Editar usuario"); username.setText(u.getUsername()); password.clear(); rol.setValue(u.getRol()); activo.setSelected(u.getActivo());
            bombero.setValue(u.getBombero()==null?null:bombero.getItems().stream().filter(b -> b.getId().equals(u.getBombero().getId())).findFirst().orElse(null));
        }); cargar();
    }
    @FXML private void cargar() { recargar("Administra las cuentas y sus permisos de acceso."); }
    private void recargar(String texto) { Ui.ejecutar(root,() -> new Datos(servicio.listar(sesion),new BomberoService().listar(sesion)),d -> {
        limpiar(); bombero.getItems().setAll(d.bomberos()); registros.setAll(d.usuarios()); mensaje.setText(texto);
    }); }
    @FXML private void limpiar() { id=null; tabla.getSelectionModel().clearSelection(); tituloForm.setText("Nuevo usuario"); username.clear(); password.clear(); rol.setValue(Rol.BOMBERO); bombero.setValue(null); activo.setSelected(true); }
    @FXML private void quitarBombero() { bombero.setValue(null); }
    @FXML private void guardar() {
        Integer actual=id,bid=bombero.getValue()==null?null:bombero.getValue().getId(); String n=username.getText(),p=password.getText(); Rol r=rol.getValue(); boolean a=activo.isSelected();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,p,r,bid,a); return true; },ok -> recargar("Cuenta guardada correctamente."));
    }
}
