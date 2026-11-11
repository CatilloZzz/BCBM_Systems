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

public class BomberoController {
    @FXML private VBox root;
    @FXML private VBox formulario;
    @FXML private TableView<Bombero> tabla;
    @FXML private TableColumn<Bombero,String> colNombre,colCedula,colRango,colEstado;
    @FXML private TextField buscar,nombre,apellido,cedula,rango,telefono;
    @FXML private DatePicker ingreso;
    @FXML private Label contador,tituloForm,mensaje;
    @FXML private Button bajaButton,reactivarButton;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final BomberoService servicio=new BomberoService();
    private final ObservableList<Bombero> registros=FXCollections.observableArrayList();
    private Integer id;
    public BomberoController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("personal.fxml",this); }
    @FXML private void initialize() {
        Ui.columna(colNombre,b -> b.getNombre()+" "+b.getApellido()); Ui.columna(colCedula,Bombero::getCedula); Ui.columna(colRango,Bombero::getRango); Ui.estado(colEstado,Bombero::getEstado);
        Ui.buscar(tabla,buscar,registros,b -> b.getNombre()+" "+b.getApellido()+" "+b.getCedula()+" "+b.getRango()+" "+Ui.nombre(b.getEstado()),contador);
        Ui.visible(formulario,sesion.rol()==Rol.ADMIN); Ui.fecha(ingreso); Ui.carga(root,cargando);
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            if (b==null) return; id=b.getId(); tituloForm.setText("Editar bombero"); nombre.setText(b.getNombre()); apellido.setText(b.getApellido());
            cedula.setText(b.getCedula()); rango.setText(b.getRango()); telefono.setText(b.getTelefono()); ingreso.setValue(b.getFechaIngreso());
            bajaButton.setDisable(b.getEstado()!=EstadoBombero.DISPONIBLE); reactivarButton.setDisable(b.getEstado()!=EstadoBombero.DE_BAJA);
        });
        cargar();
    }
    @FXML private void cargar() { recargar("Selecciona un registro para ver sus datos."); }
    private void recargar(String texto) { Ui.ejecutar(root,() -> servicio.listar(sesion),rows -> { limpiar(); registros.setAll(rows); mensaje.setText(texto); }); }
    @FXML private void limpiar() {
        id=null; tabla.getSelectionModel().clearSelection(); tituloForm.setText("Nuevo bombero");
        nombre.clear(); apellido.clear(); cedula.clear(); rango.clear(); telefono.clear(); ingreso.setValue(LocalDate.now()); bajaButton.setDisable(true); reactivarButton.setDisable(true);
    }
    @FXML private void guardar() {
        Integer actual=id; String n=nombre.getText(),a=apellido.getText(),c=cedula.getText(),r=rango.getText(),t=telefono.getText(); LocalDate f=ingreso.getValue();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,a,c,r,t,f); return true; },ok -> recargar("Datos del bombero guardados correctamente."));
    }
    @FXML private void darBaja() { cambiarBaja(true); }
    @FXML private void reactivar() { cambiarBaja(false); }
    private void cambiarBaja(boolean baja) { Ui.seguro(root,() -> {
        if (id==null) throw new IllegalArgumentException("Selecciona un bombero."); Integer actual=id;
        if (!Ui.confirmar(root,baja?"¿Dar de baja al bombero y desactivar su cuenta? Se conservará su historial.":"¿Reactivar al bombero? Su cuenta se reactiva por separado en Usuarios.")) return;
        Ui.ejecutar(root,() -> { servicio.cambiarBaja(sesion,actual,baja); return true; },ok -> recargar(baja?"Bombero dado de baja. Su historial se conserva.":"Bombero reactivado."));
    }); }
}
