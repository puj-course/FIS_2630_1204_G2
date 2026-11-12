package service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdministradorAlertaObserverTest {
    @Test
    void programaLaAlertaEnJavaFx() throws Exception {
        try (var plataforma = mockStatic(javafx.application.Platform.class)) {
            new AdministradorAlertaObserver().actualizar("Arroz", 3, 5);
            plataforma.verify(() -> javafx.application.Platform.runLater(any(Runnable.class)));
        }
    }
}

