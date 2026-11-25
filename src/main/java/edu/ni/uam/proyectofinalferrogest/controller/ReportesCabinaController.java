package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.ReporteCabinaService.Fila;
import edu.ni.uam.proyectofinalferrogest.util.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.sf.jasperreports.engine.JRException;

public class ReportesCabinaController {
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final SesionUsuario sesion;
    private List<Fila> filas=List.of();
    private LocalDate consultadoDesde,consultadoHasta;
    @FXML private VBox root;
    @FXML private ProgressIndicator cargando;
    @FXML private ComboBox<String> seccion;
    @FXML private DatePicker desde,hasta;
    @FXML private Label periodo,resumen;
    @FXML private TableView<Fila> tabla;
    @FXML private TableColumn<Fila,String> grupo,tipo,cantidad,total,porcentaje;
    public ReportesCabinaController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("reportes-cabina.fxml",this); }
    @FXML private void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Ui.fecha(desde); Ui.fecha(hasta); desde.setValue(LocalDate.now().withDayOfMonth(1)); hasta.setValue(LocalDate.now());
        Ui.carga(root,cargando);
        Ui.columna(grupo,Fila::grupo); Ui.columna(tipo,Fila::tipo); Ui.columna(cantidad,Fila::cantidad); Ui.columna(total,Fila::total);
        Ui.columna(porcentaje,f->String.format(Locale.forLanguageTag("es-NI"),"%.1f%%",f.porcentaje()*100));
        seccion.getItems().setAll("Unidades","Emergencias","Personal"); seccion.setValue("Unidades");
        seccion.valueProperty().addListener((o,a,b)->{ actualizarEtiquetas(); mostrar(); });
        actualizarEtiquetas(); cargar();
    }
    @FXML private void cargar() {
        LocalDate inicio=desde.getValue(),fin=hasta.getValue();
        if (inicio==null || fin==null || inicio.isAfter(fin)) { Ui.seguro(root,()->{ throw new IllegalArgumentException("Selecciona fechas válidas: Desde no puede ser posterior a Hasta."); }); return; }
        periodo.setText(FECHA.format(inicio)+" – "+FECHA.format(fin)); filas=List.of(); mostrar();
        Ui.ejecutar(root,()->new ReporteCabinaService(HibernateUtil.getSessionFactory()).consultar(sesion,inicio,fin),datos->{ filas=datos; consultadoDesde=inicio; consultadoHasta=fin; mostrar(); });
    }
    private void mostrar() {
        var visibles=filas.stream().filter(f->f.seccion().equals(seccion.getValue())).toList();
        tabla.getItems().setAll(visibles);
        boolean esEmergencia="Emergencias".equals(seccion.getValue());
        String titulo=switch(seccion.getValue()) { case "Personal" -> "Personal con más participaciones"; case "Emergencias" -> "Tipo de emergencia más frecuente"; default -> "Unidad con más salidas"; };
        long max=visibles.stream().mapToLong(esEmergencia ? Fila::cantidad : Fila::total).max().orElse(0);
        String lideres=String.join(", ",visibles.stream().filter(f->(esEmergencia ? f.cantidad() : f.total())==max).map(Fila::grupo).distinct().toList());
        if (max==0) { resumen.setText("Sin actividad en este período."); return; }
        if (esEmergencia) {
            double porcentaje=visibles.stream().filter(f->f.cantidad()==max).mapToDouble(Fila::porcentaje).findFirst().orElse(0)*100;
            resumen.setText(titulo+" · "+lideres+" ("+max+" actas; "+String.format(Locale.forLanguageTag("es-NI"),"%.1f%%",porcentaje)+" del período)");
        } else resumen.setText(titulo+" · "+lideres+" ("+max+")");
    }

    private void actualizarEtiquetas() {
        String valor=seccion.getValue();
        switch (valor==null ? "Unidades" : valor) {
            case "Emergencias" -> { cantidad.setText("Actas del tipo"); total.setText("Actas del período"); porcentaje.setText("% de actas"); }
            case "Personal" -> { cantidad.setText("Participaciones"); total.setText("Total de la persona"); porcentaje.setText("% de participaciones"); }
            default -> { cantidad.setText("Salidas del tipo"); total.setText("Total de la unidad"); porcentaje.setText("% de salidas"); }
        }
    }
    @FXML private void exportar() { previsualizarCabina(); }
    @FXML private void exportarPdf() { previsualizarCabina(); }

    private void previsualizarCabina() {
        if (filas.isEmpty() || consultadoDesde == null || consultadoHasta == null) {
            Ui.seguro(root,()->{ throw new IllegalArgumentException("Aplica primero un período que tenga datos para previsualizar."); });
            return;
        }
        LocalDate inicio=consultadoDesde, fin=consultadoHasta;
        List<Fila> datos=List.copyOf(filas);
        Ui.ejecutar(root,()->{
            try { return new ReporteJasperService().prepararCabina(datos,inicio,fin); }
            catch (IOException|JRException ex) { throw new IllegalStateException("No se pudo preparar la vista previa del reporte.",ex); }
        },reporte->ReporteVistaPrevia.mostrar(root,"Reporte de cabina",reporte,tablaCabina(datos),
                "BCBM-cabina-"+inicio+"-a-"+fin,
                path->guardarPdf(reporte,path), path->guardarExcelCabina(datos,inicio,fin,path)));
    }

    @FXML private void reporteEmergencias() { new ReporteEmergenciasController(sesion,desde.getValue(),hasta.getValue()).mostrar(root); }

    @FXML private void reporteEquipos() { new ReporteEquiposController(sesion).mostrar(root); }

    @FXML private void previsualizarPersonal() {
        LocalDate inicio=desde.getValue(), fin=hasta.getValue();
        if (inicio==null || fin==null || inicio.isAfter(fin)) {
            Ui.seguro(root,()->{ throw new IllegalArgumentException("Selecciona un rango de fechas válido antes de generar el reporte."); });
            return;
        }
        periodo.setText(FECHA.format(inicio)+" – "+FECHA.format(fin));
        Ui.ejecutar(root,()->{
            try {
                List<ReportePersonalService.Fila> datos=new ReportePersonalService(HibernateUtil.getSessionFactory()).consultar(sesion,inicio,fin);
                JasperPrint reporte=new ReporteJasperService().prepararPersonal(datos,inicio,fin);
                return new PreparacionPersonal(datos,reporte);
            } catch (IOException|JRException ex) {
                throw new IllegalStateException("No se pudo preparar el reporte de participación del personal.",ex);
            }
        },resultado->ReporteVistaPrevia.mostrar(root,"Participación del personal",resultado.reporte(),
                tablaPersonal(resultado.datos()),"BCBM-personal-"+inicio+"-a-"+fin,
                path->guardarPdf(resultado.reporte(),path),path->guardarExcelPersonal(resultado.datos(),inicio,fin,path)));
    }

    private record PreparacionPersonal(List<ReportePersonalService.Fila> datos,JasperPrint reporte) {}

    private Node tablaCabina(List<Fila> datos) {
        TableView<Fila> vista=new TableView<>();
        vista.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Fila,String> cGrupo=new TableColumn<>("Unidad, persona o tipo");
        TableColumn<Fila,String> cTipo=new TableColumn<>("Actividad");
        TableColumn<Fila,String> cCantidad=new TableColumn<>("Cantidad");
        TableColumn<Fila,String> cTotal=new TableColumn<>("Total");
        TableColumn<Fila,String> cPorcentaje=new TableColumn<>("Porcentaje");
        Ui.columna(cGrupo,Fila::grupo); Ui.columna(cTipo,Fila::tipo); Ui.columna(cCantidad,Fila::cantidad);
        Ui.columna(cTotal,Fila::total); Ui.columna(cPorcentaje,f->String.format(Locale.forLanguageTag("es-NI"),"%.1f%%",f.porcentaje()*100));
        vista.getColumns().setAll(cGrupo,cTipo,cCantidad,cTotal,cPorcentaje); vista.getItems().setAll(datos);
        return vista;
    }

    private Node tablaPersonal(List<ReportePersonalService.Fila> datos) {
        TableView<ReportePersonalService.Fila> vista=new TableView<>();
        vista.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<ReportePersonalService.Fila,String> persona=new TableColumn<>("Bombero o bombera");
        TableColumn<ReportePersonalService.Fila,String> organizacion=new TableColumn<>("Compañía y escuadra");
        TableColumn<ReportePersonalService.Fila,String> emergencias=new TableColumn<>("Emergencias");
        TableColumn<ReportePersonalService.Fila,String> servicios=new TableColumn<>("Otros servicios");
        TableColumn<ReportePersonalService.Fila,String> total=new TableColumn<>("Participaciones");
        TableColumn<ReportePersonalService.Fila,String> porcentaje=new TableColumn<>("% total");
        Ui.columna(persona,ReportePersonalService.Fila::persona); Ui.columna(organizacion,ReportePersonalService.Fila::organizacion);
        Ui.columna(emergencias,ReportePersonalService.Fila::emergencias); Ui.columna(servicios,ReportePersonalService.Fila::otrosServicios);
        Ui.columna(total,ReportePersonalService.Fila::participaciones);
        Ui.columna(porcentaje,f->String.format(Locale.forLanguageTag("es-NI"),"%.1f%%",f.porcentaje()*100));
        vista.getColumns().setAll(persona,organizacion,emergencias,servicios,total,porcentaje);
        vista.getItems().setAll(datos);
        if (datos.isEmpty()) vista.setPlaceholder(new Label("Sin participaciones para este período."));
        return vista;
    }

    private void guardarPdf(JasperPrint reporte,Path destino) {
        try { new ReporteJasperService().exportarPdf(reporte,destino); }
        catch (JRException ex) { throw new IllegalStateException("No se pudo guardar el PDF. Revisa la carpeta de destino.",ex); }
    }

    private void guardarExcelCabina(List<Fila> datos,LocalDate inicio,LocalDate fin,Path destino) {
        try (Workbook libro=new XSSFWorkbook()) {
            CellStyle titulo=libro.createCellStyle(); Font ft=libro.createFont(); ft.setBold(true); ft.setFontHeightInPoints((short)16); ft.setColor(IndexedColors.WHITE.getIndex());
            titulo.setFont(ft); titulo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND); titulo.setVerticalAlignment(VerticalAlignment.CENTER);
            CellStyle encabezado=libro.createCellStyle(); Font fe=libro.createFont(); fe.setBold(true); fe.setColor(IndexedColors.WHITE.getIndex()); encabezado.setFont(fe); encabezado.setFillForegroundColor(IndexedColors.BLUE.getIndex()); encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND); encabezado.setAlignment(HorizontalAlignment.CENTER);
            CellStyle numerico=libro.createCellStyle(); numerico.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            CellStyle porcentajeStyle=libro.createCellStyle(); porcentajeStyle.setDataFormat(libro.createDataFormat().getFormat("0.0%"));
            crearResumenExcel(libro,inicio,fin,datos,titulo,encabezado,numerico);
            for (String nombre:List.of("Unidades","Emergencias","Personal")) {
                Sheet sheet=libro.createSheet(nombre.toUpperCase(Locale.ROOT));
                Row title=sheet.createRow(0); title.setHeightInPoints(28); title.createCell(0).setCellValue("BCBM · REPORTE DE CABINA · "+nombre.toUpperCase(Locale.ROOT)); title.getCell(0).setCellStyle(titulo);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,4));
                Row period=sheet.createRow(1); period.createCell(0).setCellValue("Período: "+FECHA.format(inicio)+" hasta "+FECHA.format(fin)); sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,4));
                Row criterio=sheet.createRow(2); criterio.createCell(0).setCellValue(nombre.equals("Emergencias") ? "Actas por fecha de apertura; porcentaje sobre actas del período." : "Salidas y participaciones por fecha de salida; porcentaje dentro de cada grupo."); sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2,2,0,4));
                String[] nombres=switch(nombre) { case "Emergencias" -> new String[]{"Tipo de emergencia","Medida","Actas del tipo","Actas del período","Porcentaje de actas"}; case "Personal" -> new String[]{"Persona","Tipo de actividad","Participaciones","Total de la persona","Porcentaje de participaciones"}; default -> new String[]{"Unidad","Tipo de actividad","Salidas del tipo","Total de la unidad","Porcentaje de salidas"}; };
                Row header=sheet.createRow(4); for(int i=0;i<nombres.length;i++){header.createCell(i).setCellValue(nombres[i]);header.getCell(i).setCellStyle(encabezado);}
                int row=5; for(Fila f:datos.stream().filter(x->x.seccion().equals(nombre)).toList()){Row r=sheet.createRow(row++);r.createCell(0).setCellValue(f.grupo());r.createCell(1).setCellValue(f.tipo());r.createCell(2).setCellValue(f.cantidad());r.createCell(3).setCellValue(f.total());r.createCell(4).setCellValue(f.porcentaje());r.getCell(2).setCellStyle(numerico);r.getCell(3).setCellStyle(numerico);r.getCell(4).setCellStyle(porcentajeStyle);}
                if(row==5){Row empty=sheet.createRow(5);empty.createCell(0).setCellValue("Sin registros en el período");}
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(4,Math.max(4,row-1),0,4));sheet.createFreezePane(0,5);
                sheet.setColumnWidth(0,32*256);sheet.setColumnWidth(1,34*256);sheet.setColumnWidth(2,21*256);sheet.setColumnWidth(3,24*256);sheet.setColumnWidth(4,27*256);
            }
            try(OutputStream out=Files.newOutputStream(destino)){libro.write(out);}
        } catch(IOException ex) { throw new IllegalStateException("No se pudo guardar el Excel. Revisa la carpeta de destino.",ex); }
    }

    private void guardarExcelPersonal(List<ReportePersonalService.Fila> datos,LocalDate inicio,LocalDate fin,Path destino) {
        try(Workbook libro=new XSSFWorkbook()) {
            Sheet sheet=libro.createSheet("PERSONAL");
            CellStyle titulo=libro.createCellStyle(); Font ft=libro.createFont(); ft.setBold(true); ft.setFontHeightInPoints((short)16); ft.setColor(IndexedColors.WHITE.getIndex());
            titulo.setFont(ft); titulo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle header=libro.createCellStyle(); Font fh=libro.createFont(); fh.setBold(true); fh.setColor(IndexedColors.WHITE.getIndex()); header.setFont(fh); header.setFillForegroundColor(IndexedColors.BLUE.getIndex()); header.setFillPattern(FillPatternType.SOLID_FOREGROUND); header.setAlignment(HorizontalAlignment.CENTER);
            CellStyle number=libro.createCellStyle(); number.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            CellStyle percent=libro.createCellStyle(); percent.setDataFormat(libro.createDataFormat().getFormat("0.0%"));
            Row titleRow=sheet.createRow(0); titleRow.createCell(0).setCellValue("BCBM · PARTICIPACIÓN DEL PERSONAL"); titleRow.getCell(0).setCellStyle(titulo); sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,5));
            Row period=sheet.createRow(1); period.createCell(0).setCellValue("Período: "+FECHA.format(inicio)+" hasta "+FECHA.format(fin)); sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,5));
            long all=datos.stream().mapToLong(ReportePersonalService.Fila::participaciones).sum(); long emergencies=datos.stream().mapToLong(ReportePersonalService.Fila::emergencias).sum(); long services=datos.stream().mapToLong(ReportePersonalService.Fila::otrosServicios).sum();
            Row summary=sheet.createRow(2); summary.createCell(0).setCellValue("Personas participantes: "+datos.size()+" · Participaciones: "+all+" · Emergencias: "+emergencies+" · Otros servicios: "+services); sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2,2,0,5));
            String[] labels={"Bombero o bombera","Compañía y escuadra","Salidas a emergencias","Salidas a otros servicios","Participaciones totales","% de participaciones"};
            Row headerRow=sheet.createRow(4); for(int i=0;i<labels.length;i++){headerRow.createCell(i).setCellValue(labels[i]);headerRow.getCell(i).setCellStyle(header);}
            int row=5; for(ReportePersonalService.Fila f:datos){Row r=sheet.createRow(row++);r.createCell(0).setCellValue(f.persona());r.createCell(1).setCellValue(f.organizacion());r.createCell(2).setCellValue(f.emergencias());r.createCell(3).setCellValue(f.otrosServicios());r.createCell(4).setCellValue(f.participaciones());r.createCell(5).setCellValue(f.porcentaje());for(int i=2;i<=4;i++)r.getCell(i).setCellStyle(number);r.getCell(5).setCellStyle(percent);}
            if(row==5){Row empty=sheet.createRow(5);empty.createCell(0).setCellValue("Sin participaciones en el período.");}
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(4,Math.max(4,row-1),0,5));sheet.createFreezePane(0,5);
            int[] widths={31,28,22,24,23,22};for(int i=0;i<widths.length;i++)sheet.setColumnWidth(i,widths[i]*256);
            try(OutputStream out=Files.newOutputStream(destino)){libro.write(out);}
        } catch(IOException ex) { throw new IllegalStateException("No se pudo guardar el Excel del personal. Revisa la carpeta de destino.",ex); }
    }


    private void crearResumenExcel(Workbook libro,LocalDate inicio,LocalDate fin,List<Fila> datos,
                                   CellStyle titulo,CellStyle encabezado,CellStyle numerico) {
        Sheet sheet=libro.createSheet("RESUMEN");
        Row tituloFila=sheet.createRow(0); tituloFila.setHeightInPoints(28);
        tituloFila.createCell(0).setCellValue("BCBM · RESUMEN DEL REPORTE DE CABINA"); tituloFila.getCell(0).setCellStyle(titulo);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,1));
        Row periodoFila=sheet.createRow(1); periodoFila.createCell(0).setCellValue("Período: "+FECHA.format(inicio)+" hasta "+FECHA.format(fin));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,1));
        Row criterio=sheet.createRow(2); criterio.createCell(0).setCellValue("Actas: fecha de apertura. Salidas y participaciones: fecha de salida.");
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2,2,0,1));
        Row header=sheet.createRow(4); header.createCell(0).setCellValue("Indicador"); header.createCell(1).setCellValue("Resultado");
        header.getCell(0).setCellStyle(encabezado); header.getCell(1).setCellStyle(encabezado);

        long actas=datos.stream().filter(f->f.seccion().equals("Emergencias")).mapToLong(Fila::cantidad).sum();
        long salidasEmergencia=datos.stream().filter(f->f.seccion().equals("Unidades")&&f.tipo().startsWith("Emergencia · ")).mapToLong(Fila::cantidad).sum();
        long salidasOtros=datos.stream().filter(f->f.seccion().equals("Unidades")&&f.tipo().startsWith("Servicio · ")).mapToLong(Fila::cantidad).sum();
        long unidades=datos.stream().filter(f->f.seccion().equals("Unidades")&&f.cantidad()>0).map(Fila::grupo).distinct().count();
        long personas=datos.stream().filter(f->f.seccion().equals("Personal")).map(Fila::grupo).distinct().count();
        long participaciones=datos.stream().filter(f->f.seccion().equals("Personal")).mapToLong(Fila::cantidad).sum();
        long[] resultados={actas,salidasEmergencia,salidasOtros,unidades,personas,participaciones};
        String[] indicadores={"Actas registradas","Salidas por emergencias","Salidas por otros servicios",
            "Unidades movilizadas","Personal participante (personas distintas)","Participaciones registradas por salida"};
        for(int i=0;i<indicadores.length;i++) {
            Row row=sheet.createRow(5+i); row.createCell(0).setCellValue(indicadores[i]);
            row.createCell(1).setCellValue(resultados[i]); row.getCell(1).setCellStyle(numerico);
        }
        Row nota=sheet.createRow(12); nota.createCell(0).setCellValue("Una persona puede tener varias participaciones si integra más de una salida.");
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(12,12,0,1));
        sheet.setColumnWidth(0,48*256); sheet.setColumnWidth(1,22*256); sheet.createFreezePane(0,5);
    }
}
