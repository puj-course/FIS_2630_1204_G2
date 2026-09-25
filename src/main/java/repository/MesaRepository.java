package repository;

import ConexionDB.ConexionBD;
import entity.Mesa;

import java.sql.*;
import java.util.*;

public class MesaRepository {

    private static final String SELECT_BASE =
            "SELECT m.id_mesa, m.numero_mesa, m.codigo_mesa, m.capacidad, " +
                    "       m.id_zona, z.nombre_zona, " +
                    "       m.id_estado_mesa, e.codigo_estado " +
                    "FROM mesas m " +
                    "JOIN zonas z ON m.id_zona = z.id_zona " +
                    "JOIN estados_mesa e ON m.id_estado_mesa = e.id_estado_mesa ";

    public List<Mesa> obtenerTodas() throws SQLException {
        List<Mesa> mesas = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE m.is_active = 1";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                mesas.add(mapearMesa(rs));
            }
        }
        return mesas;
    }

    // Necesario para conocer el estado_anterior antes de cambiarlo (auditoría de HU-59)
    public Optional<Mesa> findById(int idMesa) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_mesa = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMesa(rs));
                }
                return Optional.empty();
            }
        }
    }

    // Igual, pero reutilizando una Connection ya abierta (para participar en una transacción)
    public Optional<Mesa> findById(Connection conn, int idMesa) throws SQLException {
        String sql = SELECT_BASE + "WHERE m.id_mesa = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMesa);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMesa(rs));
                }
                return Optional.empty();
            }
        }
    }

    public void agregarMesa(int numeroMesa) throws SQLException {
        String sql = "INSERT INTO mesas (numero_mesa, capacidad, id_zona, id_estado_mesa) " +
                "VALUES (?, 2, 1, 1)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, numeroMesa);
            stmt.executeUpdate();
        }
    }

    public void quitarMesa(int idMesa) throws SQLException {
        String sql = "UPDATE mesas SET is_active = 0 WHERE id_mesa = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);
            stmt.executeUpdate();
        }
    }

    // Versión normal: abre su propia conexión (uso suelto, sin transacción)
    public void cambiarEstadoMesa(int idMesa, String codigoEstado) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            cambiarEstadoMesa(conn, idMesa, codigoEstado);
        }
    }

    // Versión transaccional: reutiliza la Connection que le pasen (para MesaService.cambiarEstado)
    public void cambiarEstadoMesa(Connection conn, int idMesa, String codigoEstado) throws SQLException {
        String sql = "UPDATE mesas " +
                "SET id_estado_mesa = " +
                "(SELECT id_estado_mesa FROM estados_mesa WHERE codigo_estado = ?) " +
                "WHERE id_mesa = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigoEstado);
            stmt.setInt(2, idMesa);
            stmt.executeUpdate();
        }
    }

    public void cambiarCapacidadMesa(int idMesa, int capacidad) throws SQLException {
        String sql = "UPDATE mesas SET capacidad = ? WHERE id_mesa = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, capacidad);
            stmt.setInt(2, idMesa);
            stmt.executeUpdate();
        }
    }

    private Mesa mapearMesa(ResultSet rs) throws SQLException {
        Mesa mesa = new Mesa();
        mesa.setIdMesa(rs.getInt("id_mesa"));
        mesa.setNumeroMesa(rs.getInt("numero_mesa"));
        mesa.setCodigoMesa(rs.getString("codigo_mesa"));
        mesa.setCapacidad(rs.getInt("capacidad"));
        mesa.setIdZona(rs.getInt("id_zona"));
        mesa.setNombreZona(rs.getString("nombre_zona"));
        mesa.setIdEstadoMesa(rs.getInt("id_estado_mesa"));
        mesa.setCodigoEstado(rs.getString("codigo_estado"));
        return mesa;
    }
}