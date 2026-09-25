package repository;

import ConexionDB.ConexionBD;
import entity.Reserva;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservaRepository {

    private static final String SELECT_BASE =
            "SELECT r.id_reserva, r.id_mesa, m.numero_mesa, z.nombre_zona, " +
                    "       r.fecha_hora_reserva, r.nombre_cliente, r.cantidad_personas, " +
                    "       r.is_active, r.fecha_cancelacion " +
                    "FROM reservas r " +
                    "JOIN mesas m ON r.id_mesa = m.id_mesa " +
                    "JOIN zonas z ON m.id_zona = z.id_zona " +
                    "JOIN estados_mesa e ON m.id_estado_mesa = e.id_estado_mesa ";

    // Trae todas las mesas cuyo estado actual es RESERVADA, con los datos de su reserva vigente
    public List<Reserva> obtenerMesasReservadas() throws SQLException {
        List<Reserva> reservas = new ArrayList<>();

        String sql = SELECT_BASE +
                "WHERE e.codigo_estado = 'RESERVADA' AND r.is_active = 1 " +
                "ORDER BY r.fecha_hora_reserva";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reservas.add(mapearReserva(rs));
            }
        }
        return reservas;
    }

    // Igual que arriba, pero solo las reservas de hoy (jornada/turno actual)
    public List<Reserva> obtenerReservasDelTurnoActual() throws SQLException {
        List<Reserva> reservas = new ArrayList<>();

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        String sql = SELECT_BASE +
                "WHERE e.codigo_estado = 'RESERVADA' AND r.is_active = 1 " +
                "AND r.fecha_hora_reserva >= ? AND r.fecha_hora_reserva < ? " +
                "ORDER BY r.fecha_hora_reserva";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicioDia));
            stmt.setTimestamp(2, Timestamp.valueOf(finDia));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservas.add(mapearReserva(rs));
                }
            }
        }
        return reservas;
    }

    // Necesario para HU-055: cargar la reserva antes de validar si se puede cancelar
    public Optional<Reserva> findById(int idReserva) throws SQLException {
        String sql = SELECT_BASE + "WHERE r.id_reserva = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReserva);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearReserva(rs));
                }
                return Optional.empty();
            }
        }
    }

    // Borrado/archivado lógico genérico: no se elimina el registro, solo se marca inactivo
    public void archivarReserva(int idReserva) throws SQLException {
        String sql = "UPDATE reservas SET is_active = 0 WHERE id_reserva = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReserva);
            stmt.executeUpdate();
        }
    }

    // HU-055: cancelar registra además la fecha de cancelación (auditoría)
    public void cancelar(int idReserva, LocalDateTime fechaCancelacion) throws SQLException {
        String sql = "UPDATE reservas SET is_active = 0, fecha_cancelacion = ? WHERE id_reserva = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(fechaCancelacion));
            stmt.setInt(2, idReserva);
            stmt.executeUpdate();
        }
    }

    private Reserva mapearReserva(ResultSet rs) throws SQLException {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(rs.getInt("id_reserva"));
        reserva.setIdMesa(rs.getInt("id_mesa"));
        reserva.setNumeroMesa(rs.getInt("numero_mesa"));
        reserva.setNombreZona(rs.getString("nombre_zona"));
        reserva.setFechaHoraReserva(rs.getTimestamp("fecha_hora_reserva").toLocalDateTime());
        reserva.setNombreCliente(rs.getString("nombre_cliente"));
        reserva.setCantidadPersonas(rs.getInt("cantidad_personas"));
        reserva.setActiva(rs.getInt("is_active") == 1);

        Timestamp fechaCancelacion = rs.getTimestamp("fecha_cancelacion");
        if (fechaCancelacion != null) {
            reserva.setFechaCancelacion(fechaCancelacion.toLocalDateTime());
        }

        return reserva;
    }
}