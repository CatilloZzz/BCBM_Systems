package edu.ni.uam.proyectofinalferrogest.util;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import edu.ni.uam.proyectofinalferrogest.model.*;

/** Base común de apoyo. Al integrar, Josué debe registrar también las otras entidades. */
public final class HibernateUtil {
    private static SessionFactory factory;
    private HibernateUtil() {}
    public static synchronized SessionFactory getSessionFactory() {
        if (factory == null || factory.isClosed()) {
            Properties p = new Properties();
            Path path = Path.of(System.getProperty("bcbm.config", "config.properties"));
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) { p.load(in); }
                catch (Exception e) { throw new IllegalStateException("No se pudo leer la configuración local.", e); }
            }
            Configuration cfg = new Configuration();
            cfg.setProperty("hibernate.connection.url", value(p, "db.url", "BCBM_DB_URL"));
            cfg.setProperty("hibernate.connection.username", value(p, "db.user", "BCBM_DB_USER"));
            cfg.setProperty("hibernate.connection.password", value(p, "db.password", "BCBM_DB_PASSWORD"));
            cfg.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            cfg.setProperty("hibernate.hbm2ddl.auto", "validate");
            cfg.setProperty("hibernate.show_sql", "false");
            cfg.setProperty("hibernate.connection.pool_size", "5");
            cfg.addAnnotatedClass(Bombero.class).addAnnotatedClass(Usuario.class)
                    .addAnnotatedClass(Equipo.class).addAnnotatedClass(MantenimientoEquipo.class)
                    .addAnnotatedClass(Unidad.class);
            factory = cfg.buildSessionFactory();
        }
        return factory;
    }
    private static String value(Properties p, String key, String env) {
        String v = System.getenv(env);
        if (v == null) v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Falta configurar " + key + ". Consulta LEEME.md.");
        return v;
    }
    public static synchronized void close() { if (factory != null) { factory.close(); factory = null; } }
}