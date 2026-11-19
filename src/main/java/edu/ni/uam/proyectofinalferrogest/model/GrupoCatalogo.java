package edu.ni.uam.proyectofinalferrogest.model;

public enum GrupoCatalogo {
    RANGO("Rangos",40), COMPANIA("Compañías",80), CATEGORIA_EQUIPO("Categorías de equipo",40),
    TIPO_UNIDAD("Tipos de unidad",30), DEPARTAMENTO("Escuadras y departamentos de servicio",100),
    TIPO_SERVICIO("Tipos de servicio",100);
    private final String nombre;
    private final int limite;
    GrupoCatalogo(String nombre,int limite) { this.nombre=nombre; this.limite=limite; }
    public int limite() { return limite; }
    @Override public String toString() { return nombre; }
}
