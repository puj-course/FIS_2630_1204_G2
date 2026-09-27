package conf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionDB {

    private ConexionDB() {
    }

    public static Connection obtenerConexion() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/gastroflow");
        String usuario = System.getenv().getOrDefault("DB_USER", "postgres");
        String clave = System.getenv().getOrDefault("DB_PASSWORD", "");

        Properties propiedades = new Properties();
        propiedades.setProperty("user", usuario);
        propiedades.setProperty("password", clave);
        propiedades.setProperty("connectTimeout", "5");
        propiedades.setProperty("socketTimeout", "15");
        propiedades.setProperty("loginTimeout", "5");

        return DriverManager.getConnection(url, propiedades);
    }
}