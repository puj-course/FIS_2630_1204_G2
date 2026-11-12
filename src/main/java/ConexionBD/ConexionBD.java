package ConexionBD;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto unico de conexion a PostgreSQL.
 *
 * Los datos de conexion no estan escritos en el codigo: se leen, en este orden,
 * de las variables de entorno y de un archivo de configuracion local que no se
 * sube al repositorio.
 *
 * 1. Variables de entorno, las mismas que usa psql:
 *        PGHOST  PGPORT  PGDATABASE  PGUSER  PGPASSWORD
 *
 * 2. El archivo conf/database.properties, con estas claves:
 *        db.host  db.port  db.name  db.user  db.password
 *
 * Si falta la contrasena en los dos sitios, la conexion falla con un mensaje
 * que explica que hay que configurar. Se incluye conf/database.properties.example
 * como plantilla: copielo a conf/database.properties y escriba su contrasena.
 */
public final class ConexionBD {

    private static final Logger LOG = Logger.getLogger(ConexionBD.class.getName());

    private static final String ARCHIVO_CONFIGURACION = "conf/database.properties";

    private static final Properties CONFIGURACION = cargarConfiguracion();

    private ConexionBD() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Devuelve una conexion abierta a la base de datos del sistema.
     *
     * @throws SQLException si falla la conexion o si no hay contrasena configurada
     */
    public static Connection getConnection() throws SQLException {
        String usuario = valor("PGUSER", "db.user", "postgres");
        String contrasena = valor("PGPASSWORD", "db.password", null);

        if (contrasena == null) {
            throw new SQLException(
                    "No hay contraseña de base de datos configurada. "
                            + "Defina la variable de entorno PGPASSWORD o cree el archivo "
                            + ARCHIVO_CONFIGURACION + " a partir de "
                            + ARCHIVO_CONFIGURACION + ".example");
        }

        return DriverManager.getConnection(url(), usuario, contrasena);
    }

    /** Arma la URL de JDBC con el host, el puerto y el nombre de la base. */
    private static String url() {
        return "jdbc:postgresql://"
                + valor("PGHOST", "db.host", "localhost") + ":"
                + valor("PGPORT", "db.port", "5432") + "/"
                + valor("PGDATABASE", "db.name", "gastroflow");
    }

    /**
     * Busca un dato primero en el entorno y despues en el archivo de
     * configuracion. Si no esta en ninguno de los dos, devuelve el valor por
     * defecto, que puede ser nulo.
     */
    private static String valor(String variableDeEntorno, String clave, String porDefecto) {
        String delEntorno = System.getenv(variableDeEntorno);

        if (delEntorno != null && !delEntorno.isBlank()) {
            return delEntorno;
        }

        String delArchivo = CONFIGURACION.getProperty(clave);

        if (delArchivo != null && !delArchivo.isBlank()) {
            return delArchivo;
        }

        return porDefecto;
    }

    /**
     * Lee conf/database.properties si existe. Si no existe, se sigue adelante
     * con lo que haya en el entorno.
     */
    private static Properties cargarConfiguracion() {
        Properties propiedades = new Properties();
        Path ruta = Path.of(ARCHIVO_CONFIGURACION);

        if (Files.isReadable(ruta)) {
            try (InputStream entrada = Files.newInputStream(ruta)) {
                propiedades.load(entrada);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "No se pudo leer " + ARCHIVO_CONFIGURACION, e);
            }
        }

        return propiedades;
    }
}
