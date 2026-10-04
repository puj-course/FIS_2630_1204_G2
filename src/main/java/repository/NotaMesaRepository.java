package repository;

import ConexionDB.ConexionBD;
import entity.NotaMesa;

import java.sql.*;
import java.util.Optional;

public class NotaMesaRepository {

    // Trae la nota activa de una mesa, si tiene alguna
    public Optional<NotaMesa> obtenerNotaPorMesa(int idMesa) throws SQLException {
        String sql = "SELECT id_nota, id_mesa, texto_nota, fecha_creacion " +
                "FROM nota_mesa WHERE id_mesa = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearNota(rs));
                }
                return Optional.empty();
            }
        }
    }

    // Crea la nota si la mesa no tiene una, o actualiza el texto si ya existe
    public void guardarNota(int idMesa, String textoNota) throws SQLException {
        String sqlUpdate = "UPDATE nota_mesa SET texto_nota = ? WHERE id_mesa = ?";
        String sqlInsert = "INSERT INTO nota_mesa (id_mesa, texto_nota) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConnection()) {

            try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
                stmtUpdate.setString(1, textoNota);
                stmtUpdate.setInt(2, idMesa);

                int filasActualizadas = stmtUpdate.executeUpdate();

                if (filasActualizadas == 0) {
                    try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {
                        stmtInsert.setInt(1, idMesa);
                        stmtInsert.setString(2, textoNota);
                        stmtInsert.executeUpdate();
                    }
                }
            }
        }
    }

    // Elimina la nota de una mesa (acción manual del mesero)
    public void eliminarNota(int idMesa) throws SQLException {
        String sql = "DELETE FROM nota_mesa WHERE id_mesa = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);
            stmt.executeUpdate();
        }
    }

    private NotaMesa mapearNota(ResultSet rs) throws SQLException {
        NotaMesa nota = new NotaMesa();
        nota.setIdNota(rs.getInt("id_nota"));
        nota.setIdMesa(rs.getInt("id_mesa"));
        nota.setTextoNota(rs.getString("texto_nota"));
        nota.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
        return nota;
    }
}
