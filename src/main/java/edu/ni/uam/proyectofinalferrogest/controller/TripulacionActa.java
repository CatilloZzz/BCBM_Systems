package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.service.DespachoService.*;
import edu.ni.uam.proyectofinalferrogest.model.EstadoUnidad;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Despacho opcional al abrir un acta. */
final class TripulacionActa extends VBox {
    private final CheckBox incluir = new CheckBox("Despachar una unidad al guardar");
    private final ComboBox<Vehiculo> unidad = new ComboBox<>();
    private final ComboBox<Persona> operador = new ComboBox<>(), jefe = new ComboBox<>();
    private final ListView<Persona> adicionales = new ListView<>();
    private final DespachoController.FechaHora salida = new DespachoController.FechaHora();

    TripulacionActa(Catalogos catalogos) {
        super(8);
        unidad.getItems().setAll(catalogos.unidades().stream()
                .filter(u -> u.estado().equals(EstadoUnidad.DISPONIBLE.name())).toList());
        operador.getItems().setAll(catalogos.personal());
        jefe.getItems().setAll(catalogos.personal());
        adicionales.getItems().setAll(catalogos.personal());
        adicionales.setPrefHeight(110);
        adicionales.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        var campos = new VBox(5,new Label("Unidad disponible"),unidad,new Label("Operador"),operador,
                new Label("Jefe de unidad (opcional)"),jefe,new Label("Otros integrantes (Ctrl para seleccionar)"),
                adicionales,new Label("Fecha y hora exacta de salida"),salida.contenido);
        campos.visibleProperty().bind(incluir.selectedProperty());
        campos.managedProperty().bind(incluir.selectedProperty());
        getChildren().addAll(incluir,campos);
    }
    Tripulacion valor() {
        if (!incluir.isSelected()) return null;
        if (unidad.getValue()==null || operador.getValue()==null)
            throw new IllegalArgumentException("Selecciona la unidad y su operador para despachar.");
        return new Tripulacion(unidad.getValue().id(),operador.getValue().id(),
                jefe.getValue()==null ? null : jefe.getValue().id(),
                adicionales.getSelectionModel().getSelectedItems().stream().map(Persona::id).toList(),salida.valor());
    }
}
