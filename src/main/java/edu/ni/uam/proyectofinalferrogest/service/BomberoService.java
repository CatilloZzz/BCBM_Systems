package edu.ni.uam.proyectofinalferrogest.service;

import java.time.LocalDate;
import java.util.List;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
public class BomberoService {
    private final BomberoDao dao = new BomberoDao();
    public List<Bombero> listar(SesionUsuario sesion) {
        return Transacciones.ejecutar(s -> {
            Usuario u = Acceso.usuario(s, sesion);
            if (u.getRol() == Rol.BOMBERO) return u.getBombero() == null ? List.of() : List.of(u.getBombero());
            return dao.listar(s);
        });
    }
    public void guardar(SesionUsuario sesion, Integer id, String nombre, String apellido, String cedula,
                        String rango, String telefono, LocalDate ingreso) {
        String n = Validaciones.texto(nombre,"Nombre",100), a = Validaciones.texto(apellido,"Apellido",100);
        String c = Validaciones.texto(cedula,"Cédula",20), r = Validaciones.texto(rango,"Rango",40);
        String t = Validaciones.opcional(telefono,"Teléfono",20);
        Validaciones.pasada(ingreso, "Fecha de ingreso");
        Transacciones.ejecutar(s -> {
            Acceso.admin(s,sesion);
            Long repetidos = s.createQuery("select count(b) from Bombero b where b.cedula = :c and (:id is null or b.id <> :id)",Long.class)
                    .setParameter("c",c).setParameter("id",id).getSingleResult();
            if (repetidos > 0) throw new IllegalArgumentException("La cédula ya está registrada.");
            Bombero b = id == null ? new Bombero() : dao.bloquear(s,id);
            b.setNombre(n); b.setApellido(a); b.setCedula(c); b.setRango(r); b.setTelefono(t); b.setFechaIngreso(ingreso);
            if (id == null) dao.crear(s,b); return null;
        });
    }
    public void cambiarBaja(SesionUsuario sesion, Integer id, boolean baja) {
        Transacciones.ejecutar(s -> {
            // Mismo orden de bloqueo que UsuarioService: usuarios, luego bombero.
            new UsuarioDao().bloquearTodos(s);
            Usuario actor = Acceso.admin(s,sesion);
            Bombero b = dao.bloquear(s,id);
            if (b.getEstado() == EstadoBombero.EN_EMERGENCIA)
                throw new IllegalArgumentException("No se puede dar de baja a un bombero que atiende una emergencia.");
            if (baja && actor.getBombero() != null && actor.getBombero().getId().equals(id))
                throw new IllegalArgumentException("No puedes dar de baja al bombero asociado a tu propia cuenta.");
            b.setEstado(baja ? EstadoBombero.DE_BAJA : EstadoBombero.DISPONIBLE);
            if (baja) {
                for (Usuario u : new UsuarioDao().listar(s))
                    if (u.getBombero() != null && u.getBombero().getId().equals(id)) u.setActivo(false);
            }
            return null;
        });
    }
}