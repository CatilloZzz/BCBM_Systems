package edu.ni.uam.proyectofinalferrogest.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import edu.ni.uam.proyectofinalferrogest.model.*;

/** SessionFactory compartida por todos los módulos integrados. */
public final class HibernateUtil {

    private static SessionFactory factory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (factory == null || factory.isClosed()) {
            Properties propiedades = new Properties();

            Path ruta = Path.of(
                    System.getProperty("bcbm.config", "config.properties")
            );

            if (Files.exists(ruta)) {
                try (InputStream entrada = Files.newInputStream(ruta)) {
                    propiedades.load(entrada);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "No se pudo leer la configuración local.", e
                    );
                }
            }

            Configuration cfg = new Configuration();

            cfg.setProperty(
                    "hibernate.connection.url",
                    value(propiedades, "db.url", "BCBM_DB_URL")
            );
            cfg.setProperty(
                    "hibernate.connection.username",
                    value(propiedades, "db.user", "BCBM_DB_USER")
            );
            cfg.setProperty(
                    "hibernate.connection.password",
                    value(propiedades, "db.password", "BCBM_DB_PASSWORD")
            );

            cfg.setProperty(
                    "hibernate.connection.driver_class",
                    "org.postgresql.Driver"
            );
            cfg.setProperty("hibernate.hbm2ddl.auto", "validate");
            cfg.setProperty("hibernate.show_sql", "false");
            cfg.setProperty("hibernate.connection.pool_size", "5");

            cfg.addAnnotatedClass(Bombero.class);
            cfg.addAnnotatedClass(Usuario.class);
            cfg.addAnnotatedClass(Equipo.class);
            cfg.addAnnotatedClass(MantenimientoEquipo.class);
            cfg.addAnnotatedClass(Unidad.class);
            cfg.addAnnotatedClass(Emergencia.class);
            cfg.addAnnotatedClass(TipoEmergencia.class);
            cfg.addAnnotatedClass(Asignacion.class);

            factory = cfg.buildSessionFactory();
        }

        return factory;
    }

    private static String value(
            Properties propiedades, String clave, String variableEntorno
    ) {
        String valor = System.getenv(variableEntorno);

        if (valor == null) {
            valor = propiedades.getProperty(clave);
        }

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar " + clave + ". Consulta README_BASE_DATOS.md."
            );
        }

        return valor;
    }

    public static synchronized void close() {
        if (factory != null) {
            factory.close();
            factory = null;
        }
    }

    public static synchronized void cerrar() {
        close();
    }
}
