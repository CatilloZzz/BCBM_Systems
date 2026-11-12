package edu.ni.uam.proyectofinalferrogest.util;

import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.model.*;

public class VerificarConexion {
    public static void main(String[] args) {
        try {
            var auth = new AuthService();
            var admin = auth.login("admin", "admin123");
            var despacho = auth.login("despachador", "despacho123");
            var bombero = auth.login("bombero1", "bombero123");
            if (admin.rol() != Rol.ADMIN || despacho.rol() != Rol.DESPACHADOR || bombero.rol() != Rol.BOMBERO)
                throw new IllegalStateException("Roles inesperados");
            System.out.println("LOGIN_OK: admin, despachador, bombero1");
            System.out.println("BOMBEROS=" + new BomberoService().listar(admin).size());
            System.out.println("USUARIOS=" + new UsuarioService().listar(admin).size());
            System.out.println("EQUIPOS=" + new EquipoService().listar(admin).size());
            System.out.println("UNIDADES=" + new EquipoService().unidades(admin).size());
            System.out.println("MANTENIMIENTOS_EQUIPO_1=" + new MantenimientoService().listar(admin,1).size());
            if (new BomberoService().listar(bombero).size() != 1)
                throw new IllegalStateException("La cuenta de bombero debe ver una ficha");
            System.out.println("HIBERNATE_Y_PERMISOS_OK");
        } finally { HibernateUtil.close(); }
    }
}
