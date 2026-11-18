package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.dao.UsuarioDao;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
public class AuthService {
    private final UsuarioDao dao = new UsuarioDao();
    public SesionUsuario login(String username, String password) {
        return Transacciones.ejecutar(s -> {
            Usuario u = dao.porUsername(s, username == null ? "" : username.strip());
            if (u == null || !Passwords.verifica(password, u.getPasswordHash()) || !u.getActivo()
                    || (u.getBombero() != null && u.getBombero().getEstado() == EstadoBombero.DE_BAJA))
                throw new IllegalArgumentException("Credenciales incorrectas o cuenta inactiva.");
            return new SesionUsuario(u.getId(), u.getUsername(), u.getRol());
        });
    }
    /** Disponible únicamente si usuario está vacía; username fijo evita dos altas simultáneas. */
    public void crearPrimerAdmin(String password) {
        String hash = Passwords.hash(password);
        Transacciones.ejecutar(s -> {
            if (s.createQuery("select count(u) from Usuario u", Long.class).getSingleResult() != 0)
                throw new IllegalArgumentException("Ya existen usuarios. Solicita acceso a un administrador.");
            Usuario u = new Usuario(); u.setUsername("admin"); u.setPasswordHash(hash); u.setRol(Rol.ADMIN);
            dao.crear(s, u); return null;
        });
    }
}
