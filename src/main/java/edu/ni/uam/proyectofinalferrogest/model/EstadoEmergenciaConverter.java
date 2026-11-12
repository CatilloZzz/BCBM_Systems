package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Conserva los estados del esquema PostgreSQL y la API del módulo de emergencias. */
@Converter
public class EstadoEmergenciaConverter implements AttributeConverter<EstadoEmergencia, String> {
    @Override public String convertToDatabaseColumn(EstadoEmergencia estado) {
        if (estado == null) return null;
        return switch (estado) {
            case PENDIENTE -> "REPORTADA";
            case EN_PROCESO -> "EN_CURSO";
            case CERRADA -> "CERRADA";
        };
    }
    @Override public EstadoEmergencia convertToEntityAttribute(String estado) {
        if (estado == null) return null;
        return switch (estado) {
            case "REPORTADA" -> EstadoEmergencia.PENDIENTE;
            case "EN_CURSO" -> EstadoEmergencia.EN_PROCESO;
            case "CERRADA" -> EstadoEmergencia.CERRADA;
            default -> throw new IllegalArgumentException("Estado de emergencia desconocido: " + estado);
        };
    }
}
