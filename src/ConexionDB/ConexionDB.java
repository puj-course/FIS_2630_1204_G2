import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    // 1. Configuración de parámetros de conexión a PostgreSQL
    private static final String URL = "jdbc:postgresql://localhost:5432/GastroFlow_DB";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234567890"; // <-- Cambia por tu contraseña real

    /**
     * Obten un objeto Connection activo a la base de datos gastroflow.
     * @return Connection
     * @throws SQLException Si falla la conexión
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}