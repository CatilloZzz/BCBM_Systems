package edu.ni.uam.proyectofinalferrogest.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Genera reportes PDF con JasperReports usando los datos ya consultados por la aplicación. */
public class ReporteJasperService {
    private static final String REPORTE_COMPILADO = "/edu/ni/uam/proyectofinalferrogest/reports/reporte-cabina.jasper";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void exportarCabina(List<ReporteCabinaService.Fila> filas, LocalDate desde, LocalDate hasta, Path destino)
            throws IOException, JRException {
        if (filas == null || desde == null || hasta == null || destino == null || desde.isAfter(hasta))
            throw new IllegalArgumentException("Selecciona un período válido antes de generar el PDF.");

        try (InputStream reporteCompilado = ReporteJasperService.class.getResourceAsStream(REPORTE_COMPILADO)) {
            if (reporteCompilado == null) throw new IOException("No se encontró el reporte Jasper compilado de cabina.");

            JasperReport reporte = (JasperReport) JRLoader.loadObject(reporteCompilado);
            List<Map<String, ?>> registros = new ArrayList<>();
            for (ReporteCabinaService.Fila fila : filas) {
                Map<String, Object> registro = new HashMap<>();
                registro.put("seccion", fila.seccion());
                registro.put("grupo", fila.grupo());
                registro.put("tipo", fila.tipo());
                registro.put("cantidad", fila.cantidad());
                registro.put("total", fila.total());
                registro.put("porcentaje", fila.porcentaje());
                registros.add(registro);
            }

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("PERIODO", FECHA.format(desde) + " al " + FECHA.format(hasta));
            parametros.put("RESUMEN", resumen(filas));
            JasperPrint documento = JasperFillManager.fillReport(
                    reporte, parametros, new JRMapCollectionDataSource(registros));
            JasperExportManager.exportReportToPdfFile(documento, destino.toString());
        }
    }

    private String resumen(List<ReporteCabinaService.Fila> filas) {
        long actas=suma(filas,f->f.seccion().equals("Emergencias"),ReporteCabinaService.Fila::cantidad);
        long salidasEmergencia=suma(filas,f->f.seccion().equals("Unidades")&&f.tipo().startsWith("Emergencia · "),ReporteCabinaService.Fila::cantidad);
        long salidasOtros=suma(filas,f->f.seccion().equals("Unidades")&&f.tipo().startsWith("Servicio · "),ReporteCabinaService.Fila::cantidad);
        long unidades=filas.stream().filter(f->f.seccion().equals("Unidades")&&f.cantidad()>0).map(ReporteCabinaService.Fila::grupo).distinct().count();
        long personas=filas.stream().filter(f->f.seccion().equals("Personal")).map(ReporteCabinaService.Fila::grupo).distinct().count();
        long participaciones=suma(filas,f->f.seccion().equals("Personal"),ReporteCabinaService.Fila::cantidad);
        return "Resumen: "+actas+" actas · "+salidasEmergencia+" salidas por emergencias · "+salidasOtros+
                " por otros servicios · "+unidades+" unidades movilizadas · "+personas+" personas distintas · "+
                participaciones+" participaciones";
    }

    private long suma(List<ReporteCabinaService.Fila> filas,Predicate<ReporteCabinaService.Fila> filtro,
                      java.util.function.ToLongFunction<ReporteCabinaService.Fila> medida) {
        return filas.stream().filter(filtro).mapToLong(medida).sum();
    }
}
