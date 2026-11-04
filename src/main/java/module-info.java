module edu.ni.uam.proyectofinalferrogest {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.ni.uam.proyectofinalferrogest to javafx.fxml;
    exports edu.ni.uam.proyectofinalferrogest;
}