package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.service.ReporteEmergenciasService.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Convierte el reporte de emergencias a PDF (JasperReports) y a Excel. */
public class ReporteEmergenciasExportacion {
    private static final String PLANTILLA = "/edu/ni/uam/proyectofinalferrogest/reports/reporte-emergencias.jasper";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLUMNAS_CONTEO = {"Categoría", "Actas", "% del período", "Abiertas", "Cerradas", ""};
    private static final String[] COLUMNAS_ACTAS = {"Acta y apertura", "Tipo", "Prioridad", "Estado", "Dirección", "Resultado"};
    private static final Locale ES = Locale.forLanguageTag("es-NI");

    public JasperPrint preparar(Datos datos) throws IOException, JRException {
        if (datos == null) throw new IllegalArgumentException("Consulta el reporte antes de exportarlo.");
        List<Map<String, ?>> filas = new ArrayList<>();
        conteos(filas, "Tipos de emergencia más frecuentes", "Tipo de emergencia", datos.porTipo(), "Sin actas en el período.");
        conteos(filas, "Actas por prioridad", "Prioridad", datos.porPrioridad(), "Sin actas en el período.");
        conteos(filas, "Actas por estado", "Estado", datos.porEstado(), "Sin actas en el período.");
        conteos(filas, "Actas por resultado", "Resultado", datos.porResultado(), "Sin actas en el período.");
        for (Acta acta : datos.actas()) {
            filas.add(fila("Detalle de actas", COLUMNAS_ACTAS, acta.referencia() + "\n" + fechaHora(acta.apertura()),
                    acta.tipo(), acta.prioridad(), acta.estado(), acta.direccionVisible(), acta.resultadoVisible()));
        }
        if (datos.actas().isEmpty())
            filas.add(fila("Detalle de actas", COLUMNAS_ACTAS, "Sin actas para los filtros seleccionados.", "", "", "", "", ""));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("PERIODO", datos.filtro().periodo());
        parametros.put("FILTROS", datos.filtro().descripcion());
        parametros.put("RESUMEN", datos.resumen());
        parametros.put("FRECUENCIA", datos.frecuencia());
        parametros.put("PENDIENTES", datos.pendientes());
        parametros.put("CORTE", fechaHora(datos.generado()));
        try (InputStream entrada = getClass().getResourceAsStream(PLANTILLA)) {
            if (entrada == null) throw new IOException("No se encontró la plantilla Jasper del reporte de emergencias.");
            JasperReport reporte = (JasperReport) JRLoader.loadObject(entrada);
            return JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(filas));
        }
    }

    private static void conteos(List<Map<String, ?>> filas, String seccion, String categoria,
                                List<Conteo> conteos, String vacio) {
        String[] columnas = COLUMNAS_CONTEO.clone();
        columnas[0] = categoria;
        for (Conteo c : conteos) {
            filas.add(fila(seccion, columnas, c.categoria(), Long.toString(c.actas()), porcentaje(c.porcentaje()),
                    Long.toString(c.abiertas()), Long.toString(c.cerradas()), ""));
        }
        if (conteos.isEmpty()) filas.add(fila(seccion, columnas, vacio, "", "", "", "", ""));
    }

    private static Map<String, ?> fila(String seccion, String[] titulos, String... valores) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("seccion", seccion);
        for (int i = 0; i < 6; i++) {
            fila.put("h" + (i + 1), titulos[i]);
            fila.put("c" + (i + 1), valores[i] == null ? "" : valores[i]);
        }
        return fila;
    }

    public void guardarExcel(Datos datos, Path destino) {
        if (datos == null || destino == null) throw new IllegalArgumentException("El reporte y el destino son obligatorios.");
        try (Workbook libro = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(libro);
            Sheet resumen = hoja(libro, "Resumen", new String[]{"Indicador", "Resultado"}, datos, estilos);
            Object[][] indicadores = {
                    {"Actas en el período", datos.total()},
                    {"Actas que siguen abiertas", datos.abiertas()},
                    {"Actas cerradas", datos.cerradas()},
                    {"Actas abiertas de fechas anteriores", datos.abiertasAnteriores()},
                    {"Tipo más frecuente", datos.masFrecuentes().isEmpty() ? "Sin actas"
                            : String.join(", ", datos.masFrecuentes().stream().map(Conteo::categoria).toList())}
            };
            int indice = 5;
            for (Object[] indicador : indicadores) escribir(resumen.createRow(indice++), indicador, estilos);
            resumen.setColumnWidth(0, 42 * 256);
            resumen.setColumnWidth(1, 36 * 256);

            hojaConteo(libro, "Por tipo", "Tipo de emergencia", datos.porTipo(), datos, estilos);
            hojaConteo(libro, "Por prioridad", "Prioridad", datos.porPrioridad(), datos, estilos);
            hojaConteo(libro, "Por estado", "Estado", datos.porEstado(), datos, estilos);
            hojaConteo(libro, "Por resultado", "Resultado", datos.porResultado(), datos, estilos);

            String[] columnas = {"N.º de acta", "Apertura", "Tipo", "Prioridad", "Estado", "Dirección", "Resultado", "Cierre"};
            Sheet actas = hoja(libro, "Actas", columnas, datos, estilos);
            indice = 5;
            for (Acta acta : datos.actas()) {
                escribir(actas.createRow(indice++), new Object[]{acta.referencia(), acta.apertura(), acta.tipo(),
                        acta.prioridad(), acta.estado(), acta.direccionVisible(), acta.resultadoVisible(), acta.cierre()}, estilos);
            }
            terminar(actas, columnas.length, indice, new int[]{16, 18, 26, 14, 14, 44, 24, 18});

            try (OutputStream salida = Files.newOutputStream(destino)) { libro.write(salida); }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el reporte de emergencias en Excel.", ex);
        }
    }

    private static void hojaConteo(Workbook libro, String nombre, String categoria, List<Conteo> conteos,
                                   Datos datos, Estilos estilos) {
        String[] columnas = {categoria, "Actas", "% del período", "Abiertas", "Cerradas"};
        Sheet hoja = hoja(libro, nombre, columnas, datos, estilos);
        int indice = 5;
        for (Conteo c : conteos) {
            Row fila = hoja.createRow(indice++);
            escribir(fila, new Object[]{c.categoria(), c.actas(), c.porcentaje(), c.abiertas(), c.cerradas()}, estilos);
            fila.getCell(2).setCellStyle(estilos.porcentaje);
        }
        terminar(hoja, columnas.length, indice, new int[]{34, 12, 16, 12, 12});
    }

    private static Sheet hoja(Workbook libro, String nombre, String[] columnas, Datos datos, Estilos estilos) {
        Sheet hoja = libro.createSheet(nombre);
        Row titulo = hoja.createRow(0);
        titulo.setHeightInPoints(26);
        titulo.createCell(0).setCellValue("BCBM · REPORTE DE EMERGENCIAS · " + nombre.toUpperCase(ES));
        titulo.getCell(0).setCellStyle(estilos.titulo);
        hoja.createRow(1).createCell(0).setCellValue("Período: " + datos.filtro().periodo() + " · " + datos.filtro().descripcion());
        hoja.createRow(2).createCell(0).setCellValue("Actas por fecha de apertura. Estado y resultado al "
                + fechaHora(datos.generado()) + ".");
        int ultima = Math.max(1, columnas.length - 1);
        for (int i = 0; i < 3; i++) hoja.addMergedRegion(new CellRangeAddress(i, i, 0, ultima));
        Row encabezado = hoja.createRow(4);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = encabezado.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilos.encabezado);
        }
        return hoja;
    }

    private static void terminar(Sheet hoja, int columnas, int siguiente, int[] anchos) {
        if (siguiente == 5) hoja.createRow(5).createCell(0).setCellValue("Sin registros.");
        hoja.createFreezePane(0, 5);
        hoja.setAutoFilter(new CellRangeAddress(4, Math.max(4, siguiente - 1), 0, columnas - 1));
        for (int i = 0; i < anchos.length; i++) hoja.setColumnWidth(i, anchos[i] * 256);
    }

    private static void escribir(Row fila, Object[] valores, Estilos estilos) {
        for (int i = 0; i < valores.length; i++) {
            Cell celda = fila.createCell(i);
            Object valor = valores[i];
            if (valor instanceof Double d) { celda.setCellValue(d); celda.setCellStyle(estilos.porcentaje); }
            else if (valor instanceof Number n) { celda.setCellValue(n.doubleValue()); celda.setCellStyle(estilos.numero); }
            else if (valor instanceof LocalDateTime fecha) { celda.setCellValue(fecha); celda.setCellStyle(estilos.fecha); }
            else { celda.setCellValue(valor == null ? "" : valor.toString()); celda.setCellStyle(estilos.texto); }
        }
    }

    private static final class Estilos {
        final CellStyle titulo, encabezado, texto, numero, porcentaje, fecha;
        Estilos(Workbook libro) {
            titulo = libro.createCellStyle();
            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true); fuenteTitulo.setFontHeightInPoints((short) 14); fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
            titulo.setFont(fuenteTitulo); titulo.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND); titulo.setVerticalAlignment(VerticalAlignment.CENTER);
            encabezado = libro.createCellStyle();
            Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true); fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            encabezado.setFont(fuenteEncabezado); encabezado.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND); encabezado.setAlignment(HorizontalAlignment.CENTER);
            texto = libro.createCellStyle(); texto.setWrapText(true); texto.setVerticalAlignment(VerticalAlignment.TOP);
            numero = libro.createCellStyle(); numero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            porcentaje = libro.createCellStyle(); porcentaje.setDataFormat(libro.createDataFormat().getFormat("0.0%"));
            fecha = libro.createCellStyle(); fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            fecha.setVerticalAlignment(VerticalAlignment.TOP);
        }
    }

    private static String porcentaje(double valor) { return String.format(ES, "%.1f%%", valor * 100); }

    private static String fechaHora(LocalDateTime fecha) { return fecha == null ? "Sin registro" : FECHA_HORA.format(fecha); }
}
