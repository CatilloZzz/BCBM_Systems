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
    @FXML private TextField buscar,nombre,apellido,cedula,telefono;
    @FXML private ComboBox<String> rango,compania,escuadra,filtroCompania,filtroEscuadra;
    @FXML private TableColumn<Bombero,String> colCompania,colEscuadra;
    @FXML private DatePicker ingreso;
    @FXML private Label contador,tituloForm,mensaje;
    @FXML private Button bajaButton,reactivarButton;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final BomberoService servicio=new BomberoService();
    private final ObservableList<Bombero> registros=FXCollections.observableArrayList();
    private final javafx.collections.transformation.FilteredList<Bombero> porOrganizacion=
            new javafx.collections.transformation.FilteredList<>(registros);
    private Integer id;
    private record Datos(java.util.List<Bombero> personal,java.util.Map<GrupoCatalogo,java.util.List<String>> opciones) {}
    public BomberoController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("personal.fxml",this); }
    @FXML private void initialize() {

        escuadra.getItems().setAll("A","B");
        Ui.columna(colCompania,Bombero::getCompania);
        Ui.columna(colEscuadra,Bombero::getEscuadra);
        Ui.columna(colNombre,b -> b.getNombre()+" "+b.getApellido()); Ui.columna(colCedula,Bombero::getCedula); Ui.columna(colRango,Bombero::getRango); Ui.estado(colEstado,Bombero::getEstado);
        filtroEscuadra.getItems().setAll("Todas las escuadras","A","B");
        filtroEscuadra.getSelectionModel().selectFirst();
        filtroCompania.valueProperty().addListener((o,a,b)->filtrarOrganizacion());
        filtroEscuadra.valueProperty().addListener((o,a,b)->filtrarOrganizacion());
        Ui.buscar(tabla,buscar,porOrganizacion,b -> b.getNombre()+" "+b.getApellido()+" "+b.getCedula()+" "+b.getRango()+" "+Ui.nombre(b.getEstado())+" "+b.getCompania()+" "+b.getEscuadra(),contador);
        Ui.visible(formulario,sesion.rol()==Rol.ADMIN); Ui.fecha(ingreso); Ui.carga(root,cargando);
        tabla.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            if (b==null) return; id=b.getId(); tituloForm.setText("Editar bombero"); nombre.setText(b.getNombre()); apellido.setText(b.getApellido());
            cedula.setText(b.getCedula()); Ui.seleccionActual(rango,b.getRango()); Ui.seleccionActual(compania,b.getCompania()); escuadra.setValue(b.getEscuadra()); telefono.setText(b.getTelefono()); ingreso.setValue(b.getFechaIngreso());
            bajaButton.setDisable(b.getEstado()!=EstadoBombero.DISPONIBLE); reactivarButton.setDisable(b.getEstado()!=EstadoBombero.DE_BAJA);
        });
        cargar();
    }
    @FXML private void cargar() { recargar("Selecciona un registro para ver sus datos."); }
    private void recargar(String texto) { Ui.ejecutar(root,() -> new Datos(servicio.listar(sesion),new ConfiguracionService(edu.ni.uam.proyectofinalferrogest.util.HibernateUtil.getSessionFactory()).opciones(sesion)),datos -> {
        var rows=datos.personal(); limpiar(); registros.setAll(rows);
        rango.getItems().setAll(datos.opciones().get(GrupoCatalogo.RANGO));
        compania.getItems().setAll(datos.opciones().get(GrupoCatalogo.COMPANIA));
        String filtro=filtroCompania.getValue();
        filtroCompania.getItems().setAll("Todas las compañías");
        filtroCompania.getItems().addAll(compania.getItems());
        filtroCompania.setValue(filtroCompania.getItems().contains(filtro) ? filtro : "Todas las compañías");
        mensaje.setText(texto); }); }
    private void filtrarOrganizacion() {
        String cia=filtroCompania.getValue(), esc=filtroEscuadra.getValue();
        porOrganizacion.setPredicate(b -> (cia==null || "Todas las compañías".equals(cia) || cia.equals(b.getCompania()))
                && (esc==null || "Todas las escuadras".equals(esc) || esc.equals(b.getEscuadra())));
    }
    @FXML private void limpiar() {
        id=null; tabla.getSelectionModel().clearSelection(); tituloForm.setText("Nuevo bombero");
        nombre.clear(); apellido.clear(); cedula.clear(); rango.setValue(null); compania.setValue(null); compania.getEditor().clear(); escuadra.setValue(null); telefono.clear(); ingreso.setValue(LocalDate.now()); bajaButton.setDisable(true); reactivarButton.setDisable(true);
    }
    @FXML private void guardar() {
        Integer actual=id; String n=nombre.getText(),a=apellido.getText(),c=cedula.getText(),r=rango.getValue(),t=telefono.getText(); LocalDate f=ingreso.getValue(); String cia=compania.getValue(), esc=escuadra.getValue();
        Ui.ejecutar(root,() -> { servicio.guardar(sesion,actual,n,a,c,r,t,f,cia,esc); return true; },ok -> recargar("Datos del bombero guardados correctamente."));
    }
    @FXML private void darBaja() { cambiarBaja(true); }
    @FXML private void reactivar() { cambiarBaja(false); }
    private void cambiarBaja(boolean baja) { Ui.seguro(root,() -> {
        if (id==null) throw new IllegalArgumentException("Selecciona un bombero."); Integer actual=id;
        if (!Ui.confirmar(root,baja?"¿Dar de baja al bombero y desactivar su cuenta? Se conservará su historial.":"¿Reactivar al bombero? Su cuenta se reactiva por separado en Usuarios.")) return;
        Ui.ejecutar(root,() -> { servicio.cambiarBaja(sesion,actual,baja); return true; },ok -> recargar(baja?"Bombero dado de baja. Su historial se conserva.":"Bombero reactivado."));
    }); }
}
