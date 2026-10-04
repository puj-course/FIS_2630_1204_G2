

import session.SesionUsuario;
import javafx.application.Application;
import controller.LoginController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.util.List;

/**
 * Punto de entrada único de GastroFlow.
 *
 * La pantalla de inicio es la navegación principal: una barra lateral con la
 * marca, el rol activo y el cierre de sesión, y un panel con los módulos que
 * ese rol puede abrir. Cada módulo se abre en su propia ventana con su propia
 * hoja de estilos: las hojas definen reglas sobre `.root`, que en JavaFX aplica
 * al nodo raíz de la escena, así que mezclarlas en una sola escena haría que
 * una pisara a la otra.
 *
 * Toda la apariencia vive en /css/navegacion.css; aquí solo se asignan clases.
 */
public class Main extends Application {

    /** Un módulo del sistema, el rol que lo tiene asignado y su icono. */
    private record Modulo(String nombre, String detalle, String rol, String icono,
                          String tituloVentana, String fxml, String css,
                          int ancho, int alto) {
    }

    // Trazos de los iconos, en una caja de 24x24.
    private static final String ICONO_LISTA =
            "M22,7h-9v2h9V7z M22,15h-9v2h9V15z M5.54,11L2,7.46l1.41-1.41l2.12,2.12l4.24-4.24l1.41,1.41L5.54,11z "
          + "M5.54,19L2,15.46l1.41-1.41l2.12,2.12l4.24-4.24l1.41,1.41L5.54,19z";
    private static final String ICONO_MENU =
            "M11,9H9V2H7v7H5V2H3v7c0,2.12,1.66,3.84,3.75,3.97V22h2.5v-9.03C11.34,12.84,13,11.12,13,9V2h-2V9z "
          + "M16,6v8h2.5v8H21V2C18.24,2,16,4.24,16,6z";
    private static final String ICONO_MESAS =
            "M3,3h8v8H3V3z M13,3h8v8h-8V3z M3,13h8v8H3V13z M13,13h8v8h-8V13z";
    private static final String ICONO_PAGO =
            "M20,4H4C2.89,4,2.01,4.89,2.01,6L2,18c0,1.11,0.89,2,2,2h16c1.11,0,2-0.89,2-2V6C22,4.89,21.11,4,20,4z "
          + "M20,18H4v-6h16V18z M20,8H4V6h16V8z";
    private static final String ICONO_VACIO =
            "M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M12,20c-4.41,0-8-3.59-8-8s3.59-8,8-8"
          + "s8,3.59,8,8S16.41,20,12,20z M11,7h2v6h-2V7z M11,15h2v2h-2V15z";
    private static final String ICONO_COCINA =
            "M11,9H9V2H7v7H5V2H3v7c0,2.12,1.66,3.84,3.75,3.97V22h2.5v-9.03C11.34,12.84,13,11.12,13,9V2h-2V9z";

    /**
     * Módulos disponibles. La asignación por rol es la que ya tenía el menú
     * anterior en el texto de sus botones; esta historia no cambia quién accede
     * a qué, solo cómo se muestra.
     */
    private static final List<Modulo> MODULOS = List.of(
            new Modulo("Disponibilidad de platos",
                    "Estado de los platos según el inventario", "ADMINISTRADOR", ICONO_LISTA,
                    "GastroFlow · Disponibilidad de platos",
                    "/views/admin-disponibilidad.fxml", "/css/disponibilidad-menu.css", 1180, 720),
            new Modulo("Menú y pedidos",
                    "Carta del restaurante y toma de pedidos", "MESERO", ICONO_MENU,
                    "GastroFlow · Menú y pedidos",
                    "/views/menu-mesero.fxml", "/css/disponibilidad-menu.css", 1180, 720),
            new Modulo("Mapa de salón",
                    "Estado y ocupación de las mesas", "MESERO", ICONO_MESAS,
                    "GastroFlow · Mapa de salón",
                    "/views/MapaSalon.fxml", null, 950, 660),
            new Modulo("Pago de cuentas",
                    "Cobro, propinas y descuentos", "CAJERO", ICONO_PAGO,
                    "GastroFlow · Pago de cuentas",
                    "/views/cajero-pago-view.fxml", "/css/cajero-pago.css", 900, 760),
            new Modulo("Tablero de comandas",
                    "Personal de cocina — Tablero de comandas", "COCINA", ICONO_COCINA,
                    "Gastroflow - Cocina",
                    "/views/cocina-view.fxml", "/css/cocina.css", 1200, 760)
    );

    private static String nombreVisible(String rol) {
        return switch (rol) {
            case "ADMINISTRADOR" -> "Administrador del restaurante";
            case "MESERO" -> "Mesero";
            case "COCINA" -> "Personal de cocina";
            case "CAJERO" -> "Cajero";
            default -> rol;
        };
    }

    private Label avatar;
    private Label usuarioNombre;
    private Label usuarioEstado;
    private Button cerrarSesion;
    private Label subtituloPanel;
    private VBox listaModulos;
    private String moduloAbierto;

    private Stage ventana;

    @Override
    public void start(Stage stage) {
        this.ventana = stage;

        stage.setTitle("GastroFlow");
        stage.setMinWidth(820);
        stage.setMinHeight(520);
        mostrarLogin();
        stage.show();
    }

    /** Pantalla de acceso. Es lo primero que se ve al abrir el sistema. */
    private void mostrarLogin() {
        try {
            FXMLLoader cargador = new FXMLLoader(Main.class.getResource("/views/login.fxml"));
            Parent raiz = cargador.load();

            LoginController control = cargador.getController();
            control.setAlEntrar(this::mostrarNavegacion);

            Scene escena = new Scene(raiz, 860, 580);
            escena.getStylesheets().add(Main.class.getResource("/css/login.css").toExternalForm());

            ventana.setScene(escena);

        } catch (Exception e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("No se pudo abrir el inicio de sesión");
            alerta.setContentText(describirCausa(e));
            alerta.showAndWait();
        }
    }

    /** Navegación principal, ya con la sesión abierta. */
    private void mostrarNavegacion() {
        moduloAbierto = null;

        HBox raiz = new HBox(construirLateral(), construirPanel());

        Scene escena = new Scene(raiz, 880, 560);
        escena.getStylesheets().add(Main.class.getResource("/css/navegacion.css").toExternalForm());

        refrescarNavegacion();
        ventana.setScene(escena);
    }

    // ------------------------------------------------------------------
    // Barra lateral
    // ------------------------------------------------------------------

    private VBox construirLateral() {
        Label marca = new Label("GastroFlow");
        marca.getStyleClass().add("marca");

        Label marcaSub = new Label("Sistema de gestión del restaurante");
        marcaSub.getStyleClass().add("marca-sub");
        marcaSub.setWrapText(true);

        VBox bloqueMarca = new VBox(2, marca, marcaSub);

        Label rotulo = new Label("SESIÓN");
        rotulo.getStyleClass().add("rotulo-lateral");

        avatar = new Label();
        avatar.getStyleClass().add("avatar");

        usuarioNombre = new Label();
        usuarioNombre.getStyleClass().add("usuario-nombre");

        usuarioEstado = new Label();
        usuarioEstado.getStyleClass().add("usuario-estado");
        usuarioEstado.setWrapText(true);

        VBox textosUsuario = new VBox(1, usuarioNombre, usuarioEstado);
        HBox fichaUsuario = new HBox(11, avatar, textosUsuario);
        fichaUsuario.setAlignment(Pos.CENTER_LEFT);

        Region separador = new Region();
        separador.getStyleClass().add("separador-lateral");
        separador.setMaxWidth(Double.MAX_VALUE);

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        cerrarSesion = new Button("Cerrar sesión");
        cerrarSesion.getStyleClass().add("boton-salir");
        cerrarSesion.setMaxWidth(Double.MAX_VALUE);
        cerrarSesion.setOnAction(e -> {
            SesionUsuario.cerrarSesion();
            moduloAbierto = null;
            mostrarLogin();
        });

        VBox lateral = new VBox(18, bloqueMarca, separador, rotulo,
                fichaUsuario, espacio, cerrarSesion);
        lateral.getStyleClass().add("lateral");
        return lateral;
    }

    // ------------------------------------------------------------------
    // Panel de módulos
    // ------------------------------------------------------------------

    private VBox construirPanel() {
        Label titulo = new Label("Módulos");
        titulo.getStyleClass().add("titulo-panel");

        subtituloPanel = new Label();
        subtituloPanel.getStyleClass().add("subtitulo-panel");
        subtituloPanel.setWrapText(true);

        listaModulos = new VBox(12);
        VBox.setVgrow(listaModulos, Priority.ALWAYS);

        VBox panel = new VBox(6, titulo, subtituloPanel, new Region(), listaModulos);
        panel.getStyleClass().add("panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private static SVGPath icono(String trazo, double lado, String clase) {
        SVGPath figura = new SVGPath();
        figura.setContent(trazo);
        figura.getStyleClass().add(clase);
        figura.setScaleX(lado / 24.0);
        figura.setScaleY(lado / 24.0);
        return figura;
    }

    private Button construirTarjeta(Modulo modulo) {
        StackPane fondoIcono = new StackPane(icono(modulo.icono(), 22, "icono"));
        fondoIcono.getStyleClass().add("icono-fondo");

        Label nombre = new Label(modulo.nombre());
        nombre.getStyleClass().add("tarjeta-titulo");

        Label detalle = new Label(modulo.detalle());
        detalle.getStyleClass().add("tarjeta-detalle");

        VBox textos = new VBox(2, nombre, detalle);
        textos.setAlignment(Pos.CENTER_LEFT);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox contenido = new HBox(14, fondoIcono, textos, espacio);
        contenido.setAlignment(Pos.CENTER_LEFT);

        boolean abierto = modulo.nombre().equals(moduloAbierto);
        if (abierto) {
            Label distintivo = new Label("ABIERTO");
            distintivo.getStyleClass().add("distintivo");
            contenido.getChildren().add(distintivo);
        }

        Button tarjeta = new Button();
        tarjeta.setGraphic(contenido);
        tarjeta.getStyleClass().add("tarjeta");
        if (abierto) {
            tarjeta.getStyleClass().add("abierta");
        }
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setOnAction(e -> {
            moduloAbierto = modulo.nombre();
            refrescarNavegacion();
            abrirModulo(modulo);
        });

        contenido.prefWidthProperty().bind(tarjeta.widthProperty().subtract(40));
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Estado de la navegación
    // ------------------------------------------------------------------

    /**
     * Redibuja la navegación según la sesión actual: qué módulos se ven, cuál
     * está marcado como abierto y si se puede cerrar sesión.
     */
    private void refrescarNavegacion() {
        boolean haySesion = SesionUsuario.estaAutenticado();

        cerrarSesion.setDisable(!haySesion);
        listaModulos.getChildren().clear();

        if (!haySesion) {
            // No deberia ocurrir: a esta pantalla solo se llega tras iniciar
            // sesion. Se contempla por si la sesion se cierra desde otro sitio.
            mostrarLogin();
            return;
        }

        String rol = SesionUsuario.getRol();
        String nombreRol = nombreVisible(rol);
        String nombre = SesionUsuario.getNombre();

        avatar.setText(nombre.isBlank() || "-".equals(nombre)
                ? nombreRol.substring(0, 1).toUpperCase()
                : nombre.substring(0, 1).toUpperCase());
        usuarioNombre.setText(nombre);
        usuarioEstado.setText(nombreRol);

        List<Modulo> permitidos = MODULOS.stream()
                .filter(m -> m.rol().equals(rol))
                .toList();

        if (permitidos.isEmpty()) {
            subtituloPanel.setText("No hay módulos asignados a este rol.");
            listaModulos.getChildren().add(estadoVacio(
                    "Sin módulos disponibles",
                    "El rol " + nombreRol + " todavía no tiene módulos asignados en el sistema."));
            return;
        }

        subtituloPanel.setText(permitidos.size() == 1
                ? "1 módulo disponible para " + nombreRol
                : permitidos.size() + " módulos disponibles para " + nombreRol);

        for (Modulo modulo : permitidos) {
            listaModulos.getChildren().add(construirTarjeta(modulo));
        }
    }

    private VBox estadoVacio(String titulo, String texto) {
        SVGPath figura = icono(ICONO_VACIO, 38, "icono-vacio");
        StackPane caja = new StackPane(figura);
        caja.setMinHeight(44);

        Label rotulo = new Label(titulo);
        rotulo.getStyleClass().add("vacio-titulo");

        Label detalle = new Label(texto);
        detalle.getStyleClass().add("vacio-texto");
        detalle.setWrapText(true);
        detalle.setMaxWidth(340);

        VBox bloque = new VBox(9, caja, rotulo, detalle);
        bloque.getStyleClass().add("vacio");
        bloque.setAlignment(Pos.CENTER);
        bloque.setMaxWidth(Double.MAX_VALUE);
        return bloque;
    }

    // ------------------------------------------------------------------
    // Apertura de módulos
    // ------------------------------------------------------------------

    private void abrirModulo(Modulo modulo) {
        try {
            Parent raiz = FXMLLoader.load(Main.class.getResource(modulo.fxml()));

            Scene escena = new Scene(raiz, modulo.ancho(), modulo.alto());
            if (modulo.css() != null) {
                escena.getStylesheets().add(Main.class.getResource(modulo.css()).toExternalForm());
            }

            Stage ventana = new Stage();
            ventana.setTitle(modulo.tituloVentana());
            ventana.setScene(escena);
            ventana.setOnHidden(e -> {
                if (modulo.nombre().equals(moduloAbierto)) {
                    moduloAbierto = null;
                    refrescarNavegacion();
                }
            });
            ventana.show();

        } catch (Exception e) {
            e.printStackTrace();

            moduloAbierto = null;
            refrescarNavegacion();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("No se pudo abrir el módulo");
            alerta.setHeaderText(modulo.nombre());
            alerta.setContentText(describirCausa(e));
            alerta.showAndWait();
        }
    }

    /**
     * Una FXMLLoadException solo dice archivo y línea; el motivo real está en la
     * cadena de causas. Esto arma un mensaje con las dos cosas.
     */
    private String describirCausa(Throwable error) {
        StringBuilder mensaje = new StringBuilder();

        Throwable actual = error;
        while (actual != null) {
            mensaje.append(actual.getClass().getSimpleName());

            if (actual.getMessage() != null && !actual.getMessage().isBlank()) {
                mensaje.append(": ").append(actual.getMessage());
            }
            mensaje.append('\n');

            actual = actual.getCause() == actual ? null : actual.getCause();
        }

        return mensaje.toString().trim();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
