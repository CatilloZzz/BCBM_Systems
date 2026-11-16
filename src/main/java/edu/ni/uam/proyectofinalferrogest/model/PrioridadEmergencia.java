package edu.ni.uam.proyectofinalferrogest.model;

public enum PrioridadEmergencia {
    BAJA("Baja"), MEDIA("Media"), ALTA("Alta"), CRITICA("Crítica");

    private final String etiqueta;

    PrioridadEmergencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
