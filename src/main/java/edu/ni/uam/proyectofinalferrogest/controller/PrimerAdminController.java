package edu.ni.uam.proyectofinalferrogest.controller;

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
