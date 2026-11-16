package edu.ni.uam.proyectofinalferrogest;

import edu.ni.uam.proyectofinalferrogest.model.*;
import edu.ni.uam.proyectofinalferrogest.service.*;
import edu.ni.uam.proyectofinalferrogest.service.DespachoService.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import edu.ni.uam.proyectofinalferrogest.controller.DespachoController;
import edu.ni.uam.proyectofinalferrogest.controller.Ui;
import static org.junit.jupiter.api.Assertions.*;

/** Prueba optativa contra PostgreSQL real, en un esquema temporal aislado. Nunca usa public. */
@EnabledIfSystemProperty(named="bcbm.pgtest",matches="true")
class DespachoPostgresTest {
    private static final String URL="jdbc:postgresql://localhost:5432/bomberos_db";
    private String esquema;
    private SessionFactory factory;
    private DespachoService servicio;
    private final SesionUsuario sesion=new SesionUsuario(1,"prueba",Rol.DESPACHADOR);
    private final LocalDateTime fecha=LocalDateTime.of(2025,1,31,23,50);
    @BeforeEach void preparar() throws Exception {
        esquema="prueba_despacho_"+UUID.randomUUID().toString().replace("-","");
        try (Connection c=DriverManager.getConnection(URL,"postgres",""); Statement s=c.createStatement()) {
            s.execute("create schema "+esquema); s.execute("set search_path to "+esquema);
            s.execute(Files.readString(Path.of("database/schema.sql")));
            s.execute("insert into bombero(nombre,apellido,cedula,rango) values ('Uno','Prueba','t1','Bombero'),('Dos','Prueba','t2','Bombero'),('Tres','Prueba','t3','Bombero'),('Cuatro','Prueba','t4','Bombero')");
            s.execute("insert into usuario(username,password_hash,rol) values ('prueba','no-utilizable','DESPACHADOR'),('bombero','no-utilizable','BOMBERO')");
            s.execute("insert into unidad(codigo,tipo) values ('U1','Rescate'),('U2','Rescate')");
            s.execute("insert into tipo_emergencia(nombre) values ('Accidente')");
            s.execute("insert into emergencia(tipo_id,direccion,prioridad,registrado_por) values (1,'Managua','ALTA',1)");
        }
        Configuration cfg=new Configuration();
        cfg.setProperty("hibernate.connection.url",URL+"?currentSchema="+esquema);
        cfg.setProperty("hibernate.connection.username","postgres"); cfg.setProperty("hibernate.connection.password","");
        cfg.setProperty("hibernate.hbm2ddl.auto","validate"); cfg.setProperty("hibernate.connection.pool_size","5");
        cfg.setProperty("hibernate.show_sql","false");
        for (Class<?> tipo:List.of(Bombero.class,Usuario.class,Unidad.class,Emergencia.class,TipoEmergencia.class,
            Asignacion.class,Equipo.class,MantenimientoEquipo.class,OtroServicio.class,SalidaUnidad.class)) cfg.addAnnotatedClass(tipo);
        factory=cfg.buildSessionFactory(); servicio=new DespachoService(factory);
    }
    @AfterEach void limpiar() throws Exception {
        if (factory!=null) factory.close();
        if (esquema!=null) try (Connection c=DriverManager.getConnection(URL,"postgres",""); Statement s=c.createStatement()) {
            // Solo el esquema aleatorio que esta prueba acaba de crear.
            if (!esquema.matches("prueba_despacho_[a-f0-9]{32}")) throw new IllegalStateException("Esquema inválido");
            s.execute("drop schema "+esquema+" cascade");
        }
    }
    private Tripulacion tripulacion(int unidad,int operador,Integer jefe,Integer... personas) {
        return new Tripulacion(unidad,operador,jefe,List.of(personas),fecha);
    }
    private EstadoUnidad estado(int unidad) {
        return factory.fromSession(s -> s.find(Unidad.class,unidad).getEstado());
    }
    @Test void dosUnidadesRetornosIndependientesYOtroServicioConservanHistorial() {
        Integer uno=servicio.asignarUnidad(sesion,1,tripulacion(1,1,2,1,2));
        Integer dos=servicio.asignarUnidad(sesion,1,tripulacion(2,3,4,3,4));
        assertEquals(2,servicio.consultarSalidas(sesion,1).size());
        assertThrows(IllegalArgumentException.class,() -> servicio.registrarServicio(sesion,"A",TipoServicio.COBERTURA_EVENTOS,"Evento",tripulacion(1,1,null)));
        assertThrows(IllegalArgumentException.class,() -> servicio.cerrarEmergencia(sesion,1));
        servicio.marcarLlegada(sesion,uno,fecha.plusMinutes(5)); servicio.marcarLlegada(sesion,dos,fecha.plusMinutes(6));
        servicio.registrarRetorno(sesion,uno,fecha.plusMinutes(30));
        assertEquals(EstadoUnidad.DISPONIBLE,estado(1)); assertEquals(EstadoUnidad.EN_EMERGENCIA,estado(2));
        assertThrows(IllegalArgumentException.class,() -> servicio.registrarServicio(sesion,"Escuadra A",TipoServicio.COBERTURA_EVENTOS,"Evento",tripulacion(1,1,null)));
        Integer evento=servicio.registrarServicio(sesion,"Escuadra A",TipoServicio.COBERTURA_EVENTOS,"Evento",
            new Tripulacion(1,1,null,List.of(1),fecha.plusMinutes(40)));
        assertThrows(IllegalArgumentException.class,() -> servicio.asignarUnidad(sesion,1,tripulacion(1,2,null)));
        assertEquals(EstadoUnidad.EN_EMERGENCIA,estado(1));
        servicio.registrarRetorno(sesion,evento,fecha.plusHours(3)); servicio.registrarRetorno(sesion,dos,fecha.plusHours(2));
        assertEquals(EstadoUnidad.DISPONIBLE,estado(1)); assertEquals(EstadoUnidad.DISPONIBLE,estado(2));
        var enero=YearMonth.of(2025,1);
        assertEquals(2,servicio.listarMes(sesion,enero,false).size()); assertEquals(1,servicio.listarMes(sesion,YearMonth.of(2025,2),true).size());
        assertEquals(1,servicio.listarMes(sesion,enero,false).stream().map(Salida::emergenciaId).distinct().count());
        assertEquals(4,servicio.listarMes(sesion,enero,false).stream().flatMap(x -> x.participantes().stream()).map(Persona::id).distinct().count());
        assertTrue(servicio.listarMes(sesion,YearMonth.of(2025,2),false).isEmpty());
        assertNotNull(servicio.listarMes(sesion,YearMonth.of(2025,2),true).getFirst().retorno());
        servicio.cerrarEmergencia(sesion,1);
        assertEquals(EstadoEmergencia.CERRADA,factory.fromSession(s -> s.find(Emergencia.class,1).getEstado()));
    }
    @Test void fallosReviertenSalidaDisponibilidadYServicioYLosPermisosSeAplicanEnServidor() {
        assertThrows(IllegalArgumentException.class,() -> servicio.registrarServicio(sesion,"",TipoServicio.TALLER,"Taller",tripulacion(1,1,null)));
        assertThrows(IllegalArgumentException.class,() -> servicio.asignarUnidad(sesion,1,tripulacion(1,999,null)));
        assertEquals(EstadoUnidad.DISPONIBLE,estado(1));
        assertTrue(servicio.listarMes(sesion,YearMonth.of(2025,1),true).isEmpty());
        assertEquals(0L,(long)factory.fromSession(s -> s.createSelectionQuery("select count(*) from OtroServicio",Long.class).getSingleResult()));
        assertThrows(SecurityException.class,() -> servicio.asignarUnidad(new SesionUsuario(2,"bombero",Rol.ADMIN),1,tripulacion(1,1,null)));
        assertThrows(SecurityException.class,() -> servicio.listarMes(new SesionUsuario(2,"bombero",Rol.ADMIN),YearMonth.of(2025,1),false));
    }
    @Test void dosDespachosSimultaneosSoloReservanUnaUnidadUnaVez() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(2); CountDownLatch inicio=new CountDownLatch(1);
        try {
            Callable<Boolean> emergencia=() -> { inicio.await(); try { servicio.asignarUnidad(sesion,1,tripulacion(1,1,null)); return true; } catch (IllegalArgumentException e) { return false; } };
            Callable<Boolean> evento=() -> { inicio.await(); try { servicio.registrarServicio(sesion,"A",TipoServicio.COBERTURA_EVENTOS,"Evento",tripulacion(1,2,null)); return true; } catch (IllegalArgumentException e) { return false; } };
            Future<Boolean> a=executor.submit(emergencia),b=executor.submit(evento); inicio.countDown();
            assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,servicio.listarActivas(sesion,false).size()+servicio.listarActivas(sesion,true).size());
            assertEquals(EstadoUnidad.EN_EMERGENCIA,estado(1));
        } finally { executor.shutdownNow(); }
    }
    @Test @EnabledIfSystemProperty(named="bcbm.fxtest",matches="true")
    void ambasPantallasJavaFxCarganYConsultanPostgres() throws Exception {
        assertTrue(edu.ni.uam.proyectofinalferrogest.util.HibernateUtil.getSessionFactory().isOpen());
        Platform.startup(() -> {});
        try {
            for (boolean otros:List.of(false,true)) {
                CompletableFuture<Parent> lista=new CompletableFuture<>();
                Platform.runLater(() -> {
                    try {
                        Parent vista=new DespachoController(sesion,otros,servicio).vista();
                        new Scene(vista,1280,800); vista.applyCss(); vista.layout();
                        vista.disableProperty().addListener((o,a,b) -> { if (!b) lista.complete(vista); });
                        if (!vista.isDisabled()) lista.complete(vista);
                    } catch (Throwable e) { lista.completeExceptionally(e); }
                });
                assertNotNull(lista.get(20,TimeUnit.SECONDS));
            }
        } finally { Ui.cerrar(); Platform.exit(); }
    }
}
