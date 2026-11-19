package edu.ni.uam.proyectofinalferrogest.dao;

import org.hibernate.Session;
import jakarta.persistence.LockModeType;
public class BaseDao<T> {
    private final Class<T> tipo;
    public BaseDao(Class<T> tipo) { this.tipo = tipo; }
    public T buscar(Session s, Integer id) {
        T x = id == null ? null : s.find(tipo, id);
        if (x == null) throw new IllegalArgumentException("El registro ya no existe. Actualiza el listado.");
        return x;
    }
    public T bloquear(Session s, Integer id) {
        T x = id == null ? null : s.find(tipo, id, LockModeType.PESSIMISTIC_WRITE);
        if (x == null) throw new IllegalArgumentException("El registro ya no existe. Actualiza el listado.");
        return x;
    }
    public void crear(Session s, T entidad) { s.persist(entidad); }
    // Los cambios de entidades administradas se guardan al confirmar la transacción.
}