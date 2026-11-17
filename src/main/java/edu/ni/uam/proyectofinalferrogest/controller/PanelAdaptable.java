package edu.ni.uam.proyectofinalferrogest.controller;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Listado y formulario en columnas, o apilados en ventanas pequeñas. */
public class PanelAdaptable extends HBox {
    public PanelAdaptable() {
        setMinWidth(0);
        widthProperty().addListener((o,a,b) -> requestLayout());
    }
    private boolean compacto(double ancho) { return ancho > 0 && ancho < 900; }
    private double alto(Node nodo, int indice, double ancho) {
        return Math.max(indice==0 ? 310 : 560, nodo.minHeight(ancho));
    }
    @Override protected double computeMinWidth(double height) { return 0; }
    @Override protected double computeMinHeight(double width) {
        if (!compacto(width)) return 440;
        var nodos=getManagedChildren();
        double alto=getInsets().getTop()+getInsets().getBottom();
        for (int i=0;i<nodos.size();i++) alto+=alto(nodos.get(i),i,width)+(i==0 ? 0 : getSpacing());
        return alto;
    }
    @Override protected void layoutChildren() {
        var nodos=getManagedChildren();
        for (Node n:nodos) if (n instanceof Region r) { r.setMinWidth(0); r.setMaxWidth(Double.MAX_VALUE); }
        if (!compacto(getWidth())) { super.layoutChildren(); return; }
        double y=getInsets().getTop();
        double ancho=getWidth()-getInsets().getLeft()-getInsets().getRight();
        for (int i=0;i<nodos.size();i++) {
            Node n=nodos.get(i); double alto=alto(n,i,ancho);
            n.resizeRelocate(getInsets().getLeft(),y,ancho,alto);
            y+=alto+getSpacing();
        }
    }
}
