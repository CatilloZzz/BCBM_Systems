package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.ReporteEquiposService.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class ReporteEquiposDatosController {
    private final Datos datos;
    @FXML private TableView<Inventario> inventario;
    @FXML private TableView<Dotacion> dotacion;
    @FXML private TableView<Mantenimiento> pendientes,historial;
    @FXML private TableColumn<Inventario,String> iEquipo,iCategoria,iEstado,iTotal,iAsignado,iDisponible;
    @FXML private TableColumn<Dotacion,String> dEquipo,dUnidad,dCantidad;
    @FXML private TableColumn<Mantenimiento,String> pEquipo,pUltima,pFecha,pEstado,pDescripcion,hEquipo,hFecha,hProxima,hDescripcion,hResponsable;
    public ReporteEquiposDatosController(Datos datos) { this.datos=datos; }
    public Parent vista() { return Vistas.cargar("reporte-equipos-datos.fxml",this); }
    @FXML private void initialize() {
        Ui.columna(iEquipo,Inventario::equipo); Ui.columna(iCategoria,Inventario::categoria);
        Ui.columna(iEstado,Inventario::estado); Ui.columna(iTotal,Inventario::total);
        Ui.columna(iAsignado,Inventario::asignado); Ui.columna(iDisponible,Inventario::disponible);
        Ui.columna(dEquipo,Dotacion::equipo); Ui.columna(dUnidad,Dotacion::unidad); Ui.columna(dCantidad,Dotacion::cantidad);
        Ui.columna(pEquipo,Mantenimiento::equipo); Ui.columna(pUltima,m->Ui.fecha(m.fecha()));
        Ui.columna(pFecha,m->Ui.fecha(m.proxima())); Ui.columna(pDescripcion,Mantenimiento::descripcion);
        Ui.columna(pEstado,m->m.proxima().isBefore(datos.generado().toLocalDate()) ? "Vencido" : "Programado");
        Ui.columna(hEquipo,Mantenimiento::equipo); Ui.columna(hFecha,m->Ui.fecha(m.fecha()));
        Ui.columna(hProxima,m->Ui.fecha(m.proxima())); Ui.columna(hDescripcion,Mantenimiento::descripcion);
        Ui.columna(hResponsable,Mantenimiento::responsable);
        inventario.getItems().setAll(datos.inventario()); dotacion.getItems().setAll(datos.dotacion());
        pendientes.getItems().setAll(datos.pendientes()); historial.getItems().setAll(datos.historial());
        for(TableView<?> tabla:java.util.List.of(inventario,dotacion,pendientes,historial)) {
            tabla.setTableMenuButtonVisible(true);
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
    }
}
