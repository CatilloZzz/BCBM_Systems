package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.service.ReporteEquiposService.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReporteEquiposExportacion {
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PLANTILLA="/edu/ni/uam/proyectofinalferrogest/reports/reporte-equipos.jasper";
    public JasperPrint preparar(Datos datos) throws IOException,JRException {
        List<Map<String,?>> filas=new ArrayList<>();
        for(Inventario i:datos.inventario()) filas.add(fila("Inventario actual",i.equipo(),i.categoria(),i.estado(),
            "Total "+i.total()+"\nAsignadas "+i.asignado()+"\nDisponibles "+i.disponible(),""));
        if(datos.inventario().isEmpty()) filas.add(fila("Inventario actual","Sin equipos registrados.","","","",""));
        for(Dotacion d:datos.dotacion()) filas.add(fila("Dotación por unidad",d.equipo(),"Unidad "+d.unidad(),"",Long.toString(d.cantidad()),""));
        if(datos.dotacion().isEmpty()) filas.add(fila("Dotación por unidad","Sin recursos asignados.","","","",""));
        for(Mantenimiento m:datos.pendientes()) filas.add(fila("Plan de mantenimiento y vencidos",m.equipo(),m.descripcion(),
            m.proxima().isBefore(datos.generado().toLocalDate()) ? "Vencido" : "Programado","",fecha(m.proxima())));
        if(datos.pendientes().isEmpty()) filas.add(fila("Plan de mantenimiento y vencidos","Sin revisiones pendientes para esta consulta.","","","",""));
        for(Mantenimiento m:datos.historial()) filas.add(fila("Historial del período",m.equipo(),m.descripcion()+"\nRegistró "+m.responsable(),"","",fecha(m.fecha())));
        if(datos.historial().isEmpty()) filas.add(fila("Historial del período","Sin revisiones registradas en el período.","","","",""));
        Map<String,Object> parametros=new HashMap<>();
        parametros.put("CORTE",datos.generado().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        parametros.put("PERIODO",fecha(datos.desde())+" al "+fecha(datos.hasta()));
        parametros.put("RESUMEN",datos.resumen());
        parametros.put("PLAN",datos.pendientes().size()+" revisiones pendientes · "+datos.vencidos()+" vencidas");
        try(InputStream entrada=getClass().getResourceAsStream(PLANTILLA)) {
            if(entrada==null) throw new IOException("No se encontró la plantilla del reporte de equipos.");
            JasperReport reporte=(JasperReport)JRLoader.loadObject(entrada);
            return JasperFillManager.fillReport(reporte,parametros,new JRMapCollectionDataSource(filas));
        }
    }
    private static Map<String,?> fila(String seccion,String equipo,String detalle,String estado,String piezas,String fecha) {
        return Map.of("seccion",seccion,"equipo",equipo,"detalle",detalle,"estado",estado,"piezas",piezas,"fecha",fecha);
    }
    public void guardarExcel(Datos datos,Path destino) {
        try(Workbook libro=new XSSFWorkbook()) {
            CellStyle cabecera=libro.createCellStyle();
            Font fuente=libro.createFont(); fuente.setBold(true); fuente.setColor(IndexedColors.WHITE.getIndex());
            cabecera.setFont(fuente); cabecera.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle texto=libro.createCellStyle(); texto.setWrapText(true); texto.setVerticalAlignment(VerticalAlignment.TOP);
            CellStyle fechas=libro.createCellStyle(); fechas.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            hoja(libro,"Resumen",new String[]{"Indicador","Cantidad"},List.of(
                new Object[]{"Piezas totales (incluye bajas)",datos.total()},
                new Object[]{"Piezas asignadas a unidades",datos.asignado()},
                new Object[]{"Operativas sin asignar",datos.disponible()},
                new Object[]{"Piezas en reparación",datos.reparacion()},
                new Object[]{"Piezas de baja",datos.baja()},
                new Object[]{"Revisiones pendientes",datos.pendientes().size()},
                new Object[]{"Revisiones vencidas",datos.vencidos()}),datos,cabecera,texto,fechas);
            hoja(libro,"Inventario",new String[]{"Equipo","Categoría","Estado","Total","Asignadas","Disponibles"},
                datos.inventario().stream().map(i->new Object[]{i.equipo(),i.categoria(),i.estado(),i.total(),i.asignado(),i.disponible()}).toList(),
                datos,cabecera,texto,fechas);
            hoja(libro,"Dotación",new String[]{"Equipo","Unidad","Piezas asignadas"},
                datos.dotacion().stream().map(d->new Object[]{d.equipo(),d.unidad(),d.cantidad()}).toList(),datos,cabecera,texto,fechas);
            hoja(libro,"Plan de mantenimiento",new String[]{"Equipo","Última revisión","Próxima revisión","Situación","Descripción","Registró"},
                datos.pendientes().stream().map(m->new Object[]{m.equipo(),m.fecha(),m.proxima(),
                    m.proxima().isBefore(datos.generado().toLocalDate()) ? "Vencido" : "Programado",m.descripcion(),m.responsable()}).toList(),
                datos,cabecera,texto,fechas);
            hoja(libro,"Historial",new String[]{"Equipo","Fecha de revisión","Próxima revisión","Descripción","Registró"},
                datos.historial().stream().map(m->new Object[]{m.equipo(),m.fecha(),m.proxima(),m.descripcion(),m.responsable()}).toList(),
                datos,cabecera,texto,fechas);
            try(OutputStream out=Files.newOutputStream(destino)){libro.write(out);}
        } catch(IOException ex) {
            throw new IllegalStateException("No se pudo guardar el reporte de equipos en Excel.",ex);
        }
    }
    private static void hoja(Workbook libro,String nombre,String[] columnas,List<Object[]> filas,Datos datos,
                             CellStyle cabecera,CellStyle texto,CellStyle fechas) {
        Sheet hoja=libro.createSheet(nombre);
        hoja.createRow(0).createCell(0).setCellValue("BCBM · EQUIPOS Y MANTENIMIENTO · "+nombre);
        hoja.createRow(1).createCell(0).setCellValue("Inventario consultado el "+datos.generado().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        hoja.createRow(2).createCell(0).setCellValue("Mantenimiento del "+fecha(datos.desde())+" al "+fecha(datos.hasta())+". El plan incluye vencidos anteriores.");
        for(int i=0;i<3;i++) hoja.addMergedRegion(new CellRangeAddress(i,i,0,columnas.length-1));
        Row encabezado=hoja.createRow(4);
        for(int i=0;i<columnas.length;i++) {
            Cell celda=encabezado.createCell(i); celda.setCellValue(columnas[i]); celda.setCellStyle(cabecera);
            hoja.setColumnWidth(i,32*256);
        }
        int indice=5;
        for(Object[] fila:filas) {
            Row registro=hoja.createRow(indice++);
            int lineas=2;
            for(Object valor:fila) if(valor instanceof String textoCelda) lineas=Math.max(lineas,(textoCelda.length()+31)/32);
            registro.setHeightInPoints(Math.min(409,14*lineas));
            for(int i=0;i<fila.length;i++) {
                Cell celda=registro.createCell(i); Object valor=fila[i];
                if(valor instanceof Number n) celda.setCellValue(n.doubleValue());
                else if(valor instanceof LocalDate fecha) {celda.setCellValue(fecha);celda.setCellStyle(fechas);}
                else {celda.setCellValue(Objects.toString(valor,""));celda.setCellStyle(texto);}
            }
        }
        if(filas.isEmpty()) hoja.createRow(5).createCell(0).setCellValue("Sin registros.");
        hoja.createFreezePane(0,5);
        hoja.setAutoFilter(new CellRangeAddress(4,Math.max(4,indice-1),0,columnas.length-1));
    }
    private static String fecha(LocalDate fecha) { return fecha==null ? "Sin fecha" : FECHA.format(fecha); }
}
