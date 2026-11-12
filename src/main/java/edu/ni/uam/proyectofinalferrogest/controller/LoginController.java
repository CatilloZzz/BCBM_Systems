package edu.ni.uam.proyectofinalferrogest.controller;

import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.ni.uam.proyectofinalferrogest.service.*;

public class LoginController {
    private final AuthService auth = new AuthService();
    public VBox vista(Consumer<SesionUsuario> entrar) {
        TextField usuario = new TextField(); usuario.setPromptText("Usuario");
        PasswordField clave = new PasswordField(); clave.setPromptText("Contraseña");
        GridPane form = Ui.formulario(); Ui.campo(form,0,"Usuario",usuario); Ui.campo(form,1,"Contraseña",clave);
        VBox root = Ui.pagina("BCBM · Iniciar sesión",new Label("Benemérito Cuerpo de Bomberos de Managua"),form);
        Button login = Ui.boton("Entrar",() -> {
            String n = usuario.getText(), p = clave.getText(); clave.clear();
            Ui.ejecutar(root,() -> auth.login(n,p),entrar);
        }); login.setDefaultButton(true);
        Button inicial = Ui.boton("Crear primer administrador",() -> {
            Dialog<String> dialog = new Dialog<>(); dialog.setTitle("Primer administrador");
            PasswordField p = new PasswordField(), repetir = new PasswordField();
            GridPane g = Ui.formulario(); Ui.campo(g,0,"Contraseña de admin",p); Ui.campo(g,1,"Repetir contraseña",repetir);
            dialog.getDialogPane().setContent(g); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
            dialog.setResultConverter(b -> b == ButtonType.OK ? p.getText() : null);
            dialog.showAndWait().ifPresent(password -> {
                if (!password.equals(repetir.getText())) throw new IllegalArgumentException("Las contraseñas no coinciden.");
                Ui.ejecutar(root,() -> { auth.crearPrimerAdmin(password); return true; },ok -> {
                    usuario.setText("admin"); Ui.informar("Administrador creado. Inicia sesión con admin y la contraseña que elegiste.");
                });
            });
        });
        root.getChildren().addAll(new HBox(10,login,inicial),new Label("La creación inicial solo funciona cuando no hay usuarios registrados."));
        root.setMaxWidth(650); return root;
    }
}