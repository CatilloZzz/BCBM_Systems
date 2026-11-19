package edu.ni.uam.proyectofinalferrogest.service;

import org.hibernate.Session;
import edu.ni.uam.proyectofinalferrogest.model.*;
public final class Acceso {
    private Acceso() {}
    public static Usuario usuario(Session s, SesionUsuario sesion) {
        Usuario u = sesion == null ? null : s.find(Usuario.class, sesion.id());
        if (u == null || !u.getActivo() || (u.getBombero() != null && u.getBombero().getEstado() == EstadoBombero.DE_BAJA))
            throw new SecurityException("La sesión ya no tiene acceso. Inicia sesión nuevamente.");
        return u;
    }
    public static Usuario admin(Session s, SesionUsuario sesion) {
        Usuario u = usuario(s, sesion);
        if (u.getRol() != Rol.ADMIN) throw new SecurityException("Esta operación requiere el rol ADMIN.");
        return u;
    }
}