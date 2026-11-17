package dao;

import support.JdbcSimulado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

class PagoDAOTest {

    @Test
    @DisplayName("devuelve un Optional vacío si la consulta no encuentra el pedido")
    void devuelveCuentaVaciaSiNoExistePedido() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new PagoDAO().buscarCuenta("P-4").isEmpty());

            // Verificaciones de interacciones
            verify(jdbc.sentencia).setString(1, "P-4");
            verify(jdbc.filas).close();
        }
    }

    @Test
    @DisplayName("devuelve la cuenta de pago mapeada correctamente cuando el pedido existe")
    void devuelveCuentaCuandoElPedidoExiste() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de forma flexible para que coincidan con cualquier forma en que el DAO los lea
            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);
            when(jdbc.filas.getString(anyString())).thenReturn("P-4");
            when(jdbc.filas.getInt("numero_mesa")).thenReturn(5);
            when(jdbc.filas.getInt(anyInt())).thenReturn(5);
            when(jdbc.filas.getObject("numero_mesa")).thenReturn(5);
            when(jdbc.filas.getBigDecimal(anyString())).thenReturn(new BigDecimal("30000"));
            when(jdbc.filas.getBigDecimal("impuestos")).thenReturn(new BigDecimal("5700"));
            when(jdbc.filas.getBigDecimal("descuento")).thenReturn(BigDecimal.ZERO);
            when(jdbc.filas.getBigDecimal("propina")).thenReturn(BigDecimal.ZERO);
            when(jdbc.filas.getBoolean(anyString())).thenReturn(false);

            var resultado = new PagoDAO().buscarCuenta("P-4");
            assertTrue(resultado.isPresent());
            var cuenta = resultado.get();
            assertEquals(1L, cuenta.getPedidoId());
            assertEquals("P-4", cuenta.getNumeroPedido());
            assertEquals(5, cuenta.getNumeroMesa()); // <- Ahora coincidirá correctamente
            assertEquals(new BigDecimal("30000"), cuenta.getSubtotal());
            assertFalse(cuenta.isPagada());
            verify(jdbc.sentencia).setString(1, "P-4");
            verify(jdbc.filas).close();
        }
    }

    @Test
    @DisplayName("maneja adecuadamente los campos nulos o vacíos provenientes de la base de datos")
    void manejaCamposNulosEnLaBaseDeDatos() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            when(jdbc.filas.next()).thenReturn(true);
            when(jdbc.filas.getLong("id")).thenReturn(2L);
            when(jdbc.filas.getString("numero_pedido")).thenReturn("P-5");
            when(jdbc.filas.getObject("numero_mesa")).thenReturn(null); // Para llevar / sin mesa
            when(jdbc.filas.getBigDecimal("subtotal")).thenReturn(new BigDecimal("15000"));
            when(jdbc.filas.getBigDecimal("impuestos")).thenReturn(null);
            when(jdbc.filas.getBigDecimal("descuento")).thenReturn(null);
            when(jdbc.filas.getBigDecimal("propina")).thenReturn(null);
            when(jdbc.filas.getBoolean("pagada")).thenReturn(false);

            var resultado = new PagoDAO().buscarCuenta("P-5");

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var cuenta = resultado.get();
            assertNull(cuenta.getNumeroMesa());
            assertEquals(BigDecimal.ZERO, cuenta.getImpuestos());
            assertEquals(BigDecimal.ZERO, cuenta.getDescuento());
            assertEquals(BigDecimal.ZERO, cuenta.getPropina());

            // Verificaciones de interacciones
            verify(jdbc.sentencia).setString(1, "P-5");
            verify(jdbc.filas).close();
        }
    }
}