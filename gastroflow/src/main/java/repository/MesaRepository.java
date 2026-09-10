package repository;

import conf.ConexionDB;
import entity.Mesa;
import java.sql.*;
import java.util.*;

public class MesaRepository {

    public List<Mesa> obtenerTodas() throws SQLException {
        List<Mesa> mesas = new ArrayList<>();
        String sql = "SELECT m.id_mesa, m.numero_mesa, m.codigo_mesa, m.capacidad, " +
                "       m.id_zona, z.nombre_zona, " +
                "       m.id_estado_mesa, e.codigo_estado " +
                "FROM mesas m " +
                "JOIN zonas z ON m.id_zona = z.id_zona " +
                "JOIN estados_mesa e ON m.id_estado_mesa = e.id_estado_mesa " +
                "WHERE m.is_active = 1";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Mesa mesa = new Mesa();
                mesa.setIdMesa(rs.getInt("id_mesa"));
                mesa.setNumeroMesa(rs.getInt("numero_mesa"));
                mesa.setCodigoMesa(rs.getString("codigo_mesa"));
                mesa.setCapacidad(rs.getInt("capacidad"));
                mesa.setIdZona(rs.getInt("id_zona"));
                mesa.setNombreZona(rs.getString("nombre_zona"));
                mesa.setIdEstadoMesa(rs.getInt("id_estado_mesa"));
                mesa.setCodigoEstado(rs.getString("codigo_estado"));
                mesas.add(mesa);
            }
        }
        return mesas;
    }
    public void agregarMesa(int numeroMesa) throws SQLException {
        String sql = "INSERT INTO mesas (numero_mesa, capacidad, id_zona, id_estado_mesa) " +
                "VALUES (?, 2, 1, 1)";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, numeroMesa);
            stmt.executeUpdate();
        }
    }
    public void quitarMesa(int idMesa) throws SQLException {
        String sql = "UPDATE mesas SET is_active = 0 WHERE id_mesa = ?";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);
            stmt.executeUpdate();
        }
    }
}