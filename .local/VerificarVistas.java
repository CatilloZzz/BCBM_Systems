package edu.ni.uam.proyectofinalferrogest.util;

import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.zip.*;
import edu.ni.uam.proyectofinalferrogest.controller.*;
import edu.ni.uam.proyectofinalferrogest.service.*;

/** Inspección local de las vistas: no guarda ni modifica registros. */
public class VerificarVistas {
    static final Path CAPTURAS=Path.of(".local/capturas");
    public static void main(String[] args) throws Exception {
        Files.createDirectories(CAPTURAS);
        var auth=new AuthService();
        var admin=auth.login("admin","admin123");
        var bombero=auth.login("bombero1","bombero123");
        var despachador=auth.login("despachador","despacho123");
        CountDownLatch start=new CountDownLatch(1); Platform.startup(start::countDown); start.await();
        try {
            Parent login=construir(() -> new LoginController().vista(s -> {}),1320,860);
            capturar(login,"login",null);
            ejecutarFX(() -> { buscarBoton(login,"Iniciar sesión").fire(); check(((Label)login.lookup("#error")).isVisible(),"Validación de login"); });
            Parent dialog=construir(() -> Vistas.cargar("primer-admin.fxml",new PrimerAdminController()),480,420);
            capturar(dialog,"primer-admin",null);
            Parent menu=construir(() -> new MenuController().vista(admin,() -> {}),1320,860);
            capturar(menu,"inicio",null);
            capturar(menu,"personal",() -> ((Button)menu.lookup("#personalButton")).fire());
            ejecutarFX(() -> {
                TableView<?> tabla=(TableView<?>)menu.lookup("#tabla"); check(tabla.getItems().size()==10,"Carga de personal");
                tabla.getSelectionModel().selectFirst(); check(!((TextField)menu.lookup("#nombre")).getText().isBlank(),"Selección carga formulario");
                TextField buscar=(TextField)menu.lookup("#buscar"); buscar.setText("maria"); check(tabla.getItems().size()==1,"Búsqueda sin acentos");
                buscar.clear(); buscarBoton(menu,"Nuevo").fire(); check(((TextField)menu.lookup("#nombre")).getText().isEmpty(),"Nuevo limpia formulario");
            });
            capturar(menu,"personal-seleccion",() -> ((TableView<?>)menu.lookup("#tabla")).getSelectionModel().selectFirst());
            capturar(menu,"equipos",() -> ((Button)menu.lookup("#equiposButton")).fire());
            ejecutarFX(() -> check(((TableView<?>)menu.lookup("#tabla")).getItems().size()==15,"Carga de equipos"));
            capturar(menu,"usuarios",() -> ((Button)menu.lookup("#usuariosButton")).fire());
            ejecutarFX(() -> { ((TableView<?>)menu.lookup("#tabla")).getSelectionModel().selectFirst(); check(!((TextField)menu.lookup("#username")).getText().isBlank(),"Formulario de usuarios"); });
            capturar(menu,"mantenimientos",() -> ((Button)menu.lookup("#mantenimientoButton")).fire());
            capturar(menu,"mantenimientos-historial",() -> {
                @SuppressWarnings("unchecked")
                ComboBox<edu.ni.uam.proyectofinalferrogest.model.Equipo> combo=(ComboBox<edu.ni.uam.proyectofinalferrogest.model.Equipo>)menu.lookup("#equipo");
                combo.setValue(combo.getItems().stream().filter(e -> e.getId()==1).findFirst().orElseThrow());
                buscarBoton(menu,"Consultar historial").fire();
            });
            ejecutarFX(() -> check(((TableView<?>)menu.lookup("#tabla")).getItems().size()>0,"Consulta de historial"));
            Parent pequeno=construir(() -> new MenuController().vista(admin,() -> {}),1120,760);
            capturar(pequeno,"inicio-compacto",null);
            capturar(pequeno,"personal-compacto",() -> ((Button)pequeno.lookup("#personalButton")).fire());
            Parent soloBombero=construir(() -> new MenuController().vista(bombero,() -> {}),1320,860);
            capturar(soloBombero,"bombero",() -> ((Button)soloBombero.lookup("#personalButton")).fire());
            ejecutarFX(() -> {
                check(!soloBombero.lookup("#usuariosButton").isVisible(),"Menú protegido");
                check(!soloBombero.lookup("#formulario").isManaged(),"Formulario oculto para BOMBERO");
                check(((TableView<?>)soloBombero.lookup("#tabla")).getItems().size()==1,"Ficha propia");
            });
            Parent soloDespacho=construir(() -> new MenuController().vista(despachador,() -> {}),1320,860);
            capturar(soloDespacho,"despachador",() -> ((Button)soloDespacho.lookup("#equiposButton")).fire());
            ejecutarFX(() -> check(!soloDespacho.lookup("#formulario").isManaged(),"Inventario de solo consulta"));
            System.out.println("VISTAS_OK: 8 FXML, navegación, búsqueda, selección, formularios, permisos y capturas en dos tamaños.");
        } finally { Ui.cerrar(); Platform.exit(); }
    }
    static Parent construir(Supplier<Parent> supplier,int w,int h) throws Exception {
        CompletableFuture<Parent> result=new CompletableFuture<>();
        Platform.runLater(() -> { try { Parent root=supplier.get(); Scene scene=new Scene(root,w,h); scene.getStylesheets().add(Vistas.estilos()); result.complete(root); } catch(Throwable e) { result.completeExceptionally(e); } });
        return result.get(30,TimeUnit.SECONDS);
    }
    static void capturar(Parent root,String name,Runnable accion) throws Exception {
        CompletableFuture<Void> done=new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                if (accion!=null) accion.run();
                Ui.ejecutar(new VBox(),() -> true,ok -> {
                    try { root.applyCss(); root.layout(); check(!root.isDisabled(),"Carga terminada: "+name); png(root.snapshot(null,null),CAPTURAS.resolve(name+".png")); done.complete(null); }
                    catch(Throwable e) { done.completeExceptionally(e); }
                },done::completeExceptionally);
            } catch(Throwable e) { done.completeExceptionally(e); }
        });
        done.get(40,TimeUnit.SECONDS);
    }
    static void ejecutarFX(Runnable r) throws Exception {
        CompletableFuture<Void> done=new CompletableFuture<>(); Platform.runLater(() -> { try { r.run(); done.complete(null); } catch(Throwable e) { done.completeExceptionally(e); } }); done.get(10,TimeUnit.SECONDS);
    }
    static Button buscarBoton(Parent root,String text) {
        for (Node node:root.lookupAll(".button")) if (node instanceof Button b && b.getText().equals(text)) return b;
        throw new AssertionError("No se encontró botón: "+text);
    }
    static void check(boolean condition,String message) { if (!condition) throw new AssertionError(message); }
    static void png(WritableImage image,Path path) throws Exception {
        int w=(int)image.getWidth(),h=(int)image.getHeight();
        try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(path))) {
            out.writeLong(0x89504e470d0a1a0aL);
            ByteArrayOutputStream header=new ByteArrayOutputStream(); DataOutputStream ihdr=new DataOutputStream(header);
            ihdr.writeInt(w); ihdr.writeInt(h); ihdr.write(new byte[]{8,6,0,0,0}); chunk(out,"IHDR",header.toByteArray());
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(DeflaterOutputStream z=new DeflaterOutputStream(bytes)) {
                var pixels=image.getPixelReader();
                for(int y=0;y<h;y++) { z.write(0); for(int x=0;x<w;x++) { int argb=pixels.getArgb(x,y); z.write(argb>>>16&255); z.write(argb>>>8&255); z.write(argb&255); z.write(argb>>>24&255); } }
            }
            chunk(out,"IDAT",bytes.toByteArray()); chunk(out,"IEND",new byte[0]);
        }
    }
    static void chunk(DataOutputStream out,String type,byte[] data) throws IOException {
        byte[] t=type.getBytes(java.nio.charset.StandardCharsets.US_ASCII); out.writeInt(data.length);out.write(t);out.write(data);CRC32 crc=new CRC32();crc.update(t);crc.update(data);out.writeInt((int)crc.getValue());
    }
}
