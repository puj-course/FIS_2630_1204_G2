package repository;

import entity.ListaEspera;
import ConexionBD.ConexionBD;
import enums.EstadoListaEspera;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListaEsperaRepository {

    // Ejemplo básico de consulta adaptado a JDBC (puedes adaptarlo a tu clase de conexión ConexionBD)
    public List<ListaEspera> findByEstadoOrderByHoraIngresoAsc(EstadoListaEspera estado) throws SQLException {
        String sql = "SELECT * FROM lista_espera WHERE estado = ? ORDER BY hora_ingreso ASC";
        List<ListaEspera> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, estado);
             try (ResultSet rs = pstmt.executeQuery()) {
                 while (rs.next()) {
                     ListaEspera item = new ListaEspera();
                     item.setId(rs.getLong("id"));
                     item.setNombreCliente(rs.getString("nombre_cliente"));
                     item.setCantidadPersonas(rs.getInt("cantidad_personas"));
                     item.setHoraIngreso(rs.getTimestamp("hora_ingreso").toLocalDateTime());
                     item.setEstado(rs.getString("estado"));
                     lista.add(item);
                 }
             }
         }
        return lista;
    }

    public ListaEspera save(ListaEspera item) throws SQLException {
        // Lógica de inserción en base de datos...
        return item;
    }

    public Optional<ListaEspera> findById(long id) throws SQLException {
        // Lógica para buscar por ID...
        return Optional.empty();
    }

    public void update(ListaEspera item) throws SQLException {
        // Lógica de actualización (cambiar estado a ASIGNADO o CANCELADO)...
    }
}
