package edu.ni.uam.proyectofinalferrogest;

import edu.ni.uam.proyectofinalferrogest.model.EstadoEmergencia;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasExportacion;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.*;
import net.sf.jasperreports.engine.JasperExportManager;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteEmergenciasExportacionTest {
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 10, 8, 30);

    private static Datos ejemplo() {
        var actas = List.of(
                new Acta(1, "A-001", BASE, "Incendio", "Alta", "Cerrada", "Barrio Monseñor Lezcano", "Atendida", BASE.plusHours(3)),
                new Acta(2, "A-002", BASE.plusDays(1), "Incendio", "Crítica", "En proceso", "Km 7 Carretera Norte " + "con referencia extensa ".repeat(6), null, null),
                new Acta(3, null, BASE.plusDays(2), "Rescate", "Media", "Pendiente", null, null, null),
                new Acta(4, "A-004", BASE.plusDays(3), "Sin clasificar", "Sin prioridad", "Cerrada", "Mercado Oriental", "Falsa alarma", BASE.plusDays(3).plusHours(1)));
        return new Datos(new Filtro(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), BASE.plusDays(20), actas, 2);
    }

    @Test void cuentaFrecuenciasYActasAbiertas() {
        Datos datos = ejemplo();
        assertEquals(4, datos.total());
        assertEquals(2, datos.abiertas());
        assertEquals(2, datos.cerradas());
        Conteo incendio = datos.porTipo().get(0);
        assertEquals("Incendio", incendio.categoria());
        assertEquals(2, incendio.actas());
        assertEquals(1, incendio.abiertas());
        assertEquals(0.5, incendio.porcentaje(), 1e-9);
        assertEquals(List.of("Incendio"), datos.masFrecuentes().stream().map(Conteo::categoria).toList());
        assertEquals("Crítica", datos.porPrioridad().get(0).categoria());
        assertEquals(1, datos.porPrioridad().get(0).abiertas());
        assertEquals(EstadoEmergencia.values().length, datos.porEstado().size());
        var pendientes = datos.porResultado().stream().filter(c -> c.categoria().equals("Pendiente de cierre")).findFirst().orElseThrow();
        assertEquals(2, pendientes.actas());
        assertEquals("#3", datos.actas().get(2).referencia());
        assertTrue(datos.frecuencia().contains("Incendio"));
    }

    @Test void exportaLasMismasActasAPdfYExcel() throws Exception {
        Datos datos = ejemplo();
        var exportador = new ReporteEmergenciasExportacion();
        var reporte = exportador.preparar(datos);
        assertFalse(reporte.getPages().isEmpty());
        Path pdf = Path.of("target", "verificacion-emergencias.pdf"), excel = Path.of("target", "verificacion-emergencias.xlsx");
        Files.createDirectories(pdf.getParent());
        JasperExportManager.exportReportToPdfFile(reporte, pdf.toString());
        assertEquals("%PDF-", new String(Files.readAllBytes(pdf), 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
        exportador.guardarExcel(datos, excel);
        try (var entrada = Files.newInputStream(excel); var libro = new XSSFWorkbook(entrada)) {
            assertEquals(6, libro.getNumberOfSheets());
            assertEquals(4, libro.getSheet("Resumen").getRow(5).getCell(1).getNumericCellValue());
            assertEquals(2, libro.getSheet("Resumen").getRow(6).getCell(1).getNumericCellValue());
            var tipo = libro.getSheet("Por tipo").getRow(5);
            assertEquals("Incendio", tipo.getCell(0).getStringCellValue());
            assertEquals(2, tipo.getCell(1).getNumericCellValue());
            var acta = libro.getSheet("Actas").getRow(5);
            assertEquals("A-001", acta.getCell(0).getStringCellValue());
            assertEquals(BASE, acta.getCell(1).getLocalDateTimeCellValue());
            assertEquals("Atendida", acta.getCell(6).getStringCellValue());
        }
    }

    @Test void exportaUnPeriodoSinActas() throws Exception {
        var datos = new Datos(new Filtro(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)), BASE, List.of(), 0);
        var exportador = new ReporteEmergenciasExportacion();
        assertFalse(exportador.preparar(datos).getPages().isEmpty());
        assertTrue(datos.masFrecuentes().isEmpty());
        Path destino = Path.of("target", "verificacion-emergencias-vacio.xlsx");
        Files.createDirectories(destino.getParent());
        exportador.guardarExcel(datos, destino);
        try (var entrada = Files.newInputStream(destino); var libro = new XSSFWorkbook(entrada)) {
            assertEquals("Sin registros.", libro.getSheet("Actas").getRow(5).getCell(0).getStringCellValue());
        }
    }

    @Test void rechazaPeriodoInvertido() {
        assertThrows(IllegalArgumentException.class, () -> new Filtro(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)));
    }
}
