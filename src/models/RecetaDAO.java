
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecetaDAO {

    private final Connection connection;

    public RecetaDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Verifica si un producto existe en la base de datos por su ID.
     */
    public boolean existeProducto(Long productoId) throws SQLException {
        String sql = "SELECT 1 FROM productos WHERE producto_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, productoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Devuelve true si encuentra al menos un registro
            }
        }
    }

    /**
     * Verifica si un ingrediente existe en la base de datos por su ID.
     */
    public boolean existeIngrediente(Long ingredienteId) throws SQLException {
        String sql = "SELECT 1 FROM ingredientes WHERE ingrediente_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, ingredienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Agrega un ingrediente a la receta o actualiza la cantidad si ya existe,
     * previa validación de existencia de IDs.
     */
    public boolean guardarOActualizarIngredienteReceta(Long productoId, Long ingredienteId, double cantidad) throws SQLException {
        // Validaciones previas
        if (!existeProducto(productoId)) {
            System.err.println("Error: El producto con ID " + productoId + " no existe en la base de datos.");
            return false;
        }

        if (!existeIngrediente(ingredienteId)) {
            System.err.println("Error: El ingrediente con ID " + ingredienteId + " no existe en la base de datos.");
            return false;
        }

        String sql = """
            INSERT INTO producto_ingrediente (producto_id, ingrediente_id, cantidad)
            VALUES (?, ?, ?)
            ON CONFLICT (producto_id, ingrediente_id)
            DO UPDATE SET cantidad = EXCLUDED.cantidad
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, productoId);
            stmt.setLong(2, ingredienteId);
            stmt.setDouble(3, cantidad);

            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;
        }
    }

    public List<RecetaItem> obtenerRecetaPorProductoId(Long productoId) throws SQLException {
        List<RecetaItem> receta = new ArrayList<>();

        String sql = """
            SELECT 
                p.producto_id,
                p.nombre AS producto_nombre,
                i.ingrediente_id,
                i.nombre AS ingrediente_nombre,
                pi.cantidad,
                i.unidad_medida,
                i.costo_unitario,
                (pi.cantidad * i.costo_unitario) AS costo_subtotal
            FROM producto_ingrediente pi
            INNER JOIN productos p ON pi.producto_id = p.producto_id
            INNER JOIN ingredientes i ON pi.ingrediente_id = i.ingrediente_id
            WHERE pi.producto_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, productoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RecetaItem item = new RecetaItem(
                            rs.getLong("producto_id"),
                            rs.getString("producto_nombre"),
                            rs.getLong("ingrediente_id"),
                            rs.getString("ingrediente_nombre"),
                            rs.getDouble("cantidad"),
                            rs.getString("unidad_medida"),
                            rs.getDouble("costo_unitario"),
                            rs.getDouble("costo_subtotal")
                    );
                    receta.add(item);
                }
            }
        }
        return receta;
    }
}