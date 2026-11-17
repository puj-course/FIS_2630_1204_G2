package service;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class DisponibilidadServiceTest {

    @Test
    @DisplayName("marca un plato como disponible cuando no tiene insumos requeridos o cumple con el stock")
    void marcaDisponibleUnPlatoSinInsumosRequeridos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Ejecutamos el servicio de disponibilidad para el plato con ID 7
            new DisponibilidadService().actualizarDisponibilidad(7);

            // Verificamos las interacciones clave de negocio y actualización de estado
            verify(jdbc.sentencia).setString(1, "DISPONIBLE");
            verify(jdbc.sentencia).setInt(2, 7);
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}