package edu.ni.uam.proyectofinalferrogest.util;

import edu.ni.uam.proyectofinalferrogest.model.Unidad;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            Configuration configuracion = new Configuration();

            // Hibernate carga hibernate.properties automáticamente.
            configuracion.addAnnotatedClass(Unidad.class);

            sessionFactory = configuracion.buildSessionFactory();
        }

        return sessionFactory;
    }

    public static synchronized void cerrar() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}