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
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import edu.ni.uam.proyectofinalferrogest.controller.DespachoController;
import edu.ni.uam.proyectofinalferrogest.controller.MenuController;
import edu.ni.uam.proyectofinalferrogest.controller.Ui;
import edu.ni.uam.proyectofinalferrogest.dao.EmergenciaDao;
import static org.junit.jupiter.api.Assertions.*;

/** Prueba optativa contra PostgreSQL real, en un esquema temporal aislado. Nunca usa public. */
@EnabledIfSystemProperty(named="bcbm.pgtest",matches="true")
class DespachoPostgresTest {
    private static final String URL="jdbc:postgresql://localhost:5432/bomberos_db";
    private String esquema;
    private SessionFactory factory;
    private DespachoService servicio;
    private String usuarioPg;
    private String clavePg;
    private final SesionUsuario sesion=new SesionUsuario(1,"prueba",Rol.DESPACHADOR);
    private final LocalDateTime fecha=LocalDateTime.of(2025,1,31,23,50);
    @BeforeEach void preparar() throws Exception {
        var config = new Properties();
        try (var lector = Files.newBufferedReader(Path.of("config.properties"))) { config.load(lector); }
        usuarioPg = config.getProperty("db.user");
        clavePg = config.getProperty("db.password");
        esquema="prueba_despacho_"+UUID.randomUUID().toString().replace("-","");
        try (Connection c=DriverManager.getConnection(URL,usuarioPg,clavePg); Statement s=c.createStatement()) {
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
        cfg.setProperty("hibernate.connection.username",usuarioPg); cfg.setProperty("hibernate.connection.password",clavePg);
        cfg.setProperty("hibernate.hbm2ddl.auto","validate"); cfg.setProperty("hibernate.connection.pool_size","5");
        cfg.setProperty("hibernate.show_sql","false");
        for (Class<?> tipo:List.of(Bombero.class,Usuario.class,Unidad.class,Emergencia.class,TipoEmergencia.class,
            Asignacion.class,Equipo.class,MantenimientoEquipo.class,OtroServicio.class,SalidaUnidad.class)) cfg.addAnnotatedClass(tipo);
        factory=cfg.buildSessionFactory(); servicio=new DespachoService(factory);
    }
    @AfterEach void limpiar() throws Exception {
        if (factory!=null) factory.close();
        if (esquema!=null) try (Connection c=DriverManager.getConnection(URL,usuarioPg,clavePg); Statement s=c.createStatement()) {
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

    @Test void despachoInicialReservaTripulacionYReporteCuentaActasUnaVez() {
        var dao=new EmergenciaDao(factory);
        var datos=new Emergencia.DatosActa("A-025",fecha,"Escuadra A",null,null,null,null,1,null,null,null);
        int acta=dao.guardarActa(sesion,null,0,datos,tripulacion(1,1,2));
        assertEquals(EstadoBombero.EN_EMERGENCIA,factory.fromSession(s->s.find(Bombero.class,2).getEstado()));
        var duplicada=new Emergencia.DatosActa("A-026",fecha,null,null,null,null,null,null,null,null,null);
        assertThrows(IllegalArgumentException.class,()->dao.guardarActa(sesion,null,0,duplicada,tripulacion(2,2,null)));
        assertTrue(dao.listarMes(sesion,YearMonth.from(fecha),"A-026",null,null,null).isEmpty());
        assertEquals(EstadoUnidad.DISPONIBLE,estado(2));
        servicio.asignarUnidad(sesion,acta,tripulacion(2,3,null));
        var salida=servicio.consultarSalidas(sesion,acta).stream().filter(x->x.unidadId()==1).findFirst().orElseThrow();
        assertEquals(fecha,salida.salida());
        servicio.marcarLlegada(sesion,salida.id(),fecha.plusMinutes(1));
        servicio.registrarRetorno(sesion,salida.id(),fecha.plusMinutes(2));
        assertEquals(EstadoBombero.DISPONIBLE,factory.fromSession(s->s.find(Bombero.class,2).getEstado()));
        servicio.registrarServicio(sesion,"DPC",TipoServicio.SIMULACRO,"Prueba",new Tripulacion(1,1,null,List.of(),fecha.plusMinutes(3)));
        assertEquals(EstadoBombero.EN_SERVICIO,factory.fromSession(s->s.find(Bombero.class,1).getEstado()));
        var filas=new ReporteCabinaService(factory).consultar(sesion,YearMonth.from(fecha));
        assertEquals(1,filas.stream().filter(f->f.seccion().equals("Emergencias")).mapToLong(ReporteCabinaService.Fila::cantidad).sum());
        var unidad=filas.stream().filter(f->f.grupo().equals("U1")).toList();
        assertEquals(2,unidad.size());
        assertTrue(unidad.stream().allMatch(f->f.total()==2 && f.cantidad()==1));
        assertTrue(filas.stream().anyMatch(f->f.seccion().equals("Personal") && f.grupo().contains("Uno Prueba") && f.total()==2));
    }
    @Test void flotaControlaDotacionMantenimientoYPermisos() {
        var flota=new FlotaService(factory);
        assertThrows(SecurityException.class,()->flota.guardar(sesion,null,"M3","Rescate",EstadoUnidad.DISPONIBLE));
        factory.inTransaction(s->{
            s.createNativeMutationQuery("update usuario set rol='ADMIN' where id=1").executeUpdate();
            s.createNativeMutationQuery("insert into equipo(nombre,categoria,cantidad) values ('Manguera','Herramienta',3)").executeUpdate();
        });
        flota.guardar(sesion,1,"U1","Rescate",EstadoUnidad.MANTENIMIENTO);
        assertThrows(IllegalArgumentException.class,()->servicio.asignarUnidad(sesion,1,tripulacion(1,1,null)));
        flota.guardar(sesion,1,"U1","Rescate",EstadoUnidad.DISPONIBLE);
        flota.moverEquipo(sesion,1,1,true);
        assertEquals("U1",flota.consultar(sesion).equipos().getFirst().getUnidad().getCodigo());
        assertThrows(IllegalArgumentException.class,()->flota.moverEquipo(sesion,2,1,true));
        flota.moverEquipo(sesion,1,1,false);
        assertNull(flota.consultar(sesion).equipos().getFirst().getUnidad());
        servicio.registrarServicio(sesion,"A",TipoServicio.RECORRIDO,"Prueba",tripulacion(1,1,null));
        assertThrows(IllegalArgumentException.class,()->flota.guardar(sesion,1,"U1","Rescate",EstadoUnidad.MANTENIMIENTO));
        assertThrows(IllegalArgumentException.class,()->flota.moverEquipo(sesion,1,1,true));
    }
    @Test void actaParcialFiltrosYDespachoDesdeLaFicha() {
        var dao = new EmergenciaDao(factory);
        var parcial = new Emergencia.DatosActa("A-023", fecha, null, null, null, null, null, null, null, null, null);
        int id = dao.guardarActa(sesion, null, 0, parcial);
        var enero = YearMonth.from(fecha);
        var registro = dao.listarMes(sesion, enero, "a-023", null, null, null).getFirst().emergencia();
        assertEquals(id, registro.getId());
        assertNull(registro.getDescripcion());
        assertTrue(servicio.consultarSalidas(sesion, id).isEmpty());
        var completo = new Emergencia.DatosActa("A-023", fecha, "Escuadra A", "Solicitante", "8888-8888",
                "Managua", "Accidente", 1, PrioridadEmergencia.ALTA, 12.13, -86.25);
        dao.guardarActa(sesion, id, registro.getVersion(), completo);
        var actualizado = dao.buscar(sesion, id).emergencia();
        assertEquals("8888-8888", actualizado.getTelefono());
        assertEquals(12.13, actualizado.getLatitud());
        assertThrows(IllegalArgumentException.class, () -> dao.guardarActa(sesion, null, 0,
                new Emergencia.DatosActa("!", fecha, null, null, null, null, null, null, null, null, null)));
        assertThrows(IllegalStateException.class, () -> dao.guardarActa(sesion, 99999, 0, completo));
        assertThrows(RuntimeException.class, () -> dao.guardarActa(sesion, null, 0, parcial));
        assertThrows(SecurityException.class, () -> dao.guardarActa(new SesionUsuario(2, "bombero", Rol.ADMIN), null, 0, parcial));

        int uno = servicio.asignarUnidad(sesion, id, tripulacion(1, 1, 2));
        int dos = servicio.asignarUnidad(sesion, id, tripulacion(2, 3, 4));
        servicio.marcarLlegada(sesion, uno, fecha.plusMinutes(5));
        servicio.marcarLlegada(sesion, dos, fecha.plusMinutes(7));
        servicio.registrarRetorno(sesion, uno, fecha.plusMinutes(30));
        var salidas = servicio.consultarSalidas(sesion, List.of(id));
        assertEquals(2, salidas.size());
        assertEquals(4, salidas.stream().flatMap(x -> x.participantes().stream()).map(Persona::id).distinct().count());
        assertEquals(fecha.plusMinutes(30), salidas.stream().filter(x -> x.id() == uno).findFirst().orElseThrow().retorno());
        assertNull(salidas.stream().filter(x -> x.id() == dos).findFirst().orElseThrow().retorno());
        assertEquals(1, dao.listarMes(sesion, enero, "A-023", "Escuadra A", 1, EstadoEmergencia.EN_PROCESO).size());
        assertTrue(dao.listarMes(sesion, enero.plusMonths(1), "A-023", null, null, null).isEmpty());
        assertTrue(dao.listarMes(sesion, enero, "A-023", "Escuadra B", 1, null).isEmpty());
        long version = dao.buscar(sesion, id).emergencia().getVersion();
        assertThrows(IllegalArgumentException.class, () -> servicio.cerrarEmergencia(sesion, id, version, "Final", ResultadoEmergencia.ATENDIDA));
        assertNull(dao.buscar(sesion, id).emergencia().getResultado());
        servicio.registrarRetorno(sesion, dos, fecha.plusHours(1));
        servicio.cerrarEmergencia(sesion, id, version, "Final", ResultadoEmergencia.ATENDIDA);
        var cerrada = dao.buscar(sesion, id).emergencia();
        assertEquals("Accidente", cerrada.getDescripcion());
        assertEquals("Final", cerrada.getDescripcionFinal());
        assertEquals(ResultadoEmergencia.ATENDIDA, cerrada.getResultado());
        assertEquals(EstadoEmergencia.CERRADA, cerrada.getEstado());

        var aviso = new Emergencia.DatosActa("A-024", fecha, null, null, null, null, null, null, null, null, null);
        int falsa = dao.guardarActa(sesion, null, 0, aviso);
        servicio.cerrarEmergencia(sesion, falsa, 0L, "Aviso descartado", ResultadoEmergencia.FALSA_ALARMA);
        assertEquals(ResultadoEmergencia.FALSA_ALARMA, dao.buscar(sesion, falsa).emergencia().getResultado());
        assertTrue(servicio.consultarSalidas(sesion, falsa).isEmpty());
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
        assertEquals(EstadoUnidad.EN_SERVICIO,estado(1));
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
            assertEquals(a.get() ? EstadoUnidad.EN_EMERGENCIA : EstadoUnidad.EN_SERVICIO,estado(1));
        } finally { executor.shutdownNow(); }
    }
    @Test @EnabledIfSystemProperty(named="bcbm.fxtest",matches="true")
    void ambasPantallasJavaFxCarganYConsultanPostgres() throws Exception {
        assertTrue(edu.ni.uam.proyectofinalferrogest.util.HibernateUtil.getSessionFactory().isOpen());
        var admin = new AuthService().login("admin", "admin123");
        Platform.startup(() -> {});
        try {
            var menu = new FutureTask<Void>(() -> {
                Parent vista = new MenuController().vista(admin, () -> {});
                new Scene(vista, 1320, 860);
                vista.applyCss();
                var emergencias = (Button) vista.lookup("#emergenciasButton");
                assertNotNull(emergencias);
                ((Button)vista.lookup("#unidadesButton")).fire();
                vista.applyCss(); vista.layout();
                assertNotNull(vista.lookup("#dotacion"));
                ((Button)vista.lookup("#reportesButton")).fire();
                vista.applyCss(); vista.layout();
                assertNotNull(vista.lookup("#resumen"));
                emergencias.fire();
                vista.applyCss(); vista.layout();
                var tabla = (TableView<?>) vista.lookup("#tabla");
                assertNotNull(tabla);
                assertTrue(tabla.getColumns().stream().anyMatch(c -> c.getText().equals("N.º de acta")));
                assertTrue(tabla.getColumns().stream().anyMatch(c -> c.getText().equals("Retorna")));
                var estado = (javafx.scene.control.ComboBox<?>) vista.lookup("#estado");
                var tipo = (javafx.scene.control.ComboBox<?>) vista.lookup("#tipo");
                var filtrar = (Button) vista.lookup("#aplicarFiltros");
                var mes = (javafx.scene.control.ComboBox<?>) vista.lookup("#mes");
                mes.getSelectionModel().select(0);
                estado.getSelectionModel().select(1);
                tipo.getSelectionModel().select(1);
                filtrar.fire();
                assertEquals(1, tipo.getSelectionModel().getSelectedIndex());
                estado.getSelectionModel().selectFirst();
                filtrar.fire();
                assertEquals("Todos", estado.getValue().toString());
                assertEquals(1, tipo.getSelectionModel().getSelectedIndex());
                estado.getSelectionModel().select(1);
                tipo.getSelectionModel().selectFirst();
                filtrar.fire();
                assertEquals(1, estado.getSelectionModel().getSelectedIndex());
                assertEquals("Todos", tipo.getValue().toString());
                return null;
            });
            Platform.runLater(menu);
            menu.get(20, TimeUnit.SECONDS);
            servicio.asignarUnidad(sesion, 1, tripulacion(1, 1, null));
            var movimientos = new FutureTask<Void>(() -> {
                var vista = edu.ni.uam.proyectofinalferrogest.controller.EmergenciasController.cargar(factory, sesion);
                var ventana = new javafx.stage.Stage();
                ventana.setScene(new Scene(vista, 900, 680));
                ventana.show();
                try {
                    var crear = (Button) vista.lookup("#nueva");
                    assertEquals("Nueva emergencia", crear.getText());
                    assertFalse(crear.isDisabled());
                    var tabla = (TableView<?>) vista.lookup("#tabla");
                    tabla.getSelectionModel().selectFirst();
                    for (String accion : List.of("#llegada", "#retorno")) {
                        var boton = (Button) vista.lookup(accion);
                        assertFalse(boton.isDisabled(), accion);
                        Platform.runLater(() -> javafx.stage.Window.getWindows().stream()
                                .filter(w -> w.getScene().getRoot() instanceof javafx.scene.control.DialogPane)
                                .map(w -> (javafx.scene.control.DialogPane) w.getScene().getRoot())
                                .findFirst().ifPresent(p -> ((Button)p.lookupButton(javafx.scene.control.ButtonType.OK)).fire()));
                        boton.fire();
                        assertTrue(boton.isDisabled(), accion);
                    }
                    return null;
                } finally { ventana.close(); }
            });
            Platform.setImplicitExit(false);
            Platform.runLater(movimientos);
            movimientos.get(25, TimeUnit.SECONDS);
            var salida = servicio.consultarSalidas(sesion, 1).getFirst();
            assertNotNull(salida.llegada());
            assertNotNull(salida.retorno());
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
