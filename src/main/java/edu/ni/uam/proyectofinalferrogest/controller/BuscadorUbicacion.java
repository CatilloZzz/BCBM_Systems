package edu.ni.uam.proyectofinalferrogest.controller;

import edu.ni.uam.proyectofinalferrogest.model.Emergencia;
import edu.ni.uam.proyectofinalferrogest.service.ConsultasCabina;
import edu.ni.uam.proyectofinalferrogest.service.ConsultasCabina.Lugar;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class BuscadorUbicacion extends VBox {
    private final TextField referencia = new TextField();
    private final Button buscar = new Button("Buscar referencia");
    private final ComboBox<Lugar> resultados = new ComboBox<>();
    private final Label aviso = new Label();
    private final Label coordenadas = new Label();
    private Double latitud, longitud;

    BuscadorUbicacion(Emergencia emergencia) {
        super(6);
        referencia.setPromptText("Lugar público de Managua, p. ej. Parque Central");
        referencia.setPrefColumnCount(26);
        resultados.setMaxWidth(440);
        resultados.setPromptText("Seleccionar un lugar");
        var confirmar = new Button("Usar ubicación");
        confirmar.disableProperty().bind(resultados.valueProperty().isNull());
        confirmar.setOnAction(event -> {
            Lugar lugar = resultados.getValue();
            ubicar(lugar.latitud(), lugar.longitud());
        });
        var quitar = new Button("Quitar ubicación");
        quitar.setOnAction(event -> ubicar(null, null));
        ubicar(emergencia == null ? null : emergencia.getLatitud(), emergencia == null ? null : emergencia.getLongitud());
        buscar.setOnAction(event -> buscarReferencia());
        referencia.textProperty().addListener((obs, anterior, actual) -> {
            resultados.getItems().clear();
            aviso.setText("");
        });
        getChildren().addAll(new Label("Referencia pública opcional · Nominatim"),
                new HBox(6, referencia, buscar), resultados, new HBox(6, confirmar, quitar),
                coordenadas, aviso, new Label("© OpenStreetMap contributors · openstreetmap.org/copyright"));
    }

    Double latitud() { return latitud; }
    Double longitud() { return longitud; }

    private void ubicar(Double latitud, Double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
        coordenadas.setText(latitud == null ? "Sin coordenadas" : "Coordenadas elegidas: " + latitud + ", " + longitud);
    }

    private void buscarReferencia() {
        String texto = referencia.getText().strip();
        if (texto.length() < 3 || texto.length() > 150) {
            aviso.setText("Escribe una referencia de entre 3 y 150 caracteres.");
            return;
        }
        resultados.getItems().clear();
        referencia.setDisable(true);
        buscar.setDisable(true);
        aviso.setText("Buscando…");
        ConsultasCabina.buscar(texto).whenComplete((lugares, error) -> Platform.runLater(() -> {
            buscar.setDisable(false);
            referencia.setDisable(false);
            if (error != null) {
                aviso.setText("Consulta no disponible. Puedes guardar la dirección manualmente.");
                return;
            }
            resultados.getItems().setAll(lugares);
            aviso.setText(lugares.isEmpty() ? "Sin coincidencias." : "Elige el resultado y pulsa Usar ubicación.");
        }));
    }
}
