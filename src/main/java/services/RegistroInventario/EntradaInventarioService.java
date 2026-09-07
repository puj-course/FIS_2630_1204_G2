package services.RegistroInventario;
import conf.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import models.RegistroInventario.EntradaInventario;
import repositories.RegistroInventario.EntradaInventarioRepository;

public class EntradaInventarioService {
    // Crea un objeto del Repository.
    //comunicación entre base de datos para guardar la entrada
    private EntradaInventarioRepository repo = new EntradaInventarioRepository();

    //metodo para registrar
    public void registrar(EntradaInventario entrada) throws Exception {
        if (entrada.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        repo.guardar(entrada); // esto ya incluye el guardado
        actualizarStock(entrada.getIngredienteId(), entrada.getCantidad());
    }

    private void actualizarStock(Long ingredienteId, double cantidad) throws Exception {
        String sql = "UPDATE productos SET stock_actual = stock_actual + ? WHERE producto_id = ?";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, cantidad);
            stmt.setLong(2, ingredienteId);
            stmt.executeUpdate();
        }
    }
}
