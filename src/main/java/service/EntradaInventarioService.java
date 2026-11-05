package service;
import ConexionBD.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import entity.EntradaInventario;
import repository.EntradaInventarioRepository;

public class EntradaInventarioService {

    // Crea un objeto del Repository.
    private EntradaInventarioRepository repo = new EntradaInventarioRepository();

    public void registrar(EntradaInventario entrada) throws Exception {
        if (entrada.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        // Manejo transaccional para asegurar la integridad entre el registro y el stock
        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Guardar la entrada (Nota: si tu repo.guardar requiere la conexión,
                // asegúrate de pasarle 'conn' como parámetro: repo.guardar(conn, entrada);)
                repo.guardar(entrada);

                // 2. Actualizar el stock exclusivamente en la tabla 'ingredientes'
                actualizarStock(conn, entrada.getIngredienteId(), entrada.getCantidad());

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Actualiza el stock de manera exclusiva en la tabla ingredientes.
     */
    private void actualizarStock(Connection conn, Long ingredienteId, double cantidad) throws SQLException {
        String sql = "UPDATE ingredientes " +
                "SET stock_actual = stock_actual + ?, " +
                "fecha_actualizacion = CURRENT_TIMESTAMP " +
                "WHERE ingrediente_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cantidad);
            stmt.setLong(2, ingredienteId);
            stmt.executeUpdate();
        }
    }
}