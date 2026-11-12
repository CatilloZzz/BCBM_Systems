package edu.ni.uam.proyectofinalferrogest.controller;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import edu.ni.uam.proyectofinalferrogest.model.Rol;
import edu.ni.uam.proyectofinalferrogest.service.SesionUsuario;

public class MenuController {
    public BorderPane vista(SesionUsuario sesion,Runnable salir) {
        BorderPane root=new BorderPane();
        VBox menu=new VBox(12); menu.setPadding(new Insets(20)); menu.setPrefWidth(220);
        menu.setStyle("-fx-background-color: #e9edf2;");
        menu.getChildren().addAll(new Label("BCBM · "+sesion.username()),new Label("Rol: "+sesion.rol()),
                Ui.boton(sesion.rol()==Rol.BOMBERO?"Mi ficha":"Personal",() -> root.setCenter(new BomberoController(sesion).vista())),
                Ui.boton("Equipos",() -> root.setCenter(new EquipoController(sesion).vista())),
                Ui.boton("Mantenimientos",() -> root.setCenter(new MantenimientoController(sesion).vista())));
        if (sesion.rol()==Rol.ADMIN) menu.getChildren().add(Ui.boton("Usuarios",() -> root.setCenter(new UsuarioController(sesion).vista())));
        menu.getChildren().add(Ui.boton("Cerrar sesión",salir)); root.setLeft(menu);
        root.setCenter(Ui.pagina("Gestión de personal y equipos",new Label("Selecciona un módulo en el menú lateral."),
                new Label("ADMIN administra. DESPACHADOR consulta. BOMBERO consulta su ficha, equipos e historial de mantenimiento.")));
        return root;
    }
}