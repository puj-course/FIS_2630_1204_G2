package support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Datos de prueba compartidos para los módulos que trabajan sobre mesas.
 *
 * Estos tres métodos estaban escritos dos veces, palabra por palabra, en
 * MesaModuloFuncionalTest y en PedidoModuloFuncionalTest. Eran el bloque
 * duplicado más grande del proyecto. Al quedar en un solo sitio, las dos
 * clases los comparten y cualquier corrección se hace una vez.
 */
public final class DatosDeMesas {

    private DatosDeMesas() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Inserta los estados de mesa que las pruebas necesitan, solo si no están.
     *
     * Es idempotente a propósito: se puede llamar en cada clase de prueba sin
     * preocuparse de si otra ya los creó.
     */
    public static void asegurarEstados(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO estados_mesa (codigo_estado, descripcion) " +
                        "SELECT ?, ? WHERE NOT EXISTS " +
                        "  (SELECT 1 FROM estados_mesa WHERE UPPER(codigo_estado) = ?)")) {

            for (String[] estado : new String[][]{
                    {"LIBRE", "Libre"}, {"OCUPADA", "Ocupada"}, {"RESERVADA", "Reservada"}}) {
                stmt.setString(1, estado[0]);
                stmt.setString(2, estado[1]);
                stmt.setString(3, estado[0]);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Devuelve el identificador de la zona indicada, creándola si no existe.
     *
     * @param nombreZona nombre con el que cada clase de prueba marca sus datos
     */
    public static int crearZona(Connection conn, String nombreZona) throws SQLException {
        try (PreparedStatement buscar = conn.prepareStatement(
                "SELECT id_zona FROM zonas WHERE nombre_zona = ?")) {
            buscar.setString(1, nombreZona);
            try (ResultSet rs = buscar.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_zona");
                }
            }
        }

        try (PreparedStatement insertar = conn.prepareStatement(
                "INSERT INTO zonas (nombre_zona) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            insertar.setString(1, nombreZona);
            insertar.executeUpdate();

            try (ResultSet llaves = insertar.getGeneratedKeys()) {
                assertTrue(llaves.next(), "no se pudo crear la zona de prueba");
                return llaves.getInt(1);
            }
        }
    }

    /**
     * Borra la mesa con ese número si existía y la vuelve a crear libre.
     *
     * Se borra primero para que cada ejecución arranque igual, sin arrastrar
     * el estado que dejó la anterior.
     */
    public static int crearMesa(Connection conn, int idZona, int numeroMesa, int capacidad)
            throws SQLException {

        try (PreparedStatement borrar = conn.prepareStatement(
                "DELETE FROM mesas WHERE numero_mesa = ?")) {
            borrar.setInt(1, numeroMesa);
            borrar.executeUpdate();
        }

        try (PreparedStatement insertar = conn.prepareStatement(
                "INSERT INTO mesas (numero_mesa, codigo_mesa, capacidad, id_zona, " +
                "                   id_estado_mesa, is_active) " +
                "VALUES (?, ?, ?, ?, (SELECT id_estado_mesa FROM estados_mesa " +
                "                     WHERE UPPER(codigo_estado) = 'LIBRE'), 1)",
                Statement.RETURN_GENERATED_KEYS)) {

            insertar.setInt(1, numeroMesa);
            insertar.setString(2, String.format("P-%04d", numeroMesa));
            insertar.setInt(3, capacidad);
            insertar.setInt(4, idZona);
            insertar.executeUpdate();

            try (ResultSet llaves = insertar.getGeneratedKeys()) {
                assertTrue(llaves.next(), "no se pudo crear la mesa de prueba " + numeroMesa);
                return llaves.getInt(1);
            }
        }
    }
}
