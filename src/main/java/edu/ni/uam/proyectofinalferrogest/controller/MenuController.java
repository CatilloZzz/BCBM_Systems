package edu.ni.uam.proyectofinalferrogest.controller;

import java.time.LocalDate;
import java.io.IOException;
import edu.ni.uam.proyectofinalferrogest.util.HibernateUtil;
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
import edu.ni.uam.proyectofinalferrogest.service.DespachoService;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class MenuController {
    @FXML private BorderPane root;
    @FXML private ScrollPane navegacion;
    @FXML private Button menuButton;
    @FXML private StackPane contenido;
    @FXML private Label usuario,rol,fecha,seccion;
    @FXML private Label operacionesSection,administracionSection;
    @FXML private Button emergenciasButton,inicioButton,personalButton,equiposButton,mantenimientoButton,usuariosButton,despachoButton,serviciosButton,unidadesButton,reportesButton,configuracionButton;
    private SesionUsuario sesion;
    private Runnable salir;
    public BorderPane vista(SesionUsuario sesion,Runnable salir) { this.sesion=sesion; this.salir=salir; return Vistas.cargar("menu.fxml",this); }
    @FXML private void initialize() {
        usuario.setText(sesion.username()); rol.setText(Ui.nombre(sesion.rol()));
        root.widthProperty().addListener((o,a,b) -> {
            if (a.doubleValue()==0 || (a.doubleValue()<1100)!=(b.doubleValue()<1100)) mostrarMenu(b.doubleValue()>=1100);
            Ui.visible(fecha,b.doubleValue()>=1000);
        });
        fecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",Locale.forLanguageTag("es-NI"))));
        Ui.visible(usuariosButton,sesion.rol()==Rol.ADMIN);
        Ui.visible(configuracionButton,sesion.rol()==Rol.ADMIN);
        Ui.visible(administracionSection,sesion.rol()==Rol.ADMIN);
        Ui.visible(operacionesSection,sesion.rol()!=Rol.BOMBERO);
        Ui.visible(emergenciasButton,sesion.rol()!=Rol.BOMBERO);
        Ui.visible(despachoButton,sesion.rol()!=Rol.BOMBERO);
        Ui.visible(reportesButton,sesion.rol()!=Rol.BOMBERO);
        Ui.visible(serviciosButton,sesion.rol()!=Rol.BOMBERO);
        if (sesion.rol()==Rol.BOMBERO) personalButton.setText("Mi ficha");
        inicio();
    }
    private void mostrar(Button activo,String titulo,Supplier<Parent> pantalla) {
        Ui.seguro(root,() -> {
            Parent vista=pantalla.get();
            ScrollPane scroll=new ScrollPane(vista);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(!vista.getStyleClass().contains("dashboard-viewport"));
            scroll.getStyleClass().add("page-scroll");
            contenido.getChildren().setAll(scroll); seccion.setText(titulo);
            if (root.getWidth()>0 && root.getWidth()<1100) mostrarMenu(false);
            for (Button b:List.of(emergenciasButton,inicioButton,personalButton,equiposButton,mantenimientoButton,usuariosButton,despachoButton,serviciosButton,unidadesButton,reportesButton,configuracionButton)) b.getStyleClass().remove("nav-active");
            activo.getStyleClass().add("nav-active");
        });
    }
    @FXML private void alternarMenu() { mostrarMenu(!navegacion.isVisible()); }
    private void mostrarMenu(boolean visible) {
        Ui.visible(navegacion,visible);
        menuButton.setText(visible ? "Ocultar menú" : "Menú");
    }
    @FXML private void inicio() { mostrar(inicioButton,"Inicio",() -> new InicioController(sesion,this::personal,this::equipos,this::mantenimiento).vista()); }
    @FXML private void emergencias() {
        if (sesion.rol()==Rol.BOMBERO) return;
        mostrar(emergenciasButton,"Emergencias del mes",() -> {
            try { return EmergenciasController.cargar(HibernateUtil.getSessionFactory(),sesion); }
            catch (IOException e) { throw new IllegalStateException("No se pudo abrir Emergencias",e); }
        });
    }
    @FXML private void despacho() {
        if (sesion.rol()==Rol.BOMBERO) return;
        mostrar(despachoButton,"Despacho y movimientos",() -> new DespachoController(sesion,false).vista());
    }
    @FXML private void servicios() {
        if (sesion.rol()==Rol.BOMBERO) return;
        mostrar(serviciosButton,"Otros servicios del mes",() -> new DespachoController(sesion,true).vista());
    }
    @FXML private void configuracion() {
        if(sesion.rol()==Rol.ADMIN) mostrar(configuracionButton,"Configuración general",()->new ConfiguracionController(sesion).vista());
    }
    @FXML private void unidades() { mostrar(unidadesButton,"Unidades y dotación",() -> new UnidadesController(sesion).vista()); }
    @FXML private void reportes() {
        if (sesion.rol()!=Rol.BOMBERO) mostrar(reportesButton,"Reportes de cabina",() -> new ReportesCabinaController(sesion).vista());
    }
    @FXML private void personal() { mostrar(personalButton,"Personal",() -> new BomberoController(sesion).vista()); }
    @FXML private void equipos() { mostrar(equiposButton,"Equipos",() -> new EquipoController(sesion).vista()); }
    @FXML private void mantenimiento() { mostrar(mantenimientoButton,"Mantenimiento",() -> new MantenimientoController(sesion).vista()); }
    @FXML private void usuarios() { mostrar(usuariosButton,"Usuarios",() -> new UsuarioController(sesion).vista()); }
    @FXML private void cerrarSesion() { if (Ui.confirmar(root,"¿Deseas cerrar tu sesión? Los cambios sin guardar se descartarán.")) salir.run(); }
}
