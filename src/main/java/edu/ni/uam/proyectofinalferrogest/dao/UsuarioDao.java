package edu.ni.uam.proyectofinalferrogest.dao;

import java.util.List;
import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.Usuario;
public class UsuarioDao extends BaseDao<Usuario> {
    public UsuarioDao() { super(Usuario.class); }
    public List<Usuario> listar(Session s) { return s.createQuery("select u from Usuario u left join fetch u.bombero order by u.username", Usuario.class).getResultList(); }

    public void bloquearTodos(Session s) {
        s.createQuery("from Usuario u order by u.id", Usuario.class)
                .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
    }
    public Usuario porUsername(Session s, String username) {
        return s.createQuery("select u from Usuario u left join fetch u.bombero where u.username = :n", Usuario.class)
                .setParameter("n", username).uniqueResult();
    }

}