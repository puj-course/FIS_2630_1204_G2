package repository;

import ConexionBD.ConexionBD;//importa la conexión a la base de datos
import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,
import entity.Plato;

public class PlatoRepository {

    /**
     * Guarda un plato nuevo en la carta.
     *
     * La consulta escribia en una columna llamada "categoria", que no existe:
     * la tabla productos guarda categoria_id, una llave foranea a categorias.
     * Con eso, cualquier llamada terminaba en "column categoria does not
     * exist". Ahora el nombre de la categoria se traduce a su identificador, y
     * si no coincide con ninguna, el plato queda en la primera categoria en vez
     * de perderse.
     */
    public void guardar(Plato plato) throws SQLException {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, categoria_id, precio_venta, costo) "
                + "VALUES (?, ?, ?, "
                + "COALESCE((SELECT categoria_id FROM categorias WHERE nombre = ? LIMIT 1), "
                + "(SELECT min(categoria_id) FROM categorias)), ?, ?)";

        try (Connection conn = ConexionBD.getConnection(); //conecta con la base de datos
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            //coloca el contenido del plato en el ?
            //el numero significa la posición en que va
            stmt.setString(1, plato.getCodigo());
            stmt.setString(2, plato.getNombre());
            stmt.setString(3, plato.getDescripcion());
            stmt.setString(4, plato.getCategoria());
            stmt.setDouble(5, plato.getPrecioVenta());
            stmt.setDouble(6, plato.getCosto());

            stmt.executeUpdate(); //ejecuta la consulta
        }
    }
    
}