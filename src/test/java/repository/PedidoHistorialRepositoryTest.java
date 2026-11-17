package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import entity.PedidoHistorial;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.sql.Timestamp;

class PedidoHistorialRepositoryTest {

    @Test
    @DisplayName("inserta correctamente el evento del pedido con sus respectivos campos y fecha")
    void insertaElEventoConSuFecha() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            PedidoHistorial evento = new PedidoHistorial();
            evento.setPedidoId(4L);
            evento.setTipoEvento("CREADO");
            evento.setFechaEvento(LocalDateTime.of(2026, 1, 1, 12, 0));

            new PedidoHistorialRepository().registrarEvento(evento);

            // Usamos anyInt() (que es un int) para la posición del parámetro y eq(4L) para el valor long
            verify(jdbc.sentencia).setLong(anyInt(), eq(4L));
            verify(jdbc.sentencia).setString(anyInt(), eq("CREADO"));
            verify(jdbc.sentencia).setTimestamp(anyInt(), any(Timestamp.class));

            // Confirmamos que la sentencia fue ejecutada y cerrada correctamente
            verify(jdbc.sentencia).executeUpdate();
            verify(jdbc.sentencia).close();
        }
    }
}