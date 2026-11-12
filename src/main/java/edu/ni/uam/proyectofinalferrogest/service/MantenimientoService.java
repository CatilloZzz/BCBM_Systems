package edu.ni.uam.proyectofinalferrogest.service;

import java.time.LocalDate;
import java.util.List;
import edu.ni.uam.proyectofinalferrogest.dao.*;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.util.*;
public class MantenimientoService {
    private final MantenimientoEquipoDao dao = new MantenimientoEquipoDao();
    public List<MantenimientoEquipo> listar(SesionUsuario sesion,Integer equipoId) {
        return Transacciones.ejecutar(s -> { Acceso.usuario(s,sesion); return dao.listar(s,equipoId); });
    }
    public void registrar(SesionUsuario sesion,Integer equipoId,LocalDate fecha,String descripcion,LocalDate proxima) {
        Validaciones.pasada(fecha,"Fecha de mantenimiento");
        String d = Validaciones.texto(descripcion,"Descripción",5000);
        if (proxima != null && proxima.isBefore(fecha)) throw new IllegalArgumentException("La próxima fecha no puede ser anterior al mantenimiento.");
        Transacciones.ejecutar(s -> {
            Usuario autor = Acceso.admin(s,sesion);
            Equipo e = new EquipoDao().bloquear(s,equipoId);
            if (e.getEstado() == EstadoEquipo.BAJA) throw new IllegalArgumentException("No se registra mantenimiento de un equipo dado de baja.");
            MantenimientoEquipo m = new MantenimientoEquipo();
            m.setEquipo(e); m.setFecha(fecha); m.setDescripcion(d); m.setProximaFecha(proxima); m.setRegistradoPor(autor);
            dao.crear(s,m);
            if (e.getUltimoMantenimiento() == null || fecha.isAfter(e.getUltimoMantenimiento())) e.setUltimoMantenimiento(fecha);
            // Historial y fecha resumen quedan confirmados juntos; cualquier error revierte ambos.
            return null;
        });
    }
}