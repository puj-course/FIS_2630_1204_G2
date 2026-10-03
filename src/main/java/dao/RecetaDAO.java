package dao;

import modelo.Receta;
import modelo.RecetaDetalle;
import util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO encargado de persistir la receta de un producto en la base de datos.
 *
 * Se asume la siguiente tabla intermedia (ajusta nombres si difieren):
 *
 * CREATE TABLE receta_ingrediente (
 *     id_producto     INT NOT NULL,
 *     id_ingrediente  INT NOT NULL,
 *     cantidad        DECIMAL(10,2) NOT NULL,
 *     unidad_medida   VARCHAR(20),
 *     PRIMARY KEY (id_producto, id_ingrediente),
 *     FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
 *     FOREIGN KEY (id_ingrediente) REFERENCES ingrediente(id_ingrediente)
 * );
 */
public class RecetaDAO implements IRecetaDAO {

    private static final String SQL_INSERT_DETALLE =
            "INSERT INTO receta_ingrediente (id_producto, id_ingrediente, cantidad, unidad_medida) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_ELIMINAR_RECETA_PREVIA =
            "DELETE FROM receta_ingrediente WHERE id_producto = ?";

    /**
     * Crea (o reemplaza) la receta completa de un producto de forma transaccional:
     * si algo falla, no se guarda ningún ingrediente (todo o nada).
     *
     * @param receta receta con el producto y su lista de ingredientes/cantidades
     * @throws SQLException si ocurre un error de base de datos
     * @throws IllegalArgumentException si la receta no tiene ingredientes
     */
    public void crearReceta(Receta receta) throws SQLException {
        if (receta.getDetalles() == null || receta.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La receta debe tener al menos un ingrediente.");
        }

        Connection conexion = null;

        try {
            conexion = ConexionBD.obtenerConexion();
            conexion.setAutoCommit(false); // iniciar transacción

            int idProducto = receta.getProducto().getIdProducto();

            // 1. Si ya existía una receta para este producto, se elimina primero
            //    (evita duplicados si el usuario está "recreando" la receta).
            try (PreparedStatement stmtDelete = conexion.prepareStatement(SQL_ELIMINAR_RECETA_PREVIA)) {
                stmtDelete.setInt(1, idProducto);
                stmtDelete.executeUpdate();
            }

            // 2. Insertar cada ingrediente de la receta
            try (PreparedStatement stmtInsert = conexion.prepareStatement(SQL_INSERT_DETALLE)) {
                for (RecetaDetalle detalle : receta.getDetalles()) {
                    stmtInsert.setInt(1, idProducto);
                    stmtInsert.setInt(2, detalle.getIngrediente().getIdIngrediente());
                    stmtInsert.setDouble(3, detalle.getCantidad());
                    stmtInsert.setString(4, detalle.getUnidadMedida());
                    stmtInsert.addBatch();
                }
                stmtInsert.executeBatch();
            }

            conexion.commit(); // todo salió bien, confirmar cambios

        } catch (SQLException e) {
            if (conexion != null) {
                try {
                    conexion.rollback(); // revertir todo ante cualquier error
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e; // se relanza para que la capa de servicio/UI la maneje

        } finally {
            if (conexion != null) {
                conexion.setAutoCommit(true);
                conexion.close();
            }
        }
    }
}
