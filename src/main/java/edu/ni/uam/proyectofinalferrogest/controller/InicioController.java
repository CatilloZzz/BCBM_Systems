package edu.ni.uam.proyectofinalferrogest.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.util.Vistas;

public class InicioController {
    @FXML private ScrollPane root;
    @FXML private Label saludo,totalPersonal,personalDisponible,totalEquipos,equiposReparacion,alcancePersonal;
    @FXML private ProgressIndicator cargando;
    private final SesionUsuario sesion;
    private final Runnable personal,equipos,mantenimiento;
    private record Resumen(List<Bombero> bomberos,List<Equipo> equipos) {}
    public InicioController(SesionUsuario sesion,Runnable personal,Runnable equipos,Runnable mantenimiento) {
        this.sesion=sesion; this.personal=personal; this.equipos=equipos; this.mantenimiento=mantenimiento;
    }
    public Parent vista() { return Vistas.cargar("inicio.fxml",this); }
    @FXML private void initialize() {
        saludo.setText("Bienvenido, "+sesion.username()+"."); Ui.carga(root,cargando);
        if (sesion.rol()==Rol.BOMBERO) alcancePersonal.setText("Mi ficha de personal");
        actualizar();
    }
    @FXML private void actualizar() {
        Ui.ejecutar(root,() -> new Resumen(new BomberoService().listar(sesion),new EquipoService().listar(sesion)),r -> {
            totalPersonal.setText(""+r.bomberos().size());
            personalDisponible.setText(r.bomberos().stream().filter(b -> b.getEstado()==EstadoBombero.DISPONIBLE).count()+" disponibles");
            totalEquipos.setText(""+r.equipos().size());
            equiposReparacion.setText(""+r.equipos().stream().filter(e -> e.getEstado()==EstadoEquipo.EN_REPARACION).count());
        });
    }
    @FXML private void verPersonal() { personal.run(); }
    @FXML private void verEquipos() { equipos.run(); }
    @FXML private void verMantenimiento() { mantenimiento.run(); }
}
