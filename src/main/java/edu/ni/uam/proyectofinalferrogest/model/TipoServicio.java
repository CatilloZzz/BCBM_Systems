package edu.ni.uam.proyectofinalferrogest.model;
public enum TipoServicio {
    COBERTURA_EVENTOS("Cobertura de eventos"), GESTION_ADMINISTRATIVA("Gestiones administrativas"),
    TALLER("Servicios de taller"), CAPACITACION("Capacitaciones"), GESTION_DPC("Gestiones DPC"),
    CAPACITACION_PERSONAL("Capacitación al personal"),
    REUNION_OPERATIVA("Reuniones operativas"),
    GESTIONES_VARIAS("Gestiones varias"),
    LABOR_PREVENTIVA("Labor preventiva"),
    INFORME("Informe"),
    PROTECCION_SERVICIOS("Protección de servicios públicos"),
    SERVICIO_ESPECIAL("Servicios especiales"),
    SERVICIO_FUNEBRE("Servicios fúnebres"),
    SERVICIO_OPERATIVO("Servicios operativos"),
    SIMULACRO("Simulacro"),
    SUMINISTRO_COMBUSTIBLE("Suministro de combustible"),
    SUMINISTRO_AGUA("Suministro de agua"),
    TRASLADO_BOMBEROS("Traslado de bomberos"),
    TRASLADO_PACIENTES("Traslado de pacientes"),
    VIAJE_AGUA("Viaje de agua"),
    RECORRIDO("Recorrido"), OTRO("Otro servicio");
    private final String nombre;
    TipoServicio(String nombre) { this.nombre=nombre; }
    @Override public String toString() { return nombre; }
}
