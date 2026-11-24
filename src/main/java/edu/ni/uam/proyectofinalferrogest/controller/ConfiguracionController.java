package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.dao.TipoEmergenciaDao;
import edu.ni.uam.proyectofinalferrogest.util.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.stream.Collectors;

public class ConfiguracionController {
    private final SesionUsuario sesion;
    private final ConsultasCabina.ConfiguracionService servicio=new ConsultasCabina.ConfiguracionService(HibernateUtil.getSessionFactory());
    private final TipoEmergenciaDao tipos=new TipoEmergenciaDao(HibernateUtil.getSessionFactory());
    @FXML private VBox root;
    @FXML private ComboBox<GrupoCatalogo> grupo;
    @FXML private TableView<OpcionCatalogo> catalogo;
    @FXML private TableColumn<OpcionCatalogo,String> nombreCol,activoCol;
    @FXML private TextField nombre,nombreEmergencia;
    @FXML private CheckBox activo,todas;
    @FXML private ComboBox<ConsultasCabina.ConfiguracionService.Operador> operador;
    @FXML private ListView<Unidad> autorizadas;
    @FXML private TableView<TipoEmergencia> emergencias;
    @FXML private TableColumn<TipoEmergencia,String> emergenciaCol;
    @FXML private Label mensaje;
    @FXML private ProgressIndicator cargando;
    @FXML private Tab unidadesTab,operadoresTab,tiposTab;
    public ConfiguracionController(SesionUsuario sesion) { this.sesion=sesion; }
    public Parent vista() { return Vistas.cargar("configuracion.fxml",this); }
    @FXML private void initialize() {
        Ui.carga(root,cargando);
        Ui.columna(nombreCol,OpcionCatalogo::getNombre);
        Ui.columna(activoCol,o->o.isActivo() ? "Activo" : "Inactivo");
        Ui.columna(emergenciaCol,TipoEmergencia::getNombre);
        catalogo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        emergencias.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        grupo.getItems().setAll(GrupoCatalogo.values());
        grupo.valueProperty().addListener((o,a,b)->cargarCatalogo());
        catalogo.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{
            nombre.setText(b==null ? "" : b.getNombre()); activo.setSelected(b==null || b.isActivo());
        });
        emergencias.getSelectionModel().selectedItemProperty().addListener((o,a,b)->nombreEmergencia.setText(b==null ? "" : b.getNombre()));
        autorizadas.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        autorizadas.disableProperty().bind(todas.selectedProperty().or(operador.valueProperty().isNull()));
        operador.valueProperty().addListener((o,a,b)->{
            todas.setSelected(b!=null && b.todas()); autorizadas.getSelectionModel().clearSelection();
            if (b!=null) for (Unidad u:autorizadas.getItems()) if (b.unidades().contains(u.getId())) autorizadas.getSelectionModel().select(u);
        });
        operadoresTab.setOnSelectionChanged(e->{ if (operadoresTab.isSelected()) cargarOperadores(); });
        tiposTab.setOnSelectionChanged(e->{ if (tiposTab.isSelected()) cargarTipos(); });
        unidadesTab.setOnSelectionChanged(e->{
            if (unidadesTab.isSelected()) {
                ScrollPane contenido=new ScrollPane(new UnidadesController(sesion).vista());
                contenido.setFitToWidth(true); contenido.setFitToHeight(true);
                contenido.getStyleClass().add("page-scroll");
                unidadesTab.setContent(contenido);
            }
        });
        grupo.getSelectionModel().selectFirst();
    }
    @FXML private void cargarCatalogo() {
        GrupoCatalogo g=grupo.getValue(); if(g==null) return;
        Ui.ejecutar(root,()->servicio.listar(sesion,g),datos->{
            catalogo.getItems().setAll(datos); nuevo(); mensaje.setText("Los cambios se reflejan al abrir o actualizar los formularios.");
        });
    }
    @FXML private void nuevo() { catalogo.getSelectionModel().clearSelection(); nombre.clear(); activo.setSelected(true); }
    @FXML private void guardarCatalogo() {
        var actual=catalogo.getSelectionModel().getSelectedItem();
        var g=grupo.getValue(); var texto=nombre.getText(); boolean habilitado=activo.isSelected();
        Ui.ejecutar(root,()->{
            servicio.guardar(sesion,actual==null ? null : actual.getId(),actual==null ? 0 : actual.getVersion(),g,texto,habilitado);
            return true;
        },ok->cargarCatalogo());
    }
    @FXML private void cargarOperadores() {
        Ui.ejecutar(root,()->servicio.permisos(sesion),datos->{
            operador.setValue(null); operador.getItems().setAll(datos.personal());
            autorizadas.getItems().setAll(datos.unidades());
            mensaje.setText(datos.personal().isEmpty() ? "Registra primero el personal en su módulo." : "Selecciona una persona para consultar sus autorizaciones.");
        });
    }
    @FXML private void guardarOperador() {
        var persona=operador.getValue(); boolean general=todas.isSelected();
        Set<Integer> ids=autorizadas.getSelectionModel().getSelectedItems().stream().map(Unidad::getId).collect(Collectors.toSet());
        Ui.ejecutar(root,()->{servicio.guardarOperador(sesion,persona==null ? null : persona.id(),general,ids);return true;},ok->{
            cargarOperadores(); mensaje.setText("Autorizaciones guardadas.");
        });
    }
    @FXML private void sinUnidades() { todas.setSelected(false); autorizadas.getSelectionModel().clearSelection(); }
    @FXML private void cargarTipos() { Ui.ejecutar(root,tipos::listar,datos->{ emergencias.getItems().setAll(datos); nuevoTipo(); }); }
    @FXML private void nuevoTipo() { emergencias.getSelectionModel().clearSelection(); nombreEmergencia.clear(); }
    @FXML private void guardarTipo() {
        TipoEmergencia actual=emergencias.getSelectionModel().getSelectedItem(); String texto=nombreEmergencia.getText();
        Ui.ejecutar(root,()->{tipos.guardar(sesion,actual==null ? null : actual.getId(),actual==null ? 0 : actual.getVersion(),texto);return true;},ok->cargarTipos());
    }
    @FXML private void eliminarTipo() {
        TipoEmergencia actual=emergencias.getSelectionModel().getSelectedItem(); if(actual==null) {mensaje.setText("Selecciona un tipo de emergencia.");return;}
        if(!Ui.confirmar(root,"¿Eliminar el tipo "+actual.getNombre()+"? Solo se permite si está sin uso.")) return;
        Ui.ejecutar(root,()->{tipos.eliminar(sesion,actual.getId(),actual.getVersion());return true;},ok->cargarTipos());
    }
}
