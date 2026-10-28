package service;

import Interface.InsumoObserver;
import javafx.scene.control.Alert; // Mostrarlo en JavaFX

public class AdministradorAlertaObserver implements InsumoObserver {

    @Override
    public void actualizar(String nombreInsumo, double stockActual, double stockMinimo) {
        // Aquí manejas el aviso al administrador
        System.out.println("[ALERTA DE STOCK] El insumo '" + nombreInsumo +
                "' está por debajo del mínimo. Stock actual: " +
                stockActual + " (Mínimo: " + stockMinimo + ")");

        // Alerta gráfica en JavaFX
        javafx.application.Platform.runLater(() -> {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Alerta de Stock Mínimo");
            alerta.setHeaderText("Insumo crítico: " + nombreInsumo);
            alerta.setContentText("El stock actual es " + stockActual + ", el cual es menor o igual al mínimo permitido (" + stockMinimo + ").");
            alerta.show();
        });
    }
}