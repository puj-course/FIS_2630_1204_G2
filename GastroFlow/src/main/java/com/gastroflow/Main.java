package com.gastroflow;

import com.gastroflow.session.SesionUsuario;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Punto de entrada único de GastroFlow.
 *
 * Muestra un menú desde el que se abre cada vista del sistema. Cada vista se abre
 * en su propia ventana con su propia hoja de estilos: las hojas definen reglas
 * sobre `.root`, que en JavaFX aplica al nodo raíz de la escena, así que
 * mezclarlas en una sola escena haría que una pisara a la otra.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        cargarSesionDePruebaSiFueIndicada();

        Label titulo = new Label("GastroFlow");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitulo = new Label("Seleccione el módulo que desea abrir");
        subtitulo.setStyle("-fx-text-fill: #555555;");

        Button btnAdmin = new Button("Administrador — Disponibilidad de platos");
        Button btnMesero = new Button("Mesero — Menú y pedidos");
        Button btnCajero = new Button("Cajero — Pago de cuentas");

        for (Button boton : new Button[]{btnAdmin, btnMesero, btnCajero}) {
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setPrefHeight(44);
        }

        btnAdmin.setOnAction(e -> abrirVista(
                "Gastroflow - Administrador",
                "/com/gastroflow/admin-disponibilidad.fxml",
                "/com/gastroflow/hu28.css",
                1180, 720));

        btnMesero.setOnAction(e -> abrirVista(
                "Gastroflow - Mesero",
                "/com/gastroflow/menu-mesero.fxml",
                "/com/gastroflow/hu28.css",
                1180, 720));

        btnCajero.setOnAction(e -> abrirVista(
                "Gastroflow - Caja",
                "/com/gastroflow/cajero-pago-view.fxml",
                "/com/gastroflow/cajero-pago.css",
                900, 760));

        VBox raiz = new VBox(14, titulo, subtitulo, btnAdmin, btnMesero, btnCajero);
        raiz.setAlignment(Pos.CENTER_LEFT);
        raiz.setPadding(new Insets(32));

        stage.setTitle("GastroFlow");
        stage.setScene(new Scene(raiz, 460, 320));
        stage.show();
    }

    private void abrirVista(String titulo, String fxml, String css, int ancho, int alto) {
        try {
            Parent raiz = FXMLLoader.load(Main.class.getResource(fxml));

            Scene escena = new Scene(raiz, ancho, alto);
            escena.getStylesheets().add(Main.class.getResource(css).toExternalForm());

            Stage ventana = new Stage();
            ventana.setTitle(titulo);
            ventana.setScene(escena);
            ventana.show();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("No se pudo abrir el módulo");
            alerta.setHeaderText(titulo);
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

    /**
     * Sesión de prueba para el módulo de Caja, que exige un usuario con rol CAJERO.
     * Se activa con:
     *   mvn javafx:run -Dgastroflow.cajeroId=1 -Dgastroflow.cajeroNombre="Ana"
     */
    private void cargarSesionDePruebaSiFueIndicada() {
        String id = System.getProperty("gastroflow.cajeroId");

        if (id == null || id.isBlank()) {
            return;
        }

        String nombre = System.getProperty("gastroflow.cajeroNombre", "Cajero");

        try {
            SesionUsuario.iniciarSesion(Integer.parseInt(id.trim()), nombre, "CAJERO");
        } catch (NumberFormatException e) {
            // Antes esto tumbaba el arranque entero: la aplicacion no abria
            // ninguna ventana y solo quedaba el rastro en consola.
            System.err.println("gastroflow.cajeroId debe ser un numero entero; se recibio: " + id);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
