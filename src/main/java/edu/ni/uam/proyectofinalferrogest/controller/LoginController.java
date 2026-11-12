package edu.ni.uam.proyectofinalferrogest.controller;

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
