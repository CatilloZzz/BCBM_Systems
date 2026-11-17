package edu.ni.uam.proyectofinalferrogest;

import edu.ni.uam.proyectofinalferrogest.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SalidaUnidadTest {
    private final Bombero operador=new Bombero(),jefe=new Bombero();
    private final LocalDateTime salida=LocalDateTime.of(2025,1,31,23,50);
    private SalidaUnidad emergencia() {
        return new SalidaUnidad(new Emergencia("Managua",null,new TipoEmergencia("Accidente"),PrioridadEmergencia.ALTA,1),
            null,new Unidad("U1","Rescate"),operador,jefe,List.of(operador,jefe),1,salida);
    }
    @Test void conservaFechasAlCruzarMedianocheYFinDeMes() {
        var x=emergencia(); x.marcarLlegada(salida.plusMinutes(5)); x.registrarRetorno(salida.plusMinutes(30));
        assertEquals(LocalDateTime.of(2025,2,1,0,20),x.getRetorno());
        assertEquals(2,x.getParticipantes().size());
        assertThrows(UnsupportedOperationException.class,() -> x.getParticipantes().clear());
    }
    @Test void exigeSecuenciaYNoPermiteReescribirMovimientos() {
        var x=emergencia();
        assertThrows(IllegalArgumentException.class,() -> x.registrarRetorno(salida.plusMinutes(10)));
        assertThrows(IllegalArgumentException.class,() -> x.marcarLlegada(salida.minusMinutes(1)));
        assertNull(x.getLlegada()); x.marcarLlegada(salida.plusMinutes(5));
        assertThrows(IllegalArgumentException.class,() -> x.marcarLlegada(salida.plusMinutes(6)));
        assertThrows(IllegalArgumentException.class,() -> x.registrarRetorno(salida.plusMinutes(4)));
        x.registrarRetorno(salida.plusMinutes(10));
        assertThrows(IllegalArgumentException.class,() -> x.registrarRetorno(salida.plusMinutes(15)));
    }
    @Test void servicioRegistraEntradaSinLlegadaEIncluyeOperador() {
        var x=new SalidaUnidad(null,new OtroServicio("Escuadra A",TipoServicio.COBERTURA_EVENTOS,"Evento"),
            new Unidad("U1","Rescate"),operador,null,List.of(),1,salida);
        assertEquals(1,x.getParticipantes().size());
        assertThrows(IllegalArgumentException.class,() -> x.marcarLlegada(salida.plusMinutes(5)));
        x.registrarRetorno(salida.plusHours(2)); assertNotNull(x.getRetorno());
    }
}
