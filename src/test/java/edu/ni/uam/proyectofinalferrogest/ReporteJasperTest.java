package edu.ni.uam.proyectofinalferrogest;

import edu.ni.uam.proyectofinalferrogest.service.ReporteJasperService;
import edu.ni.uam.proyectofinalferrogest.service.ReporteCabinaService.Fila;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteJasperTest {
    @Test void exportaPdfConLaPlantillaCompiladaYTextosLargos() throws Exception {
        var filas=new ArrayList<Fila>();
        for(int i=0;i<65;i++) filas.add(new Fila("Personal",
            "Verificación técnica "+i+" · "+"Nombre extenso con acentos áéíóú y eñe ".repeat(4),
            "Emergencia · Comprobación de una descripción larga para verificar el ajuste del texto en varias líneas",
            1L,65L));
        Path destino=Path.of("target","verificacion-jasper.pdf");
        new ReporteJasperService().exportarCabina(filas,LocalDate.of(2026,11,1),LocalDate.of(2026,11,17),destino);
        byte[] pdf=Files.readAllBytes(destino);
        assertTrue(pdf.length>1000);
        assertEquals("%PDF-",new String(pdf,0,5,java.nio.charset.StandardCharsets.US_ASCII));
    }
    @Test void rechazaPeriodoInvertido() {
        assertThrows(IllegalArgumentException.class,()->new ReporteJasperService().exportarCabina(
            List.of(),LocalDate.of(2026,11,17),LocalDate.of(2026,11,1),Path.of("target","invalido.pdf")));
    }
}
