package edu.ni.uam.proyectofinalferrogest.model;

public enum EstadoEmergencia {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    CERRADA("Cerrada");

    private final String etiqueta;

    EstadoEmergencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public boolean permiteTransicionA(EstadoEmergencia destino) {
        return switch (this) {
            case PENDIENTE -> destino == EN_PROCESO || destino == CERRADA;
            case EN_PROCESO -> destino == CERRADA;
            case CERRADA -> false;
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
