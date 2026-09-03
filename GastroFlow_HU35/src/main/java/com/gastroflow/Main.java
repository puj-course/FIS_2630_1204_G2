package com.gastroflow;

import com.gastroflow.session.SesionUsuario;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        cargarSesionDePruebaSiFueIndicada();

        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/com/gastroflow/cajero-pago-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 900, 760);
        scene.getStylesheets().add(
                Main.class.getResource("/com/gastroflow/cajero-pago.css").toExternalForm()
        );

        stage.setTitle("Gastroflow - Caja");
        stage.setScene(scene);
        stage.show();
    }

    private void cargarSesionDePruebaSiFueIndicada() {
        String id = System.getProperty("gastroflow.cajeroId");

        if (id == null || id.isBlank()) {
            return;
        }

        String nombre = System.getProperty("gastroflow.cajeroNombre", "Cajero");
        SesionUsuario.iniciarSesion(Integer.parseInt(id), nombre, "CAJERO");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
