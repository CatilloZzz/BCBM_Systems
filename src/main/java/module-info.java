module edu.ni.uam.proyectofinalferrogest {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    opens edu.ni.uam.proyectofinalferrogest to javafx.fxml;

    exports edu.ni.uam.proyectofinalferrogest;
}