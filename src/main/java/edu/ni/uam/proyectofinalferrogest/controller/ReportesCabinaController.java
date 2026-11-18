package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.ReporteCabinaService.Fila;
import edu.ni.uam.proyectofinalferrogest.util.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
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
    @FXML private TableColumn<Fila,String> grupo,tipo,cantidad,total;
    public ReportesCabinaController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("reportes-cabina.fxml",this); }
    @FXML private void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Ui.fecha(desde); Ui.fecha(hasta); desde.setValue(LocalDate.now().withDayOfMonth(1)); hasta.setValue(LocalDate.now());
        Ui.carga(root,cargando);
        Ui.columna(grupo,Fila::grupo); Ui.columna(tipo,Fila::tipo); Ui.columna(cantidad,Fila::cantidad); Ui.columna(total,Fila::total);
        seccion.getItems().setAll("Unidades","Emergencias","Personal"); seccion.setValue("Unidades");
        seccion.valueProperty().addListener((o,a,b)->mostrar()); cargar();
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
        String titulo=switch(seccion.getValue()) { case "Personal" -> "Personal con más salidas"; case "Emergencias" -> "Emergencia más frecuente"; default -> "Unidad con más salidas"; };
        long max=visibles.stream().mapToLong(Fila::total).max().orElse(0);
        String lideres=String.join(", ",visibles.stream().filter(f->f.total()==max).map(Fila::grupo).distinct().toList());
        resumen.setText(max==0 ? "Sin actividad en este período." : titulo+" · "+lideres+" ("+max+")");
    }
    @FXML private void exportar() {
        if (filas.isEmpty()) { Ui.seguro(root,()->{ throw new IllegalArgumentException("No hay datos para exportar. Aplica primero el filtro de fechas."); }); return; }
        LocalDate inicio=consultadoDesde,fin=consultadoHasta;
        FileChooser chooser=new FileChooser(); chooser.setTitle("Guardar reporte de cabina en Excel");
        chooser.setInitialFileName("BCBM-cabina-"+inicio+"-a-"+fin+".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro Excel (*.xlsx)","*.xlsx"));
        File archivo=chooser.showSaveDialog(root.getScene().getWindow()); if (archivo==null) return;
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) archivo=new File(archivo.getParentFile(),archivo.getName()+".xlsx");
        try (Workbook libro=new XSSFWorkbook()) {
            CellStyle titulo=libro.createCellStyle(); Font fuenteTitulo=libro.createFont(); fuenteTitulo.setBold(true); fuenteTitulo.setFontHeightInPoints((short)16); fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
            titulo.setFont(fuenteTitulo); titulo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND); titulo.setVerticalAlignment(VerticalAlignment.CENTER);
            CellStyle encabezado=libro.createCellStyle(); Font fuente=libro.createFont(); fuente.setBold(true); fuente.setColor(IndexedColors.WHITE.getIndex()); encabezado.setFont(fuente); encabezado.setFillForegroundColor(IndexedColors.BLUE.getIndex()); encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND); encabezado.setAlignment(HorizontalAlignment.CENTER);
            CellStyle numerico=libro.createCellStyle(); numerico.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            for (String nombre:List.of("Unidades","Emergencias","Personal")) {
                Sheet sheet=libro.createSheet(nombre.toUpperCase(Locale.ROOT));
                Row tituloFila=sheet.createRow(0); tituloFila.setHeightInPoints(28); tituloFila.createCell(0).setCellValue("BCBM · REPORTE DE CABINA · "+nombre.toUpperCase(Locale.ROOT)); tituloFila.getCell(0).setCellStyle(titulo);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,5));
                Row periodoFila=sheet.createRow(1); periodoFila.createCell(0).setCellValue("Período: "+FECHA.format(inicio)+" hasta "+FECHA.format(fin)); sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1,1,0,5));
                Row header=sheet.createRow(3); String[] nombres={"Grupo","Tipo de actividad","Cantidad","Total del grupo"};
                for (int i=0;i<nombres.length;i++) { header.createCell(i).setCellValue(nombres[i]); header.getCell(i).setCellStyle(encabezado); }
                int row=4;
                for (Fila f:filas.stream().filter(x->x.seccion().equals(nombre)).toList()) {
                    Row r=sheet.createRow(row++); r.createCell(0).setCellValue(f.grupo()); r.createCell(1).setCellValue(f.tipo());
                    r.createCell(2).setCellValue(f.cantidad()); r.createCell(3).setCellValue(f.total()); r.getCell(2).setCellStyle(numerico); r.getCell(3).setCellStyle(numerico);
                }
                if (row==4) { Row empty=sheet.createRow(4); empty.createCell(0).setCellValue("Sin registros en el período"); }
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3,Math.max(3,row-1),0,3)); sheet.createFreezePane(0,4);
                sheet.setColumnWidth(0,36*256); sheet.setColumnWidth(1,38*256); sheet.setColumnWidth(2,16*256); sheet.setColumnWidth(3,20*256);
            }
            try (OutputStream out=Files.newOutputStream(archivo.toPath())) { libro.write(out); }
            resumen.setText("Excel guardado: "+archivo.getName());
        } catch (IOException ex) { Ui.seguro(root,()->{ throw new IllegalStateException("No se pudo guardar el Excel. Revisa la carpeta de destino.",ex); }); }
    }

    @FXML private void exportarPdf() {
        if (filas.isEmpty()) { Ui.seguro(root,()->{ throw new IllegalArgumentException("No hay datos para exportar. Aplica primero el filtro de fechas."); }); return; }
        LocalDate inicio=consultadoDesde,fin=consultadoHasta;
        FileChooser chooser=new FileChooser(); chooser.setTitle("Guardar reporte de cabina en PDF");
        chooser.setInitialFileName("BCBM-cabina-"+inicio+"-a-"+fin+".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)","*.pdf"));
        File archivo=chooser.showSaveDialog(root.getScene().getWindow()); if (archivo==null) return;
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) archivo=new File(archivo.getParentFile(),archivo.getName()+".pdf");
        Path destino=archivo.toPath(); String nombre=archivo.getName(); List<Fila> datos=List.copyOf(filas);
        Ui.ejecutar(root,()->{
            try { new ReporteJasperService().exportarCabina(datos,inicio,fin,destino); }
            catch (IOException|JRException ex) { throw new IllegalStateException("No se pudo generar el reporte PDF con JasperReports.",ex); }
            return nombre;
        },guardado->resumen.setText("PDF generado con JasperReports: "+guardado));
    }
}
