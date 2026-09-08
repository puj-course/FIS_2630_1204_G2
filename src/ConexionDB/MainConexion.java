
import java.sql.Connection;
import java.sql.SQLException;

public class MainConexion {

    public static void main(String[] args) {
        System.out.println("Probando conexión a PostgreSQL...");

        try (Connection conn = ConexionBD.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("==================================================");
                System.out.println("¡CONEXIÓN EXITOSA!");
                System.out.println("Conectado a la base de datos: " + conn.getCatalog());
                System.out.println("==================================================");
            }
        } catch (SQLException e) {
            System.err.println("==================================================");
            System.err.println("ERROR AL CONECTAR CON LA BASE DE DATOS:");
            System.err.println(e.getMessage());
            System.err.println("==================================================");
        }
    }
}