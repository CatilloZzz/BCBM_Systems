package edu.ni.uam.proyectofinalferrogest.model;

public enum EstadoUnidad { DISPONIBLE, EN_EMERGENCIA, EN_SERVICIO, MANTENIMIENTO;
    public boolean enSalida() { return this==EN_EMERGENCIA || this==EN_SERVICIO; }
}
