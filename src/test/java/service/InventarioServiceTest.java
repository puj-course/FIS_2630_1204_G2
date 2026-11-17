package service;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import javafx.application.Platform;
import org.junit.jupiter.api.DisplayName;

class InventarioServiceTest {

    @Test
    @DisplayName("notifica al observador cuando el stock alcanza el umbral mínimo y permite eliminarlo correctamente")
    void notificaAlObservadorEnElUmbralYPermiteEliminarlo() throws Exception {
        // Mockeamos el comportamiento estático de Platform para evitar problemas con hilos de JavaFX en los tests
        try (var plataforma = mockStatic(Platform.class)) {
            var observador = mock(Interface.InsumoObserver.class);
            var servicio = new InventarioService();

            servicio.agregarObservador(observador);

            // Verificaciones de stock (el segundo activa la notificación al llegar exactamente al límite mínimo de 5)
            servicio.verificarStockMinimo("Arroz", 6, 5);
            servicio.verificarStockMinimo("Arroz", 5, 5);

            // Eliminamos el observador para comprobar que las siguientes verificaciones ya no lo notifiquen
            servicio.eliminarObservador(observador);
            servicio.verificarStockMinimo("Arroz", 4, 5);

            // Validamos que el observador fue notificado exactamente una vez (en el umbral de 5) y no más
            verify(observador, times(1)).actualizar("Arroz", 5, 5);
            verifyNoMoreInteractions(observador);
        }
    }
}