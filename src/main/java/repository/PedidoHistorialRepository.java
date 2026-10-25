package repository;

import entity.PedidoHistorial;
import ConexionBD.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PedidoHistorialRepository {

    public void registrarEvento(PedidoHistorial evento) throws SQLException {
        String sql = "INSERT INTO pedido_historial_eventos (pedido_id, tipo_evento, descripcion, usuario_id, fecha_evento) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, evento.getPedidoId());
            stmt.setString(2, evento.getTipoEvento());
            stmt.setString(3, evento.getDescripcion());
            stmt.setObject(4, evento.getUsuarioId());
            stmt.setTimestamp(5, Timestamp.valueOf(evento.getFechaEvento()));

            stmt.executeUpdate();
        }
    }
}
