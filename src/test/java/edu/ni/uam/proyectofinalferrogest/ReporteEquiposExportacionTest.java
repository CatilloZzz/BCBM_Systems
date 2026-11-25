package edu.ni.uam.proyectofinalferrogest;

import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEquiposService.*;
import net.sf.jasperreports.engine.JasperExportManager;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReporteEquiposExportacionTest {
    @Test void exportaElMismoInventarioAPdfYExcelConFechasYNumeros() throws Exception {
        LocalDate hoy=LocalDate.now();
        var revision=new Mantenimiento("Manguera",hoy.minusDays(2),hoy.plusDays(7),"Revisión técnica ".repeat(25),"admin");
        var datos=new Datos(hoy,hoy.plusDays(30),hoy.atTime(10,0),
            List.of(new Inventario(1,"Manguera","Extinción","Operativo",10,5,5)),
            List.of(new Dotacion("Manguera","M-01",3),new Dotacion("Manguera","M-02",2)),
            List.of(revision),List.of(revision));
        var exportador=new ReporteEquiposExportacion();
        var reporte=exportador.preparar(datos);
        assertFalse(reporte.getPages().isEmpty());
        Path pdf=Path.of("target","verificacion-equipos.pdf"),excel=Path.of("target","verificacion-equipos.xlsx");
        JasperExportManager.exportReportToPdfFile(reporte,pdf.toString());
        assertEquals("%PDF-",new String(Files.readAllBytes(pdf),0,5,java.nio.charset.StandardCharsets.US_ASCII));
        exportador.guardarExcel(datos,excel);
        try(var input=Files.newInputStream(excel); var libro=new XSSFWorkbook(input)) {
            assertEquals(5,libro.getNumberOfSheets());
            var equipo=libro.getSheet("Inventario").getRow(5);
            assertEquals(10,equipo.getCell(3).getNumericCellValue());
            assertEquals(5,equipo.getCell(4).getNumericCellValue());
            assertEquals(5,equipo.getCell(5).getNumericCellValue());
            assertEquals("M-02",libro.getSheet("Dotación").getRow(6).getCell(1).getStringCellValue());
            assertEquals(hoy.plusDays(7),libro.getSheet("Plan de mantenimiento").getRow(5).getCell(2).getLocalDateTimeCellValue().toLocalDate());
        }
    }
    @Test void exportaUnReporteVacioConSeccionesYEncabezados() throws Exception {
        LocalDate hoy=LocalDate.now();
        var datos=new Datos(hoy,hoy,hoy.atTime(10,0),List.of(),List.of(),List.of(),List.of());
        var exportador=new ReporteEquiposExportacion();
        assertFalse(exportador.preparar(datos).getPages().isEmpty());
        Path destino=Path.of("target","verificacion-equipos-vacio.xlsx");
        exportador.guardarExcel(datos,destino);
        try(var input=Files.newInputStream(destino);var libro=new XSSFWorkbook(input)){
            assertEquals("Sin registros.",libro.getSheet("Inventario").getRow(5).getCell(0).getStringCellValue());
        }
    }
}
