package edu.ni.uam.proyectofinalferrogest.controller;

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
