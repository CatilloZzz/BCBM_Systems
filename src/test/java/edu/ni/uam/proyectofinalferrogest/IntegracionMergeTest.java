package edu.ni.uam.proyectofinalferrogest;

import edu.ni.uam.proyectofinalferrogest.model.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class IntegracionMergeTest {
    @Test void estadosConservanLosValoresDelEsquemaCompartido() {
        var converter = new EstadoEmergenciaConverter();
        assertEquals("REPORTADA", converter.convertToDatabaseColumn(EstadoEmergencia.PENDIENTE));
        assertEquals("EN_CURSO", converter.convertToDatabaseColumn(EstadoEmergencia.EN_PROCESO));
        for (var estado : EstadoEmergencia.values())
            assertEquals(estado, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(estado)));
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("INVALIDO"));
    }

    @Test void emergenciaRespetaLimitesYCierreDelEsquema() {
        var tipo = new TipoEmergencia("Incendio");
        assertThrows(IllegalArgumentException.class, () -> new TipoEmergencia("x".repeat(61)));
        assertThrows(IllegalArgumentException.class, () -> new Emergencia("x".repeat(201), null, tipo, PrioridadEmergencia.ALTA, 1));
        var emergencia = new Emergencia("Managua", null, tipo, PrioridadEmergencia.CRITICA, 1);
        emergencia.cambiarEstado(EstadoEmergencia.EN_PROCESO);
        emergencia.cambiarEstado(EstadoEmergencia.CERRADA);
        assertNotNull(emergencia.getFechaCierre());
        assertFalse(emergencia.getFechaCierre().isBefore(emergencia.getFechaRegistro()));
        assertThrows(IllegalStateException.class, () -> emergencia.cambiarEstado(EstadoEmergencia.PENDIENTE));
    }

    @Test void fxmlDeEmergenciasCargaEnElProyectoModular() throws Exception {
        Platform.startup(() -> {});
        try {
            var task = new FutureTask<Void>(() -> {
                var resource = getClass().getResource("/edu/ni/uam/proyectofinalferrogest/fxml/emergencias.fxml");
                assertNotNull(resource);
                var loader = new FXMLLoader(resource);
                assertNotNull(loader.load());
                assertNotNull(loader.getController());
                return null;
            });
            Platform.runLater(task);
            task.get(20, TimeUnit.SECONDS);
        } finally { Platform.exit(); }
    }
}
