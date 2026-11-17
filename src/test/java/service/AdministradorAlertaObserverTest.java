package service;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import javafx.application.Platform;

class AdministradorAlertaObserverTest {

    @Test
    @DisplayName("programa la ejecución de la alerta en el hilo de JavaFX utilizando Platform.runLater")
    void programaLaAlertaEnJavaFx() throws Exception {
        // Mockeamos la clase estática Platform de JavaFX para interceptar la ejecución por hilos
        try (var plataforma = mockStatic(Platform.class)) {

            // Ejecutamos la acción que dispara el observer con datos de prueba (insumo, stock actual, stock mínimo)
            new AdministradorAlertaObserver().actualizar("Arroz", 3, 5);

            // Verificamos que se haya invocado Platform.runLater para actualizar la interfaz de manera segura
            plataforma.verify(() -> Platform.runLater(any(Runnable.class)), times(1));
        }
    }
}