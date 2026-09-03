package com.gastroflow.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

    private ConexionBD() {
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
}
