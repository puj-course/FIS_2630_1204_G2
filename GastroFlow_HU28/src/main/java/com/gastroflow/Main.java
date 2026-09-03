package com.gastroflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent admin = FXMLLoader.load(getClass().getResource("/com/gastroflow/admin-disponibilidad.fxml"));
        Parent mesero = FXMLLoader.load(getClass().getResource("/com/gastroflow/menu-mesero.fxml"));

        Tab tabAdmin = new Tab("Administrador - Disponibilidad", admin);
        Tab tabMesero = new Tab("Mesero - Menú", mesero);
        tabAdmin.setClosable(false);
        tabMesero.setClosable(false);

        TabPane tabs = new TabPane(tabAdmin, tabMesero);
        Scene scene = new Scene(tabs, 1180, 720);
        scene.getStylesheets().add(getClass().getResource("/com/gastroflow/hu28.css").toExternalForm());
        stage.setTitle("Gastroflow - HU-28 Disponibilidad automática");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
