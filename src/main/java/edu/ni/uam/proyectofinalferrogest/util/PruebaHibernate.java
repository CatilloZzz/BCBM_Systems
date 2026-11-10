package edu.ni.uam.proyectofinalferrogest.util;

import edu.ni.uam.proyectofinalferrogest.model.Unidad;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PruebaHibernate {

    public static void main(String[] args) {
        try {
            try (Session session = HibernateUtil
                    .getSessionFactory()
                    .openSession()) {

                Transaction transaccion = session.beginTransaction();

                try {
                    List<Unidad> unidades = session.createQuery(
                            "from Unidad order by codigo",
                            Unidad.class
                    ).getResultList();

                    transaccion.commit();

                    System.out.println("Conexión y consulta correctas.");
                    System.out.println("Unidades encontradas: " + unidades.size());

                    for (Unidad unidad : unidades) {
                        System.out.println(
                                unidad.getCodigo() + " | "
                                        + unidad.getTipo() + " | "
                                        + unidad.getEstado()
                        );
                    }

                } catch (RuntimeException e) {
                    if (transaccion.isActive()) {
                        transaccion.rollback();
                    }

                    throw e;
                }
            }
        } finally {
            HibernateUtil.cerrar();
        }
    }
}