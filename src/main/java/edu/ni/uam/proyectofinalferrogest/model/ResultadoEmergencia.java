package edu.ni.uam.proyectofinalferrogest.model;

public enum ResultadoEmergencia {
    ATENDIDA("Atendida"),
    FALSA_ALARMA("Falsa alarma"),
    NO_ATENDIDA("No atendida");

    private final String etiqueta;

    ResultadoEmergencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
