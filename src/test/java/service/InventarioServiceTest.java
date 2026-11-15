package service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceTest {
    @Test
    void notificaAlObservadorEnElUmbralYPermiteEliminarlo() throws Exception {
        try (var plataforma = mockStatic(javafx.application.Platform.class)) {
            var observador = mock(Interface.InsumoObserver.class);
            var servicio = new InventarioService();
            servicio.agregarObservador(observador);
            servicio.verificarStockMinimo("Arroz", 6, 5);
            servicio.verificarStockMinimo("Arroz", 5, 5);
            servicio.eliminarObservador(observador);
            servicio.verificarStockMinimo("Arroz", 4, 5);
            verify(observador).actualizar("Arroz", 5, 5);
            verifyNoMoreInteractions(observador);
        }
    }
}

