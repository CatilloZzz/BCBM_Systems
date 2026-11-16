package edu.ni.uam.proyectofinalferrogest.model;
public enum TipoServicio {
    COBERTURA_EVENTOS("Cobertura de eventos"), GESTION_ADMINISTRATIVA("Gestiones administrativas"),
    TALLER("Servicios de taller"), CAPACITACION("Capacitaciones"), OTRO("Otro servicio");
    private final String nombre;
    TipoServicio(String nombre) { this.nombre=nombre; }
    @Override public String toString() { return nombre; }
}
