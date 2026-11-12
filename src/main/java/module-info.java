module edu.ni.uam.proyectofinalferrogest {
    // Interfaz gráfica
    requires javafx.controls;
    requires javafx.fxml;

    // Servicios de Java utilizados por la persistencia
    requires java.sql;
    requires java.logging;
    requires java.naming;

    // JPA, Hibernate y PostgreSQL
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.postgresql.jdbc;

    // Contraseñas BCrypt
    requires jbcrypt;

    // Acceso para posibles vistas FXML
    opens edu.ni.uam.proyectofinalferrogest to javafx.fxml;
    opens edu.ni.uam.proyectofinalferrogest.controller to javafx.fxml;

    // Hibernate necesita acceder por reflexión a las entidades
    opens edu.ni.uam.proyectofinalferrogest.model;

    // Clase principal accesible para JavaFX
    exports edu.ni.uam.proyectofinalferrogest;
}