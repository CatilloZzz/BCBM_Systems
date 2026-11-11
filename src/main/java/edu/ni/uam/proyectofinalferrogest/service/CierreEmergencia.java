package edu.ni.uam.proyectofinalferrogest.service;

@FunctionalInterface
public interface CierreEmergencia {
    /** Retorna después del commit y propaga el error si la transacción falla. */
    void cerrar(Integer emergenciaId);
}
