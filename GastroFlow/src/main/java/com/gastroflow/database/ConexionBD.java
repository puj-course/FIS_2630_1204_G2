package com.gastroflow.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionBD {

    private static final String URL = System.getenv().getOrDefault(
            "GASTROFLOW_DB_URL",
            "jdbc:postgresql://localhost:5432/gastroflow"
    );

    private static final String USUARIO = System.getenv().getOrDefault(
            "GASTROFLOW_DB_USER",
            "postgres"
    );

    private static final String PASSWORD = System.getenv().getOrDefault(
            "GASTROFLOW_DB_PASSWORD",
            "postgres"
    );

    /** Segundos que se espera a que la base responda antes de rendirse. */
    private static final String TIEMPO_CONEXION = "5";
    private static final String TIEMPO_CONSULTA = "15";

    private ConexionBD() {
    }

    public static Connection conectar() throws SQLException {
        Properties propiedades = new Properties();
        propiedades.setProperty("user", USUARIO);
        propiedades.setProperty("password", PASSWORD);

        // Sin estos limites, si el servidor esta caido o la red se cae, la
        // aplicacion se queda esperando el timeout del sistema operativo:
        // varios minutos con la ventana congelada.
        propiedades.setProperty("connectTimeout", TIEMPO_CONEXION);
        propiedades.setProperty("socketTimeout", TIEMPO_CONSULTA);
        propiedades.setProperty("loginTimeout", TIEMPO_CONEXION);

        return DriverManager.getConnection(URL, propiedades);
    }
}
