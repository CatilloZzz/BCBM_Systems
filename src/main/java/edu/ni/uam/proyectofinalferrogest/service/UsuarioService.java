package edu.ni.uam.proyectofinalferrogest.service;

import java.util.List;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
public class UsuarioService {
    private final UsuarioDao dao = new UsuarioDao();
    public List<Usuario> listar(SesionUsuario sesion) {
        return Transacciones.ejecutar(s -> { Acceso.admin(s,sesion); return dao.listar(s); });
    }
    public void guardar(SesionUsuario sesion, Integer id, String username, String password,
                        Rol rol, Integer bomberoId, boolean activo) {
        String n = Validaciones.texto(username,"Usuario",50);
        if (rol == null) throw new IllegalArgumentException("Selecciona un rol.");
        if (rol == Rol.BOMBERO && bomberoId == null) throw new IllegalArgumentException("El rol BOMBERO requiere un bombero asociado.");
        String hash = id != null && (password == null || password.isEmpty()) ? null : Passwords.hash(password);
        Transacciones.ejecutar(s -> {
            dao.bloquearTodos(s);
            Acceso.admin(s,sesion);
            if (id != null && id.equals(sesion.id()) && (!activo || rol != Rol.ADMIN))
                throw new IllegalArgumentException("No puedes desactivar tu propia cuenta ni quitarte el rol ADMIN.");
            Usuario repetido = dao.porUsername(s,n);
            if (repetido != null && !repetido.getId().equals(id)) throw new IllegalArgumentException("El nombre de usuario ya existe.");
            Bombero b = bomberoId == null ? null : new BomberoDao().bloquear(s,bomberoId);
            if (b != null && activo && b.getEstado() == EstadoBombero.DE_BAJA)
                throw new IllegalArgumentException("Un bombero de baja no puede tener una cuenta activa.");
            if (b != null) {
                Long vinculados = s.createQuery("select count(u) from Usuario u where u.bombero.id = :bid and (:id is null or u.id <> :id)",Long.class)
                        .setParameter("bid",bomberoId).setParameter("id",id).getSingleResult();
                if (vinculados > 0) throw new IllegalArgumentException("Ese bombero ya tiene una cuenta.");
            }
            Usuario u = id == null ? new Usuario() : dao.buscar(s,id);
            u.setUsername(n); u.setRol(rol); u.setBombero(b); u.setActivo(activo);
            if (hash != null) u.setPasswordHash(hash);
            if (id == null) dao.crear(s,u); return null;
        });
    }
}