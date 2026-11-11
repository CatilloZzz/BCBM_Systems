module edu.ni.uam.proyectofinalferrogest {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires java.naming;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    opens edu.ni.uam.proyectofinalferrogest to javafx.fxml;
    opens edu.ni.uam.proyectofinalferrogest.controller to javafx.fxml;
    opens edu.ni.uam.proyectofinalferrogest.model to org.hibernate.orm.core;

    exports edu.ni.uam.proyectofinalferrogest;
}
