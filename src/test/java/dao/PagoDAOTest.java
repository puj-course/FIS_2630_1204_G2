package dao;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagoDAOTest {
    @Test
    void devuelveCuentaVaciaSiNoExistePedido() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            assertTrue(new PagoDAO().buscarCuenta("P-4").isEmpty());
            verify(jdbc.sentencia).setString(1, "P-4");
            verify(jdbc.filas).close();
        }
    }
}

