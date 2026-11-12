from pathlib import Path
from textwrap import dedent

root=Path(__file__).resolve().parent.parent
package='edu.ni.uam.proyectofinalferrogest'
java='src/main/java/'+package.replace('.','/')+'/'
resources='src/main/resources/'+package.replace('.','/')+'/'
def put(path,text):
    p=root/path; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(dedent(text).strip()+'\n',encoding='utf-8')
def cls(path,text):
    sub=path.rsplit('/',1)[0] if '/' in path else ''
    put(java+path,'package '+package+('.'+sub if sub else '')+';\n\n'+dedent(text).strip())
def fxml(name,controller,body):
    imports='''<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.image.*?>
<?import javafx.scene.shape.*?>
<?import javafx.geometry.Insets?>
'''
    body=body.replace('CONTROLLER',package+'.controller.'+controller)
    put(resources+'fxml/'+name+'.fxml',imports+dedent(body))

cls('util/Vistas.java','''
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;

/** Punto único de carga de vistas FXML y recursos institucionales. */
public final class Vistas {
    public static final String BASE = "/edu/ni/uam/proyectofinalferrogest/";
    private Vistas() {}
    public static <T extends Parent> T cargar(String archivo, Object controlador) {
        FXMLLoader loader = new FXMLLoader(recurso("fxml/" + archivo));
        loader.setControllerFactory(tipo -> {
            if (!tipo.isInstance(controlador)) throw new IllegalArgumentException("Controlador incompatible: " + archivo);
            return controlador;
        });
        try { return loader.load(); }
        catch (IOException e) { throw new IllegalStateException("No se pudo abrir la pantalla " + archivo, e); }
    }
    public static URL recurso(String ruta) { return Objects.requireNonNull(Vistas.class.getResource(BASE+ ruta), "Recurso ausente: " + ruta); }
    public static String estilos() { return recurso("css/bcbm.css").toExternalForm(); }
    public static Image logo() { return new Image(recurso("images/logo-bcbm.png").toExternalForm()); }
}
''')

cls('controller/Ui.java','''
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Window;
import javafx.util.StringConverter;
import edu.ni.uam.proyectofinalferrogest.util.*;

/** Comportamiento compartido; la estructura visual reside en los archivos FXML. */
public final class Ui {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> new Thread(r,"bcbm-database"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Ui() {}
    public static void visible(Node node,boolean value) { node.setVisible(value); node.setManaged(value); }
    public static String fecha(LocalDate value) { return value == null ? "Sin registro" : FECHA.format(value); }
    public static String nombre(Object value) {
        if (value == null) return "";
        return switch (value.toString()) {
            case "ADMIN" -> "Administrador"; case "DESPACHADOR" -> "Despachador"; case "BOMBERO" -> "Bombero";
            case "DISPONIBLE" -> "Disponible"; case "EN_EMERGENCIA" -> "En emergencia"; case "DE_BAJA", "BAJA" -> "De baja";
            case "OPERATIVO" -> "Operativo"; case "EN_REPARACION" -> "En reparación"; case "MANTENIMIENTO" -> "Mantenimiento";
            default -> value.toString();
        };
    }
    public static <T> void opciones(ComboBox<T> combo) {
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(T value) { return nombre(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Selector no editable"); }
        });
    }
    public static void fecha(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : FECHA.format(date); }
            @Override public LocalDate fromString(String text) { return text == null || text.isBlank() ? null : LocalDate.parse(text,FECHA); }
        });
        picker.setEditable(false);
    }
    public static <T> void columna(TableColumn<T,String> column,Function<T,?> value) {
        column.setCellValueFactory(c -> new SimpleStringProperty(Objects.toString(value.apply(c.getValue()),"—")));
    }
    public static <T> void estado(TableColumn<T,String> column,Function<T,?> value) {
        columna(column,item -> nombre(value.apply(item)));
        column.setCellFactory(c -> new TableCell<>() {
            private final Label label = new Label();
            @Override protected void updateItem(String text,boolean empty) {
                super.updateItem(text,empty); setText(null);
                if (empty || text == null) { setGraphic(null); return; }
                label.setText(text); label.getStyleClass().setAll("status-badge");
                label.getStyleClass().add(switch (text) {
                    case "Disponible", "Operativo", "Activa" -> "status-ok";
                    case "En emergencia", "De baja", "Inactiva" -> "status-danger";
                    case "En reparación", "Mantenimiento" -> "status-warning";
                    default -> "status-neutral";
                });
                setGraphic(label);
            }
        });
    }
    private static String normalizar(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\\\p{M}","").toLowerCase(Locale.ROOT).strip();
    }
    public static <T> void buscar(TableView<T> tabla,TextField entrada,ObservableList<T> datos,Function<T,String> texto,Label contador) {
        FilteredList<T> filtro = new FilteredList<>(datos);
        entrada.textProperty().addListener((o,a,b) -> {
            String q=normalizar(b); filtro.setPredicate(item -> normalizar(texto.apply(item)).contains(q));
        });
        SortedList<T> ordenados = new SortedList<>(filtro); ordenados.comparatorProperty().bind(tabla.comparatorProperty()); tabla.setItems(ordenados);
        contador.textProperty().bind(Bindings.size(filtro).asString("%d registros"));
    }
    public static void carga(Node root,ProgressIndicator indicador) {
        indicador.visibleProperty().bind(root.disableProperty()); indicador.managedProperty().bind(indicador.visibleProperty());
    }
    public static <T> void ejecutar(Node root,Supplier<T> trabajo,Consumer<T> despues) {
        ejecutar(root,trabajo,despues,e -> error(root,e));
    }
    public static <T> void ejecutar(Node root,Supplier<T> trabajo,Consumer<T> despues,Consumer<Throwable> fallo) {
        root.setDisable(true);
        CompletableFuture.supplyAsync(trabajo,WORKER).whenComplete((resultado,error) -> Platform.runLater(() -> {
            root.setDisable(false);
            if (error != null) fallo.accept(error.getCause() == null ? error : error.getCause());
            else seguro(root,() -> despues.accept(resultado));
        }));
    }
    public static void seguro(Node root,Runnable accion) { try { accion.run(); } catch (RuntimeException e) { error(root,e); } }
    public static String mensaje(Throwable e) {
        if (e instanceof IllegalArgumentException || e instanceof SecurityException) return e.getMessage();
        if (e instanceof IllegalStateException && e.getMessage()!=null && e.getMessage().startsWith("Falta configurar"))
            return "Falta configurar la conexión. Solicita ayuda al administrador del sistema.";
        return "No se completó la operación. Verifica que PostgreSQL esté disponible y vuelve a intentarlo.";
    }
    public static void dialogo(Dialog<?> dialog,Node root) {
        dialog.getDialogPane().getStylesheets().add(Vistas.estilos());
        if (root!=null && root.getScene()!=null && root.getScene().getWindow()!=null) dialog.initOwner(root.getScene().getWindow());
    }
    private static Alert alerta(Node root,Alert.AlertType tipo,String titulo,String texto,ButtonType... botones) {
        Alert a=new Alert(tipo,texto,botones); a.setTitle("BCBM Systems"); a.setHeaderText(titulo); dialogo(a,root); return a;
    }
    public static void informar(Node root,String texto) { alerta(root,Alert.AlertType.INFORMATION,"Operación completada",texto,ButtonType.OK).showAndWait(); }
    public static boolean confirmar(Node root,String texto) {
        ButtonType aceptar=new ButtonType("Confirmar",ButtonBar.ButtonData.OK_DONE);
        return alerta(root,Alert.AlertType.CONFIRMATION,"Confirmar cambio",texto,aceptar,ButtonType.CANCEL).showAndWait().orElse(ButtonType.CANCEL)==aceptar;
    }
    private static void error(Node root,Throwable e) { alerta(root,Alert.AlertType.ERROR,"Revisa la operación",mensaje(e),ButtonType.OK).showAndWait(); }
    /** Deja terminar una operación pendiente antes de cerrar la conexión. */
    public static void cerrar() { WORKER.execute(HibernateUtil::close); WORKER.shutdown(); }
}
''')

cls('MainApp.java','''
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import edu.ni.uam.proyectofinalferrogest.controller.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class MainApp extends Application {
    private Scene scene;
    @Override public void start(Stage stage) {
        stage.setTitle("BCBM Systems · Benemérito Cuerpo de Bomberos de Managua");
        stage.getIcons().add(Vistas.logo()); stage.setMinWidth(1120); stage.setMinHeight(760);
        scene=new Scene(new LoginController().vista(this::entrar),1320,860);
        scene.getStylesheets().add(Vistas.estilos()); stage.setScene(scene); stage.show();
    }
    private void entrar(edu.ni.uam.proyectofinalferrogest.service.SesionUsuario sesion) {
        scene.setRoot(new MenuController().vista(sesion,this::salir));
    }
    private void salir() { scene.setRoot(new LoginController().vista(this::entrar)); }
    @Override public void stop() { Ui.cerrar(); }
    public static void main(String[] args) { launch(args); }
}
''')

cls('controller/LoginController.java','''
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class LoginController {
    @FXML private HBox root;
    @FXML private TextField usuario;
    @FXML private PasswordField clave;
    @FXML private Label error;
    @FXML private ProgressIndicator cargando;
    private Consumer<SesionUsuario> entrar;
    private final AuthService auth=new AuthService();
    public Parent vista(Consumer<SesionUsuario> entrar) { this.entrar=entrar; return Vistas.cargar("login.fxml",this); }
    @FXML private void initialize() {
        Ui.visible(error,false); Ui.carga(root,cargando);
        usuario.textProperty().addListener((o,a,b) -> Ui.visible(error,false));
        clave.textProperty().addListener((o,a,b) -> Ui.visible(error,false));
    }
    @FXML private void acceder() {
        String n=usuario.getText(),p=clave.getText();
        if (n.isBlank() || p.isEmpty()) { mostrarError("Escribe tu usuario y contraseña para continuar."); return; }
        Ui.visible(error,false); clave.clear();
        Ui.ejecutar(root,() -> auth.login(n,p),entrar,e -> mostrarError(Ui.mensaje(e)));
    }
    private void mostrarError(String texto) { error.setText(texto); Ui.visible(error,true); }
    @FXML private void primerAdmin() {
        Ui.seguro(root,() -> new PrimerAdminController().mostrar(root).ifPresent(password ->
            Ui.ejecutar(root,() -> { auth.crearPrimerAdmin(password); return true; },ok -> {
                usuario.setText("admin"); Ui.informar(root,"Administrador creado. Ingresa con admin y la contraseña que elegiste.");
            },e -> mostrarError(Ui.mensaje(e)))));
    }
}
''')
cls('controller/PrimerAdminController.java','''
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class PrimerAdminController {
    @FXML private DialogPane root;
    @FXML private PasswordField clave,repetir;
    @FXML private Label error;
    public Optional<String> mostrar(Node owner) {
        Vistas.cargar("primer-admin.fxml",this);
        Dialog<String> dialog=new Dialog<>(); dialog.setTitle("Primer administrador"); dialog.setDialogPane(root); Ui.dialogo(dialog,owner);
        root.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,event -> {
            String p=clave.getText();
            String m=!p.equals(repetir.getText()) ? "Las contraseñas no coinciden." :
                p.length()<8 || p.getBytes(StandardCharsets.UTF_8).length>72 ? "Usa al menos 8 caracteres y máximo 72 bytes UTF-8." : null;
            if (m!=null) { error.setText(m); Ui.visible(error,true); event.consume(); }
        });
        dialog.setResultConverter(b -> b==ButtonType.OK ? clave.getText() : null); return dialog.showAndWait();
    }
    @FXML private void initialize() { Ui.visible(error,false); }
}
''')

cls('controller/MenuController.java','''
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.ni.uam.proyectofinalferrogest.model.Rol;
import edu.ni.uam.proyectofinalferrogest.service.SesionUsuario;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class MenuController {
    @FXML private BorderPane root;
    @FXML private StackPane contenido;
    @FXML private Label usuario,rol,fecha,seccion;
    @FXML private Button inicioButton,personalButton,equiposButton,mantenimientoButton,usuariosButton;
    private SesionUsuario sesion;
    private Runnable salir;
    public BorderPane vista(SesionUsuario sesion,Runnable salir) { this.sesion=sesion; this.salir=salir; return Vistas.cargar("menu.fxml",this); }
    @FXML private void initialize() {
        usuario.setText(sesion.username()); rol.setText(Ui.nombre(sesion.rol()));
        fecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",Locale.forLanguageTag("es-NI"))));
        Ui.visible(usuariosButton,sesion.rol()==Rol.ADMIN);
        if (sesion.rol()==Rol.BOMBERO) personalButton.setText("Mi ficha");
        inicio();
    }
    private void mostrar(Button activo,String titulo,Supplier<Parent> pantalla) {
        Ui.seguro(root,() -> {
            Parent vista=pantalla.get(); contenido.getChildren().setAll(vista); seccion.setText(titulo);
            for (Button b:List.of(inicioButton,personalButton,equiposButton,mantenimientoButton,usuariosButton)) b.getStyleClass().remove("nav-active");
            activo.getStyleClass().add("nav-active");
        });
    }
    @FXML private void inicio() { mostrar(inicioButton,"Inicio",() -> new InicioController(sesion,this::personal,this::equipos,this::mantenimiento).vista()); }
    @FXML private void personal() { mostrar(personalButton,"Personal",() -> new BomberoController(sesion).vista()); }
    @FXML private void equipos() { mostrar(equiposButton,"Equipos",() -> new EquipoController(sesion).vista()); }
    @FXML private void mantenimiento() { mostrar(mantenimientoButton,"Mantenimiento",() -> new MantenimientoController(sesion).vista()); }
    @FXML private void usuarios() { mostrar(usuariosButton,"Usuarios",() -> new UsuarioController(sesion).vista()); }
    @FXML private void cerrarSesion() { if (Ui.confirmar(root,"¿Deseas cerrar tu sesión? Los cambios sin guardar se descartarán.")) salir.run(); }
}
''')

cls('controller/InicioController.java','''
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class InicioController {
    @FXML private VBox root;
    @FXML private Label saludo,totalPersonal,personalDisponible,totalEquipos,equiposReparacion,alcancePersonal;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final Runnable personal,equipos,mantenimiento;
    private record Resumen(List<Bombero> bomberos,List<Equipo> equipos) {}
    public InicioController(SesionUsuario sesion,Runnable personal,Runnable equipos,Runnable mantenimiento) {
        this.sesion=sesion; this.personal=personal; this.equipos=equipos; this.mantenimiento=mantenimiento;
    }
    public Parent vista() { return Vistas.cargar("inicio.fxml",this); }
    @FXML private void initialize() {
        saludo.setText("Bienvenido, "+sesion.username()+"."); Ui.carga(root,cargando);
        if (sesion.rol()==Rol.BOMBERO) alcancePersonal.setText("Mi ficha de personal");
        actualizar();
    }
    @FXML private void actualizar() {
        Ui.ejecutar(root,() -> new Resumen(new BomberoService().listar(sesion),new EquipoService().listar(sesion)),r -> {
            totalPersonal.setText(""+r.bomberos().size());
            personalDisponible.setText(r.bomberos().stream().filter(b -> b.getEstado()==EstadoBombero.DISPONIBLE).count()+" disponibles");
            totalEquipos.setText(""+r.equipos().size());
            equiposReparacion.setText(""+r.equipos().stream().filter(e -> e.getEstado()==EstadoEquipo.EN_REPARACION).count());
        });
    }
    @FXML private void verPersonal() { personal.run(); }
    @FXML private void verEquipos() { equipos.run(); }
    @FXML private void verMantenimiento() { mantenimiento.run(); }
}
''')

cls('controller/BomberoController.java','''
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
    @FXML private ScrollPane formulario;
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
''')

cls('controller/UsuarioController.java','''
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
''')

cls('controller/EquipoController.java','''
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
    @FXML private ScrollPane formulario;
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
''')

cls('controller/MantenimientoController.java','''
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
    @FXML private ScrollPane formulario;
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
        contador.textProperty().bind(javafx.beans.binding.Bindings.size(tabla.getItems()).asString("%d registros"));
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
''')

fxml('login','LoginController','''
<HBox fx:id="root" styleClass="login-root" xmlns:fx="http://javafx.com/fxml/1" fx:controller="CONTROLLER">
    <VBox styleClass="login-brand" prefWidth="490" minWidth="420" maxWidth="540" spacing="24" HBox.hgrow="ALWAYS">
        <Label text="BENEMÉRITO CUERPO DE BOMBEROS" styleClass="brand-eyebrow" wrapText="true"/>
        <Region VBox.vgrow="ALWAYS"/>
        <StackPane styleClass="logo-tile" maxWidth="184" minHeight="184" prefHeight="184">
            <ImageView fitWidth="160" fitHeight="160" preserveRatio="true" smooth="true">
                <image><Image url="@../images/logo-bcbm.png"/></image>
            </ImageView>
        </StackPane>
        <VBox spacing="10">
            <Label text="Al servicio de&#10;Managua." styleClass="login-title"/>
            <Region styleClass="gold-rule" minHeight="4" prefHeight="4" maxWidth="62"/>
            <Label text="Personas, equipos y compromiso&#10;al servicio de nuestra comunidad." styleClass="brand-copy" wrapText="true"/>
        </VBox>
        <Region VBox.vgrow="ALWAYS"/>
        <VBox spacing="8">
            <Label text="HONOR · DISCIPLINA · ABNEGACIÓN" styleClass="brand-motto"/>
            <Label text="MANAGUA, NICARAGUA" styleClass="brand-caption"/>
        </VBox>
    </VBox>
    <StackPane styleClass="login-content" HBox.hgrow="ALWAYS" minWidth="500">
        <VBox maxWidth="410" spacing="18">
            <Label text="BCBM / ACCESO INSTITUCIONAL" styleClass="eyebrow"/>
            <VBox spacing="8">
                <Label text="Bienvenido de nuevo" styleClass="login-heading"/>
                <Label text="Ingresa a tu espacio de trabajo." styleClass="subtitle"/>
            </VBox>
            <Region minHeight="12"/>
            <VBox spacing="8">
                <Label text="Usuario" labelFor="$usuario" styleClass="field-label"/>
                <TextField fx:id="usuario" promptText="Escribe tu usuario" styleClass="login-input" onAction="#acceder"/>
            </VBox>
            <VBox spacing="8">
                <Label text="Contraseña" labelFor="$clave" styleClass="field-label"/>
                <PasswordField fx:id="clave" promptText="Escribe tu contraseña" styleClass="login-input" onAction="#acceder"/>
            </VBox>
            <Label fx:id="error" styleClass="inline-error" wrapText="true" maxWidth="1.7976931348623157E308"/>
            <Button text="Iniciar sesión" onAction="#acceder" defaultButton="true" styleClass="primary, login-submit" maxWidth="1.7976931348623157E308"/>
            <HBox spacing="10" alignment="CENTER_LEFT">
                <ProgressIndicator fx:id="cargando" prefWidth="20" prefHeight="20"/>
                <Label text="Usa la cuenta asignada por tu administrador." styleClass="muted" wrapText="true"/>
            </HBox>
            <Separator/>
            <TitledPane text="Configuración inicial" expanded="false" animated="false" styleClass="setup-pane">
                <VBox spacing="10">
                    <Label text="Solo si todavía no existen usuarios en el sistema." styleClass="muted" wrapText="true"/>
                    <Button text="Crear primer administrador" onAction="#primerAdmin" styleClass="secondary"/>
                </VBox>
            </TitledPane>
        </VBox>
    </StackPane>
</HBox>
''')

icons={
'inicio':'M2 10 L12 2 L22 10 L20 12 L20 22 L14 22 L14 15 L10 15 L10 22 L4 22 L4 12 Z',
'personal':'M12 2 A5 5 0 1 1 11.99 2 Z M3 22 L3 19 C3 10 21 10 21 19 L21 22 Z',
'equipos':'M3 4 L21 4 L21 8 L3 8 Z M5 10 L19 10 L19 22 L5 22 Z M9 12 L9 14 L15 14 L15 12 Z',
'mantenimiento':'M15 2 L12 5 L12 9 L9 12 L5 12 L2 15 L2 20 L7 22 L10 19 L10 15 L15 10 L19 10 L22 7 L22 2 L18 6 L15 5 Z',
'usuarios':'M12 1 L22 5 L21 14 Q19 20 12 23 Q5 20 3 14 L2 5 Z M10 14 L8 12 L6 14 L10 18 L18 10 L16 8 Z'}
def nav(name,label,action):
    return f'''<Button fx:id="{name}Button" text="{label}" onAction="#{action}" styleClass="nav-button" maxWidth="1.7976931348623157E308"><graphic><SVGPath content="{icons[action]}" styleClass="nav-icon" scaleX="0.74" scaleY="0.74"/></graphic></Button>'''
navs='\n'.join([nav('inicio','Inicio','inicio'),nav('personal','Personal','personal'),nav('equipos','Equipos','equipos'),nav('mantenimiento','Mantenimiento','mantenimiento'),nav('usuarios','Usuarios','usuarios')])
fxml('menu','MenuController','''
<BorderPane fx:id="root" styleClass="app-shell" xmlns:fx="http://javafx.com/fxml/1" fx:controller="CONTROLLER">
    <left>
        <VBox styleClass="sidebar" minWidth="232" prefWidth="232" maxWidth="232" spacing="8">
            <VBox styleClass="sidebar-brand" alignment="CENTER" spacing="12">
                <StackPane styleClass="logo-tile" minWidth="96" prefWidth="96" maxWidth="96" minHeight="96" maxHeight="96">
                    <ImageView fitWidth="82" fitHeight="82" preserveRatio="true" smooth="true"><image><Image url="@../images/logo-bcbm.png"/></image></ImageView>
                </StackPane>
                <Label text="BCBM SYSTEMS" styleClass="sidebar-title"/>
                <Label text="Bomberos de Managua" styleClass="sidebar-subtitle"/>
            </VBox>
            <Separator styleClass="sidebar-divider"/>
            <Label text="ESPACIO DE TRABAJO" styleClass="nav-section"/>
            '''+navs+'''
            <Region VBox.vgrow="ALWAYS"/>
            <VBox styleClass="session-block" spacing="5">
                <Label text="SESIÓN ACTIVA" styleClass="nav-section"/>
                <Label fx:id="usuario" styleClass="session-name"/>
                <Label fx:id="rol" styleClass="sidebar-subtitle"/>
            </VBox>
            <Button text="Cerrar sesión" onAction="#cerrarSesion" styleClass="logout-button" maxWidth="1.7976931348623157E308"/>
            <Label text="MANAGUA · NICARAGUA" styleClass="sidebar-footer"/>
        </VBox>
    </left>
    <center>
        <BorderPane>
            <top>
                <HBox styleClass="topbar" alignment="CENTER_LEFT" spacing="10">
                    <Label text="BCBM" styleClass="topbar-brand"/>
                    <Label text="/" styleClass="muted"/>
                    <Label fx:id="seccion" styleClass="topbar-section"/>
                    <Region HBox.hgrow="ALWAYS"/>
                    <Label fx:id="fecha" styleClass="muted"/>
                </HBox>
            </top>
            <center><StackPane fx:id="contenido"/></center>
        </BorderPane>
    </center>
</BorderPane>
''')

fxml('inicio','InicioController','''
<VBox fx:id="root" styleClass="page" spacing="20" xmlns:fx="http://javafx.com/fxml/1" fx:controller="CONTROLLER">
    <HBox alignment="CENTER_LEFT">
        <VBox spacing="5"><Label text="RESUMEN INSTITUCIONAL" styleClass="eyebrow"/><Label fx:id="saludo" styleClass="page-title"/></VBox>
        <Region HBox.hgrow="ALWAYS"/>
        <ProgressIndicator fx:id="cargando" prefWidth="22" prefHeight="22"/>
        <Button text="Actualizar" onAction="#actualizar" styleClass="secondary"/>
    </HBox>
    <VBox styleClass="welcome-banner" spacing="10">
        <Label text="PERSONAL E INVENTARIO" styleClass="brand-eyebrow"/>
        <Label text="Personal y recursos,&#10;en un mismo lugar." styleClass="welcome-title"/>
        <Label text="Consulta la disponibilidad del personal y mantén al día el cuidado de los equipos." styleClass="brand-copy" wrapText="true"/>
    </VBox>
    <HBox spacing="16">
        <VBox styleClass="metric-card" spacing="10" HBox.hgrow="ALWAYS" prefWidth="250">
            <Label fx:id="alcancePersonal" text="Personal registrado" styleClass="metric-label"/>
            <Label fx:id="totalPersonal" text="—" styleClass="metric-value"/>
            <Label fx:id="personalDisponible" text="Consultando disponibilidad" styleClass="metric-note"/>
        </VBox>
        <VBox styleClass="metric-card" spacing="10" HBox.hgrow="ALWAYS" prefWidth="250">
            <Label text="Inventario de equipos" styleClass="metric-label"/>
            <Label fx:id="totalEquipos" text="—" styleClass="metric-value"/>
            <Label text="Registros de equipos y grupos" styleClass="muted"/>
        </VBox>
        <VBox styleClass="metric-card, metric-warning" spacing="10" HBox.hgrow="ALWAYS" prefWidth="250">
            <Label text="Equipos en reparación" styleClass="metric-label"/>
            <Label fx:id="equiposReparacion" text="—" styleClass="metric-value"/>
            <Label text="Para seguimiento de mantenimiento" styleClass="muted" wrapText="true"/>
        </VBox>
    </HBox>
    <Label text="Accesos rápidos" styleClass="section-title"/>
    <HBox spacing="16">
        <VBox styleClass="quick-card" spacing="12" HBox.hgrow="ALWAYS" prefWidth="250">
            <Label text="Personal" styleClass="card-title"/>
            <Label text="Consulta fichas, rangos y disponibilidad." styleClass="muted" wrapText="true"/>
            <Button text="Ver personal  →" onAction="#verPersonal" styleClass="text-button"/>
        </VBox>
        <VBox styleClass="quick-card" spacing="12" HBox.hgrow="ALWAYS" prefWidth="250">
            <Label text="Equipos" styleClass="card-title"/>
            <Label text="Revisa el inventario y sus unidades." styleClass="muted" wrapText="true"/>
            <Button text="Ver equipos  →" onAction="#verEquipos" styleClass="text-button"/>
        </VBox>
        <VBox styleClass="quick-card" spacing="12" HBox.hgrow="ALWAYS" prefWidth="250">
            <Label text="Mantenimiento" styleClass="card-title"/>
            <Label text="Consulta los trabajos y próximas fechas." styleClass="muted" wrapText="true"/>
            <Button text="Ver historial  →" onAction="#verMantenimiento" styleClass="text-button"/>
        </VBox>
    </HBox>
    <Region VBox.vgrow="ALWAYS"/>
    <Label text="Los datos de personal y las acciones disponibles se ajustan a los permisos de tu cuenta." styleClass="footnote" wrapText="true"/>
</VBox>
''')

def field(label,control):
    return '<VBox spacing="6"><Label text="'+label+'" styleClass="field-label"/>'+control+'</VBox>'
def textfield(id,prompt=''):
    return f'<TextField fx:id="{id}" promptText="{prompt}"/>'
def combo(id,prompt=''):
    return f'<ComboBox fx:id="{id}" promptText="{prompt}" maxWidth="1.7976931348623157E308"/>'
def column(id,label,width):
    return f'<TableColumn fx:id="{id}" text="{label}" prefWidth="{width}"/>'
def page(name,controller,title,subtitle,search,cols,form,formtitle,extra='',formnote='Los campos con * son obligatorios.'):
    fxml(name,controller,f'''
<VBox fx:id="root" styleClass="page" spacing="18" xmlns:fx="http://javafx.com/fxml/1" fx:controller="CONTROLLER">
    <HBox alignment="CENTER_LEFT" spacing="12">
        <VBox spacing="5"><Label text="GESTIÓN INSTITUCIONAL" styleClass="eyebrow"/><Label text="{title}" styleClass="page-title"/><Label text="{subtitle}" styleClass="subtitle"/></VBox>
        <Region HBox.hgrow="ALWAYS"/>
        <ProgressIndicator fx:id="cargando" prefWidth="22" prefHeight="22"/>
        <Button text="Actualizar" onAction="#cargar" styleClass="secondary"/>
    </HBox>
    <HBox spacing="18" VBox.vgrow="ALWAYS">
        <VBox styleClass="list-card" spacing="14" HBox.hgrow="ALWAYS" minWidth="350">
            <HBox alignment="CENTER_LEFT" spacing="12"><TextField fx:id="buscar" promptText="{search}" HBox.hgrow="ALWAYS" styleClass="search-field"/><Label fx:id="contador" styleClass="count-label"/></HBox>
            <TableView fx:id="tabla" VBox.vgrow="ALWAYS" fixedCellSize="46">
                <placeholder><Label text="No hay registros para mostrar." styleClass="muted"/></placeholder>
                <columns>{cols}</columns>
            </TableView>
            <Label text="Selecciona una fila para consultar sus datos." styleClass="footnote"/>
        </VBox>
        <ScrollPane fx:id="formulario" fitToWidth="true" hbarPolicy="NEVER" styleClass="form-scroll" minWidth="300" prefWidth="300" maxWidth="300">
            <content><VBox styleClass="form-card" spacing="14">
                <HBox alignment="CENTER_LEFT"><Label fx:id="tituloForm" text="{formtitle}" styleClass="card-title"/><Region HBox.hgrow="ALWAYS"/><Button text="Nuevo" onAction="#limpiar" styleClass="text-button"/></HBox>
                <Label text="{formnote}" styleClass="muted" wrapText="true"/>
                <Separator/>
                {form}
                <Button text="Guardar cambios" onAction="#guardar" styleClass="primary" maxWidth="1.7976931348623157E308"/>
                {extra}
            </VBox></content>
        </ScrollPane>
    </HBox>
    <Label fx:id="mensaje" styleClass="feedback" wrapText="true"/>
</VBox>
''')

page('personal','BomberoController','Personal','Fichas del personal, rangos y disponibilidad.','Buscar por nombre, cédula o rango…',
     ''.join([column('colNombre','Nombre completo',195),column('colCedula','Cédula',150),column('colRango','Rango',100),column('colEstado','Estado',135)]),
     field('Nombre *',textfield('nombre','Ej. Carlos'))+field('Apellido *',textfield('apellido','Ej. Mendoza'))+
     field('Cédula *',textfield('cedula','000-000000-0000A'))+field('Rango *',textfield('rango','Ej. Bombero'))+
     field('Teléfono',textfield('telefono','Opcional'))+field('Fecha de ingreso *','<DatePicker fx:id="ingreso" maxWidth="1.7976931348623157E308"/>'),
     'Nuevo bombero','''<Separator/><HBox spacing="8"><Button fx:id="bajaButton" text="Dar de baja" onAction="#darBaja" styleClass="danger-outline" HBox.hgrow="ALWAYS" maxWidth="1.7976931348623157E308"/><Button fx:id="reactivarButton" text="Reactivar" onAction="#reactivar" styleClass="secondary" HBox.hgrow="ALWAYS" maxWidth="1.7976931348623157E308"/></HBox><Label text="Las bajas conservan el historial del bombero." styleClass="footnote" wrapText="true"/>''')

page('usuarios','UsuarioController','Usuarios y acceso','Administra las cuentas y los permisos del sistema.','Buscar usuario, rol o bombero…',
     ''.join([column('colUsuario','Usuario',125),column('colRol','Rol',145),column('colBombero','Bombero asociado',195),column('colActivo','Acceso',100)]),
     field('Usuario *',textfield('username','Nombre de acceso'))+
     field('Contraseña','<PasswordField fx:id="password" promptText="Mínimo 8 caracteres"/><Label text="Al editar, deja vacío para conservarla." styleClass="footnote" wrapText="true"/>')+
     field('Rol *',combo('rol','Selecciona un rol'))+field('Bombero asociado',combo('bombero','Selecciona un bombero')+'<Button text="Quitar vínculo" onAction="#quitarBombero" styleClass="text-button"/>')+
     '<CheckBox fx:id="activo" text="Cuenta activa"/>','Nuevo usuario',formnote='El rol Bombero requiere una ficha asociada. Para desactivar una cuenta, desmarca Cuenta activa.')

page('equipos','EquipoController','Inventario de equipos','Controla las existencias, su estado y la unidad asignada.','Buscar equipo, categoría o unidad…',
     ''.join([column('colNombre','Equipo',190),column('colCategoria','Categoría',105),column('colCantidad','Cant.',65),column('colEstado','Estado',125),column('colUnidad','Unidad',85),column('colMantenimiento','Últ. mant.',115)]),
     field('Nombre del equipo *',textfield('nombre','Ej. Casco de bombero'))+field('Categoría *',textfield('categoria','Ej. Protección'))+
     field('Cantidad *',textfield('cantidad','Número de piezas'))+field('Estado *',combo('estado'))+
     field('Unidad asignada',combo('unidad','Sin unidad')+'<Button text="Quitar unidad" onAction="#quitarUnidad" styleClass="text-button"/>'),
     'Nuevo equipo','<Label text="Para dar de baja un equipo, selecciona el estado De baja y guarda." styleClass="footnote" wrapText="true"/>',
     'Cada registro representa un equipo o grupo de piezas. Los campos con * son obligatorios.')

fxml('mantenimientos','MantenimientoController','''
<VBox fx:id="root" styleClass="page" spacing="18" xmlns:fx="http://javafx.com/fxml/1" fx:controller="CONTROLLER">
    <HBox alignment="CENTER_LEFT" spacing="12">
        <VBox spacing="5"><Label text="CUIDADO DE LOS RECURSOS" styleClass="eyebrow"/><Label text="Mantenimiento" styleClass="page-title"/><Label text="Consulta el historial y registra los trabajos realizados." styleClass="subtitle"/></VBox>
        <Region HBox.hgrow="ALWAYS"/><ProgressIndicator fx:id="cargando" prefWidth="22" prefHeight="22"/>
        <Button text="Actualizar equipos" onAction="#cargarEquipos" styleClass="secondary"/>
    </HBox>
    <HBox styleClass="equipment-picker" alignment="CENTER_LEFT" spacing="14">
        <Label text="Equipo" styleClass="field-label"/>
        <ComboBox fx:id="equipo" promptText="Selecciona un equipo" HBox.hgrow="ALWAYS" maxWidth="1.7976931348623157E308"/>
        <Button text="Consultar historial" onAction="#historial" styleClass="primary"/>
    </HBox>
    <HBox spacing="18" VBox.vgrow="ALWAYS">
        <VBox styleClass="list-card" spacing="14" HBox.hgrow="ALWAYS" minWidth="350">
            <HBox alignment="CENTER_LEFT"><Label text="Historial de trabajos" styleClass="card-title"/><Region HBox.hgrow="ALWAYS"/><Label fx:id="contador" styleClass="count-label"/></HBox>
            <TableView fx:id="tabla" VBox.vgrow="ALWAYS" fixedCellSize="48">
                <placeholder><Label text="Selecciona un equipo y consulta su historial." styleClass="muted" wrapText="true"/></placeholder>
                <columns><TableColumn fx:id="colFecha" text="Fecha" prefWidth="105"/><TableColumn fx:id="colDescripcion" text="Trabajo realizado" prefWidth="235"/><TableColumn fx:id="colProxima" text="Próxima fecha" prefWidth="115"/><TableColumn fx:id="colAutor" text="Registrado por" prefWidth="110"/></columns>
            </TableView>
            <Label fx:id="detalle" text="Selecciona una fila para leer la descripción completa." styleClass="detail-note" wrapText="true" maxWidth="1.7976931348623157E308"/>
        </VBox>
        <ScrollPane fx:id="formulario" fitToWidth="true" hbarPolicy="NEVER" styleClass="form-scroll" minWidth="300" prefWidth="300" maxWidth="300">
            <content><VBox styleClass="form-card" spacing="14">
                <Label text="Registrar mantenimiento" styleClass="card-title"/>
                <Label text="El trabajo se asociará al equipo seleccionado." styleClass="muted" wrapText="true"/>
                <Separator/>
                <VBox spacing="6"><Label text="Fecha del trabajo *" styleClass="field-label"/><DatePicker fx:id="fecha" maxWidth="1.7976931348623157E308"/></VBox>
                <VBox spacing="6"><Label text="Descripción del trabajo *" styleClass="field-label"/><TextArea fx:id="descripcion" promptText="Describe la revisión, reparación o trabajo realizado…" wrapText="true" prefRowCount="5"/></VBox>
                <VBox spacing="6"><Label text="Próximo mantenimiento" styleClass="field-label"/><DatePicker fx:id="proxima" promptText="Opcional" maxWidth="1.7976931348623157E308"/></VBox>
                <Button text="Registrar mantenimiento" onAction="#guardar" styleClass="primary" maxWidth="1.7976931348623157E308"/>
                <Button text="Limpiar formulario" onAction="#limpiar" styleClass="secondary" maxWidth="1.7976931348623157E308"/>
                <Label text="El historial conserva el autor y la fecha de cada trabajo." styleClass="footnote" wrapText="true"/>
            </VBox></content>
        </ScrollPane>
    </HBox>
    <Label fx:id="mensaje" styleClass="feedback" wrapText="true"/>
</VBox>
''')

fxml('primer-admin','PrimerAdminController','''
<DialogPane fx:id="root" prefWidth="440" headerText="Crear primer administrador" xmlns:fx="http://javafx.com/fxml/1" fx:controller="CONTROLLER">
    <content><VBox spacing="14" styleClass="dialog-form">
        <Label text="La cuenta se llamará admin. Solo se puede crear si todavía no existen usuarios." wrapText="true" styleClass="muted"/>
        <VBox spacing="6"><Label text="Contraseña" styleClass="field-label"/><PasswordField fx:id="clave" promptText="Mínimo 8 caracteres"/></VBox>
        <VBox spacing="6"><Label text="Repetir contraseña" styleClass="field-label"/><PasswordField fx:id="repetir" promptText="Escribe nuevamente la contraseña"/></VBox>
        <Label fx:id="error" styleClass="inline-error" wrapText="true"/>
    </VBox></content>
    <buttonTypes><ButtonType fx:constant="OK"/><ButtonType fx:constant="CANCEL"/></buttonTypes>
</DialogPane>
''')

put(resources+'css/bcbm.css','''
/* BCBM · Rojo institucional, dorado del escudo y superficies de trabajo claras. */
.root {
    -fx-font-family: "Segoe UI"; -fx-font-size: 13px;
    -fx-base: #ffffff; -fx-background: #f3f5f7; -fx-control-inner-background: #ffffff;
    -fx-accent: #b8242c; -fx-focus-color: #b8242c; -fx-faint-focus-color: #b8242c20;
    -fx-text-background-color: #20282f;
}
.label { -fx-text-fill: #20282f; }
.app-shell, .page { -fx-background-color: #f3f5f7; }
.page { -fx-padding: 26 26 18 26; }
.eyebrow { -fx-text-fill: #a6232a; -fx-font-size: 10px; -fx-font-weight: bold; }
.page-title { -fx-font-family: "Bahnschrift"; -fx-font-size: 30px; -fx-text-fill: #20282f; }
.subtitle { -fx-text-fill: #697580; -fx-font-size: 13px; }
.muted { -fx-text-fill: #697580; -fx-font-size: 12px; }
.footnote { -fx-text-fill: #73808a; -fx-font-size: 11px; }
.card-title, .section-title { -fx-font-family: "Bahnschrift"; -fx-font-size: 18px; -fx-text-fill: #25313b; }
.field-label { -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #42505b; }
.button { -fx-cursor: hand; -fx-padding: 10 16; -fx-background-radius: 6; -fx-border-radius: 6; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #e8edf1; -fx-text-fill: #35434e; }
.button:focused { -fx-border-color: #b8242c; -fx-border-width: 1; -fx-border-insets: -3; }
.primary { -fx-background-color: #bc252d; -fx-text-fill: white; }
.primary:hover { -fx-background-color: #9d1b23; }
.primary:pressed { -fx-background-color: #7f1820; }
.secondary { -fx-background-color: #ffffff; -fx-border-color: #d8dfe5; -fx-border-width: 1; -fx-text-fill: #42515c; }
.secondary:hover { -fx-background-color: #edf1f4; }
.text-button { -fx-background-color: transparent; -fx-text-fill: #af2029; -fx-padding: 5 0; }
.text-button:hover { -fx-text-fill: #75171d; -fx-underline: true; }
.danger-outline { -fx-background-color: #fff7f7; -fx-text-fill: #ab252d; -fx-border-color: #ebc8cc; -fx-border-width: 1; -fx-padding: 10 9; }
.danger-outline:hover { -fx-background-color: #fce8e9; }
.text-field, .password-field, .combo-box, .date-picker, .text-area {
    -fx-background-color: #ffffff; -fx-background-radius: 5; -fx-border-radius: 5;
    -fx-border-color: #d9e0e6; -fx-border-width: 1; -fx-prompt-text-fill: #89949e;
    -fx-text-fill: #24313b; -fx-font-size: 12px;
}
.text-field, .password-field { -fx-padding: 10 11; }
.text-field:focused, .password-field:focused, .text-area:focused, .combo-box:focused, .date-picker:focused { -fx-border-color: #b8242c; -fx-effect: dropshadow(gaussian,#b8242c18,4,0,0,0); }
.combo-box .list-cell { -fx-padding: 9 10; -fx-text-fill: #34424d; -fx-background-color: transparent; }
.combo-box-popup .list-cell { -fx-background-color: white; -fx-text-fill: #34424d; }
.combo-box-popup .list-cell:hover, .combo-box-popup .list-cell:filled:selected { -fx-background-color: #f9e9ea; -fx-text-fill: #931e26; }
.combo-box .arrow-button, .date-picker .arrow-button { -fx-background-color: transparent; -fx-padding: 8; }
.date-picker .text-field { -fx-border-width: 0; -fx-background-color: transparent; }
.text-area .content { -fx-background-color: #ffffff; -fx-padding: 10; }
.text-area { -fx-padding: 0; }
.check-box .box { -fx-background-color: white; -fx-border-color: #cbd4db; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 4; }
.check-box:selected .box { -fx-background-color: #b8242c; -fx-border-color: #b8242c; }
.check-box:selected .mark { -fx-background-color: white; }
.separator .line { -fx-border-color: #e7ebee; -fx-border-width: 1 0 0 0; }
.progress-indicator { -fx-progress-color: #b8242c; }
.list-card, .metric-card, .quick-card, .equipment-picker { -fx-background-color: #ffffff; -fx-background-radius: 9; -fx-border-color: #e0e5e9; -fx-border-radius: 9; -fx-border-width: 1; }
.list-card { -fx-padding: 18; }
.form-scroll { -fx-background-color: #ffffff; -fx-background-radius: 9; -fx-border-color: #e0e5e9; -fx-border-radius: 9; -fx-padding: 0; }
.form-scroll > .viewport { -fx-background-color: #ffffff; }
.form-card { -fx-padding: 20 18; -fx-background-color: #ffffff; }
.search-field { -fx-background-color: #f6f8fa; -fx-border-color: #e5eaee; }
.count-label { -fx-font-size: 11px; -fx-text-fill: #7b8790; -fx-min-width: 65; }
.feedback { -fx-font-size: 12px; -fx-text-fill: #576b5f; }
.table-view { -fx-background-color: transparent; -fx-border-color: #e7ecef; -fx-border-width: 1 0 1 0; -fx-padding: 0; }
.table-view .column-header-background, .table-view .column-header, .table-view .filler { -fx-background-color: #f5f7f9; -fx-border-width: 0; }
.table-view .column-header { -fx-size: 40; }
.table-view .column-header .label { -fx-font-size: 11px; -fx-text-fill: #657582; -fx-alignment: CENTER_LEFT; -fx-padding: 0 10; -fx-font-weight: bold; }
.table-cell { -fx-border-color: transparent transparent #edf0f3 transparent; -fx-padding: 0 10; -fx-alignment: CENTER_LEFT; -fx-font-size: 12px; -fx-text-fill: #34414c; }
.table-row-cell { -fx-background-color: white; -fx-background-insets: 0; }
.table-row-cell:odd { -fx-background-color: #fcfdfe; }
.table-row-cell:filled:hover { -fx-background-color: #f5f7fa; }
.table-row-cell:filled:selected { -fx-background-color: #f9e9eb; }
.table-row-cell:filled:selected .table-cell { -fx-text-fill: #642128; }
.table-row-cell:empty .table-cell { -fx-border-color: transparent; }
.table-view .placeholder .label { -fx-text-fill: #7b8790; }
.status-badge { -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; }
.status-ok { -fx-background-color: #eaf3ee; -fx-text-fill: #337453; }
.status-danger { -fx-background-color: #fce5e7; -fx-text-fill: #b12c38; }
.status-warning { -fx-background-color: #fff3d5; -fx-text-fill: #88661a; }
.status-neutral { -fx-background-color: #edf0f5; -fx-text-fill: #586b81; }
.scroll-bar:horizontal, .scroll-bar:vertical { -fx-background-color: #f6f8fa; -fx-pref-width: 10; -fx-pref-height: 10; }
.scroll-bar .thumb { -fx-background-color: #c8d1d9; -fx-background-radius: 8; }
.scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 2; -fx-background-color: transparent; }
.sidebar { -fx-background-color: #651a21; -fx-padding: 24 15 18; }
.sidebar-brand { -fx-padding: 0 0 18; }
.logo-tile { -fx-background-color: white; -fx-background-radius: 14; }
.sidebar-title { -fx-font-family: "Bahnschrift"; -fx-font-size: 19px; -fx-text-fill: #ffffff; }
.sidebar-subtitle { -fx-font-size: 12px; -fx-text-fill: #e0bfc2; }
.sidebar-divider .line { -fx-border-color: #843b40; }
.nav-section { -fx-font-size: 9px; -fx-text-fill: #d2a3a8; -fx-font-weight: bold; -fx-padding: 14 12 8; }
.nav-button { -fx-alignment: CENTER_LEFT; -fx-padding: 12 13; -fx-graphic-text-gap: 12; -fx-background-color: transparent; -fx-text-fill: #f2dfe1; -fx-font-size: 13px; -fx-font-weight: normal; }
.nav-icon { -fx-fill: #dcbec1; }
.nav-button:hover { -fx-background-color: #7a2b32; -fx-text-fill: white; }
.nav-active { -fx-background-color: #8e333b; -fx-border-color: #e3b447; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 5 5 0; -fx-background-radius: 0 5 5 0; -fx-text-fill: white; -fx-font-weight: bold; }
.nav-active .nav-icon { -fx-fill: #edc773; }
.nav-button:focused, .logout-button:focused { -fx-border-color: #edc773; }
.session-block { -fx-padding: 6 12 12; -fx-background-color: #59161c; -fx-background-radius: 6; }
.session-block .nav-section { -fx-padding: 7 0 2; }
.session-name { -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; }
.logout-button { -fx-background-color: transparent; -fx-text-fill: #f1d6d9; -fx-alignment: CENTER_LEFT; -fx-padding: 10 13; }
.logout-button:hover { -fx-background-color: #7a2b32; }
.sidebar-footer { -fx-font-size: 9px; -fx-text-fill: #bd8e94; -fx-padding: 12 10 0; }
.topbar { -fx-background-color: #ffffff; -fx-padding: 17 26; -fx-border-color: #e2e7eb; -fx-border-width: 0 0 1 0; }
.topbar-brand { -fx-font-family: "Bahnschrift"; -fx-font-size: 14px; -fx-text-fill: #a3242d; }
.topbar-section { -fx-font-size: 12px; -fx-text-fill: #465560; }
.welcome-banner { -fx-background-color: #7b2028; -fx-background-radius: 10; -fx-padding: 25 30; -fx-border-color: #e3b447; -fx-border-width: 0 0 0 5; -fx-border-radius: 0 10 10 0; }
.welcome-title { -fx-font-family: "Bahnschrift"; -fx-font-size: 31px; -fx-text-fill: white; }
.brand-eyebrow { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #edc773; }
.brand-copy { -fx-font-size: 13px; -fx-text-fill: #efd5d8; -fx-line-spacing: 4; }
.metric-card { -fx-padding: 20; }
.metric-label { -fx-font-size: 12px; -fx-text-fill: #64727d; }
.metric-value { -fx-font-family: "Bahnschrift"; -fx-font-size: 38px; -fx-text-fill: #273640; }
.metric-note { -fx-text-fill: #3d7a5a; -fx-font-size: 12px; }
.metric-warning { -fx-border-color: #e7d8b0; }
.metric-warning .metric-value { -fx-text-fill: #9d7120; }
.quick-card { -fx-padding: 20; }
.equipment-picker { -fx-padding: 14 18; }
.detail-note { -fx-background-color: #f5f7fa; -fx-padding: 12; -fx-text-fill: #687884; -fx-font-size: 12px; -fx-background-radius: 5; }
.login-root { -fx-background-color: #ffffff; }
.login-brand { -fx-background-color: #751b24; -fx-padding: 42 46; }
.login-title { -fx-font-family: "Bahnschrift"; -fx-font-size: 47px; -fx-text-fill: white; }
.gold-rule { -fx-background-color: #e3b447; }
.brand-motto { -fx-font-size: 10px; -fx-text-fill: #edc773; -fx-font-weight: bold; }
.brand-caption { -fx-font-size: 10px; -fx-text-fill: #d1a5aa; }
.login-content { -fx-padding: 45; -fx-background-color: #fcfdfd; }
.login-heading { -fx-font-family: "Bahnschrift"; -fx-font-size: 32px; }
.login-input { -fx-padding: 14 13; -fx-font-size: 14px; }
.login-submit { -fx-padding: 14; -fx-font-size: 14px; }
.inline-error { -fx-background-color: #fcebee; -fx-text-fill: #a32636; -fx-padding: 11; -fx-background-radius: 5; -fx-font-size: 12px; }
.setup-pane > .title { -fx-background-color: transparent; -fx-padding: 8 0; }
.setup-pane > .title > .text { -fx-fill: #70808b; -fx-font-size: 11px; }
.setup-pane > .content { -fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0; }
.dialog-pane { -fx-background-color: #ffffff; }
.dialog-pane .header-panel { -fx-background-color: #f6f8fa; }
.dialog-pane .header-panel .label { -fx-font-size: 18px; -fx-font-family: "Bahnschrift"; }
.dialog-form { -fx-padding: 18; }
''')

print('Vistas FXML, controladores y estilos BCBM preparados.')
