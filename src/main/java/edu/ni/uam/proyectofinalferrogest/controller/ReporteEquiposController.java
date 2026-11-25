package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.ReporteEquiposService.Datos;
import edu.ni.uam.proyectofinalferrogest.util.*;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.sf.jasperreports.engine.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ReporteEquiposController {
    private final SesionUsuario sesion;
    private Datos consultado;
    @FXML private VBox root;
    @FXML private DatePicker desde,hasta;
    @FXML private Label resumen,corte,plan;
    @FXML private StackPane contenido;
    @FXML private Button previsualizar;
    @FXML private ProgressIndicator cargando;
    public ReporteEquiposController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("reporte-equipos.fxml",this); }
    public void mostrar(Node propietario) {
        Stage ventana=new Stage(); ventana.setTitle("BCBM · Equipos y mantenimiento");
        ventana.initOwner(propietario.getScene().getWindow()); ventana.initModality(Modality.WINDOW_MODAL);
        Scene scene=new Scene(vista(),1100,760); scene.getStylesheets().add(Vistas.estilos());
        ventana.setMinWidth(780); ventana.setMinHeight(560); ventana.setScene(scene); ventana.show();
    }
    @FXML private void initialize() {
        Ui.fecha(desde); Ui.fecha(hasta); desde.setValue(LocalDate.now()); hasta.setValue(LocalDate.now().plusDays(30));
        Ui.carga(root,cargando); consultar();
    }
    @FXML private void consultar() {
        LocalDate inicio=desde.getValue(),fin=hasta.getValue();
        Ui.ejecutar(root,()->new ReporteEquiposService(HibernateUtil.getSessionFactory()).consultar(sesion,inicio,fin),datos->{
            consultado=datos; contenido.getChildren().setAll(new ReporteEquiposDatosController(datos).vista());
            resumen.setText(datos.resumen());
            corte.setText("Inventario consultado el "+datos.generado().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            plan.setText(datos.pendientes().size()+" revisiones pendientes · "+datos.vencidos()+" vencidas · "+
                datos.historial().size()+" revisiones registradas en el período");
            previsualizar.setDisable(false);
        });
    }
    private record Preparado(Datos datos,JasperPrint documento) {}
    @FXML private void previsualizar() {
        if(consultado==null) return;
        Datos datos=consultado;
        Ui.ejecutar(root,()->{
            try { return new Preparado(datos,new ReporteEquiposExportacion().preparar(datos)); }
            catch(IOException|JRException ex) { throw new IllegalStateException("No se pudo preparar el reporte de equipos.",ex); }
        },reporte->ReporteVistaPrevia.mostrar(root,"Equipos y mantenimiento",reporte.documento(),
            new ReporteEquiposDatosController(reporte.datos()).vista(),"BCBM-equipos-"+datos.generado().toLocalDate(),
            destino->{
                try { new ReporteJasperService().exportarPdf(reporte.documento(),destino); }
                catch(JRException ex) { throw new IllegalStateException("No se pudo guardar el PDF.",ex); }
            },destino->new ReporteEquiposExportacion().guardarExcel(reporte.datos(),destino)));
    }
}
