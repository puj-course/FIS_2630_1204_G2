package repository;

import support.JdbcSimulado;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoHistorialRepositoryTest {
    @Test
    void insertaElEventoConSuFecha() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            var evento = new entity.PedidoHistorial();
            evento.setPedidoId(4L);
            evento.setTipoEvento("CREADO");
            evento.setFechaEvento(java.time.LocalDateTime.of(2026, 1, 1, 12, 0));
            new PedidoHistorialRepository().registrarEvento(evento);
            verify(jdbc.sentencia).setLong(1, 4L);
            verify(jdbc.sentencia).setString(2, "CREADO");
            verify(jdbc.sentencia).setTimestamp(5, java.sql.Timestamp.valueOf(evento.getFechaEvento()));
            verify(jdbc.sentencia).executeUpdate();
        }
    }
}

