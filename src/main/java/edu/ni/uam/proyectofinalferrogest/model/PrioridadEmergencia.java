package edu.ni.uam.proyectofinalferrogest.model;

public enum PrioridadEmergencia {
    BAJA("Baja"), MEDIA("Media"), ALTA("Alta");

    private final String etiqueta;

    PrioridadEmergencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
