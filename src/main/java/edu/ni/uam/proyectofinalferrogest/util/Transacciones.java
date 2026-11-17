package edu.ni.uam.proyectofinalferrogest.util;

import org.hibernate.*;
import org.hibernate.exception.ConstraintViolationException;
import java.util.function.Function;

public final class Transacciones {
    private Transacciones() {}
    public static <T> T ejecutar(Function<Session,T> trabajo) {
        return ejecutar(HibernateUtil.getSessionFactory(),trabajo);
    }
    public static <T> T ejecutar(SessionFactory factory, Function<Session,T> trabajo) {
        try (Session s = factory.openSession()) {
            Transaction tx = s.beginTransaction();
            try { T result = trabajo.apply(s); tx.commit(); return result; }
            catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                if (e instanceof ConstraintViolationException)
                    throw new IllegalArgumentException("No se pudo guardar: un dato único ya existe o una relación no es válida.", e);
                throw e;
            }
        }
    }
}
