package conf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionDB {

    private ConexionDB() {
    }

    public static Connection obtenerConexion() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/gastroflow_test");
        String usuario = System.getenv().getOrDefault("DB_USER", "postgres");
        String clave = System.getenv().getOrDefault("DB_PASSWORD", "123");

        return DriverManager.getConnection(url, usuario, clave);
    }
}
